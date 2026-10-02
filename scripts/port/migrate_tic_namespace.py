"""Move TiC references only when the destination class exists on the resolved 1.21.1 classpath."""
import pathlib, re, zipfile
classes = set()
for entry in pathlib.Path('/tmp/tinkers-port-classpath.txt').read_text().splitlines():
    path = pathlib.Path(entry)
    if not path.is_file() or path.suffix != '.jar':
        continue
    with zipfile.ZipFile(path) as archive:
        classes.update(name[:-6].replace('/', '.').replace('$', '.') for name in archive.namelist() if name.endswith('.class'))

renamed = {
    'net.minecraftforge.common.MinecraftForge': 'net.neoforged.neoforge.common.NeoForge',
    'net.minecraftforge.common.extensions.IForgeItem': 'net.neoforged.neoforge.common.extensions.IItemExtension',
    'net.minecraftforge.common.loot.CanToolPerformAction': 'net.neoforged.neoforge.common.loot.CanItemPerformAbility',
    'net.minecraftforge.entity.IEntityAdditionalSpawnData': 'net.neoforged.neoforge.entity.IEntityWithComplexSpawn',
    'net.minecraftforge.event.entity.SpawnPlacementRegisterEvent': 'net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent',
    'net.minecraftforge.items.wrapper.EmptyHandler': 'net.neoforged.neoforge.items.wrapper.EmptyItemHandler',
    'net.minecraftforge.registries.IForgeRegistry': 'net.minecraft.core.Registry',
    'net.minecraftforge.common.ForgeHooks': 'net.neoforged.neoforge.common.CommonHooks',
    'net.minecraftforge.common.ForgeMod': 'net.neoforged.neoforge.common.NeoForgeMod',
    'net.minecraftforge.common.ForgeI18n': 'net.neoforged.neoforge.common.NeoForgeI18n',
    'net.minecraftforge.client.ForgeHooksClient': 'net.neoforged.neoforge.client.ClientHooks',
    'net.minecraftforge.event.ForgeEventFactory': 'net.neoforged.neoforge.event.EventHooks',
    'net.minecraftforge.fluids.ForgeFlowingFluid': 'net.neoforged.neoforge.fluids.BaseFlowingFluid',
    'net.minecraftforge.common.ToolAction': 'net.neoforged.neoforge.common.ItemAbility',
    'net.minecraftforge.common.ToolActions': 'net.neoforged.neoforge.common.ItemAbilities',
    'net.minecraftforge.common.IForgeShearable': 'net.neoforged.neoforge.common.IShearable',
    'net.minecraftforge.common.extensions.IForgeBlockEntity': 'net.neoforged.neoforge.common.extensions.IBlockEntityExtension',
    'net.minecraftforge.common.world.ForgeBiomeModifiers': 'net.neoforged.neoforge.common.world.BiomeModifiers',
    'net.minecraftforge.fml.event.IModBusEvent': 'net.neoforged.fml.event.IModBusEvent',
    'net.minecraftforge.registries.RegistryObject': 'net.neoforged.neoforge.registries.DeferredHolder',
}
prefixes = [
    ('net.minecraftforge.common.crafting.conditions.', 'net.neoforged.neoforge.common.conditions.'),
    ('net.minecraftforge.eventbus.api.', 'net.neoforged.bus.api.'),
    ('net.minecraftforge.fml.', 'net.neoforged.fml.'),
    ('net.minecraftforge.', 'net.neoforged.neoforge.'),
]
pattern = re.compile(r'net\.minecraftforge(?:\.[A-Za-z_]\w*)+')
changed, unresolved = [], set()
for path in sorted(pathlib.Path('src/main/java').rglob('*.java')):
    old = path.read_text()
    text, simple_renames = old, {}
    for qualified in sorted(set(pattern.findall(old)), key=len, reverse=True):
        parts = qualified.split('.')
        replacement = None
        for end in range(len(parts), 2, -1):
            base = '.'.join(parts[:end])
            candidates = [renamed[base]] if base in renamed else []
            candidates.extend(base.replace(src, dest, 1) for src, dest in prefixes if base.startswith(src))
            target = next((candidate for candidate in candidates if candidate in classes), None)
            if target:
                replacement = target + ('.' + '.'.join(parts[end:]) if end < len(parts) else '')
                if base.split('.')[-1] != target.split('.')[-1]:
                    simple_renames[base.split('.')[-1]] = target.split('.')[-1]
                break
        if replacement:
            text = text.replace(qualified, replacement)
        else:
            unresolved.add(qualified)
    for source, target in simple_renames.items():
        text = re.sub(r'\b' + re.escape(source) + r'\b', target, text)
    if 'RegistryObject' in simple_renames:
        text = re.sub(r'\bDeferredHolder\s*<', 'DeferredHolder<?, ', text)
    verified_type_moves = {
        'net.minecraft.world.level.block.GlassBlock': 'net.minecraft.world.level.block.TransparentBlock',
        'net.minecraft.world.level.block.AbstractGlassBlock': 'net.minecraft.world.level.block.TransparentBlock',
        'net.minecraft.world.level.pathfinder.BlockPathTypes': 'net.minecraft.world.level.pathfinder.PathType',
        'net.minecraft.core.BlockSource': 'net.minecraft.core.dispenser.BlockSource',
        'net.minecraft.data.worldgen.BootstapContext': 'net.minecraft.data.worldgen.BootstrapContext',
        'net.minecraft.advancements.FrameType': 'net.minecraft.advancements.AdvancementType',
        'net.minecraft.advancements.RequirementsStrategy': 'net.minecraft.advancements.AdvancementRequirements.Strategy',
        'mezz.jei.api.forge.ForgeTypes': 'mezz.jei.api.neoforge.NeoForgeTypes',
        'net.neoforged.neoforge.event.entity.living.MobSpawnEvent.FinalizeSpawn': 'net.neoforged.neoforge.event.entity.living.FinalizeSpawnEvent',
        'net.neoforged.neoforge.event.entity.living.LivingEvent.LivingTickEvent': 'net.neoforged.neoforge.event.entity.living.LivingTickEvent',
    }
    for source, target in verified_type_moves.items():
        if source in text and target in classes:
            text = text.replace(source, target)
            old_name, new_name = source.split('.')[-1], target.split('.')[-1]
            if old_name != new_name:
                text = re.sub(r'\b' + old_name + r'\b', new_name, text)
    registry_fields = {
        'BLOCKS': 'BLOCK', 'ITEMS': 'ITEM', 'FLUIDS': 'FLUID',
        'ENTITY_TYPES': 'ENTITY_TYPE', 'MOB_EFFECTS': 'MOB_EFFECT',
        'POTIONS': 'POTION', 'SOUND_EVENTS': 'SOUND_EVENT',
        'BLOCK_ENTITY_TYPES': 'BLOCK_ENTITY_TYPE', 'MENU_TYPES': 'MENU',
        'PARTICLE_TYPES': 'PARTICLE_TYPE', 'RECIPE_TYPES': 'RECIPE_TYPE',
        'RECIPE_SERIALIZERS': 'RECIPE_SERIALIZER', 'STAT_TYPES': 'STAT_TYPE',
        'ATTRIBUTES': 'ATTRIBUTE', 'FEATURES': 'FEATURE',
        'TREE_DECORATOR_TYPES': 'TREE_DECORATOR_TYPE',
        'STRUCTURE_TYPES': 'STRUCTURE_TYPE',
        'STRUCTURE_PIECE_TYPES': 'STRUCTURE_PIECE',
        'BIOME_SOURCE_SERIALIZERS': 'BIOME_SOURCE',
        'CHUNK_GENERATOR_SERIALIZERS': 'CHUNK_GENERATOR',
    }
    for source, target in registry_fields.items():
        text = text.replace('ForgeRegistries.Keys.' + source, 'Registries.' + target)
        text = re.sub(r'\bForgeRegistries\.' + source + r'\b', 'BuiltInRegistries.' + target, text)
    for source in ('ENTITY_DATA_SERIALIZERS', 'GLOBAL_LOOT_MODIFIER_SERIALIZERS', 'BIOME_MODIFIER_SERIALIZERS'):
        text = text.replace('ForgeRegistries.Keys.' + source, 'NeoForgeRegistries.Keys.' + source)
    for cls, qualified in (
        ('BuiltInRegistries', 'net.minecraft.core.registries.BuiltInRegistries'),
        ('Registries', 'net.minecraft.core.registries.Registries'),
        ('NeoForgeRegistries', 'net.neoforged.neoforge.registries.NeoForgeRegistries'),
    ):
        if re.search(r'\b' + cls + r'\.', text) and 'import ' + qualified + ';' not in text:
            text = text.replace('\n\nimport ', '\n\nimport ' + qualified + ';\nimport ', 1)
    if 'ForgeRegistries' not in re.sub(r'import [^;]+;', '', text):
        text = re.sub(r'import net\.minecraftforge\.registries\.ForgeRegistries;\n', '', text)
    # Mantle's register wrappers retain a single-type RegistryObject view.
    # Correct only declarations made by the earlier RegistryObject -> DeferredHolder pass.
    if re.search(r'\bDeferredHolder\s*<\?\s*,', text):
        text = re.sub(r'\bDeferredHolder\s*<\?\s*,\s*', 'RegistryObject<', text)
        if 'import slimeknights.mantle.registration.deferred.RegistryObject;' not in text:
            text = text.replace('\n\nimport ', '\n\nimport slimeknights.mantle.registration.deferred.RegistryObject;\nimport ', 1)
        if 'DeferredHolder' not in re.sub(r'import [^;]+;', '', text):
            text = text.replace('import net.neoforged.neoforge.registries.DeferredHolder;\n', '')
        if 'import net.neoforged.neoforge.registries.DeferredRegister;' in text and not any(token in text for token in ('makeRegistry(', 'getEntries(', 'DeferredRegister.Items', 'DeferredRegister.Blocks')):
            text = re.sub(r'\bDeferredRegister\b', 'SynchronizedDeferredRegister', text)
            text = text.replace('import net.neoforged.neoforge.registries.SynchronizedDeferredRegister;', 'import slimeknights.mantle.registration.deferred.SynchronizedDeferredRegister;')
    # NeoForge EntityTeleportEvent itself implements ICancellableEvent.
    if '@Cancelable' in text and re.search(r'\bextends\s+(EntityTeleportEvent|ModifierTeleportEvent)\b', text):
        text = re.sub(r'^\s*@Cancelable\s*\n', '', text, flags=re.MULTILINE)
        text = text.replace('import net.minecraftforge.eventbus.api.Cancelable;\n', '')
    capability_receiver = r'\b([A-Za-z_]\w*(?:\.[A-Za-z_]\w*(?:\(\))?)*)\.getCapability\((TinkerDataCapability|PersistentDataCapability)\.CAPABILITY\)'
    text = re.sub(capability_receiver, lambda match: match[2] + '.getOptionalData(' + match[1] + ')', text)
    text = re.sub(r'\b(TinkerDataCapability|PersistentDataCapability)\.getOptionalData\(([^;\n]+?)\)\.resolve\(\)', r'\1.getOptionalData(\2)', text)
    text = re.sub(r'LazyOptional<(TinkerDataCapability\.Holder|ModDataNBT)>(\s+\w+\s*=\s*(?:TinkerDataCapability|PersistentDataCapability)\.getOptionalData\()', r'Optional<\1>\2', text)
    if re.search(r'\bOptional\s*<', text) and 'import java.util.Optional;' not in text and 'import java.util.*;' not in text:
        text = text.replace('\n\nimport ', '\n\nimport java.util.Optional;\nimport ', 1)
    if 'LazyOptional' not in re.sub(r'import [^;]+;', '', text):
        text = re.sub(r'import net\.(?:minecraftforge|neoforged\.neoforge)\.common\.util\.LazyOptional;\n', '', text)
    legacy_bus = 'FMLJavaModLoadingContext.get().getModEventBus()'
    if legacy_bus in text:
        text = text.replace(legacy_bus, 'TConstruct.getModEventBus()')
        text = re.sub(r'import net\.minecraftforge\.fml\.javafmlmod\.FMLJavaModLoadingContext;\n', '', text)
        if 'import slimeknights.tconstruct.TConstruct;' not in text and 'package slimeknights.tconstruct;' not in text:
            text = text.replace('\n\nimport ', '\n\nimport slimeknights.tconstruct.TConstruct;\nimport ', 1)
    if 'FMLJavaModLoadingContext' not in re.sub(r'import [^;]+;', '', text):
        text = re.sub(r'import net\.minecraftforge\.fml\.javafmlmod\.FMLJavaModLoadingContext;\n', '', text)
    if 'TConstruct.getModEventBus()' in text and 'import slimeknights.tconstruct.TConstruct;' not in text and 'package slimeknights.tconstruct;' not in text:
        text = text.replace('\n\nimport ', '\n\nimport slimeknights.tconstruct.TConstruct;\nimport ', 1)
    if 'Mod.EventBusSubscriber' in text:
        text = text.replace('Mod.EventBusSubscriber', 'EventBusSubscriber')
        if 'import net.neoforged.fml.common.EventBusSubscriber;' not in text:
            text = text.replace('\n\nimport ', '\n\nimport net.neoforged.fml.common.EventBusSubscriber;\nimport ', 1)
    if text != old:
        path.write_text(text)
        changed.append(str(path))
print('Migrated Java files:', len(changed))
print('Unresolved legacy qualified references:', len(unresolved))
# Report unresolved API names only; do not export private source files.
for name in sorted(unresolved):
    print('Unresolved API:', name)
