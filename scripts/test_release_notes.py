import unittest

from release_notes import format_in_app_notes, group_release_commits


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

    def test_empty_or_noise_only_history_does_not_claim_a_fix(self):
        groups = group_release_commits(
            "a" * 40 + "\x1fchore(release): set version 1.7.0\x1f\x1e"
        )
        self.assertTrue(all(not items for items in groups.values()))
        self.assertEqual("No categorized commit details are available.", format_in_app_notes(groups))


if __name__ == "__main__":
    unittest.main()
