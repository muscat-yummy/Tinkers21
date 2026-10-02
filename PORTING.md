# Tinkers' Construct 1.21.1 NeoForge Porting Notes

This document tracks the port of Tinkers' Construct from Minecraft 1.20.1 / Forge to Minecraft 1.21.1 / NeoForge / Java 21.

## Current checkpoint (2026-10-02)

**Phase 1 (build environment): complete under its stated gate. Phase 2 (bootstrap): in progress.** The NeoForge 21.1.251 / Java 21 multi-project Gradle build now resolves the required Mantle and JEI compile dependencies and reaches javac with actionable 1.21 source diagnostics. This does **not** mean `compileJava`, a distributable JAR, or a Minecraft load succeeds. The remaining source migrations are Phase 2 onward. Mantle now compiles, and the standalone typed-ID behavioral test passes; TiC still does not compile or load.

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
- The [command/capability checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36878333427) stopped after 63 unresolved declaration diagnostics; body verification remains blocked. The next batch ports InventoryBlockEntity persistence to registry-aware ItemStack save/parse and adds an explicit nullable sided item-handler accessor. Mantle block-entity adapter/deferred helper types are tracked without resolving suppliers during construction and registered as item providers at RegisterCapabilitiesEvent. Unrelated world types are not scanned or instantiated. TiC subclasses with specialized old getCapability implementations must migrate their sided behavior to the new accessor or their own providers; runtime ordering and automation are still unverified.
- The [inventory provider checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36878959735) remains blocked during type declarations; inventory provider methods have not yet received full body verification. The following slice replaces Mantle's missing-mapping helper with explicit NeoForge registry aliases and wires TiC's 3 block and 8 item legacy names at LOWEST priority after normal RegisterEvent registrations. Targets are validated against actual registered resource keys, and existing source names retain precedence. Old-world alias resolution must be tested after the complete mod can load.
- An [external import audit](https://github.com/muscat-yummy/Tinkers21/actions/runs/36292910976) found additional TiC integration imports for JSON Things, Crafting Tweaks, Immersive Engineering, MmmMmmMmmMmm and Diet; their 1.21.1 API availability and compile dependency coordinates remain to be resolved.
- No gameplay, runtime load, or addon compatibility has been verified. The imported source is still Forge 1.20.1 code; this branch is not a usable 1.21.1 mod.

- The [registry alias checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36879544341) remains blocked at 54 unresolved declaration diagnostics; these are not a full remaining-error count. The next slice adapts combat attributes to holders/resource IDs, weapon damage and knockback to data-driven enchantment effects, NeoForge critical/sweep events, and the split item hit/post-hit hooks. Post-attack eligibility treats an offhand weapon as an attacking weapon but keeps the actual equipment slot for enchantment damage/break callbacks. Victim equipment effects are retained; transient anti-knockback cleanup uses finally. Vanilla item overrides can still emit a mainhand break animation; offhand break notification is also emitted and requires runtime verification. Removed global tier-order diagnostics are replaced by a dump of actual incorrect-blocks-for-drops tag membership and tiered item users; the output path/format changes accordingly. LogicHelper.orElseNull now accepts java Optional rather than removed LazyOptional. Combat, commands and downstream callers await full compilation/runtime validation.

- The [combat checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36881842295) stopped at 46 datagen declaration diagnostics; combat method bodies remain unverified. The next slice introduces Mantle's raw-JSON FinishedRecipe contract, a registry-aware RecipeConsumer implementing native RecipeOutput, and a provider writing recipes/advancements to 1.21 singular directories with duplicate checks. Native recipes use registry-aware codecs; conditions also protect advancements. Optional-mod ingredient markers are removed before writing finished JSON; no fake item instances are registered. Strict legacy NBT passes through vanilla item data fixing from the imported 1.20.1 format before emitting component ingredients; mod-specific component migration remains an addon audit item. Advancement builders are recreated and snapshots captured per recipe. Provider constructors receive GatherDataEvent's lookup future; remaining builder types/providers are migrated mechanically in the one-shot workflow. This datagen implementation still awaits full compilation and generated-pack validation.

- The [datagen/output compile checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36915898145) now reaches method-body checks and reports 305 diagnostics, versus 876 at the last comparable full checkpoint. The intervening 82/74/63/57/54/46 declaration-only counts are not comparable. The new raw/native output bridge, optional-item factories, registry-aware advancements and providers have no direct diagnostics. Gameplay and generated-pack loading remain unverified. The next batch adapts tooltip context, holder armor materials and food effect records; fixes combat item-ability calls to the actual NeoForge method; ports remaining condition serialization/evaluation to codecs while retaining Mantle's own conditions schema; propagates context into conditional true branches; and registers only matching built-in Forge condition type aliases after normal registration. These aliases preserve tag/resource names. Menu and eye-water access entries use mapped field names; deleted Item.foodProperties access is removed. NBT GSON networking uses the current reader.

- The [item/condition checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36917147083) reports 276 unique full-compile diagnostics (Gradle repeats some diagnostics in its failure summary), down from 305. Compilation and game loading still fail. The next recipe slice moves logging serializers to registry-aware stream codecs, record serializers to MapCodec, and cooking/common inputs to RecipeInput/HolderLookup. Recipe IDs belong to RecipeHolder: cooking payloads no longer require ContextKey.ID, and zero-payload serializers use idless constructors. Explicit-ID legacy cooking constructors remain available; unported TiC records that require a payload ID must migrate rather than receiving a fabricated ID. Nested CodecLoadable JSON operations retain RegistryOps in an exception-safe scope; network codecs use the connection registry lookup. Retextured crafting retains pattern-symbol and legacy ingredient texture formats, match-all behavior, and base shaped recipe codecs. Source migration alone does not validate pack or network round trips.

- The [recipe codec checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36918097641) reports 237 unique full-compile diagnostics, down from 276, with no direct diagnostics in the new recipe codec/input slice. The following slice resolves holders through public RecipeManager APIs, sorts UI/JEI recipes by holder identity, updates loot pool list append behavior/access, and fixes book tooltip context. ItemStackLoadable now reads legacy nbt through vanilla data fixing or modern components, emits component patches, and uses registry-aware full-stack network codecs. Tag-preference item outputs retain the lookup context for deferred legacy metadata conversion and accept modern component patches. Imported custom mod NBT still requires a mod-specific component mapping audit. Plain FriendlyByteBuf callers must use RegistryFriendlyByteBuf for component networking; wire format changes require the matching port on both peers. An independent registry-scope smoke check covers nested failure cleanup and thread isolation while the main source is still unfinished. Full recipe/data/network round trips remain pending.

- The [component/holder checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36918790936) reports 212 unique full-compile diagnostics, down from 237. The independently compiled registry-scope test passes nested failure restoration, thread isolation and cleanup. No direct diagnostics remain in that item/holder/loot slice. The next client slice ports fluid vertices and normalized atlas UVs, geometry baking entry points and face record accessors; preserves custom texture/book data in CUSTOM_DATA with copy-on-write; moves missing-item name/lore to dedicated components; updates screen scrolling/background APIs and advancement holder/node callbacks; and manages preview buffer lifetime. Arbitrary NBT-key models now read CUSTOM_DATA: models targeting vanilla metadata moved to dedicated components still require an explicit mapping audit. Visual appearance, resource reloads and multiplayer remain unverified.

- The [client model checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36919507754) reports 119 unique full-compile diagnostics, down from 212. Fluid/model changes have no direct diagnostics; advancement progress callbacks require AdvancementNode and are corrected in the following slice. That slice updates block property copying and wood/stair/liquid constructors, potion effect holders, texture placement/pick/tooltip APIs, fluid fire/cure APIs, menu component comparisons and single-slot stack copying/persistence. SingleItemHandler has explicit lookup-aware read/write methods; its deprecated convenience overloads require an attached parent level, so TiC block-entity load/save callers must pass the provided registry lookup before load testing. Initialization, registration order, inventory simulation/persistence and gameplay remain unverified.

- The [block registration checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36920053119) reports 74 unique full-compile diagnostics, down from 119, with no direct diagnostics in that block/inventory slice. Selected public API inspection confirms ModLoader.hasErrors(), registry-aware FluidStack stream codecs, potionBrewing(), and the private flowing-fluid helpers accessed by the original inverted-fluid algorithm. The next slice updates those access descriptors, condition codecs, command recipe holders and singular data directories, public hunger setters and registry/tag access, component-aware lectern payloads/loot ingredients and fluid output/loadables. Modern fluid metadata retains all component patches; imported legacy fluid NBT remains CUSTOM_DATA. Dynamic enchantment loadables/predicates now use Holder<Enchantment> and require lookup-aware codec parsing. Template previews delegate potion brewing to the current client level. Runtime validation, old-world migration, dynamic-predicate registry context and TiC callers remain pending.

- The [data/fluid checkpoint](https://github.com/muscat-yummy/Tinkers21/actions/runs/36920741266) reports 18 unique full-compile diagnostics, down from 74. Flowing-fluid access, modern fluid metadata, conditions, dynamic enchantment loadables and updated commands have no direct diagnostics; the newly exposed loot-source suggestion and preview tick-rate methods are corrected next. JEI adapts to native holder categories and singleton per-holder crafting extensions while preserving texture focus links. Virtual multi-recipe children receive display-only parent-derived identities (overrideable through IMultiRecipe.getDisplayRecipeId); these are not registered server recipe IDs or serializer placeholders. Entity previews use the bounded render API. Visual/JEI focus behavior, virtual-child ordering and runtime registry contexts still need verification.

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
Port mod initialization, registries, configuration, resource/data loading, and enough infrastructure for Minecraft to load the mod. Mantle compiles; TiC still has declaration and method-body migration errors, so Minecraft loading has not been reached.

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
- Mantle compiles on NeoForge 1.21.1; TiC full compilation, game loading and runtime compatibility remain unresolved.

## Upstream

This is an unofficial community port based on Tinkers' Construct by SlimeKnights. Port-specific issues should be reported to this project rather than to the upstream maintainers.


### TiC bootstrap checkpoint: Mantle compilation succeeds

- Mantle `:mantle-port:compileJava` passed on Java 21 / NeoForge 21.1.251 in run 36921363344, commit 2b430709e0649fd7ba71b84ec906b944f3933c44.
- TiC `:compileJava` reached the 2,200 diagnostic limit. This is a new full-source baseline, not comparable to previous Mantle error counts. Phase 2 remains incomplete; neither TiC compilation nor game loading has passed.
- TiC entry point now receives the NeoForge ModContainer and mod event bus explicitly. Config registration and shared deferred-register initialization use those injected values. Global loot modifiers use MapCodec.
- A one-shot migration resolves the actual compile classpath and changes legacy qualified references only when the destination class exists. It preserves unresolved APIs for explicit implementation and commits the resulting source changes to the port branch before compiling.


### TiC foundation migration checkpoints

- Run 36922448390 migrated 250 TiC Java files by checking destination class presence on the resolved 1.21.1 compile classpath. Mantle compiled successfully again. TiC compilation reached the expanded 4,000 error limit; this is not an error-count improvement measurement against the earlier 2,200 capped run.
- Run 36923052433 migrated another 35 files, including NeoForge condition package references and use of the injected TiC mod bus. Mantle compilation succeeded; TiC still reached the 4,000 diagnostic limit.
- Six custom ItemDisplayContext values are now supplied by NeoForge's enumextensions.json, with the existing serialized names and NONE/FIXED fallback semantics. Runtime loading remains pending.
- ResourceLocation is final in 1.21.1. Two narrowly scoped access-transformer entries retain TiC's existing typed ID API rather than changing all material/modifier/stat ID call sites at once. ResourceId delegates validation to vanilla factories; the removed vanilla Dummy constructor marker is replaced by a private/protected Void marker. A standalone behavioral smoke test checks invalid input, equality symmetry, hashing, sorting and default namespaces independently of TiC's remaining compile failures.
- Effect tick implementations return true to retain prior effect lifetime semantics under the new boolean API. Full effect functionality remains part of runtime testing.
- Verified built-in registry fields are migrated to BuiltInRegistries/Registries; NeoForge-only keys remain in NeoForgeRegistries. Dynamic enchantment registries are deliberately left for lookup-aware changes.


### Typed-ID validation and entity-state migration

- Run 36923710392: Mantle compilation succeeded; the ResourceId behavioral smoke test printed PASS. Destination-checked API and built-in registry migration changed 42 more Java files. TiC still hit the 4,000 diagnostic cap. No full-build or load success is claimed.
- TinkerDataCapability uses a transient AttachmentType and a NeoForge EntityCapability sharing the same holder for every living entity. It remains nonserialized, matching equipment-derived state. EquipmentContext caches a standard Optional and sizes slot arrays from EquipmentSlot.values() to include the new BODY slot.
- PersistentDataCapability uses a serialized attachment with copyOnDeath, retaining modifier NBT and the existing login/respawn/dimension sync packet flow. Serialization and copying use defensive CompoundTag copies. Legacy Forge capability/world-data import still requires validation before old-world compatibility can be claimed.
- Calls to the two migrated capability types use explicit Optional lookup helpers; remaining capabilities require their own migration. Entity attachment lifecycle, respawn and packet exchange are not yet tested in a running game.


### Registry wrapper correction (2026-10-02)

- Run 36924425142: Mantle compilation and typed-ID behavior passed again. The entity attachment migration changed 13 further Java files. TiC compilation still hit its 4,000 diagnostic limit.
- The first TiC namespace pass incorrectly represented Mantle registrations as native DeferredHolder declarations. Mantle's existing SynchronizedDeferredRegister and EnumDeferredRegister intentionally return its single-type RegistryObject adapter. Correct those TiC declarations to the actual adapter; direct generic registers without custom registry hooks use the same synchronized wrapper.
- New native attachment DeferredHolder declarations with explicit registry types are preserved. Registers with makeRegistry/getEntries require individual review and are excluded from automatic wrapper replacement.
- The next TiC compiler limit is 12,000 to capture a more complete baseline; counts from differently capped runs must not be compared as progress metrics. Phase 2 remains incomplete until TiC builds and loads.


### Full TiC source baseline and declaration diagnosis

- Run 36978088592: corrected registry handles in 19 Java files; Mantle compilation and the typed-ID smoke test passed. TiC reported 6,767 errors with a 12,000 diagnostic limit, yielding a complete baseline rather than another capped count.
- Run 36978343726: a separate annotation/declaration task reported 643 diagnostics; the normal TiC compile still reported 6,767. Declaration and full-source counts are different checks and must not be compared with each other.
- The mapped 1.21.1 ParticleType MapCodec/StreamCodec API and TransparentBlock constructor were inspected from the resolved Minecraft artifact. Fluid particles now use the complete native fluid codec/component payload. Glass and pane beacon colors use opaque ARGB while retaining the existing custom RGB values.
- Spectator menu opening uses NeoForge's public player openMenu provider/BlockPos overload, retaining the block-position payload and syncOnOpen flow.
- Legacy AT members are renamed only when the resolved mapped class contains the comment-named member with the original descriptor. Unmatched/removed members remain for deliberate source/API review. Minecraft artifacts are rebuilt after these AT changes.


### Mapped access / particle checkpoint

- Run 36979136764: 37 Java files migrated and 71 AT member names verified against mapped descriptors and corrected. Seventy-three legacy AT entries still require manual review.
- Mantle compilation and ResourceId smoke test passed. TiC declaration diagnostics decreased from 643 to 561, while the normal compile decreased from 6,767 to 6,446 using the same compiler settings. Neither check constitutes a full build or game-load success.
- BlockContainerOpenedTrigger now implements the 1.21 criterion Codec/SimpleInstance contract, preserving optional player predicates and the block-entity type JSON key. Its singleton is registered on the mod bus through the native trigger registry instead of modifying the vanilla registry during common setup. Datagen produces a Criterion wrapper referencing the same registered singleton.
- Teleport events inherit cancellation from NeoForge EntityTeleportEvent; obsolete Forge @Cancelable annotations are removed only from those subclasses.
- Particle and criterion implementation sources receive an independent normal JavaCompile check, in addition to declaration and full-source diagnostics. This check does not alter production source sets or remove any TiC systems.


### Criterion / particle implementation verification

- Run 36979892903: Mantle compile, ResourceId behavior and the independent normal JavaCompile of FluidParticleData plus BlockContainerOpenedTrigger all passed. TiC declarations decreased from 561 to 539; normal compilation decreased from 6,446 to 6,413 with unchanged settings. Phase 2 remains incomplete and no game loading is claimed.
- Move remaining ResourceLocation constructors to the native validated parse/fromNamespaceAndPath factories with a token-aware migration that preserves nested calls, quoted commas, comments and Java text blocks. Parser fixtures are checked before source mutation. Unhandled constructor forms are counted for manual review.


### Validated factory / event migration (2026-10-02)

- Run 36980904262 changed 11 ResourceLocation constructor calls in 10 files with zero unhandled forms reported. Mantle, ResourceId behavior and particle/criterion compilation passed again. TiC declaration diagnostics were 537; full compilation was 6,400. Phase 2 remains incomplete.
- TiC harvest/shear events own their real mutable Result decision rather than using the removed Forge bus @HasResult contract. Existing ALLOW/DEFAULT/DENY semantics and post-then-read dispatch are retained; listeners and modules reference the TiC enum.
- Native right-click item/block decisions use TriState, and name rendering reads RenderNameTagEvent.canRender. TRUE/FALSE preserve prior allow/deny semantics.
- Boot walking, magnet and slime bounce handlers use EntityTickEvent.Pre with an explicit LivingEntity filter, retaining execution before normal entity work. These handlers still require full compile and runtime validation.


### Native loot condition Codec checkpoint

- Run 36981466076: event changes retained successful Mantle compilation, ResourceId behavior and particle/criterion implementation compilation. TiC declarations decreased from 537 to 521, normal compilation from 6,400 to 6,351. No build/load success is claimed.
- BlockOrEntityCondition uses a singleton MapCodec; HasLootContextSetCondition uses a validated named-set codec retaining the existing set JSON field. Both expose the exact LootItemConditionType registered by TinkerCommons, preserving type identity. Unknown/missing/unregistered parameter sets are rejected rather than silently substituted.
- A separate behavioral check round-trips empty/chest/block/entity parameter set JSON and checks invalid inputs through the production codecs. Its result is pending until the next run completes.
- Fixed a non-idempotent legacy registry-key substitution that matched ForgeRegistries inside NeoForgeRegistries and accumulated Neo prefixes on each run. The migration repairs the accumulated identifiers against the actual classpath and matches complete identifiers. CI reruns namespace/resource/access migration and rejects any second-pass source changes before persistence.


### Recipe-condition Codec migration and corrected loot API

- Run 36982017195: namespace/resource/AT migration idempotence passed, Mantle compiled and ResourceId behavior passed. TiC declarations were 518, full compile 6,340. Expanded leaf compilation failed with three references to removed LootContextParamSets.get/getKey methods, so the loot behavioral check did not run; step conclusion under continue-on-error must not be mistaken for success.
- HasLootContextSetCondition now delegates to the public LootContextParamSets.CODEC, verified in the NeoForge 1.21.1 LootTable patch, retaining the set field and vanilla validation. The test compares decoded identity against actual EMPTY/CHEST/BLOCK/ENTITY constants.
- Legacy tag intersection, difference, nonempty and config conditions implement ICondition.codec and register their MapCodecs on the injected mod bus at NeoForgeRegistries.Keys.CONDITION_CODECS. Loot conditions reuse their identical registered type objects. Legacy JSON registry/tags/base/subtracted/tag/prop fields and existing logical evaluation are preserved.
- Tag collections are defensively copied and reject mixed registries, which cannot be represented by the legacy single-registry JSON schema. A standalone production-codec behavioral check covers shared/disjoint/covered/empty tags, JSON round trips, empty combination rejection and mixed registry rejection. Results remain pending.


### Verified tag condition behavior / loot encoder validation

- Run 36982814261: migration idempotence, Mantle compilation, ResourceId behavior, expanded leaf implementation compilation and TagConditionSmoke all passed. Tag tests verified intersection/difference/nonempty logic, legacy JSON round trips and invalid combinations. TiC declarations were 506 and normal compilation 6,295. Phase 2 remains incomplete.
- LootConditionSmoke reached invalid-set encoding after its registered-set JSON/identity checks, where vanilla LootContextParamSets.CODEC threw NullPointerException for an unregistered set. Wrap only encoding to convert that invalid-data exception to DataResult.error; decoding and successful encoding still delegate to the native codec. Rerun the production-codec behavior check.
- The retained tconstruct:tag_preference loot entry now uses MapCodec plus the native list-based singleton loot fields, preserves its tag preference selection and deprecated-ID warning, and returns its registered type identity. It is included in the independent implementation compile check.


### Loot behavior verified / player and projectile attachment migration

- Run 36983711606: migration idempotence, Mantle compile, ResourceId behavior, leaf implementations (including legacy tag preference loot), LootConditionSmoke and TagConditionSmoke all passed. Loot checks cover registered-set identity, legacy JSON and rejection of unknown/missing/unregistered sets. TiC declarations were 505 and full compilation 6,288; phase 2 remains incomplete.
- EquipmentChangeWatcher uses a nonserialized player attachment and native EntityCapability. Its entity provider remains client-only; player Post ticks compare copied stacks and fire the same modifier hooks. No copy-on-death or persistence is introduced for equipment-derived client state. All equipment slots remain tracked.
- Client keybinding processing uses PlayerTickEvent.Pre and the player's actual logical side; game event subscriber bus constants use the verified FML GAME enum, retaining separation from MOD subscribers.
- EntityModifierCapability uses a serialized ListTag attachment preserving ModifierNBT's existing serialized shape, modifier list merge behavior, entity predicates and the EMPTY fallback. Reads/writes defensively copy tags, and native entity lookup returns the same stored provider. Remaining callers use explicit Optional lookup helpers.
- Attachment lifecycle, projectile serialization/sync and client equipment hooks still require full compilation and runtime verification. Import of legacy Forge capability data in existing worlds is not yet implemented or claimed.


### Equipment/projectile attachment checkpoint and chest dye components

- Run 36984408450: migration idempotence, Mantle compile, ResourceId behavior, expanded leaf compile, loot JSON validation and tag behavior all passed. TiC declarations decreased from 505 to 465 and normal compilation from 6,288 to 6,229. Full build/game loading still have not passed.
- TinkersChestBlockItem stores color in DataComponents.DYED_COLOR and retains its public getColor/hasCustomColor/setColor/clearColor behavior. Placement and clone stack color transfer remain intact. Rendering supplies opaque ARGB while item/block storage retains RGB.
- Add tconstruct:tinkers_chest to the native minecraft:dyeable item tag so the vanilla dye recipe still recognizes the chest after removal of DyeableLeatherItem. The tag merges with vanilla data rather than replacing it. Legacy-world and visible render/dye behavior remain unverified.

- ToolClientEvents registers its screen through RegisterMenuScreensEvent instead of private MenuScreens.register, geometry loaders use namespaced ResourceLocation IDs, and the movement speed query uses the registered attribute Holder. Existing setup/reload/keybinding work remains in its previous phase. Full compile validation is pending.


### Chest/client API verification and diagnosed fluid copies

- Run 36985250995 verified DyedItemColor(int, boolean) and both BlockItem updateCustomBlockEntityTag signatures from the resolved mapped artifact. Migration idempotence, Mantle compile, ResourceId behavior, leaf compile, loot validation and tag behavior all passed. TiC declarations were 461 and full compilation 6,215; phase 2 remains incomplete.
- Replace 29 FluidStack(FluidStack, int) sites in 19 files only where the previous full compiler identified the first argument as FluidStack. Use copyWithAmount to retain fluid components and leave the original stack amount unchanged. Raw Fluid/Holder constructors are not broadly rewritten.
- NeoForge 1.21.1 FluidStack.copyWithAmount delegates to copy and changes only the copied amount; this source API was inspected directly. Exact diagnosed line matching, balanced token parsing, parser fixtures and migration idempotence guard the automated rewrite. Fluid game behavior remains a later runtime check.


### Fluid-copy checkpoint / native block-tag ingredient migration

- Run 36985970623 migrated all 29 diagnosed FluidStack-copy sites with zero pending forms; the second pass changed zero sites. Migration idempotence, Mantle compile, ResourceId behavior, leaf compile, loot JSON validation and tag behavior all passed. TiC declaration diagnostics remained 461 and normal compilation decreased from 6,215 to 6,185.
- BlockTagIngredient implements NeoForge ICustomIngredient and registers a native IngredientType on the injected mod bus. Its tag JSON field, item-form filtering, ordering and simple ingredient behavior remain intact; native codecs/wire wrappers replace the removed AbstractIngredient/IIngredientSerializer infrastructure.
- Direct construction at native Ingredient call sites is wrapped with toVanilla, guarded by token-aware parsing and idempotence. Custom ingredient equality/hash follow the underlying tag. Matching reads currently bound tags instead of relying on removed Forge invalidation methods. Public addon callers must use the native wrapper as part of their 1.21 API migration.
- Include the actual block-tag implementation in independent normal compilation. Runtime tag reload and recipe matching remain unverified.


### Native block-tag implementation verified / no-container ingredients

- Run 36986709814: native BlockTagIngredient compiled in the independent leaf task. One direct recipe construction call was wrapped; its second pass made zero changes. All established independent behavior checks passed. TiC declarations were 456 and normal compilation 6,159. Phase 2 remains incomplete.
- NoContainerIngredient uses ICustomIngredient/IngredientType with native registry-aware wire codecs. Decode retains flat item/tag fields and nested match JSON, while serialization emits the canonical nested representation. Nonempty decoding follows the prior getIngredient(..., false) contract. Factories return native Ingredient wrappers.
- Matching still delegates to the nested ingredient and rejects stacks with crafting remainders. The native item-stack extension method was checked in NeoForge 1.21.1 source. Equality/hash include the nested ingredient. Independent implementation compilation is expanded; actual container crafting and recipe synchronization remain unverified.


### No-container implementation verified / tool-hook native ingredient

- Run 36987548119: NoContainerIngredient joined the successfully compiled leaf implementations, and every established independent behavior check passed. TiC declarations were 452 and normal compilation 6,129. Container crafting and full pack/client-server behavior still have not been tested; phase 2 remains incomplete.
- ToolHookIngredient implements ICustomIngredient with Mantle's already ported RecordLoadable ingredient codec/stream bridge and native IngredientType registration in TinkerTools. Existing tag/hook JSON fields, default modifiable tag, matching by current tool definition hook, simple item matching and empty-tag barrier display are retained.
- Factories produce native Ingredient wrappers. Equality/hash use tag and hook ID. The barrier label uses CUSTOM_NAME. Remaining recipe/stats/predicate registration in TinkerTools is retained; only the obsolete Forge ingredient serializer registration is replaced by mod-bus deferred type registration.


### Tool-hook checkpoint / native block providers and pattern IDs

- Run 36988393676: all established independent implementation and behavioral checks passed. Declaration diagnostics decreased from 452 to 446 and normal compilation from 6,129 to 6,111. ToolHookIngredient still depends on core Lombok-generated accessors while unresolved declaration errors prevent complete annotation processing; it is not independently verified.
- BlockItemProviderCapability uses native ItemCapability and RegisterCapabilitiesEvent. The stateless fallback is registered at LOW priority, after normal custom providers across mod containers. NeoForge CapabilityHooks and FancyModLoader priority dispatch were inspected to verify this ordering. Native lookup returns nullable providers; original stack identity, placement data and consume behavior are preserved.
- Pattern retains typed ResourceId inheritance and validated nullable factories, replacing the removed ResourceLocation.Dummy marker with the existing validated Void constructor. Full TiC compilation and game loading remain pending; phase 2 is incomplete.


### Native tool capabilities and menu synchronization

- Replace the legacy ICapabilityProvider/LazyOptional dispatcher with registered native ItemCapability callbacks for modifiable tools. Built-in fluid, inventory, energy and block providers retain their selection predicates and first-successful-provider ordering. Tool views refresh before selection and retained handler operations.
- Native lookups construct providers per query. Constructor callbacks must keep persistent state in the tool data, not in provider identity. Addons with custom capabilities register them explicitly through registerCapability during RegisterCapabilitiesEvent; the provider-constructor extension point is retained.
- Register capabilities on the mod bus and replace five obsolete item initCapabilities overrides with that registration. These tools' gameplay methods and handlers remain present. No stack/provider cache retains discarded item stacks.
- Inventory blacklisting, fallback handlers and destruction drops use native nullable item capability lookup. Tool inventory menus use ServerPlayer.openMenu with registry-aware extra data and ItemStack.STREAM_CODEC on both sender and receiver, retaining FULL_STACK, MINIMAL and no-stack sync modes. CUSTOM_DATA replaces the legacy client NBT-presence guard.
- These changes are awaiting the full declaration/body compiler checkpoint. Runtime fluid transfer, inventory opening/synchronization and addon providers still need game tests. Phase 2 remains incomplete.

- Previous block-provider/pattern checkpoint run 36989960608: all established independent compilation and behavior checks passed; no diagnostics targeted the two migrated implementations. TiC declarations decreased from 446 to 433 and full compilation from 6,111 to 6,092. The implementations are not isolated runtime-tested.


### Tool-capability checkpoint / piggyback attachment and inventory helper corrections

- Run 36990705230: native tool capability declarations compiled without new dispatcher/provider API diagnostics. All established independent checks passed. TiC declarations decreased from 433 to 390 and full compilation from 6,092 to 6,024. Core Lombok-dependent tool handlers still cannot be independently compiled until the remaining declaration errors are resolved; runtime behavior is unverified.
- Piggyback passenger tracking uses a transient player AttachmentType and native EntityCapability with the existing ID. Its passenger-change tracking, vanilla packet synchronization and no-save/no-death-copy behavior are retained. The carrying effect uses an Optional adapter around native nullable lookup.
- Address genuine native inventory helper differences found by the full compiler: use ItemStack.copyWithCount instead of the removed helper, provide an Iterable of dropped stacks, and supply the optional cached recipe argument when updating the crafting grid. These changes preserve existing inventory/drop/crafting work.


### Piggyback checkpoint / component-preserving tank and can port

- Run 36991369043: all established independent checks passed. Declarations decreased from 390 to 377 and normal compilation from 6,024 to 6,002. Piggyback native capability/attachment APIs produced no direct diagnostics; the handler still depends on Lombok processing and the carrying effect has other unported effect APIs. Phase 2 remains incomplete.
- Introduce immutable StoredFluid snapshots (holder, amount, component patch) with native fluid JSON/wire codecs. FluidItemData registers the persistent/network-synchronized fluid_contents component. Access produces fresh mutable stacks; stored amounts are per item.
- TankItem and CopperCanItem retain crafting remainders, filling/draining, tooltips and creative variants while using native item capability registration and component storage. Legacy tank FluidName/Amount/Tag and can fluid/fluid_tag data are read from CUSTOM_DATA. Writes remove only migrated keys and retain unrelated data; full native fluid component patches are retained. Copper-can legacy setters preserve unresolved IDs in custom data.
- Scaled tanks preserve integral transfer increments and simulation, and normalize storage to one item. Native provider-aware NBT supports complete fluid components; legacy flat tank reading remains supported. Reject zero/negative scale rather than divide by zero.
- Register base tank/lantern block capabilities and retain their stable tank objects. NeoForge automatically invalidates block capabilities on removal/placement; no fluid-content-only invalidation is required because provider identity and availability remain unchanged. Native block entity component hooks and explicit placement/clone paths preserve tank contents.
- Tank persistence receives real HolderLookup.Provider from the world/load lifecycle. The multiblock servant base also uses provider-aware load/save; optional block offsets accept old XYZ compounds and native int arrays. No fabricated registry provider is introduced.
- Port the constant-fluid handler to the native fluid interface, retaining simulation, one-use draining and replacement containers. Expand independent compilation and behavior checks for snapshots, JSON/wire codecs, transfer scaling, native/legacy tank persistence, constant-container simulation and both block-offset forms.
- New checks are awaiting CI. In-game placement, drops/loot component copying, light/comparator updates, models, old-world upgrade and multiplayer synchronization remain unverified. Other smeltery block capability families and recipe APIs remain to be ported.


### Tank compiler checkpoint / faucet and channel native capability caches

- Run 36992829608: Mantle, migration idempotence and ResourceId behavior passed. TiC declarations decreased from 377 to 342 and full compilation from 6,002 to 5,911. The expanded leaf compile failed with three missing Lombok-generated getContainer diagnostics because that custom task lacked its annotation processor path. Loot, tag and new fluid behavior tasks depend on this compile and did not run. Do not count them as passes. Configure the leaf task with the real annotationProcessor configuration and rerun.
- Faucet neighbor lookups use native BlockCapabilityCache on ServerLevel. Caches track absent/present capabilities and reloads; their validity predicate follows the owning block entity, with no world access or immediate re-query in invalidation callbacks. Input caches are recreated when facing or owning level changes. Existing transfer simulation, buffer, redstone and stop behavior remain intact.
- Channel capabilities preserve top insertion, per-side IN insertion, OUT empty-handler signaling and no bottom capability. Native caches handle neighbor capability availability changes. Explicit connection/neighbor changes invalidate the channel capability so consumers refresh its selected handler. Existing flow allocation, one-tick locking and side render updates are retained.
- Faucet/channel/locked-tank persistence now receives the actual registry provider and accepts legacy fluid NBT through StoredFluid. Both renderer extensions explicitly retain the expanded render bounds that were removed from block-entity APIs. Normal/world/client tests for these coupled systems remain pending.
- Channel item/empty-hand interactions use native block hooks, preserving channel/gauge placement and connection toggling. Base tank/lantern clone hooks use the verified native LevelReader signature. No underlying feature is excluded from full compilation.


### Faucet/channel checkpoint and native instrument ingredient

- Run 36994097133: Mantle, migration idempotence, ResourceId, expanded leaf compilation, loot and tag behavior passed. Declarations decreased from 342 to 314 and full compilation from 5,911 to 5,842. No direct native capability/persistence API errors remain in the migrated faucet/channel implementations; packet constructor errors are part of the unresolved Lombok cascade. In-game behavior remains unverified.
- The fluid behavior task now compiled but stopped during vanilla bootstrap because NeoForge feature flags require LoadingModList. Initialize the real loader with an empty mod-file list for these isolated vanilla-registry tests. This is not a modded game launch or runtime validation; the fluid checks have not yet passed.
- InstrumentIngredient uses native ICustomIngredient/IngredientType and registry-aware codecs with the same item/instrument/ignore JSON fields. Factories return native Ingredient wrappers. Native INSTRUMENT components take precedence; legacy instrument strings in CUSTOM_DATA remain readable. Preserve specific-instrument priority, tag exclusion and acceptance of unspecified instruments in ignore mode. Display stacks contain actual registered instrument holders.
- Register the ingredient type through the deferred native registry. Expand isolated checks for native/legacy matching, excluded tags, missing/unknown instruments, component precedence, invalid JSON and JSON/wire round trips. Await CI results before counting these as passes. Phase 2 remains incomplete.
