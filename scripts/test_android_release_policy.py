import unittest

from android_release_policy import is_newer_stable, parse_release_version, supported_series_for


class AndroidReleasePolicyTest(unittest.TestCase):
    def test_stable_patch_releases_stay_on_the_same_minor_line(self):
        self.assertEqual("1.9.x", supported_series_for("1.9.0"))
        self.assertEqual("1.9.x", supported_series_for("v1.9.9"))
        self.assertEqual("1.9.0", parse_release_version("1.9.9").supported_floor)
        self.assertEqual("2.0.x", supported_series_for("2.0.0"))

    def test_prereleases_are_valid_but_never_define_or_advance_support(self):
        version = parse_release_version("v2.0.0-rc.1+build.5")
        self.assertFalse(version.is_stable)
        self.assertIsNone(supported_series_for("2.0.0-rc.1"))
        self.assertFalse(is_newer_stable("2.0.0-rc.1", "1.9.9"))

    def test_release_baseline_advances_only_to_newer_stable_versions(self):
        self.assertTrue(is_newer_stable("1.9.0", "1.7.0"))
        self.assertTrue(is_newer_stable("2.0.0", "1.9.9"))
        self.assertFalse(is_newer_stable("1.9.9", "1.9.9"))
        self.assertFalse(is_newer_stable("1.8.9", "1.9.0"))

    def test_rejects_malformed_or_non_semver_values(self):
        for value in ("1.9", "01.9.0", "1.9.00", "1.9.0-01", "1.9.0-"):
            with self.subTest(value=value), self.assertRaises(ValueError):
                parse_release_version(value)


if __name__ == "__main__":
    unittest.main()
