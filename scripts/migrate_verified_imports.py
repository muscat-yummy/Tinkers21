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

PROJECT_ROOT = Path(__file__).resolve().parents[1]
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
    "net.minecraftforge.registries.DeferredRegister": "net.neoforged.neoforge.registries.DeferredRegister",
    "net.minecraftforge.common.data.DatapackBuiltinEntriesProvider": "net.neoforged.neoforge.common.data.DatapackBuiltinEntriesProvider",
    "net.minecraftforge.common.data.ExistingFileHelper": "net.neoforged.neoforge.common.data.ExistingFileHelper",
    "net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent": "net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent",
    "net.minecraftforge.common.crafting.CraftingHelper": "net.neoforged.neoforge.common.crafting.CraftingHelper",
    "net.minecraftforge.data.event.GatherDataEvent": "net.neoforged.neoforge.data.event.GatherDataEvent",
    "net.minecraftforge.registries.RegisterEvent": "net.neoforged.neoforge.registries.RegisterEvent",
    "net.minecraftforge.event.entity.player.PlayerInteractEvent": "net.neoforged.neoforge.event.entity.player.PlayerInteractEvent",
    "net.minecraftforge.fml.loading.FMLEnvironment": "net.neoforged.fml.loading.FMLEnvironment",
    "net.minecraftforge.fml.config.ModConfig.Type": "net.neoforged.fml.config.ModConfig.Type",
    "net.minecraftforge.fml.common.Mod.EventBusSubscriber": "net.neoforged.fml.common.EventBusSubscriber",
    "net.minecraftforge.fml.common.Mod.EventBusSubscriber.Bus": "net.neoforged.fml.common.EventBusSubscriber.Bus",
    "net.minecraftforge.fluids.FluidStack": "net.neoforged.neoforge.fluids.FluidStack",
    "net.minecraftforge.fluids.FluidType": "net.neoforged.neoforge.fluids.FluidType",
    "net.minecraftforge.fluids.capability.IFluidHandler": "net.neoforged.neoforge.fluids.capability.IFluidHandler",
    "net.minecraftforge.fluids.capability.IFluidHandlerItem": "net.neoforged.neoforge.fluids.capability.IFluidHandlerItem",
    "net.minecraftforge.common.crafting.conditions.ICondition": "net.neoforged.neoforge.common.conditions.ICondition",
    "net.minecraftforge.common.data.BlockTagsProvider": "net.neoforged.neoforge.common.data.BlockTagsProvider",
    "net.minecraftforge.common.util.INBTSerializable": "net.neoforged.neoforge.common.util.INBTSerializable",
    "net.minecraftforge.common.ForgeConfigSpec": "net.neoforged.neoforge.common.ModConfigSpec",
    "net.minecraftforge.network.NetworkDirection": "slimeknights.mantle.network.NetworkDirection",
    "net.minecraftforge.network.NetworkEvent": "slimeknights.mantle.network.NetworkEvent",
    "net.minecraftforge.network.NetworkEvent.Context": "slimeknights.mantle.network.NetworkEvent.Context",
    "net.minecraftforge.network.PacketDistributor": "slimeknights.mantle.network.PacketDistributor",
    "net.minecraftforge.common.util.FakePlayer": "net.neoforged.neoforge.common.util.FakePlayer",
    "net.minecraftforge.common.MinecraftForge": "net.neoforged.neoforge.common.NeoForge",
}
IMPORT_RE = re.compile(r"^(?P<prefix>\s*import\s+(?:static\s+)?)(?P<name>[\w.]+)(?P<suffix>\s*;)", re.MULTILINE)


def migrate(source: str, counts: Counter[str]) -> str:
    has_old_config_spec = "import net.minecraftforge.common.ForgeConfigSpec" in source
    has_minecraft_forge = bool(re.search(r"^\s*import\s+net\.minecraftforge\.common\.MinecraftForge\s*;", source, re.MULTILINE))

    def replace_import(match: re.Match[str]) -> str:
        old = match.group("name")
        new = IMPORTS.get(old)
        if new is None and old.startswith("net.minecraftforge.network.PacketDistributor."):
            new = old.replace("net.minecraftforge.network.PacketDistributor.", "slimeknights.mantle.network.PacketDistributor.", 1)
        if new is None and old.startswith("net.minecraftforge.common.ForgeConfigSpec."):
            new = old.replace("net.minecraftforge.common.ForgeConfigSpec.", "net.neoforged.neoforge.common.ModConfigSpec.", 1)
        if new is None:
            return match.group(0)
        counts[old] += 1
        return match.group("prefix") + new + match.group("suffix")

    result = IMPORT_RE.sub(replace_import, source)
    if has_old_config_spec or "import net.neoforged.neoforge.common.ModConfigSpec" in result:
        result, replacements = re.subn(r"\bForgeConfigSpec\b", "ModConfigSpec", result)
        counts["ForgeConfigSpec usage"] += replacements
    if has_minecraft_forge:
        result, replacements = re.subn(r"\bMinecraftForge\.EVENT_BUS\b", "NeoForge.EVENT_BUS", result)
        counts["MinecraftForge.EVENT_BUS"] += replacements
    return result


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--root", choices=("tconstruct", "mantle-port"), default="tconstruct")
    parser.add_argument("--check", action="store_true", help="report pending changes without writing")
    args = parser.parse_args()
    root = PROJECT_ROOT / ("mantle-port/src/main/java" if args.root == "mantle-port" else "src/main/java")
    if not root.is_dir():
        parser.error(f"missing source tree: {root}")

    counts: Counter[str] = Counter()
    changed = 0
    for path in sorted(root.rglob("*.java")):
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
