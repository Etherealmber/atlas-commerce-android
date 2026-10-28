#!/usr/bin/env python3
"""AST-based static audit of Compose display strings in the Atlas Android app.

Scope: direct display arguments to Text, accessibility content descriptions on
Icon/Image, selected Atlas text components, Toast/onToast/Snackbar calls, and
literal ViewModel/platform notice emissions under ui/. Dynamic values, server-
provided messages, IDs, URLs, symbols, pure numbers, and computed text with no
auditable literal are reported separately/excluded. Direct display literals
must use atlasText; ViewModel notice literals must have an exact catalog key or
a registered atlasNotice template. The audit also checks Android XML resource
key parity. This is source coverage, not runtime screenshot or locale QA.

Run: python3 -m pip install -r scripts/requirements-localization-audit.txt
     python3 scripts/audit_ui_localization.py
"""
from __future__ import annotations

from collections import Counter
from pathlib import Path
import re
import sys
import xml.etree.ElementTree as ET

from tree_sitter import Language, Parser
import tree_sitter_kotlin

ROOT = Path(__file__).resolve().parents[1]
UI_ROOT = ROOT / "app/src/main/java/ma/etherealmber/atlascommerce/ui"
WIDGETS = {
    "Text", "AtlasOutlinedButton", "AtlasChip", "AtlasInfoRow",
    "AtlasSectionHeader", "AtlasErrorCard", "AtlasEmptyState",
    "AtlasStatTile", "AtlasTextField", "AtlasTopBar", "AtlasButton",
    "AtlasLoading", "AtlasOfflineBanner", "AtlasBullet", "SectionTitle", "LegalPage",
    "Icon", "Image", "onToast", "showSnackbar", "Toast.makeText",
}
POSITIONAL_COPY_ARGUMENTS = {
    "onToast": {0},
    "showSnackbar": {0},
    "Toast.makeText": {1},
}
COMPONENT_LOOKUP_WIDGETS = {
    "AtlasOutlinedButton", "AtlasChip", "AtlasInfoRow", "AtlasSectionHeader",
    "AtlasErrorCard", "AtlasEmptyState", "AtlasStatTile", "AtlasTextField",
    "AtlasTopBar", "AtlasButton", "AtlasLoading", "AtlasOfflineBanner",
    "AtlasBullet", "SectionTitle", "LegalPage",
}
TEXT_PARAMETERS = {
    "text", "title", "subtitle", "label", "message", "placeholder",
    "supportingText", "actionLabel", "contentDescription",
}
INVARIANT_LITERALS = {
    "Atlas Commerce", "CDN 1", "MIT", "contact@exemple.com",
    "https://.../login-qr?t=…", "https://votre-domaine.exemple.com",
    "•", "✓", "  ▾",
}
NON_COPY_TOKENS = {
    "open", "pending", "paid", "closed", "completed", "month", "year", "custom", "partial", "overdue",
    "summary", "image/*", "android.settings.passwords_and_accounts_settings",
    "payé", "venir", "payer", "sku", "build", "github", "com",
}
LOCALIZED_NOTICE_TEMPLATES = {
    "{value} · ajouté automatiquement au panier",
    "{value} · ajouté au panier",
    "Produit « {value} » créé au catalogue.",
    "Produit « {value} » créé et demande validée.",
    "Ticket {value} enregistré.",
    "Notification envoyée à : {value}.",
    "Notification « {value} » marquée comme traitée.",
    "Demande « {value} » validée.",
    "Demande « {value} » refusée.",
    "{value} · stock {value}",
    "Connecté en tant que {value}.",
    "Compte @{value} oublié.",
    "Serveur sélectionné : {value}",
    "{value} révoqué.",
    "Données locales supprimées.{value} Les données de l'entreprise sur le serveur n'ont pas été modifiées.",
    "{value} rapport(s) PDF temporaire(s) supprimé(s).",
    "Seules les {value} premières images ont été retenues (max 3).",
    "Ticket GitHub créé et ouvert{value}.",
    "Ticket créé{value}, mais aucun navigateur n'est disponible.",
    "Rapport reçu par Atlas{value}. Le formulaire GitHub prérempli est ouvert; envoyez-le pour créer l'issue.",
    "Rapport reçu · {value}. Impossible d'ouvrir le suivi sur cet appareil.",
}

catalog_source = (UI_ROOT / "i18n/AtlasLocalization.kt").read_text(encoding="utf-8")
catalog_entries = re.findall(r'^\s*"((?:[^"\\]|\\.)*)"\s+to\s+\(', catalog_source, re.M)
CATALOG_KEYS = set(catalog_entries)
catalog_duplicates = sorted(key for key, count in Counter(catalog_entries).items() if count > 1)
parser = Parser(Language(tree_sitter_kotlin.language()))
counts: Counter[str] = Counter()
remaining: list[tuple[str, str, int]] = []
notice_counts: Counter[str] = Counter()
remaining_notices: list[tuple[str, str, int]] = []
unmapped_lookup: set[tuple[str, str, int]] = set()
parse_errors: list[str] = []

for path in sorted(UI_ROOT.rglob("*.kt")):
    data = path.read_bytes()
    tree = parser.parse(data)
    relative = path.relative_to(ROOT).as_posix()
    if tree.root_node.has_error:
        parse_errors.append(relative)

    def text(node) -> str:
        return data[node.start_byte:node.end_byte].decode("utf-8", "replace")

    def is_valid_atlas_text_call(call) -> bool:
        args = next((child for child in call.children if child.type == "value_arguments"), None)
        if args is None:
            return False
        values = [child for child in args.children if child.type == "value_argument"]
        if len(values) == 3:
            return True
        if len(values) == 1 and len(values[0].children) == 1 and values[0].children[0].type == "string_literal":
            raw = text(values[0].children[0])
            value = raw[1:-1] if raw.startswith('"') and raw.endswith('"') else raw
            if "$" not in value and value in CATALOG_KEYS:
                return True
            if "$" not in value and re.search(r"[A-Za-zÀ-ÖØ-öø-ÿ]{2,}", value):
                unmapped_lookup.add((value, relative, call.start_point[0] + 1))
        return False

    def process_notice_emission(call, callee: str) -> None:
        args = next((child for child in call.children if child.type == "value_arguments"), None)
        values = [child for child in args.children if child.type == "value_argument"] if args else []
        if not values:
            return
        literals = []
        def collect_notice_literals(current):
            if current.type == "string_literal":
                literals.append(current)
            for child in current.children:
                collect_notice_literals(child)
        collect_notice_literals(values[0])
        for literal in literals:
            raw = text(literal)
            value = raw[1:-1] if raw.startswith('"') and raw.endswith('"') else raw
            words = re.sub(r"\$\{[^{}]*\}|\$[A-Za-z_]\w*", " ", value)
            tokens = set(re.findall(r"[A-Za-zÀ-ÖØ-öø-ÿ]{2,}", words.casefold()))
            if value.casefold().strip() in NON_COPY_TOKENS or (tokens and tokens.issubset(NON_COPY_TOKENS)) or not tokens:
                continue
            normalized = re.sub(r"\$\{(?:[^{}]|\{[^{}]*\})*\}|\$[A-Za-z_]\w*", "{value}", value)
            localized = value in CATALOG_KEYS or normalized in LOCALIZED_NOTICE_TEMPLATES
            is_dynamic = "$" in value
            notice_counts["dynamic" if is_dynamic else "static"] += 1
            notice_counts["localized" if localized else "untranslated"] += 1
            if not localized:
                remaining_notices.append((raw, relative, literal.start_point[0] + 1))

    def visit(node) -> None:
        if node.type == "call_expression" and node.children:
            callee = text(node.children[0])
            is_notice_emitter = (
                callee in {"onDone", "signOut"}
                or bool(re.fullmatch(r"_?(?:messages|notices)\.(?:tryEmit|emit)", callee))
            )
            if is_notice_emitter:
                process_notice_emission(node, callee)
        if node.type == "call_expression" and node.children and text(node.children[0]) == "atlasText":
            is_valid_atlas_text_call(node)
        if node.type == "call_expression" and node.children and text(node.children[0]) in WIDGETS:
            widget = text(node.children[0])
            args = next((child for child in node.children if child.type == "value_arguments"), None)
            if args:
                value_args = [child for child in args.children if child.type == "value_argument"]
                for argument_index, arg in enumerate(value_args):
                    source = text(arg)
                    named = re.match(r"\s*([A-Za-z_]\w*)\s*=\s*", source)
                    parameter = named.group(1) if named else None
                    if parameter and parameter not in TEXT_PARAMETERS:
                        continue
                    if parameter is None:
                        if widget in POSITIONAL_COPY_ARGUMENTS:
                            if argument_index not in POSITIONAL_COPY_ARGUMENTS[widget]:
                                continue
                        elif widget not in {"Text", "AtlasBullet", "SectionTitle"}:
                            continue

                    literals: list[tuple[object, bool]] = []
                    def collect(current, inside_localizer=False):
                        localizer = inside_localizer or (
                            current.type == "call_expression"
                            and current.children
                            and text(current.children[0]) == "atlasText"
                            and is_valid_atlas_text_call(current)
                        )
                        if current.type == "string_literal":
                            literals.append((current, localizer))
                        for child in current.children:
                            collect(child, localizer)
                    collect(arg)

                    copy_literals = []
                    invariant_only = True
                    for literal, localized in literals:
                        raw = text(literal)
                        value = raw[1:-1] if raw.startswith('"') and raw.endswith('"') else raw
                        if value in INVARIANT_LITERALS:
                            continue
                        # Remove Kotlin interpolation before deciding whether the literal
                        # contains translatable copy; variable-only values don't count.
                        words = re.sub(r"\$\{[^{}]*\}|\$[A-Za-z_]\w*", " ", value)
                        tokens = set(re.findall(r"[A-Za-zÀ-ÖØ-öø-ÿ]{2,}", words.casefold()))
                        if (
                            value.casefold().strip() in NON_COPY_TOKENS
                            or (tokens and tokens.issubset(NON_COPY_TOKENS))
                            or not tokens
                        ):
                            continue
                        invariant_only = False
                        copy_literals.append((literal, localized, value))

                    if not copy_literals:
                        counts["noncopy_or_invariant_excluded"] += 1
                        continue
                    if widget in COMPONENT_LOOKUP_WIDGETS:
                        static_joined = "".join(value for _, _, value in copy_literals if "$" not in value)
                        joined_matches_catalog = static_joined in CATALOG_KEYS
                        copy_literals = [
                            (literal, localized or joined_matches_catalog or ("$" not in value and value in CATALOG_KEYS), value)
                            for literal, localized, value in copy_literals
                        ]
                    if widget in {"onToast", "Toast.makeText"}:
                        copy_literals = [
                            (
                                literal,
                                localized or value in CATALOG_KEYS or re.sub(r"\$\{(?:[^{}]|\{[^{}]*\})*\}|\$[A-Za-z_]\w*", "{value}", value) in LOCALIZED_NOTICE_TEMPLATES,
                                value,
                            )
                            for literal, localized, value in copy_literals
                        ]
                    interpolated = any("$" in value for _, _, value in copy_literals)
                    all_localized = all(localized for _, localized, _ in copy_literals)
                    if all_localized:
                        counts["localized_dynamic" if interpolated else "localized_static"] += 1
                    else:
                        category = "untranslated_dynamic" if interpolated else "untranslated_static"
                        counts[category] += 1
                        literal = next(l for l, localized, _ in copy_literals if not localized)
                        remaining.append((text(literal), relative, literal.start_point[0] + 1))
        for child in node.children:
            visit(child)

    visit(tree.root_node)

static_total = counts["localized_static"] + counts["untranslated_static"]
dynamic_total = counts["localized_dynamic"] + counts["untranslated_dynamic"]
total = static_total + dynamic_total
localized = counts["localized_static"] + counts["localized_dynamic"]
percentage = 100.0 * localized / total if total else 100.0
static_percentage = 100.0 * counts["localized_static"] / static_total if static_total else 100.0
print(f"Localized scored direct-display call sites: {localized}/{total} ({percentage:.1f}%)")
print(f"  Static literals: {counts['localized_static']}/{static_total} ({static_percentage:.1f}%)")
print(f"  Interpolated human-copy templates: {counts['localized_dynamic']}/{dynamic_total}")
print(f"Untranslated scored call sites: {counts['untranslated_static'] + counts['untranslated_dynamic']}")
notice_total = notice_counts["localized"] + notice_counts["untranslated"]
print(f"ViewModel/platform notice literals localized: {notice_counts['localized']}/{notice_total}")
all_copy_total = total + notice_total
all_copy_localized = localized + notice_counts["localized"]
all_copy_percentage = 100.0 * all_copy_localized / all_copy_total if all_copy_total else 100.0
print(f"Combined source-audited UI copy: {all_copy_localized}/{all_copy_total} ({all_copy_percentage:.1f}%)")
print(f"Computed/no-copy/invariant arguments excluded: {counts['noncopy_or_invariant_excluded']}")
print(f"French catalog: {len(CATALOG_KEYS)} unique keys; duplicate keys: {len(catalog_duplicates)}")
if catalog_duplicates:
    print(f"Duplicate catalog keys: {catalog_duplicates}")

resource_root = ROOT / "app/src/main/res"
def resource_keys(directory: Path) -> set[str]:
    xml = ET.parse(directory / "strings.xml").getroot()
    return {element.attrib["name"] for element in xml.findall("string")}

french_resource_keys = resource_keys(resource_root / "values")
resource_mismatches = []
for locale in ("values-en", "values-ar"):
    locale_keys = resource_keys(resource_root / locale)
    missing = sorted(french_resource_keys - locale_keys)
    extra = sorted(locale_keys - french_resource_keys)
    if missing or extra:
        resource_mismatches.append((locale, missing, extra))
print(f"Android XML string resources localized: {len(french_resource_keys)}/{len(french_resource_keys)} keys in en and ar")
if resource_mismatches:
    for locale, missing, extra in resource_mismatches:
        print(f"Resource key mismatch in {locale}: missing={missing}; extra={extra}")
if parse_errors:
    print("Kotlin parse errors:")
    for file in parse_errors:
        print(f"  {file}")
if unmapped_lookup:
    print("One-argument atlasText lookups missing catalog entries:")
    for value, file, line in sorted(unmapped_lookup):
        print(f"  {file}:{line}: {value}")
if remaining:
    print("Remaining untranslated literals:")
    for value, file, line in remaining:
        print(f"  {file}:{line}: {value}")
if remaining_notices:
    print("Remaining untranslated ViewModel/platform notice literals:")
    for value, file, line in remaining_notices:
        print(f"  {file}:{line}: {value}")

if (
    parse_errors
    or unmapped_lookup
    or remaining
    or remaining_notices
    or resource_mismatches
    or catalog_duplicates
):
    sys.exit(1)
