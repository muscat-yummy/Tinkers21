#!/usr/bin/env python3
"""Move Mantle's mod entrypoint to NeoForge constructor injection."""
from pathlib import Path

path = Path(__file__).resolve().parents[1] / "mantle-port/src/main/java/slimeknights/mantle/Mantle.java"
source = path.read_text(encoding="utf-8")
replacements = {
    "import net.minecraftforge.fml.ModLoadingContext;": "import net.neoforged.fml.ModContainer;",
    "import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;\n": "",
    "  public Mantle() {": "  public Mantle(IEventBus bus, ModContainer container) {",
    "ModLoadingContext.get().registerConfig": "container.registerConfig",
    "    IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();\n": "",
}
for old, new in replacements.items():
    if old not in source:
        raise SystemExit(f"expected Mantle entrypoint fragment missing: {old!r}")
    source = source.replace(old, new)
path.write_text(source, encoding="utf-8", newline="")
