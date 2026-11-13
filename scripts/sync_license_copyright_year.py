#!/usr/bin/env python3
"""Keep repository and bundled copyright notices on the current UTC year."""

from __future__ import annotations

import argparse
from datetime import datetime, timezone
from pathlib import Path
import re
import sys

ROOT = Path(__file__).resolve().parents[1]
MIT_NOTICE = re.compile(r"(?m)^Copyright \(c\) \d{4} Etherealmber$")
APACHE_NOTICE = re.compile(
    r"(?m)^   Copyright (?:\[yyyy\] \[name of copyright owner\]|\d{4} Etherealmber)$"
)


def update_notice(path: Path, pattern: re.Pattern[str], replacement: str) -> bool:
    text = path.read_text(encoding="utf-8")
    matches = list(pattern.finditer(text))
    if len(matches) != 1:
        raise ValueError(f"Expected one copyright notice matching {pattern.pattern!r} in {path}")
    updated = pattern.sub(replacement, text, count=1)
    if updated == text:
        return False
    path.write_text(updated, encoding="utf-8", newline="\n")
    return True


def main() -> int:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--year",
        type=int,
        default=datetime.now(timezone.utc).year,
        help="UTC year to use (defaults to the current UTC year)",
    )
    args = parser.parse_args()
    if not 1900 <= args.year <= 9999:
        parser.error("--year must be a four-digit year")

    try:
        changed = []
        if update_notice(ROOT / "LICENSE", MIT_NOTICE, f"Copyright (c) {args.year} Etherealmber"):
            changed.append("LICENSE")
        for relative_path in ("LICENSE-APACHE", "app/src/main/assets/licenses/LICENSE-APACHE.txt"):
            if update_notice(ROOT / relative_path, APACHE_NOTICE, f"   Copyright {args.year} Etherealmber"):
                changed.append(relative_path)
    except (OSError, ValueError) as error:
        print(f"license copyright sync failed: {error}", file=sys.stderr)
        return 1

    if changed:
        print("Updated copyright year in: " + ", ".join(changed))
    else:
        print(f"Copyright notices already use {args.year}; no changes needed.")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
