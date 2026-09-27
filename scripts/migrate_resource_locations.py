#!/usr/bin/env python3
"""Replace simple ResourceLocation constructors with Minecraft 1.21 factories.

Only single-line constructor calls without nested parentheses are touched.
Complex expressions are intentionally left for reviewed, manual migration.
"""
from collections import Counter
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1] / "src" / "main" / "java"
CONSTRUCTOR = re.compile(r"\bnew\s+ResourceLocation\s*\(([^()\n]*)\)")


def argument_count(arguments: str) -> int:
    quoted = None
    escaped = False
    commas = 0
    for character in arguments:
        if escaped:
            escaped = False
        elif quoted is not None and character == "\\":
            escaped = True
        elif quoted is not None and character == quoted:
            quoted = None
        elif quoted is None and character in ("'", '"'):
            quoted = character
        elif quoted is None and character == ",":
            commas += 1
    if quoted is not None or not arguments.strip():
        return 0
    return commas + 1


def main() -> None:
    if not ROOT.is_dir():
        raise SystemExit(f"missing source tree: {ROOT}")
    counts: Counter[str] = Counter()
    files = 0
    for path in sorted(ROOT.rglob("*.java")):
        source = path.read_text(encoding="utf-8")

        def replace(match: re.Match[str]) -> str:
            count = argument_count(match.group(1))
            if count == 1:
                counts["parse"] += 1
                return "ResourceLocation.parse(" + match.group(1) + ")"
            if count == 2:
                counts["fromNamespaceAndPath"] += 1
                return "ResourceLocation.fromNamespaceAndPath(" + match.group(1) + ")"
            return match.group(0)

        updated = CONSTRUCTOR.sub(replace, source)
        if updated != source:
            path.write_text(updated, encoding="utf-8", newline="")
            files += 1
    print(f"updated files: {files}")
    for factory, count in sorted(counts.items()):
        print(f"{factory}: {count}")


if __name__ == "__main__":
    main()
