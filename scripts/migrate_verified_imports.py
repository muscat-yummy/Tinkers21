#!/usr/bin/env python3
"""Apply only verified Forge-to-NeoForge import moves to imported TiC sources.

This does not rewrite registries, capabilities, networking, event semantics,
or any API whose replacement is not a one-to-one package move.
"""
from __future__ import annotations

import argparse
from collections import Counter
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1] / "src" / "main" / "java"
IMPORTS = {
    "net.minecraftforge.api.distmarker.Dist": "net.neoforged.api.distmarker.Dist",
    "net.minecraftforge.eventbus.api.IEventBus": "net.neoforged.bus.api.IEventBus",
    "net.minecraftforge.eventbus.api.SubscribeEvent": "net.neoforged.bus.api.SubscribeEvent",
    "net.minecraftforge.eventbus.api.Event": "net.neoforged.bus.api.Event",
    "net.minecraftforge.eventbus.api.EventPriority": "net.neoforged.bus.api.EventPriority",
    "net.minecraftforge.fml.common.Mod": "net.neoforged.fml.common.Mod",
    "net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent": "net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent",
    "net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent": "net.neoforged.fml.event.lifecycle.FMLClientSetupEvent",
    "net.minecraftforge.fml.event.lifecycle.FMLDedicatedServerSetupEvent": "net.neoforged.fml.event.lifecycle.FMLDedicatedServerSetupEvent",
    "net.minecraftforge.common.MinecraftForge": "net.neoforged.neoforge.common.NeoForge",
}
IMPORT_RE = re.compile(r"^(?P<prefix>\s*import\s+(?:static\s+)?)(?P<name>[\w.]+)(?P<suffix>\s*;)", re.MULTILINE)


def migrate(source: str, counts: Counter[str]) -> str:
    has_minecraft_forge = bool(re.search(r"^\s*import\s+net\.minecraftforge\.common\.MinecraftForge\s*;", source, re.MULTILINE))

    def replace_import(match: re.Match[str]) -> str:
        old = match.group("name")
        new = IMPORTS.get(old)
        if new is None:
            return match.group(0)
        counts[old] += 1
        return match.group("prefix") + new + match.group("suffix")

    result = IMPORT_RE.sub(replace_import, source)
    if has_minecraft_forge:
        result, replacements = re.subn(r"\bMinecraftForge\.EVENT_BUS\b", "NeoForge.EVENT_BUS", result)
        counts["MinecraftForge.EVENT_BUS"] += replacements
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--check", action="store_true", help="report pending changes without writing")
    args = parser.parse_args()
    if not ROOT.is_dir():
        parser.error(f"missing source tree: {ROOT}")

    counts: Counter[str] = Counter()
    changed = 0
    for path in sorted(ROOT.rglob("*.java")):
        original = path.read_text(encoding="utf-8")
        updated = migrate(original, counts)
        if updated != original:
            changed += 1
            if not args.check:
                path.write_text(updated, encoding="utf-8", newline="")
    print(f"{'pending' if args.check else 'updated'} files: {changed}")
    for name, count in sorted(counts.items()):
        print(f"{name}: {count}")


if __name__ == "__main__":
    main()
