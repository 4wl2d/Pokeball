"""Source-reader regressions and independent skill-distribution checks."""

from __future__ import annotations

import builtins
from pathlib import Path
import shutil
import subprocess
import sys
import tempfile
import unittest
from unittest.mock import patch


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "scripts"))

from build_skills import BuildError, SkillBuilder, core_index, generate
from check_skills import check, check_links, check_package, frontmatter, verify_sources
from skill_context import ContextError, markdown_index, section


SKILL_NAMES = (
    "pokeball",
    "pokeball-async",
    "pokeball-binding",
    "pokeball-composition",
    "pokeball-review",
)


def copy_repository(destination: Path) -> None:
    """Copy public generation inputs and outputs, without Git or governance."""
    destination.mkdir(parents=True)
    for directory in ("spec", "docs/agents", "skill-src", "skills"):
        shutil.copytree(ROOT / directory, destination / directory, symlinks=True)
    for filename in ("LICENSE", "NOTICE.md"):
        shutil.copyfile(ROOT / filename, destination / filename)
    for relative in ("README.md", "docs/ru/README.md", "docs/SKILLS.md", "docs/SKILL-AUTHORING.md"):
        document = destination / relative
        document.parent.mkdir(parents=True, exist_ok=True)
        document.write_text("# Skills\n", encoding="utf-8")


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
        self.root = Path(self.temporary.name) / "example"
        (self.root / "references").mkdir(parents=True)
        (self.root / "references/guide.md").write_text(
            '# Guide\n## Pure `Decision`\n<a id="stable"></a>\nText.\n',
            encoding="utf-8",
        )
        self.document = self.root / "SKILL.md"

    def tearDown(self):
        self.temporary.cleanup()

    def test_package_local_inline_reference_and_html_links(self):
        self.document.write_text(
            '[source](references/guide.md#pure-decision)\n'
            '[definition][guide]\n\n[guide]: references/guide.md#stable\n'
            '<a href="references/guide.md#stable">Definition</a>\n'
            '```md\n[illustrative](absent.md)\n```\n',
            encoding="utf-8",
        )
        self.assertEqual(check_links(self.root, self.document), [])

    def test_missing_file_and_fragment_are_rejected(self):
        for target in ("references/missing.md", "references/guide.md#unknown"):
            with self.subTest(target=target):
                self.document.write_text(f"[source]({target})\n", encoding="utf-8")
                self.assertTrue(check_links(self.root, self.document))

    def test_existing_outside_targets_are_rejected_in_all_link_forms(self):
        outside = self.root.parent / "outside.md"
        outside.write_text("# Outside\n", encoding="utf-8")
        for target in ("../outside.md", "%2e%2e/outside.md", outside.as_posix()):
            for template in ("[source]({target})", "[source][x]\n\n[x]: {target}", '<a href="{target}">source</a>'):
                with self.subTest(target=target, template=template):
                    self.document.write_text(template.format(target=target), encoding="utf-8")
                    self.assertTrue(check_links(self.root, self.document))

    def test_nested_reference_can_link_within_its_package(self):
        reference = self.root / "references/route.md"
        reference.write_text("[entry](../SKILL.md#example)\n[source](guide.md#stable)\n", encoding="utf-8")
        self.document.write_text("# Example\n[route](references/route.md)\n", encoding="utf-8")
        self.assertEqual(check_links(self.root, reference), [])
        self.assertEqual(check_links(self.root, self.document), [])


class StandalonePackageTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="standalone skill ")
        self.root = Path(self.temporary.name)

    def tearDown(self):
        self.temporary.cleanup()

    def copy_skill(self, name: str, case: str = "installed") -> Path:
        package = self.root / case / name
        shutil.copytree(ROOT / "skills" / name, package, symlinks=True)
        return package

    def test_each_skill_works_alone_after_source_and_siblings_are_removed(self):
        with tempfile.TemporaryDirectory(prefix="source checkout ") as checkout:
            source = Path(checkout) / "repository"
            copy_repository(source)
            packages = []
            for name in SKILL_NAMES:
                package = self.root / name / ".agents/skills" / name
                shutil.copytree(source / "skills" / name, package, symlinks=True)
                packages.append(package)
        self.assertFalse(source.exists())
        original_path_open = Path.open
        original_open = builtins.open
        for package in packages:
            with self.subTest(skill=package.name):
                self.assertEqual(list(package.parent.iterdir()), [package])
                boundary = package.resolve()

                def require_local(path):
                    candidate = Path(path).resolve()
                    self.assertTrue(candidate.is_relative_to(boundary), f"Read outside package: {candidate}")
                    return candidate

                def open_path(path, *args, **kwargs):
                    return original_path_open(require_local(path), *args, **kwargs)

                def open_file(path, *args, **kwargs):
                    return original_open(require_local(path), *args, **kwargs)

                with patch.object(Path, "open", open_path), patch("builtins.open", open_file), patch(
                    "socket.create_connection", side_effect=AssertionError("Package validation used the network")
                ):
                    self.assertEqual(check_package(package), [])

    def test_each_skill_rejects_a_missing_bundled_reference(self):
        for name in SKILL_NAMES:
            with self.subTest(skill=name):
                package = self.copy_skill(name)
                self.assertEqual(check_package(package), [])
                reference = next(iter(sorted((package / "references").rglob("*.md"))))
                reference.unlink()
                self.assertTrue(check_package(package), reference.relative_to(package))

    def test_each_skill_rejects_an_existing_reference_outside_the_package(self):
        outside = self.root / "outside.md"
        outside.write_text("# Outside\n", encoding="utf-8")
        for name in SKILL_NAMES:
            with self.subTest(skill=name):
                package = self.copy_skill(name)
                escape = package.parent / "outside.md"
                shutil.copyfile(outside, escape)
                entrypoint = package / "SKILL.md"
                entrypoint.write_bytes(entrypoint.read_bytes() + b"\n[External dependency](../outside.md)\n")
                self.assertTrue(check_package(package))

    def test_symlinks_are_rejected_even_when_targets_are_inside_the_package(self):
        for target_kind in ("internal-file", "external-file", "internal-directory", "broken"):
            with self.subTest(target_kind=target_kind):
                package = self.copy_skill("pokeball", target_kind)
                if target_kind == "internal-file":
                    target = package / "SKILL.md"
                elif target_kind == "internal-directory":
                    target = package / "references"
                else:
                    target = self.root / "outside.md"
                    if target_kind == "external-file":
                        target.write_text("# Outside\n", encoding="utf-8")
                    elif target.exists():
                        target.unlink()
                (package / "linked-reference").symlink_to(target, target_is_directory=target.is_dir())
                self.assertTrue(check_package(package))

    def test_missing_entrypoint_and_excessive_entrypoint_are_rejected(self):
        for problem in ("missing", "excessive"):
            with self.subTest(problem=problem):
                package = self.copy_skill("pokeball", problem)
                entrypoint = package / "SKILL.md"
                if problem == "missing":
                    entrypoint.unlink()
                else:
                    entrypoint.write_bytes(entrypoint.read_bytes() + b"\n" + b"irrelevant " * 801)
                self.assertTrue(check_package(package))

    def test_bundled_license_and_notice_are_required(self):
        for filename in ("LICENSE", "NOTICE.md"):
            with self.subTest(filename=filename):
                package = self.copy_skill("pokeball", filename)
                (package / filename).unlink()
                self.assertTrue(check_package(package))

    def test_frontmatter_duplicate_fields_and_multiline_values_fail(self):
        for source in ('---\nname: example\nname: other\n---\n', '---\nname: example\ndescription: >\n  hidden instructions\n---\n'):
            with self.subTest(source=source), self.assertRaises(ValueError):
                frontmatter(source)


class RepositoryTests(unittest.TestCase):
    def setUp(self):
        self.temporary = tempfile.TemporaryDirectory(prefix="skill repository ")
        self.root = Path(self.temporary.name) / "checkout"
        copy_repository(self.root)

    def tearDown(self):
        self.temporary.cleanup()

    def test_valid_repository_passes_without_git_or_governance(self):
        self.assertFalse((self.root / ".git").exists())
        self.assertFalse((self.root / "governance").exists())
        errors, counts = check(self.root)
        self.assertEqual(errors, [])
        self.assertEqual(counts["skills"], len(SKILL_NAMES))

    def test_core_and_agent_byte_changes_fail_without_rewriting_metadata(self):
        self.assertEqual(verify_sources(self.root), [])
        baseline = self.root / "docs/agents/BASELINE.md"
        original_baseline = baseline.read_bytes()
        for relative in ("spec/core/07-state-and-authority.md", "docs/agents/DESIGN-RUNBOOK.md"):
            with self.subTest(source=relative):
                source = self.root / relative
                original = source.read_bytes()
                source.write_bytes(original + b"\n")
                self.assertTrue(verify_sources(self.root))
                self.assertEqual(baseline.read_bytes(), original_baseline)
                source.write_bytes(original)
        self.assertEqual(verify_sources(self.root), [])

    def test_missing_manifest_source_fails_closed(self):
        (self.root / "spec/core/07-state-and-authority.md").unlink()
        self.assertTrue(verify_sources(self.root))

    def test_tampered_generated_reference_fails_repository_check(self):
        reference = next(iter(sorted((self.root / "skills/pokeball/references").rglob("*.md"))))
        reference.write_bytes(reference.read_bytes() + b"\n")
        self.assertEqual(verify_sources(self.root), [])
        self.assertEqual(check_package(self.root / "skills/pokeball"), [])
        errors, _ = check(self.root)
        self.assertTrue(errors)

    def test_missing_and_unexpected_generated_files_are_rejected(self):
        for problem in ("missing", "unexpected"):
            with self.subTest(problem=problem):
                if problem == "missing":
                    path = self.root / "skills/pokeball/SKILL.md"
                    original = path.read_bytes()
                    path.unlink()
                else:
                    path = self.root / "skills/pokeball/unexpected.md"
                    path.write_text("# Extra generated output\n", encoding="utf-8")
                errors, _ = check(self.root)
                self.assertTrue(errors)
                if problem == "missing":
                    path.write_bytes(original)
                else:
                    path.unlink()

    def test_advertised_runtime_index_ranges_resolve_complete_local_destinations(self):
        sections, indexes = core_index(self.root)
        builder = SkillBuilder(self.root, "pokeball-binding", sections, indexes)
        builder.dependencies("8.12", "Cause/context: §§3.3–3.4; identity: §§3.5–3.6.\n")
        self.assertEqual(builder.core_numbers, {"3.3", "3.4", "3.5", "3.6"})
        with self.assertRaises(BuildError):
            builder.dependencies("8.12", "Required mapping: §§3.5–3.99.\n")

    def test_clean_generation_is_deterministic_and_matches_published_bytes(self):
        expected, reports = generate(self.root)
        repeated, repeated_reports = generate(self.root)
        self.assertTrue(expected)
        self.assertEqual(expected, repeated)
        self.assertEqual(reports, repeated_reports)
        self.assertEqual({path.parts[1] for path in expected}, set(SKILL_NAMES))
        actual = {
            path.relative_to(self.root): path.read_bytes()
            for path in (self.root / "skills").rglob("*")
            if path.is_file()
        }
        self.assertEqual(expected, actual)

    def test_generation_depends_on_inputs_not_existing_output_or_checkout_path(self):
        expected, _ = generate(self.root)
        for name in SKILL_NAMES:
            shutil.rmtree(self.root / "skills" / name)
        without_outputs, _ = generate(self.root)
        self.assertEqual(expected, without_outputs)
        relocated = self.root.parent / "relocated source with spaces"
        self.root.rename(relocated)
        after_relocation, _ = generate(relocated)
        self.assertEqual(expected, after_relocation)


if __name__ == "__main__":
    unittest.main()
