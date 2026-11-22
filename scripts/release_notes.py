"""Deterministic, source-linked Android release note grouping."""

from __future__ import annotations

import re
from dataclasses import dataclass

CATEGORIES = ("Features", "Improvements", "Fixes", "Issues", "Other changes")
_ISSUES = re.compile(r"(?<![A-Za-z0-9_])#(\d+)\b")
_TYPE = re.compile(r"^(feat|fix|perf|refactor|ui|ux|security|docs|chore|ci)(?:\([^)]+\))?:\s*", re.I)
_NOISE = (
    re.compile(r"^ci:\s*build log", re.I),
    re.compile(r"^ci:\s*android 14 smoke log", re.I),
    re.compile(r"^chore\(release\):\s*set version", re.I),
    re.compile(r"^update readme\.md$", re.I),
    re.compile(r"^merge (branch|pull request)", re.I),
)


@dataclass(frozen=True)
class ReleaseNoteItem:
    summary: str
    commit_hash: str
    issues: tuple[str, ...]

    def markdown(self, repository_url: str) -> str:
        commit = f'["{self.commit_hash[:7]}"]({repository_url.rstrip("/")}/commit/{self.commit_hash})'
        issues = [f"[#{number}]({repository_url.rstrip('/')}/issues/{number})" for number in self.issues]
        related = "; " + ", ".join(issues) if issues else ""
        return f"{self.summary} ({commit}{related})"

    def plain(self) -> str:
        refs = [self.commit_hash[:7], *(f"#{number}" for number in self.issues)]
        return f"{self.summary} ({', '.join(refs)})"


def _clean(text: str) -> str:
    text = re.sub(r"`([^`]+)`", r"\1", text)
    text = re.sub(r"\*\*([^*]+)\*\*", r"\1", text)
    return re.sub(r"\s+", " ", text).strip().strip("-*• ").strip()


def group_release_commits(raw_log: str) -> dict[str, list[ReleaseNoteItem]]:
    """Group `git log --format=%H%x1f%s%x1f%b%x1e` output without inventing detail."""
    groups = {category: [] for category in CATEGORIES}
    seen: set[str] = set()
    for entry in raw_log.split("\x1e"):
        if not entry.strip("\r\n ") or "\x1f" not in entry:
            continue
        fields = entry.strip("\r\n ").split("\x1f", 2)
        if len(fields) != 3:
            continue
        commit_hash, subject, body = (part.strip() for part in fields)
        if not commit_hash or commit_hash in seen or any(pattern.search(subject) for pattern in _NOISE):
            continue
        seen.add(commit_hash)
        type_match = _TYPE.match(subject)
        kind = type_match.group(1).lower() if type_match else ""
        summary = subject[type_match.end():] if type_match else subject
        summary = re.sub(r"\s*\(#\d+\)\s*$", "", summary).strip()
        if not summary or len(summary) < 6 or summary.endswith(":"):
            continue
        summary = summary[0].upper() + summary[1:]
        detail = ""
        for line in body.splitlines():
            if re.match(r"^\s*(co-authored-by|signed-off-by):", line, re.I):
                continue
            detail = _clean(line)
            if detail:
                break
        if detail and detail.lower() not in summary.lower():
            summary = f"{summary} — {detail[:220]}"
        issues = tuple(sorted(set(_ISSUES.findall(subject + "\n" + body)), key=int))
        if kind == "feat":
            category = "Features"
        elif kind in ("fix", "security"):
            category = "Fixes"
        elif kind in ("perf", "refactor", "ui", "ux", "docs"):
            category = "Improvements"
        elif issues:
            category = "Issues"
        else:
            category = "Other changes"
        groups[category].append(ReleaseNoteItem(summary, commit_hash, issues))
    return groups


def format_commit_release_notes(
    raw_log: str,
    repository_url: str,
    author_logins: dict[str, str] | None = None,
) -> str:
    """Render a chronological commit list (not GitHub's pull-request based release summary)."""
    author_logins = author_logins or {}
    groups = group_release_commits(raw_log)
    by_hash = {item.commit_hash: item for items in groups.values() for item in items}
    ordered: list[ReleaseNoteItem] = []
    seen: set[str] = set()
    for entry in raw_log.split("\x1e"):
        fields = entry.strip("\r\n ").split("\x1f", 1)
        if len(fields) != 2:
            continue
        commit_hash = fields[0].strip()
        item = by_hash.get(commit_hash)
        if item is not None and commit_hash not in seen:
            ordered.append(item)
            seen.add(commit_hash)

    lines = ["## What's Changed", ""]
    if not ordered:
        lines.append("No categorized commit details were available.")
    for item in ordered:
        short_hash = item.commit_hash[:7]
        commit_link = f'["{short_hash}"]({repository_url.rstrip("/")}/commit/{item.commit_hash})'
        lines.append(f"* {item.summary} ({commit_link})")

    contributors: list[str] = []
    for item in ordered:
        login = author_logins.get(item.commit_hash, "").strip().lstrip("@")
        if login and not login.lower().endswith("[bot]") and login not in contributors:
            contributors.append(login)
    if contributors:
        lines.extend(["", "## Contributors", "", "Thanks to " + ", ".join(f"@{login}" for login in contributors) + " for contributing to this release."])
    return "\n".join(lines).strip()


def format_in_app_notes(groups: dict[str, list[ReleaseNoteItem]], limit: int = 480) -> str:
    """Return a bounded categorized summary with commit/issue IDs for update prompts."""
    parts = [f"{category}: {items[0].plain()}" for category, items in groups.items() if items]
    note = " | ".join(parts) or "No categorized commit details are available."
    return note if len(note) <= limit else note[: limit - 1].rstrip() + "…"


def should_generate_github_release_notes(remote_tag_commit: str | None, build_commit: str) -> bool:
    """Avoid GitHub's tag-based generator when an existing tag points at different code."""
    return remote_tag_commit is None or remote_tag_commit.strip().lower() == build_commit.strip().lower()


def format_github_release_body(
    *,
    tag: str,
    version_code: int,
    size_mb: float,
    sha256: str,
    primary_url: str,
    github_url: str,
    generated_notes: str,
) -> str:
    """Wrap GitHub's autogenerated What's Changed body in Atlas's signed-APK release format."""
    changed = generated_notes.strip()
    if not changed:
        changed = "## What's Changed\n\nNo categorized commit details were available."
    elif not changed.startswith("## What's Changed"):
        changed = f"## What's Changed\n\n{changed}"

    lines = [
        f"## ✨ Atlas Commerce `{tag}` (build `{version_code}`)",
        "",
        "### 📦 Paquet signé (`APK`)",
        "",
        f"- **Version :** `{tag}` (`versionCode = {version_code}`)",
        "",
        f"- **Taille :** `{size_mb:.2f} Mo`",
        "",
        f"- **SHA-256 :** `{sha256}`",
        "",
        f"- **Téléchargement :** [latest.apk]({primary_url})",
        "",
        f"- **Miroir GitHub :** [atlas-commerce.apk]({github_url})",
        "",
        changed,
    ]
    return "\n".join(lines).strip() + "\n"
