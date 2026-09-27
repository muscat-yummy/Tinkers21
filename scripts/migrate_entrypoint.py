#!/usr/bin/env python3
"""Adapt the imported TiC entrypoint to NeoForge constructor injection.

This intentionally leaves Mantle registration and data generation for separate
migration work. It aborts if upstream has changed so the patch can be reviewed.
"""
from pathlib import Path

target = Path("src/main/java/slimeknights/tconstruct/TConstruct.java")
source = target.read_text(encoding="utf-8")

changes = [
    (
        "import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;\n",
        "import net.neoforged.fml.common.EventBusSubscriber;\n",
    ),
    (
        "@Mod.EventBusSubscriber(bus = Mod.EventBusSubscriber.Bus.MOD)",
        "@EventBusSubscriber(modid = TConstruct.MOD_ID, bus = EventBusSubscriber.Bus.MOD)",
    ),
    (
        "public TConstruct() {",
        "public TConstruct(IEventBus bus) {",
    ),
    (
        "    IEventBus bus = FMLJavaModLoadingContext.get().getModEventBus();\n",
        "",
    ),
    (
        "return new ResourceLocation(MOD_ID, name);",
        "return ResourceLocation.fromNamespaceAndPath(MOD_ID, name);",
    ),
    (
        "import net.minecraftforge.data.event.GatherDataEvent;",
        "import net.neoforged.neoforge.data.event.GatherDataEvent;",
    ),
    (
        "import net.minecraftforge.common.data.ExistingFileHelper;",
        "import net.neoforged.neoforge.common.data.ExistingFileHelper;",
    ),
]
for before, after in changes:
    if source.count(before) != 1:
        raise SystemExit(f"expected exactly one occurrence of {before.strip()!r}")
    source = source.replace(before, after)

target.write_text(source, encoding="utf-8", newline="")
print(f"adapted {target}: {len(changes)} targeted changes")
