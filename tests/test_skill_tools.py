"""Behavioral fixtures for source selection and the bounded publication checks."""

from __future__ import annotations

from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from check_skills import check, check_links, frontmatter, verify_sources
from skill_context import ContextError, local_path, markdown_index, section


class ReaderTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name) / "checkout"
        self.root.mkdir()
        self.source = self.root / "guide.md"

    def tearDown(self):
        self.temporary.cleanup()

    def write(self, source: str):
        self.source.write_bytes(source.encode("utf-8"))

    def test_exact_section_contains_children_and_fenced_headings(self):
        selected = "## Decision\r\nPure rule.\r\n### Input\r\n```python\r\n# not a heading\r\n## neither is this\r\n```\r\n~~~~\r\n## also code\r\n~~~\r\n~~~~\r\nEnd.\r\n"
        self.write("# Guide\r\nBefore.\r\n" + selected + "## Resources\r\nLater.\r\n")
        _, content = section(self.root, "guide.md#decision")
        self.assertEqual(content, selected)

    def test_duplicate_headings_have_distinct_sections(self):
        self.write("# Guide\n## Same\nFirst.\n## Same\nSecond.\n## Same-1\nThird.\n")
        self.assertEqual(section(self.root, "guide.md#same-1")[1], "## Same\nSecond.\n")
        self.assertEqual(section(self.root, "guide.md#same-1-1")[1], "## Same-1\nThird.\n")

    def test_explicit_anchors_choose_the_correct_section(self):
        self.write('# Guide\n<a id="stable"></a>\n\n## Changing title\nText.\n## Table\n| <a id="row"></a>item | value |\n## End\n')
        self.assertEqual(section(self.root, "guide.md#stable")[1], "## Changing title\nText.\n")
        self.assertIn("## Table\n", section(self.root, "guide.md#row")[1])

    def test_frontmatter_and_fence_anchors_are_not_navigation(self):
        self.write('---\nname: example\n---\n# Guide\n```md\n<a id="fake"></a>\n## Fake\n```\n## Real\nText\n')
        headings, anchors = markdown_index(self.source.read_text())
        self.assertEqual([heading.fragment for heading in headings], ["guide", "real"])
        self.assertNotIn("fake", anchors)

    def test_setext_and_formatted_unicode_headings(self):
        self.write("# Guide\n## 5. What's `State`?\nMeaning.\nCafé &amp; [Read](read.md)\n---\nLookup.\n")
        self.assertEqual(section(self.root, "guide.md#5-whats-state")[1], "## 5. What's `State`?\nMeaning.\n")
        self.assertIn("Lookup.", section(self.root, "guide.md#caf%C3%A9--read")[1])

    def test_missing_sources_and_fragments_fail_without_full_document_fallback(self):
        self.write("# Guide\nText.\n")
        for request in ("missing.md#guide", "guide.md#unknown", "guide.md", "guide.md#"):
            with self.subTest(request=request), self.assertRaises(ContextError):
                section(self.root, request)

    def test_root_escape_and_outside_symlink_fail(self):
        outside = self.root.parent / "outside.md"
        outside.write_text("# Outside\n")
        (self.root / "escape.md").symlink_to(outside)
        for request in ("../outside.md#outside", "%2e%2e/outside.md#outside", "escape.md#outside", str(outside) + "#outside"):
            with self.subTest(request=request), self.assertRaises(ContextError):
                section(self.root, request)

    def install_reader(self) -> Path:
        scripts = self.root / "scripts"
        scripts.mkdir()
        script = scripts / "skill_context.py"
        shutil.copyfile(ROOT / "scripts/skill_context.py", script)
        return script

    def test_relocated_symlinked_script_uses_physical_checkout_from_any_cwd(self):
        self.write("# Guide\n## Decision\nOwned rule.\n## End\n")
        script = self.install_reader()
        alias = self.root.parent / "reader.py"
        alias.symlink_to(script)
        process = subprocess.run([sys.executable, str(alias), "guide.md#decision"], cwd=self.root.parent, text=True, capture_output=True)
        self.assertEqual(process.returncode, 0, process.stderr)
        self.assertEqual(process.stdout, "## Decision\nOwned rule.\n")

    def test_batch_failure_emits_no_partial_context(self):
        self.write("# Guide\n## Decision\nOwned rule.\n")
        process = subprocess.run([sys.executable, str(self.install_reader()), "guide.md#decision", "guide.md#typo"], text=True, capture_output=True)
        self.assertNotEqual(process.returncode, 0)
        self.assertEqual(process.stdout, "")
        self.assertIn("unknown fragment", process.stderr)


class LinkTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        (self.root / "skills/example").mkdir(parents=True)
        (self.root / "spec").mkdir()
        (self.root / "spec/core.md").write_text('# Core\n## Pure `Decision`\n<a id="stable"></a>\nText.\n')
        self.document = self.root / "skills/example/SKILL.md"

    def tearDown(self):
        self.temporary.cleanup()

    def test_whole_checkout_relative_and_reference_links(self):
        self.document.write_text('[source](../../spec/core.md#pure-decision)\n[definition][core]\n\n[core]: ../../spec/core.md#stable\n```md\n[illustrative](absent.md)\n```\n')
        self.assertEqual(check_links(self.root, self.document), [])

    def test_link_failures_are_actionable(self):
        self.document.write_text('[missing](../../spec/missing.md)\n[typo](../../spec/core.md#typo)\n[escape](../../../outside.md)\n')
        errors = check_links(self.root, self.document)
        self.assertEqual(len(errors), 3)
        self.assertTrue(any("missing.md" in error for error in errors))
        self.assertTrue(any("unknown fragment" in error for error in errors))
        self.assertTrue(any("leaves the checkout" in error for error in errors))

    def test_local_reference_symlink_is_allowed_but_escape_is_not(self):
        (self.root / "skills/reference.md").symlink_to(self.root / "spec/core.md")
        self.document.write_text('[source](../reference.md#pure-decision)\n')
        self.assertEqual(check_links(self.root, self.document), [])
        self.assertEqual(local_path(self.root, "skills/reference.md"), (self.root / "spec/core.md").resolve())


class ValidatorTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory()
        self.root = Path(self.temporary.name)
        shutil.copytree(ROOT / "spec", self.root / "spec")
        shutil.copytree(ROOT / "docs/agents", self.root / "docs/agents")
        for document in ("SKILLS.md", "SKILL-AUTHORING.md"):
            (self.root / "docs" / document).write_text("# Skills\n[Source](../spec/pokeball-architecture-core.md)\n")
        (self.root / "skills/example").mkdir(parents=True)
        self.skill = self.root / "skills/example/SKILL.md"
        self.skill.write_text('---\nname: example\ndescription: Change a local feature with its existing Pokeball binding.\n---\n# Example\n[Decision](../../spec/core/08-decision-and-acceptance.md#85-atomic-decision-acceptance)\n')

    def tearDown(self):
        self.temporary.cleanup()

    def test_valid_whole_checkout_passes_after_relocation(self):
        errors, counts = check(self.root)
        self.assertEqual(errors, [])
        self.assertEqual(counts["skills"], 1)

    def test_core_and_package_byte_changes_fail_without_rewriting_metadata(self):
        self.assertEqual(verify_sources(self.root), [])
        chapter = self.root / "spec/core/07-state-and-authority.md"
        chapter.write_bytes(chapter.read_bytes() + b"\n")
        self.assertIn("Core digest differs from BASELINE.md", verify_sources(self.root))
        runbook = self.root / "docs/agents/DESIGN-RUNBOOK.md"
        runbook.write_bytes(runbook.read_bytes() + b"\n")
        self.assertIn("Agent Pack digest differs from BASELINE.md", verify_sources(self.root))

    def test_missing_manifest_source_fails_closed(self):
        (self.root / "spec/core/07-state-and-authority.md").rename(self.root / "unavailable.md")
        self.assertTrue(any("file not found" in error for error in verify_sources(self.root)))

    def test_frontmatter_duplicate_fields_and_multiline_values_fail(self):
        for source in ('---\nname: example\nname: other\n---\n', '---\nname: example\ndescription: >\n  hidden instructions\n---\n'):
            with self.subTest(source=source), self.assertRaises(ValueError):
                frontmatter(source)

    def test_word_budget_and_definition_copy_are_rejected(self):
        self.skill.write_text(self.skill.read_text() + "\n" + "irrelevant " * 801 + "\n| `PKB-AR-DEC-002` | copied definition |\n")
        errors, _ = check(self.root)
        self.assertTrue(any("exceeds 800 words" in error for error in errors))
        self.assertTrue(any("definition row/heading" in error for error in errors))

    def test_source_records_cannot_become_skill_authority(self):
        self.skill.write_text(self.skill.read_text() + '\n<!-- pkb:pba-source:start id="PBA-07" -->\n')
        errors, _ = check(self.root)
        self.assertTrue(any("copied normative/generated source record" in error for error in errors))

    def test_long_source_prose_is_linked_instead_of_copied(self):
        original = (self.root / "spec/core/reference/21-adoption.md").read_text()
        paragraph = next(block for block in original.split("\n\n") if len(block.split()) >= 40)
        self.skill.write_text(self.skill.read_text() + "\n" + paragraph + "\n")
        errors, _ = check(self.root)
        self.assertTrue(any("copied source paragraph" in error for error in errors))


if __name__ == "__main__":
    unittest.main()
