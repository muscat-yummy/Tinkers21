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
