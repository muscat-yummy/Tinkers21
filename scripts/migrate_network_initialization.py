#!/usr/bin/env python3
"""Register Mantle and TiC payloads during the NeoForge mod event phase."""
from pathlib import Path

root = Path(__file__).resolve().parents[1]
updates = {
    root / "mantle-port/src/main/java/slimeknights/mantle/Mantle.java": (
        ("    MantleRecipes.init(bus);",
         "    MantleRecipes.init(bus);\n    MantleNetwork.registerPackets();\n    bus.addListener(MantleNetwork.INSTANCE::registerPayloads);"),
        ("    MantleNetwork.registerPackets();\n    MantleCommand.init();",
         "    MantleCommand.init();"),
    ),
    root / "src/main/java/slimeknights/tconstruct/TConstruct.java": (
        ("    TinkerNetwork.setup();",
         "    TinkerNetwork.setup();\n    bus.addListener(TinkerNetwork.getInstance()::registerPayloads);"),
    ),
}
for path, replacements in updates.items():
    source = path.read_text(encoding="utf-8")
    for old, new in replacements:
        if source.count(old) != 1:
            raise SystemExit(f"expected one match in {path}: {old!r}")
        source = source.replace(old, new)
    path.write_text(source, encoding="utf-8", newline="")
