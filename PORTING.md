# Tinkers' Construct 1.21.1 NeoForge Porting Notes

This document tracks the port of Tinkers' Construct from Minecraft 1.20.1 / Forge to Minecraft 1.21.1 / NeoForge / Java 21.

## Current checkpoint (2026-10-01)

**Phase 1 (build environment): complete under its stated gate. Phase 2 (bootstrap): in progress.** The NeoForge 21.1.251 / Java 21 multi-project Gradle build now resolves the required Mantle and JEI compile dependencies and reaches javac with actionable 1.21 source diagnostics. This does **not** mean `compileJava`, a distributable JAR, or a Minecraft load succeeds. The remaining source migrations are Phase 2 onward.

- Imported the full upstream `src` tree, `lombok.config`, and `.editorconfig` from [SlimeKnights/TinkersConstruct 1.20.1 at 2785bc3](https://github.com/SlimeKnights/TinkersConstruct/commit/2785bc3b5e7777f9f1fd77a243887171ea2c3c68) in a separate provenance commit. The repository tree now contains 1,988 Java files and 10,734 paths under `src/main/resources` (including directories).
- The NeoForge 1.21.1 / Java 21 Gradle scaffold successfully reached Java compilation. The [first compile run](https://github.com/muscat-yummy/Tinkers21/actions/runs/36289026918) failed with the compiler's initial 100 reported errors: missing Forge namespaces, absent Mantle API, and absent Lombok. The first two are expected migration work; Lombok has now been added to the compile and annotation processing classpaths, pending verification at the next meaningful phase boundary.
- Verified one-to-one NeoForge import changes touched 71 Java files ([migration run](https://github.com/muscat-yummy/Tinkers21/actions/runs/36289800563)). The entrypoint now receives the mod event bus through constructor injection and uses the 1.21 ResourceLocation factory. These are source migration steps, not a successful load test.
- Simple ResourceLocation constructors were migrated in 39 Java files (60 namespace/path and 40 parse calls; [migration run](https://github.com/muscat-yummy/Tinkers21/actions/runs/36290266362)). More complex call sites still require source review.
- Verified NeoForge registry and datagen imports were moved in another 40 Java files (41 imports; [migration run](https://github.com/muscat-yummy/Tinkers21/actions/runs/36290404099)). This is a package migration only; registration behavior and provider signatures remain to audit.
- The original MissingMappingsEvent flow and Mantle RegistrationHelper call in the entrypoint need a deliberate replacement; this event is absent in the NeoForge 1.21.1 source. Keep old mapping behavior in scope rather than deleting it.
- Official Mantle has no 1.21.1 branch at this checkpoint. Pinned Mantle 1.20.1 source (commit `2832b8e`) is imported in `mantle-port/` as a separate Gradle module with its license and provenance. It remains Forge code and needs its own 1.21.1 NeoForge API port before TiC can compile; do not stub or delete Mantle-backed systems.
- Mantle entrypoint constructor injection, verified imports and simple ResourceLocation calls were migrated in 70 files ([migration run](https://github.com/muscat-yummy/Tinkers21/actions/runs/36291174813)). A [Mantle compile checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36291218878) reached the Java compiler after one retry for an upstream Maven HTTP 502, and exposed old Forge APIs (networking, capabilities, fluids, config and data generation) plus 1.21 vanilla signature changes. The compiler's 100-error cap is not a total error count.
- A further 42 Mantle Java files received verified import moves for config, fluid handlers, conditions and tag providers ([migration run](https://github.com/muscat-yummy/Tinkers21/actions/runs/36291507349)). Class signatures and behavior still require review; no redundant compile was run for import-only edits.
- A shared NeoForge payload bridge now replaces Mantle's old SimpleChannel registration and delivery while retaining existing packet encoder/decoder contracts. Mantle and TiC register channels on their mod event buses; the associated import and initialization migration touched 321 Java files ([migration run](https://github.com/muscat-yummy/Tinkers21/actions/runs/36291871696)). A [compile checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36291957510) reached javac and reported 1,984 Mantle compilation errors across legacy Forge and 1.21 API use; no diagnostics pointed directly at the newly added network bridge files. This is not a successful compile or proof of packet exchange; client/server behavior is still unverified.
- Fixed the verified `ModConfigSpec` migration pattern and moved `FluidType` imports across 54 Mantle/TiC Java files (32 config type references, 51 fluid type imports; [migration run](https://github.com/muscat-yummy/Tinkers21/actions/runs/36292329206)). This is a source migration checkpoint; no additional compile was run for these edits.
- Compared compiler-failing Forge imports against the official NeoForge 1.21.1 source tree and moved 308 exact class imports in 184 Java files ([migration run](https://github.com/muscat-yummy/Tinkers21/actions/runs/36292523002)). The next [Mantle compile checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36292595001) reported 1,564 errors, down from 1,984. This is still a failed build and includes substantial API adaptations.
- Added the published JEI 1.21.1 common/NeoForge API jars as compile-only dependencies for both modules and the BlameJared repository to both Gradle projects. A [build environment checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36332272695) reached `:mantle-port:compileJava` and reported old Forge API source errors, with no dependency resolution error. Migrated Mantle's constant-fluid food item capability from Forge's per-stack provider hook to NeoForge's item registration event; this behavior remains untested.
- A [full Mantle compile checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36332975539) reported 1,388 errors, down from 1,564. The registration adapter migration exposed concrete `DeferredHolder` and constructor mismatches; these and seven fluid/item registry call sites were corrected in the next source batch. This batch is not yet compiler-verified, and Phase 2 load remains blocked.
- The [fluid/bootstrap compile checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36333473339) reported 1,278 Mantle errors, down from 1,388. The new registry holder and adapter constructors no longer appear in javac diagnostics; remaining areas include removed MobType, recipe conditions, item components, capabilities, menus, and data generation. Runtime load has not been reached.
- A [condition/category compile checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36333978883) reported 1,215 Mantle errors, down from 1,278. The category predicate and remaining resource-location constructors no longer appear as direct errors; `TagCondition` still uses the removed JSON serializer API and needs a codec migration. The game has not been launched.
- A [Mantle loot and codec compile checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36336104839) reported 1,131 errors, down from 1,215 at the prior documented checkpoint. Recipe tag conditions, block tag and context-set loot conditions, both global loot modifiers, custom loot entry and functions now use NeoForge 1.21.1 MapCodecs; the edited loot files have no direct javac errors at this checkpoint. Fluid item capability lookup and codec stream arguments were also adapted. This is still a failed Mantle compile; no TiC compile or Minecraft load has been attempted. Earlier intermediate compile counts were 1,189, 1,182, 1,162, 1,158 and 1,154. The remaining errors include legacy Forge capabilities and ingredients, client rendering, and 1.21 data APIs.
- A [Mantle registration and codec checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36431893108) reported 1,081 errors, down from 1,131 at the last documented checkpoint (intermediate counts: 1,117, 1,100, 1,094, 1,088). Menu, spawn egg, log-stripping and direct registry holder APIs were migrated; soulbound marker data uses item data components. Registry wire IDs and recipe/loot codecs were adapted, with two loot codec names corrected in the next batch. Mantle still does not compile or load; fluid component and custom ingredient migrations remain substantial.
- The [fluid component compile checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36432384867) reported 1,075 errors, down from 1,081. The corrected loot codecs and `FluidStack` custom-data migration no longer have direct compiler errors. The old ingredient loadable still uses removed JSON and network methods; the next source batch replaces those with 1.21 codecs. This is still a failed Mantle compile.
- The [ingredient codec compile checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36432803464) reported 1,072 errors, down from 1,075. The new enum codec implementation exposed one Java enum initialization error, corrected in the next batch. The common fluid transfer helper still uses Forge `LazyOptional`; the next batch migrates it to nullable NeoForge block/item capabilities and preserves fluid components during split transfers.
- The [fluid capability compile checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36433318663) reported 1,050 errors, down from 1,072. The shared ingredient codec loadable and fluid transfer helper have no direct diagnostics. Runtime packet exchange, bucket entities, and tank transfers remain unverified. A subsequent review fixes refund copying before a drained stack can become empty; offhand cooldown state is migrated to transient player attachments with an entity capability.
- The [offhand attachment compile checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36645689075) reported 1,028 errors, down from 1,050. The transient attachment/entity capability implementation and fluid refund ordering have no direct compiler diagnostics. Registry/attachment lifecycle and left-hand combat require runtime verification after the full build succeeds. The next batch migrates the shared JSON helper to vanilla registries, the public connection accessor and current DataResult error handling.
- The [JSON helper compile checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36646060420) reported 1,022 errors, down from 1,028. The shared registry/codec JSON helper has no direct compiler diagnostics. The next coherent batch ports the shaped fallback recipe and builder to CraftingInput, MapCodec/StreamCodec, and RecipeOutput, preserving alternatives and data-generation conditions/advancements. Recipe alternatives are resolved from current holders so data-pack reloads cannot retain stale objects. ShapedRecipe field access uses Mojang names and the Mantle access transformer is explicitly declared in mod metadata.
- The [fallback recipe compile checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36646519945) reported 995 errors, down from 1,022. The fallback recipe, builder and recipe registration have no direct compiler diagnostics. This is still a failed Mantle compile; crafting priority, conditions/advancement generation and data-pack reload behavior need runtime validation. The next batch ports the fluid-container ingredient to ICustomIngredient and IngredientType, retains legacy flat/nested fluid JSON, and uses registry-aware streams plus NeoForge item capabilities. Its factory methods now return the vanilla Ingredient wrapper; addon callers should use getCustomIngredient() to inspect the underlying custom implementation.
- The [fluid-container ingredient checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36646827435) reported 970 errors, down from 995. The fluid-container custom ingredient and type registration have no direct compiler diagnostics. Matching must still be tested with real container capabilities and recipe synchronization once the full build succeeds. The next batch migrates potion ingredients, their item/tag base and loadable serializer adapter to ICustomIngredient/IngredientType, retaining the item/tag/potion JSON fields and legacy minecraft:empty meaning.
- The [potion ingredient checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36647223213) reported 927 errors, down from 970. The potion/item ingredient family and loadable adapter have no direct compiler diagnostics. The next batch updates book crafting/smelting lookups to RecipeHolder, shaped dimensions to ShapedRecipe, common translations to Language, and fluid container JSON to the shared ingredient codec. Item/fluid metadata transfer uses explicit component patches, including potion contents and custom data; item defaults are excluded. Existing transfer IDs containing `nbt` are retained, but their payload is now components. Legacy NBT payload conversion remains a separate compatibility task. These changes await a compile checkpoint; gameplay remains unverified.
- The [fluid transfer/book checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36875810849) reported 905 errors, down from 927. Book crafting/smelting lookup and common translation changes have no direct diagnostics; transfer component methods also resolve, while constructors still show missing Lombok-generated members amid unresolved declaration types elsewhere. The next batch migrates health/offhand/gauge GUI handlers to NeoForge GUI layers and block capabilities, wraps the health layer with vanilla fallback so normal Pre/Post events remain managed by NeoForge, and updates attack indicators to HUD sprites. Shader event subscription and tooltip font imports are also updated. These GUI changes need compile and runtime verification.
- The [GUI layer checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36876255495) reported 876 errors, down from 905. Health-layer wrapping, HUD sprite drawing, gauge capability lookup and shader event subscription have no direct diagnostics. Geometry loader registration now requires ResourceLocation identifiers; the next batch corrects those six registrations and ports item-layer quad export/vertex methods, normalized sprite UVs, record face accessors and geometry bake signatures. Mantle's model/container/tooltip access transformer entries are moved to the names/signatures used by the 1.21 compiler. Book tooltip context and compressed structure resource reading are updated as part of that slice. Rendering behavior remains unverified until a successful full build and game launch.
- The [model quad checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36876856207) stopped after 82 unresolved import/type-declaration diagnostics, before checking method bodies. This count is **not comparable** to the previous 876 full diagnostics and must not be interpreted as 82 remaining errors or successful model compilation. The next batch addresses removed PotionUtils/Sensitivity/NonNullConsumer/ChunkStatus references, current fluid-source events, structure-preview MapId/game-event/time APIs, and registry-aware base block-entity persistence. The old registry-independent saveSynced hook remains bridged for subclasses. Inventory capability registration and further TiC subclass persistence changes are still pending.
- The [preview/persistence checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36877767221) stopped with 74 unresolved import/type-declaration diagnostics. Method-body verification is still blocked, so neither this count nor missing body diagnostics establishes successful compilation. The next slice migrates held potion/enchantment inspection, fluid capability commands, NeoForge biome-modifier registry IDs, inventory/gauge block interaction and menu opening, and book export errors to Brigadier command exceptions with the current model-view matrix stack. Inventory-provider registration and combat/datagen declaration blockers remain in scope.
- An [external import audit](https://github.com/muscat-yummy/Tinkers21/actions/runs/36292910976) found additional TiC integration imports for JSON Things, Crafting Tweaks, Immersive Engineering, MmmMmmMmmMmm and Diet; their 1.21.1 API availability and compile dependency coordinates remain to be resolved.
- No gameplay, runtime load, or addon compatibility has been verified. The imported source is still Forge 1.20.1 code; this branch is not a usable 1.21.1 mod.

### Next coherent slice

1. Continue porting the in-repository Mantle source, starting with capabilities, fluid and registry APIs; prioritizing the currently failing fluid/capability, registry and rendering APIs; review and verify the new payload bridge once the coherent Mantle compile blockers are addressed.
2. Classify and migrate Forge registration/event references (A), then the changed item data, resource, fluid, rendering, and networking APIs (B).
3. After a coherent group of changes, run one compile pass; at the Phase 2 boundary, perform one meaningful Minecraft load test.

### Distribution consideration

The [SlimeKnights FAQ](https://slimeknights.github.io/faq/) states that it does not grant permission for independent Forge/NeoForge ports to future Java versions while the project is active, although it permits reuse of MIT-licensed code and assets. This branch is currently intended only for a private community server. Before any public release, review the project name, presentation, and that stated policy with the maintainers. The repository's MIT license and attribution do not imply endorsement.

## Principles

- Preserve upstream Tinkers' Construct behavior and API structure wherever practical.
- Prefer compatibility layers over unnecessary rewrites.
- Do not remove or stub features merely to make the project compile.
- Keep existing data/resource formats compatible where reasonably possible.
- Document unavoidable incompatibilities for addon authors.
- Make small, meaningful commits and keep the port branch recoverable.

## Porting classification

When investigating breakage, classify changes as:

- **A — Mechanical:** imports, renamed classes/methods, registration/event API changes, mappings.
- **B — Adaptation:** Data Components, networking, rendering, recipes, capabilities/attachments, fluids, menus, or other APIs requiring implementation changes.
- **C — Architectural:** changes deeply coupled to Tinkers' Construct internals that require substantial redesign.

## Development phases

### Phase 1 — Build environment (complete: 2026-09-28)
Move Gradle/build configuration to Minecraft 1.21.1, NeoForge, and Java 21. Resolve dependencies and reach a useful compile state. The compiler now reports source-level port errors against the resolved core classpath. Compilation itself is not yet successful; optional integration APIs remain in the later-content audit.

### Phase 2 — Bootstrap (in progress)
Port mod initialization, registries, configuration, resource/data loading, and enough infrastructure for Minecraft to load the mod. The current build still fails in Mantle source before Minecraft can load.

### Phase 3 — Core tool system
Port and preserve the core material/tool/modifier systems, including MaterialId and material managers, ToolDefinition/ToolModule/ToolStack, IModifiable, Modifier/ModifierId/ModifierManager, modifier hooks/modules, and related serialization.

Pay particular attention to ItemStack data changes introduced since 1.20.1. Prefer adapters or compatibility layers where they allow existing TiC concepts and addon APIs to remain intact.

### Phase 4 — Tool construction
Port tool parts, recipes, menus/screens, Part Builder, Tinkers' Station, and the complete loop from materials to usable modified tools.

### Phase 5 — Rendering
Restore material-dependent tool and armor rendering. Do not replace TiC's composited/material rendering with fixed textures merely to simplify the port.

### Phase 6 — Smeltery and fluids
Port Smeltery, Foundry, melting, casting, tanks, fluid handling, and related block entities/recipes.

### Phase 7 — Remaining content
Port slimes, entities, world generation, special modifiers, integrations, datagen, and remaining content.

### Phase 8 — Addon compatibility
Audit public/internal APIs commonly used by addons and document differences. The long-term goal is to make ports of addons such as TiCEX practical without complete rewrites.

## High-risk migration areas

Track changes involving ItemStack NBT/Data Components, Forge-to-NeoForge registries/events, capabilities and attachments, networking, recipes, resource reloads, rendering, fluids/energy, block entities, world generation, loot, and datagen.

## Testing policy

Avoid repeatedly launching a full Minecraft instance after tiny changes. Prefer compilation and static checks while resolving a coherent group of migration issues, then perform one meaningful runtime test at an appropriate phase boundary.

## Compatibility log

| Area | Upstream 1.20.1 | 1.21.1 port | Compatibility notes |
| --- | --- | --- | --- |
| Item/tool data | TBD | TBD | TBD |
| Networking | SimpleChannel and NetworkEvent.Context | NeoForge payload bridge with Mantle packet adapter | API calls migrated; compile checkpoint has no direct bridge diagnostic, full build and client/server behavior unverified |
| Registries | TBD | TBD | TBD |
| Rendering | TBD | TBD | TBD |
| Fluids/capabilities | TBD | TBD | TBD |
| Recipes | TBD | TBD | TBD |

## Known issues

- The imported 1.20.1 source still references Forge APIs and cannot compile against NeoForge 1.21.1.
- Mantle compatibility for NeoForge 1.21.1 is unresolved.

## Upstream

This is an unofficial community port based on Tinkers' Construct by SlimeKnights. Port-specific issues should be reported to this project rather than to the upstream maintainers.
