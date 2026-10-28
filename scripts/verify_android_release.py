import json, os, time, urllib.error, urllib.request
from urllib.parse import urlsplit

live_cdn = None
cdn_base = (os.environ.get("INPUT_CDN_URL", "").strip() or os.environ.get("DEFAULT_CDN_VAR", "https://cdn1.afmarbre.com/app-updates/android")).rstrip("/")
expected_code = int(os.environ.get("EXPECTED_VERSION_CODE", "0") or "0")
expected_name = os.environ.get("EXPECTED_VERSION_NAME", "").lstrip("vV")
fetch_error = None
cdn_cache_control = ""
github_manifest = None
github_fetch_error = None
repo = os.environ.get("GITHUB_REPOSITORY", "")
try:
    github_url = f"https://github.com/{repo}/releases/download/v{expected_name}/latest.json?verify={os.environ.get('VERIFY_NONCE', 'manual')}"
    github_req = urllib.request.Request(
        github_url,
        headers={"User-Agent": "Mozilla/5.0 AtlasVerify/1.0", "Cache-Control": "no-cache"},
    )
    with urllib.request.urlopen(github_req, timeout=15) as r:
        github_manifest = json.loads(r.read().decode("utf-8"))
except Exception as e:
    github_fetch_error = str(e)
    github_manifest = {"fetchError": github_fetch_error}

for attempt in range(4):
    try:
        req = urllib.request.Request(
            f"{cdn_base}/latest.json?verify={os.environ.get('VERIFY_NONCE', 'manual')}-{attempt}",
            headers={"User-Agent": "Mozilla/5.0 AtlasVerify/1.0", "Cache-Control": "no-cache"},
        )
        with urllib.request.urlopen(req, timeout=10) as r:
            live_cdn = json.loads(r.read().decode("utf-8"))
            cdn_cache_control = r.headers.get("Cache-Control", "")
        fetch_error = None
    except Exception as e:
        fetch_error = str(e)
        live_cdn = {"fetchError": fetch_error}

    try:
        observed_code = int(live_cdn.get("versionCode", live_cdn.get("version_code", 0)) or 0)
        observed_name = str(live_cdn.get("versionName", live_cdn.get("version_name", "")) or "").lstrip("vV")
    except (AttributeError, TypeError, ValueError):
        observed_code, observed_name = 0, ""
    if observed_code == expected_code and (not observed_name or observed_name == expected_name):
        break
    if attempt < 3 and (
        os.environ.get("WORKER_ACCEPTED") == "true"
        or os.environ.get("CLOUDFLARE_API_PUBLISHED") == "true"
    ):
        time.sleep(5)

live_code = 0
live_name = ""
policy_fields = {}
if isinstance(live_cdn, dict):
    try:
        live_code = int(live_cdn.get("versionCode", live_cdn.get("version_code", 0)) or 0)
    except (TypeError, ValueError):
        live_code = 0
    live_name = str(live_cdn.get("versionName", live_cdn.get("version_name", "")) or "")
    for key in (
        "minimumVersionEnabled", "minimum_version_enabled",
        "minimumSupportedVersionCode", "minimum_supported_version_code",
        "minimumVersionCode", "minimum_version_code",
        "minimumVersionName", "minimum_version_name",
        "minimumVersionRequired", "minimum_version_required",
        "minimumVersionMandatory", "minimum_version_mandatory", "mandatory",
    ):
        if key in live_cdn:
            policy_fields[key] = live_cdn[key]

public_match = (
    live_code == expected_code and expected_code > 0
    and (not live_name or live_name.lstrip("vV") == expected_name)
)
github_code = 0
github_name = ""
if isinstance(github_manifest, dict):
    try:
        github_code = int(github_manifest.get("versionCode", github_manifest.get("version_code", 0)) or 0)
    except (TypeError, ValueError):
        github_code = 0
    github_name = str(github_manifest.get("versionName", github_manifest.get("version_name", "")) or "")
github_match = (
    github_code == expected_code and expected_code > 0
    and (not github_name or github_name.lstrip("vV") == expected_name)
)
minimum_policy_present = any(
    key in policy_fields for key in (
        "minimumSupportedVersionCode", "minimum_supported_version_code",
        "minimumVersionCode", "minimum_version_code",
    )
)
cache_control_normalized = cdn_cache_control.lower().replace(" ", "")
manifest_cache_control_ok = all(
    directive in cache_control_normalized
    for directive in ("no-cache", "max-age=0", "must-revalidate")
)

apk_url = ""
apk_status_code = None
apk_cache_control = ""
apk_probe_error = None
live_apk = live_cdn.get("cdn1Url") or live_cdn.get("apkUrl") if isinstance(live_cdn, dict) else None
if isinstance(live_apk, str):
    apk_url = live_apk.strip()
if apk_url:
    try:
        apk_req = urllib.request.Request(
            apk_url,
            method="HEAD",
            headers={"User-Agent": "Mozilla/5.0 AtlasVerify/1.0", "Cache-Control": "no-cache"},
        )
        with urllib.request.urlopen(apk_req, timeout=15) as r:
            apk_status_code = r.getcode()
            apk_cache_control = r.headers.get("Cache-Control", "")
    except urllib.error.HTTPError as e:
        apk_status_code = e.code
        apk_cache_control = e.headers.get("Cache-Control", "") if e.headers else ""
        apk_probe_error = str(e)
    except Exception as e:
        apk_probe_error = str(e)
cdn_apk_exists = apk_status_code is not None and 200 <= apk_status_code < 300
apk_cache_normalized = apk_cache_control.lower().replace(" ", "")
apk_path = urlsplit(apk_url).path.lower()
versioned_cdn_apk = apk_url.startswith(cdn_base + "/") and "-v" in apk_path and apk_path.endswith(".apk")
versioned_apk_cache_control_ok = (
    not versioned_cdn_apk
    or ("immutable" in apk_cache_normalized and "max-age=31536000" in apk_cache_normalized)
)

with open("signed-apk/publish-status.json", "w", encoding="utf-8") as fh:
    json.dump({
        "workerAccepted": os.environ.get("WORKER_ACCEPTED") == "true",
        "cloudflareApiPublished": os.environ.get("CLOUDFLARE_API_PUBLISHED") == "true",
        "cdnPublished": os.environ.get("CDN_PUBLISHED") == "true",
        "policySafeDirectApiUsed": os.environ.get("POLICY_SAFE_DIRECT_API_USED") == "true",
        "githubReleaseMatchesRelease": github_match,
        "githubReleaseLatestJson": github_manifest,
        "cdnMethod": os.environ.get("CDN_METHOD", ""),
        "policyAwareWorkerUsed": os.environ.get("POLICY_AWARE_WORKER_USED") == "true",
        "publicCdnMatchesRelease": public_match,
        "minimumVersionPolicyFieldsPresent": minimum_policy_present,
        "publicCdnPolicyFields": policy_fields,
        "manifestCacheControl": cdn_cache_control,
        "manifestCacheControlSafe": manifest_cache_control_ok,
        "publicCdnApkUrl": apk_url,
        "publicCdnApkExists": cdn_apk_exists,
        "publicCdnApkStatusCode": apk_status_code,
        "publicCdnApkProbeError": apk_probe_error,
        "versionedApkCacheControl": apk_cache_control,
        "versionedApkCacheControlImmutable": versioned_apk_cache_control_ok,
        "cfWafBlocked": os.environ.get("CF_WAF_BLOCKED") == "true",
        "liveCdnLatestJson": live_cdn,
    }, fh, ensure_ascii=False, indent=2)
summary_path = os.environ.get("GITHUB_STEP_SUMMARY")
if summary_path:
    with open(summary_path, "a", encoding="utf-8") as summary:
        gh_result = "matches" if github_match else "does not match"
        result = "matches" if public_match else "does not match"
        summary.write(f"\n### Independent release metadata verification\n- GitHub Release latest.json {gh_result} the release ({github_name or 'unknown'} / {github_code}).\n")
        summary.write(f"- Public CDN latest.json {result} the release ({live_name or 'unknown'} / {live_code}).\n")
        summary.write(f"- Policy-aware Worker accepted ingestion: `{os.environ.get('WORKER_ACCEPTED') == 'true'}`\n")
        summary.write(f"- Policy-safe Cloudflare R2 API upload completed: `{os.environ.get('CLOUDFLARE_API_PUBLISHED') == 'true'}`\n")
        summary.write(f"- Minimum-version policy fields present: `{minimum_policy_present}`\n")
        summary.write(f"- Manifest cache headers safe: `{manifest_cache_control_ok}` (`{cdn_cache_control or 'missing'}`)\n")
        summary.write(f"- Referenced CDN APK exists: `{cdn_apk_exists}` ({apk_status_code or 'no status'}; `{apk_url or 'no apkUrl'}`)\n")
        summary.write(f"- Versioned APK immutable cache headers: `{versioned_apk_cache_control_ok}` (`{apk_cache_control or 'missing'}`)\n")
        summary.write(f"- Policy fields observed: `{json.dumps(policy_fields, ensure_ascii=False, sort_keys=True) if policy_fields else 'none'}`\n")
if not github_match:
    print("::error::GitHub Release latest.json does not match the published release version/build.")
    raise SystemExit(1)
if os.environ.get("CDN_PUBLISHED") != "true":
    print("::error::Neither the policy-aware Worker nor the backend-policy-preserving Cloudflare R2 API completed publication; CDN publication is unconfirmed.")
    raise SystemExit(1)
if (
    not public_match
    or not minimum_policy_present
    or not manifest_cache_control_ok
    or not cdn_apk_exists
    or not versioned_apk_cache_control_ok
):
    print(
        "::error::Public CDN publication is incomplete: "
        f"version_match={public_match}, policy_fields={minimum_policy_present}, "
        f"manifest_no_cache={manifest_cache_control_ok}, apk_exists={cdn_apk_exists}, "
        f"versioned_apk_immutable={versioned_apk_cache_control_ok}."
    )
    raise SystemExit(1)
