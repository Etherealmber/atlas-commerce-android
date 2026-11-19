import unittest

from release_notes import format_github_release_body, format_in_app_notes, group_release_commits, should_generate_github_release_notes


class ReleaseNotesTest(unittest.TestCase):
    def test_groups_conventional_changes_and_links_exact_commit_issue_refs(self):
        raw = "".join(
            [
                "a" * 40 + "\x1ffeat(products): Add product image editing (#12)\x1fAllow managers to update product photos.\x1e",
                "b" * 40 + "\x1ffix(agent): Prevent unrequested theme changes\x1fReject questions and explain requests.\x1e",
                "c" * 40 + "\x1fImprove work status view #34\x1f\x1e",
                "d" * 40 + "\x1fperf: Reduce startup work\x1fCache role policy.\x1e",
                "e" * 40 + "\x1fchore: update tooling\x1f\x1e",
            ]
        )
        groups = group_release_commits(raw)

        self.assertEqual(1, len(groups["Features"]))
        self.assertEqual(1, len(groups["Fixes"]))
        self.assertEqual(1, len(groups["Issues"]))
        self.assertEqual(1, len(groups["Improvements"]))
        feature = groups["Features"][0]
        self.assertIn("Allow managers", feature.summary)
        self.assertIn("[\"aaaaaaa\"](https://github.com/example/repo/commit/" + "a" * 40 + ")", feature.markdown("https://github.com/example/repo"))
        self.assertIn("[#12](https://github.com/example/repo/issues/12)", feature.markdown("https://github.com/example/repo"))

    def test_release_note_in_app_summary_is_categorized_bounded_and_source_linked(self):
        groups = group_release_commits(
            "f" * 40 + "\x1ffeat: Add supported version policy\x1f\x1e"
        )
        note = format_in_app_notes(groups, limit=80)
        self.assertTrue(note.startswith("Features:"))
        self.assertLessEqual(len(note), 80)
        self.assertIn("ffffff", note)

    def test_github_generation_only_uses_new_or_matching_remote_tags(self):
        build_commit = "a" * 40
        self.assertTrue(should_generate_github_release_notes(None, build_commit))
        self.assertTrue(should_generate_github_release_notes(build_commit.upper(), build_commit))
        self.assertFalse(should_generate_github_release_notes("b" * 40, build_commit))

    def test_github_release_body_uses_signed_apk_template_and_preserves_github_notes(self):
        generated = (
            "## What's Changed\n\n"
            "* Improve tablet navigation by @loxdpsy in [#102](https://github.com/example/repo/pull/102)\n\n"
            "**Full Changelog**: [v1.7.0...v1.8.0](https://github.com/example/repo/compare/v1.7.0...v1.8.0)"
        )
        body = format_github_release_body(
            tag="v1.8.0",
            version_code=180,
            size_mb=30.38,
            sha256="a" * 64,
            primary_url="https://cdn.example/latest.apk",
            github_url="https://github.com/example/repo/releases/download/v1.8.0/atlas-commerce.apk",
            generated_notes=generated,
        )
        self.assertTrue(body.startswith("## ✨ Atlas Commerce `v1.8.0` (build `180`)\n\n### 📦 Paquet signé (`APK`)"))
        self.assertIn("- **Version :** `v1.8.0` (`versionCode = 180`)\n\n- **Taille :** `30.38 Mo`", body)
        self.assertIn("- **Téléchargement :** [latest.apk](https://cdn.example/latest.apk)", body)
        self.assertIn("## What's Changed\n\n* Improve tablet navigation by @loxdpsy in [#102](https://github.com/example/repo/pull/102)", body)
        self.assertIn("**Full Changelog**: [v1.7.0...v1.8.0](https://github.com/example/repo/compare/v1.7.0...v1.8.0)", body)

    def test_empty_or_noise_only_history_does_not_claim_a_fix(self):
        groups = group_release_commits(
            "a" * 40 + "\x1fchore(release): set version 1.7.0\x1f\x1e"
        )
        self.assertTrue(all(not items for items in groups.values()))
        self.assertEqual("No categorized commit details are available.", format_in_app_notes(groups))


if __name__ == "__main__":
    unittest.main()
