"""Exercise the README MCU badge rewrite against a disposable checkout."""

import json
from pathlib import Path
import shutil
import subprocess
import tempfile
import unittest


SCRIPT = Path(__file__).resolve().parents[1] / "scripts/readme-mcu-badge"
OLD = "1a34bd2" + "0" * 33
NEW = "5b3618b16fdc3825e21d5679bafd144662088ea1"


def badge(revision):
    return (f"[![MCU](https://img.shields.io/badge/mcu-{revision[:7]}-blue)]"
            f"(https://github.com/material-foundation/material-color-utilities/tree/{revision})")


class ReadmeMcuBadgeTest(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.addCleanup(self.temporary.cleanup)
        self.root = Path(self.temporary.name)
        (self.root / ".github/scripts").mkdir(parents=True)
        shutil.copy2(SCRIPT, self.root / ".github/scripts/readme-mcu-badge")
        (self.root / "gradle").mkdir()
        self.write_lock(NEW)
        self.readme = self.root / "README.md"
        self.readme.write_text(f"# Kolor\n[![Kotlin](kotlin)](kotlin)\n{badge(OLD)}\nBody\n")

    def write_lock(self, revision):
        (self.root / "gradle/mcu-upstream.lock.json").write_text(
            json.dumps({"schemaVersion": 1, "upstreamRevision": revision}, indent=2) + "\n")

    def run_script(self, *args):
        return subprocess.run(
            ["bash", str(self.root / ".github/scripts/readme-mcu-badge"), *args],
            cwd=self.root, text=True, capture_output=True,
        )

    def test_rewrites_the_badge_to_the_locked_revision(self):
        result = self.run_script()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual(f"# Kolor\n[![Kotlin](kotlin)](kotlin)\n{badge(NEW)}\nBody\n", self.readme.read_text())

    def test_matching_badge_is_left_alone(self):
        self.readme.write_text(f"{badge(NEW)}\n")
        result = self.run_script()
        self.assertEqual(0, result.returncode, result.stderr)
        self.assertEqual("", result.stdout)
        self.assertEqual(f"{badge(NEW)}\n", self.readme.read_text())

    def test_check_fails_on_a_stale_badge_without_writing(self):
        result = self.run_script("--check")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("does not match 5b3618b", result.stderr)
        self.assertIn(badge(OLD), self.readme.read_text())

    def test_check_passes_on_a_current_badge(self):
        self.readme.write_text(f"{badge(NEW)}\n")
        result = self.run_script("--check")
        self.assertEqual(0, result.returncode, result.stderr)

    def test_missing_badge_is_rejected(self):
        self.readme.write_text("# Kolor\n")
        result = self.run_script()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("found 0", result.stderr)

    def test_duplicate_badge_is_rejected(self):
        self.readme.write_text(f"{badge(OLD)}\n{badge(OLD)}\n")
        result = self.run_script()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("found 2", result.stderr)

    def test_lock_without_a_full_revision_is_rejected(self):
        self.write_lock("5b3618b")
        result = self.run_script()
        self.assertNotEqual(0, result.returncode)
        self.assertIn("No upstreamRevision", result.stderr)
        self.assertIn(badge(OLD), self.readme.read_text())

    def test_unknown_option_is_rejected(self):
        result = self.run_script("--fix")
        self.assertNotEqual(0, result.returncode)
        self.assertIn("Usage", result.stderr)

    def test_real_readme_matches_the_real_lock(self):
        root = SCRIPT.parents[2]
        result = subprocess.run(["bash", str(SCRIPT), "--check"], cwd=root, text=True, capture_output=True)
        self.assertEqual(0, result.returncode, result.stderr)


if __name__ == "__main__":
    unittest.main()
