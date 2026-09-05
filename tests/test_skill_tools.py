"""Structural regressions for independently copied, compact skills."""
from pathlib import Path
import shutil
import sys
import tempfile
import unittest
from unittest.mock import patch

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))
from check_skills import PACKAGE_FILES, SKILL_NAMES, check, check_package, verify_sources


class PackageTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="compact skill ")
        self.root = Path(self.temporary.name)
        self.package = self.root / "pokeball"
        self.package.mkdir()
        self.source = "---\nname: pokeball\ndescription: Implement a bounded change to an existing stateful feature.\n---\nMake the requested change.\n"
        for name in PACKAGE_FILES:
            (self.package / name).write_text(self.source if name == "SKILL.md" else "Legal notice.\n")

    def tearDown(self):
        self.temporary.cleanup()

    def test_valid_package_needs_no_source_checkout_or_other_files(self):
        self.assertEqual(check_package(self.package), [])
        (self.package / "NOTICE.md").write_text("Attribution: https://creativecommons.org/licenses/by/4.0/\n")
        self.assertEqual(check_package(self.package), [])

    def test_all_real_skills_work_when_copied_alone(self):
        for name in sorted(SKILL_NAMES):
            with self.subTest(skill=name):
                destination = self.root / "isolated" / name / name
                shutil.copytree(ROOT / "skills" / name, destination, symlinks=True)
                original_open = Path.open
                def local_open(path, *args, **kwargs):
                    self.assertTrue(path.resolve().is_relative_to(destination.resolve()))
                    return original_open(path, *args, **kwargs)
                with patch.object(Path, "open", local_open):
                    self.assertEqual(check_package(destination), [])
                self.assertEqual({p.name for p in destination.iterdir()}, PACKAGE_FILES)
                self.assertEqual(list(destination.parent.iterdir()), [destination])

    def test_missing_required_files_fail(self):
        for name in PACKAGE_FILES:
            with self.subTest(file=name):
                path = self.package / name
                original = path.read_bytes()
                path.unlink()
                self.assertTrue(check_package(self.package))
                path.write_bytes(original)

    def test_extra_files_and_empty_reference_directory_fail(self):
        for name in ("extra.md", "references"):
            path = self.package / name
            path.mkdir() if name == "references" else path.write_text("extra")
            self.assertTrue(check_package(self.package))
            path.rmdir() if path.is_dir() else path.unlink()

    def test_symlinked_package_or_required_file_fails_without_reading_target(self):
        alias = self.root / "alias"
        alias.symlink_to(self.package, target_is_directory=True)
        self.assertTrue(check_package(alias))
        (self.package / "SKILL.md").unlink()
        (self.package / "SKILL.md").symlink_to(self.root / "unavailable")
        self.assertTrue(check_package(self.package))

    def test_source_reading_references_and_copied_markers_fail(self):
        examples = ("[Read](../outside.md)", "[Read](https://example.com)",
                    "[Read][ref]\n[ref]: outside.md", '<a href="outside.md">Read</a>',
                    "https://example.com", "Read references/guide.md", "Read §8.5 or PBA-07.",
                    '<!-- pkb:term:start name="State" -->', "Open another SKILL.md")
        (self.root / "outside.md").write_text("Existing external reference.")
        for instruction in examples:
            with self.subTest(instruction=instruction):
                (self.package / "SKILL.md").write_text(self.source + instruction)
                self.assertTrue(check_package(self.package))

    def test_word_limit_counts_frontmatter_and_body(self):
        for total in (800, 801):
            with self.subTest(total_words=total):
                (self.package / "SKILL.md").write_text(self.source + "word " * (total - len(self.source.split())))
                self.assertEqual(bool(check_package(self.package)), total > 800)

    def test_invalid_or_duplicate_frontmatter_fails(self):
        invalid = (self.source.replace("name: pokeball", "name: other"),
                   self.source.replace("description:", "name: pokeball\ndescription:"),
                   self.source.replace("description: Implement", "description: >\n  Implement"),
                   self.source.replace("---\n", "", 1),
                   self.source.replace("description:", "unknown:"))
        for source in invalid:
            with self.subTest(source=source):
                (self.package / "SKILL.md").write_text(source)
                self.assertTrue(check_package(self.package))


class RepositoryTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        for directory in ("spec", "docs/agents", "skills"):
            shutil.copytree(ROOT / directory, self.root / directory, symlinks=True)

    def tearDown(self):
        self.temporary.cleanup()

    def test_exact_five_skill_inventory(self):
        self.assertEqual(check(self.root), ([], {"skills": 5}))
        (self.root / "skills/unexpected").mkdir()
        self.assertTrue(check(self.root)[0])
        (self.root / "skills/unexpected").rmdir()
        (self.root / "skills/pokeball").rename(self.root / "removed")
        self.assertTrue(check(self.root)[0])

    def test_core_and_agent_byte_changes_fail_without_mutating_baseline(self):
        self.assertEqual(verify_sources(self.root), [])
        baseline = (self.root / "docs/agents/BASELINE.md").read_bytes()
        for relative in ("spec/core/07-state-and-authority.md", "docs/agents/DESIGN-RUNBOOK.md"):
            with self.subTest(source=relative):
                path = self.root / relative
                original = path.read_bytes()
                path.write_bytes(original + b"\n")
                self.assertTrue(verify_sources(self.root))
                path.write_bytes(original)
        self.assertEqual((self.root / "docs/agents/BASELINE.md").read_bytes(), baseline)

    def test_missing_manifest_source_fails(self):
        (self.root / "spec/core/07-state-and-authority.md").unlink()
        self.assertTrue(verify_sources(self.root))


if __name__ == "__main__":
    unittest.main()
