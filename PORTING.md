# Tinkers' Construct 1.21.1 NeoForge Porting Notes

This document tracks the port of Tinkers' Construct from Minecraft 1.20.1 / Forge to Minecraft 1.21.1 / NeoForge / Java 21.

### Native brush, portals and protection slots — checkpoint 31 (2026-10-04)

- Checkpoint 30 (run 37220462678) reduces the full TiC compile to **182 errors**, from 201. Mantle and TiC declarations pass; unchanged independent production checks are skipped. Phase 2 remains open until the complete mod compiles and Minecraft actually loads.
- Modifier brushes invoke the real private native brush particle method through a Mixin invoker and use the native block interaction range attribute. Copper weathering calls the renamed native changeOverTime contract.
- Fire/blast protection slot options retain their existing config keys and defaults. Native equipment-effect iteration uses its actual enchantment holder; native level queries resolve the same registered enchantment against the actual entity registry. No mutable global enchantment definition, synthesized holder, registry fallback or dropped option is used. Configured extensions cover all equipment slots as before.
- Projectile data builders and stack persistence use native builder definitions and the entity registry provider. Shuriken portal hits delegate eligibility/cooldown/transitions to the real portal block callback. Tool fire interactions notify the native equipped-item break method.
- These new source edits await CI. Brush/protection Mixin application and portal transitions still require the real game-load gate; compile/API inspection is not that gate.

### Recipe metadata and conditional reloads — checkpoint 32 (2026-10-04)

- Checkpoint 31 (run 37221206479) reports **169 full-source javac errors**, from 182. Mantle/declarations and native API inspection pass; unchanged independent checks are skipped. Phase 2 still requires complete compilation and actual Minecraft loading.
- Casting, part-builder and modifier-worktable interfaces explicitly expose their existing real ID metadata, supplied through the native holder decode scope. No ID is inferred or synthesized. Remaining native assemble overrides accept HolderLookup.Provider.
- Material block picks use the native LevelReader/hit/player contract and preserve material data. Harvest interaction follows native item-aware/default block callbacks; break notifications, trident sounds, cannon smoke and armor binding restrictions use the native APIs/effect components.
- Custom heads extend both the real native mutable firework shape map and the recognized shape ingredient. This preserves existing vanilla shapes and makes new heads valid crafting inputs. Access changes target the verified mapped fields.
- Custom fluid/mob data checks both its legacy condition-array key and the native key, using the actual reload registry/condition context before decoding a payload. Nested loadables and redirect condition codecs inherit actual registry operations; malformed conditions remain errors.
- Source edits await CI. No complete mod build, game load or usable JAR is claimed.

### Native enchantment holder conversion — checkpoint 33 (2026-10-04)

- Checkpoint 32 (run 37221754063) reduces full-source javac to **143 errors**, from 169. Mantle/declarations/native API inspection pass. The new recipe metadata, mapped firework fields, conditional loaders and interaction edits have no compiler errors. Unchanged independent checks are skipped; no complete build/load is claimed.
- Enchantment-to-modifier mappings use actual Holder<Enchantment> values from the reload registry and registry-aware packet decoding. Tag queries use those actual holders; equivalent enchantments require the caller's actual registry generation. JEI resolves the currently connected world rather than an invented global enchantment registry.
- Conversion reads native ENCHANTMENTS/STORED_ENCHANTMENTS, removes one selected level, preserves remaining levels and the native tooltip flag, and retains the existing book/custom-name behavior. Native enchantment book instances receive real holders.
- Drill spins retain native player attack damage and the actual main-hand weapon when calling the new three-argument startAutoSpinAttack contract. Native attack bytecode confirms its damage and weapon are now captured for spins. Ranged block interaction uses native item/default callbacks and real server-scoped durability/break callbacks. Block ray traces use the actual player's native interaction-range attribute on either side.
- Dependency caching is enabled for Gradle to avoid downloading unchanged artifacts per source batch; full compilation remains required. These edits await CI, and Minecraft runtime validation remains the Phase 2 gate.

### Cancellable input and client access — checkpoint 34 (2026-10-04)

- Checkpoint 33 (run 37222358874) reduces full-source javac to **122 errors**, from 143. Mantle/declarations pass; changed holder mapping, packet, native conversion, spin, range and fluid interaction sources have no compiler errors. Unchanged independent checks are skipped. The separate API inspector failed because its BlockSource target used the wrong package; it is corrected to the source-confirmed net.minecraft.core.dispenser.BlockSource for this batch. This is not a full build/load success.
- Empty-air modifier actions use the actual cancellable InteractionKeyMappingTriggered input boundary. Native source confirms canceled use input returns from the hand loop. Consumed actions use its swing flag rather than manually double-swinging; the main-hand empty-left-click ticker reset and actual server packets are preserved. RightClickEmpty/LeftClickEmpty are notification-only and are not treated as cancellable events.
- Left-block result handling accepts the actual cancellable LeftClickBlock subtype. Survival/creative and glow/fire cancellation behavior remain explicit; native item/default and adventure placement APIs retain the real stack/context. Break callbacks use the actual tool item and equipment slot.
- Native mapped hotbar and sprite-source access signatures retain client rendering. Player head drops use native PROFILE/ResolvableProfile data instead of obsolete SkullOwner NBT.
- Material/redirect condition codecs receive actual reload RegistryOps. The integrated-server memory connection check uses NeoForge's public getConnection accessor. Bounce thresholds compare native step height with its actual attribute default to preserve the old signed addition behavior.
- These edits await CI. Mixin/input/rendering and data-pack behavior still require actual Minecraft verification; Phase 2 remains open.

### Component-aware layouts and armor stand drops — checkpoint 35 (2026-10-04)

- Checkpoint 34 (run 37222908994) reduces full-source javac to **102 errors**, from 122. Mantle/declarations pass, and the input/profile/native rendering-access/material registry edits have no compiler errors. Unchanged independent production checks are skipped. API inspection confirmed the corrected BlockSource/native signatures, then failed in the caller audit because of a doubled regex escape. That expression is corrected and syntax-validated in this batch; the separate inspector is not claimed as passed.
- Layout icons use the native optional item-stack stream codec with the real payload registry context. JSON uses Mantle's existing component-aware stack loadable, preserving complete component patches/counts and its real legacy-NBT conversion. Pattern icons and empty icons retain their own forms.
- Layout slot filters use the already-ported native ingredient loadable for JSON/networking. The layout reload listener checks both legacy/native condition keys before payload decoding and scopes GSON callbacks to the actual reload registry/condition context. Mining speed checks the native tool-data snapshot rather than removed stack tag methods.
- Custom armor stands override the verified native server-level/source drop method and preserve their custom-name component and normal equipment drops. Mapped access signatures match the actual native private methods. Material alloy conditions correctly handle the native List-based condition constructor.
- Source edits await CI. Actual Minecraft loading and runtime layout/stand behavior remain required; Phase 2 is still open.

### Native stand creation and recipe signatures — checkpoint 36 (2026-10-04)

- Checkpoint 35 (run 37223545668) reports **87 full-source javac errors**, from 102. Mantle and declaration compilation pass; unchanged independent checks are skipped. The separate native API/caller inspector now passes, including the corrected regex.
- Fancy armor stand item/dispenser generation retains the native stack configuration consumer, which applies actual name/entity components. Native record accessors and TooltipContext signatures replace removed methods.
- Recipe input/result signatures and trim/item Holder references use the actual 1.21.1 API. Turtle scute, short grass and the step-height attribute preserve their existing recipe/worldgen/modifier behavior. Material model builders explicitly require their custom loader.
- Correct the mining-speed guard to inspect native CUSTOM_DATA emptiness: ToolStackData.read always returns a compound, so a null test would incorrectly treat ordinary stacks as tool data.
- These source edits await CI. Phase 2 still requires complete compilation and actual Minecraft loading.

### Native recipe displays and sprite generation — checkpoint 37 (2026-10-04)

- Checkpoint 36 (run 37224479603) reduces full-source javac to **66 errors**, from 87. Mantle/declarations and the native inspector pass; unchanged independent checks are skipped. One tool-building assembly call requires a provider overload, included here.
- Display recipes commit mutable tool data through ToolStackData, including native damage, and copy nested stats/modifier tags. Ammo model decoding uses the actual client level registry and caches the complete native component patch.
- Sprite datagen receives GatherDataEvent's lookup future and implements the native gather lifecycle. Native armor decal pose, stat registry-varint encoding, event listener signatures, crafting ingredients and advancement builder inputs retain their existing behavior.
- Mob finalization uses the native four-argument method, preserving actual spawn group data before equipment replacement. JsonThings variants use its actual ItemBuilder Item result.
- These edits await full-source compilation. API inspection will verify native rarity, loader events and pinned JsonThings signatures for the remaining migration. Phase 2 remains open until the full build and actual Minecraft load succeed.

### Native material rarity and optional integrations — checkpoint 38 (2026-10-04)

- Checkpoint 37 (run 37224983036) reduces full-source javac to **42 errors**, from 66. The changed recipe/display/sprite/stat/model/variant sources have no compiler errors. Mantle/declarations and the native API inspector pass; unchanged independent checks are skipped.
- Native ItemStack.getRarity bytecode confirms that it reads RARITY and then promotes enchanted items. The material rarity bridge changes only that initial expression, resolving the actual currently loaded material definition and retaining native enchantment promotion. Actual Mixin application remains unverified.
- Pinned JsonThings native APIs and its original 1.20.1 source confirm the liquid constructor and render/transparency/lava/replaceability argument mapping. Native sound/effect holders and TooltipContext preserve the optional integration.
- Book component JSON uses the native codec. Registry-bearing data receives the actual client level context when present; context-free text can load before joining a world. Invalid or registry-dependent data without context fails instead of using an empty registry.
- Layout/equipment datagen receives the actual GatherDataEvent lookup future, scopes all loadable JSON to RegistryOps and uses ICondition.LIST_CODEC. Modifier model registration uses verified static ModLoader.postEvent. The native eye-in-water state retains mapped access.
- These edits await CI. Internal tool fluid/inventory component storage and the actual Minecraft load gate remain open.

### Retained common tags and native output components — checkpoint 39 (2026-10-08)

- Checkpoint 38 (run 37225467934) reports **32 full-source errors**, from 42. Mantle/declarations and native API inspection pass; unchanged independent checks are skipped. Material/book/conditional datagen/JsonThings sources compile.
- Subclass rarity overrides use the owned material rarity hook, preserving common rarity for ingots, storage blocks and anvils.
- Removed common-tag constants become explicit owned compatibility TagKeys with existing c: paths. Datagen includes quartz blocks and all vanilla/TiC stained glass/panes and copies membership to item tags. Recipes still accept tag contributors. Music discs use the native NeoForge common tag.
- The tool-forge output name uses CUSTOM_NAME. The wrapper emits native component patches within FinishedRecipe's actual registry-aware serialization scope.
- Fluid effects accept the actual Entity source supplied by Immersive Engineering; native mobProjectile supports Entity. Source/owner attribution is retained without an unsafe cast.
- Edits await CI. Full compilation and actual Minecraft loading remain Phase 2 requirements.

### Native storage foundations — checkpoint 40 (2026-10-08)

- Checkpoint 39 (run 37771638751) reports **20 full-source errors**, from 32. Three are fluid-effect callers of the intentionally widened Entity source API, corrected in this batch; the other seventeen are fluid/inventory persistence. Mantle/declarations and native inspection pass.
- ToolStorage contains immutable, defensively copied full native FluidStack/ItemStack snapshots with registry-aware native codecs and stream codecs. Sparse inventories retain arbitrary nonnegative slot indices. Component registration uses the injected mod bus and actual DATA_COMPONENT_TYPE registry.
- ToolStackView exposes owned native component access through ToolStackData; DummyToolStack retains actual item defaults in a native PatchedDataComponentMap. Existing tool-data/damage/rarity components remain managed by their dedicated APIs.
- One focused native storage check covers item/fluid input and output aliasing, potion components, counts, sparse slots, native persistence/wire, immutable removal, component synchronization/copy and external-edit cache invalidation. It includes the existing changed ToolStackData behavior check once. Unchanged independent tests are skipped; the selector now isolates storage changes.
- Storage foundations are not yet connected to all fluid/inventory behavior. Legacy-data migration, runtime Mixin application and actual Minecraft loading remain open. Source changes and the focused check await CI.

### Component-preserving fluids and legacy decode boundary — checkpoint 41 (2026-10-08)

- Checkpoint 40 (run 37772643802) reports **17 full-source errors**, all in fluid/inventory storage. Mantle/declarations, component foundations and fluid-source callers compile. The focused storage test compiled but failed in vanilla Bootstrap before assertions: the new JavaExec omitted native runtime resources. Its classpath now matches the existing native checks. This is not reported as a passing behavior test.
- Tool tanks and smashing store full native fluid snapshots in the registered tool component. Filling, draining, fluid comparison, capacity scaling and tooltips preserve component patches. Projectile snapshots save/parse with the actual projectile level registry; legacy projectile potion/custom data is converted with the same actual context.
- Both native lazy ItemStack codecs are wrapped at the source-confirmed initialization boundary, before strict/optional codecs capture them. Legacy nested fluid/items are migrated with the decoding RegistryOps, native codecs and Mantle's real vanilla data fixer. Metadata such as slot/cooking time and unrelated custom data is retained. Unknown IDs or missing context fail explicitly.
- The focused storage check adds actual legacy potion/custom-data/count/slot/cooking metadata conversion. Runtime Mixin application and old-world compatibility beyond this boundary remain unverified.
- These edits await CI; inventory-module behavior and the full Minecraft load gate remain open.

### Native inventory contents and cooking metadata — checkpoint 42 (2026-10-08)

- Checkpoint 41 is still being validated. This batch connects inventory reads, writes, searches and modifier removal validation to full native ItemStack snapshots in tool_storage. Sparse slots and complete component patches are retained; slot metadata remains in persistent tool data.
- Capacity reductions plan every slot move before committing, retain each item and its cooking metadata, and reject insufficient space without partial moves. Normal slot replacement clears its old cooking timer.
- Smelting reads actual component-backed input items and writes reduced counts back separately from remaining cooking time. Output component comparison, slot limits, recipes, sounds and experience remain intact.
- CI is now read-only, checks the triggering commit and does not retain checkout credentials or push generated source. Necessary source changes are reviewed and committed directly as part of this authorized porting work.
- Full compilation and the actual Minecraft load gate remain pending. No usable JAR or Phase 2 completion is claimed.

### Full production compilation and actual Minecraft bootstrap (2026-10-08)

- Full TiC and Mantle production compilation now succeeds without excluding source. Run 37775956085 assembles TiC and Mantle jars and reaches the actual Minecraft server launch. The original Forge JUnit fixtures remain unported; their failure in run 37775424954 is separate from the successfully compiled production sources. The runtime gate uses production assembly and native in-game tests, not a claim that the legacy JUnit suite passes.
- The first real mod load fails because NeoForge rejects event-bus registration of modules with no annotated listeners. TinkerEffects and TinkerToolParts still initialize in the same order, but are no longer registered as empty listener objects. Static fluid item-capability registration is explicitly attached to the actual mod bus.
- Existing recipe, advancement, loot, structure, function and tag directories move to the native 1.21 names while retaining their blob contents. Eight ore/geode/slime biome modifiers retain their actual biome sets, features, spawn weights and counts under the native NeoForge registry path and registered codecs.
- The isolated native GameTest now covers actual world/recipe load, both native item codec Mixins, full component-backed inventory writes, slot relocation and failed shrink atomicity, retained cooking timers, modifier removal and vanilla recipe-backed cooking. Its player and sling hook arguments match the native/current contracts.
- A Gradle success exit alone is not accepted as Minecraft load success: the real mod-construction crash returned a successful Gradle result. CI now requires the marker emitted only after the in-world assertions succeed. Runtime validation is still pending; Phase 2 remains open and no usable gameplay build is claimed.

### Native data loading and bounded tool verification (2026-10-08)

- Actual Minecraft loading passes module registration, native worldgen codecs, loot/global loot modifiers, command construction and common tags through run 37782165601. Native resource schemas preserve their existing spawn settings, loot conditions/counts, fluid amounts and tag memberships. Owned metal tags are provided under the actual c: namespace, retaining legacy files.
- The next observed fault is native ItemStack construction recursively entering tool post-load verification through an internal working copy. Verification now reads and writes tool data directly. Only internal ToolStackData snapshot construction suppresses the native hook, with scoped thread-local cleanup; normal item construction, copying and codec loading still verify tool data.
- The native world fixture adds actual TiC pickaxe copy and codec round trips with component equality. This fix is pending actual Minecraft validation in run 37783186877. No world-load success, Phase 2 completion or gameplay-ready build is claimed.

### Native common contributors — Phase 3 continuation (2026-10-08)

- The actual world gate passes in run 37785258311 with no resource reload errors. It verifies native material tools, modifier stat rebuilding, independent tool copies and native broken/damage persistence in addition to the initial storage/cooking checks.
- Parsing alone does not prove retained ingredients work: the imported iron material recipe names forge:ingots/iron, while NeoForge supplies vanilla iron under c:ingots/iron. Contributor bridges are added for 172 block/item/fluid tags with mappings verified against the primary Forge/NeoForge common-tag declarations. Existing TiC memberships are retained; each old tag includes the corresponding real native common tag.
- The in-world fixture now requires an actual vanilla iron ingot to match the retained iron material recipe and requires native string membership for old tool-construction inputs. Run 37786031222 passes the native common contributor changes and both actual iron/string ingredient assertions with no reload errors. Deeper gameplay and later phases remain open.

### Actual legacy material tools and native tooltip state (2026-10-08)

- Native item tool data previously read the presence of UNBREAKABLE but omitted its showInTooltip flag. Working snapshots and native post-load verification could therefore show an intentionally hidden tooltip. Reading native tool data now retains that bit while preserving other legacy hide flags.
- The in-world fixture exercises the actual native durability and attribute callbacks of an iron material tool. It converts that real tool's old data through vanilla DFU, then uses the actual wrapped native ItemStack codec. It checks retained materials/modifier stats, damage, name, hidden unbreakable tooltip, full nested item name/count at slot 300, cooking metadata, fluid amount, removal of duplicate legacy contents and native-copy equality.
- Run 37787434802 passes the actual legacy material-tool, native durability/attribute and tooltip checks. Its prior run exposed an NBT-to-JSON conversion bug: vanilla-fixed boolean byte tags became JSON numbers. LegacyItemData now decodes vanilla-fixed components directly with native NBT ops and the original registry lookup. This is scoped old-tool migration validation; general old-world compatibility and later gameplay remain open.

### Native harvest bridge — Phase 3 continuation (2026-10-08)

- Actual legacy material tools, native item durability and equipment attribute callbacks, hidden unbreakable tooltip state, nested native inventory/fluid contents and metadata pass in run 37787434802.
- Native GameTestHelper and ServerPlayerGameMode public signatures were inspected against the exact pinned Minecraft artifact in run 37787063570. The next test uses Minecraft's own in-level server player factory and calls the actual game-mode block destruction entry point.
- A real NeoForge break-event listener cancels one stone break and then permits it. Assertions require cancellation to preserve block/tool state, one permission event per attempt, actual block removal after approval, committed native durability consumption and agreement with ToolStack data. Listener/player cleanup is scoped in finally.
- The new actual harvest/Mixin check awaits CI. Unchanged independent production tests remain skipped; deeper modifiers, client rendering and later phases are not declared complete.

## Current checkpoint (2026-10-08)

**Phase 1 (build environment): complete. Phase 2 (bootstrap): complete. Phase 3 (core tool system): complete (2026-10-08). Phase 4 (tool construction): in progress.** Run 37785258311 on commit 887c6f687dc9917dc77a18b52a12b06eedcc42fc compiles all TiC/Mantle production sources, assembles both jars and passes the actual native Minecraft world test on NeoForge 21.1.251 / Java 21. Native reload has no ERROR entries except first-run missing server.properties. It loads 3330 recipes, 297 dynamic modifiers and 23 station layouts.

- The in-world gate checks both native item codec Mixins, native component persistence, full tool inventory contents, sparse slot migration and atomic rejection, cooking metadata and actual vanilla recipe output. It additionally creates a real three-material iron pickaxe, verifies haste changes mining speed, retains materials/name/damage/modifier stats through native save/load, breaks an independent copy, retains breakage through loading, and rebuilds stats after repair/modifier removal.
- Phase 3's actual core server gate passes in run 37823184986: material/trait/modifier stats, broken/repair/native callbacks, full native and representative legacy tool persistence, native inventory/cooking, player attachments, canceled/approved harvest and melee, construction menu transactions, actual station repair and incremental modifier continuation across save/load. Client visuals, complete old-world migration, fluid machinery and remaining gameplay phases are still open. The imported Forge JUnit fixtures remain unported; no gameplay-ready release is claimed.

- The cooking/JEI cache checkpoint passes the new crafting extension test, including a same-ID recipe reload with a new recipe instance. The production cache now uses weak reference identity because native RecipeHolder equality compares only IDs. Native cooking inputs, actual holder IDs and component-sensitive autosmelt inputs are implemented; the broader tool inventory serialization and game-load checks remain pending.
- Verified recipe consumer wrappers retain the original registry provider, copy their mutable input data and forward registry-aware advancement serialization. Generated melting recipes use native holders, world registry context, the singular 1.21 recipe directory and fluid component patches. The registry-aware recipe batch passes Mantle and independent checks and reports 303 full-source errors. The subsequent bucket/shearing/effect batch passes the same checks and reports 289.

- Native bucket interactions now supply the actual player, preserve component-sensitive fluid restrictions, and consume fluid only after successful placement. Native shearing retains the TiC event/looting context, range and durability hooks; NeoForge's native shearing callback no longer accepts a fortune argument, so the existing TiC event remains the extension point for that value. Solid fuel display uses the stack's native burn-time hook. Material effect predicates retain registered native holders.
- Station synchronization now carries the selected native holder ID, checks the current recipe manager before reusing a cached value and sends a no-match state to clear stale client displays. Armored slimes use native spawn, attribute and equipment enchantment/drop APIs; fireballs retain their actual damage source for native post-attack enchantment effects. The station/entity batch reports 274 full-source errors, and the MapCodec/worldgen and world-registry spilling batch reports 259; Mantle, declaration and independent production checks pass. The separate caller inspection failed because the runner lacked rg; its fallback is being repaired. Minecraft loading remains unverified.

- Launcher drawing snapshots now serialize against the actual level registry provider; bow arrows receive their actual firing weapon and trident throw sound uses the native holder. Crossbow arm poses read immutable custom-data snapshots. Native NBT readers preserve the previous unlimited accounting policy, and structure repaletting uses the singular 1.21 structure directory. These changes are awaiting compilation; the native API/caller audit now passes. The following batch adds native bow/crossbow firing adapters, world-registry charged-ammo parsing/tooltips, actual firing weapons and recognition of TiC crossbows for native arrow criteria. Casting assembly methods use the supplied provider, starting-book loot uses an actual loot-table resource key, and liquid cures use synchronized native cures. Full compilation and Minecraft load verification remain pending.

- Independent production checks already passing at the previous checkpoint are rerun only when their listed source inputs, Mantle, test/bootstrap code or build inputs change. Each distinct source batch still receives its required full-source compilation; no redundant final-test rerun is scheduled.

- The latest arrow bridge adds TiC knockback at the native arrow damage stage, preserves native enchantment calculation and persists the TiC amount through entity save/load. Overslime and crafting repair inputs/remainders use native CraftingInput size and the actual supplied provider. Recipe identity migration remains pending; no generated placeholder ID is introduced to make those serializers load. This batch is awaiting compilation.

- Direct-damage checks use the native source predicate; adventure fluid placement uses the native stack component permission predicate. Entity loot uses the native smelting condition. Tool casting and part-building assembly pass the supplied provider, and salvage data recipes meet native RecipeInput bounds. The unused legacy context-free armor/inverse helper retains its original math, is deprecated and is distinct from live native damage stages. This batch is awaiting compilation.

- Native recipe identity bridging uses only actual IDs: the RecipeManager entry key around conditional parsing, and the preceding native RecipeHolder wire ID around payload decoding. ID-dependent loadables and simple factories receive scoped context; nested operations and failures restore it. Native resource-location and recipe payload codecs preserve the ID-then-payload bytes. Datagen's lightweight finished recipe now receives an explicit real ID. MixinExtras was verified on the pinned compile classpath. Scope, required-ID rejection, nested/thread isolation, failure cleanup and exact native wire equality checks now pass. Actual Minecraft mixin application remains unverified until the Phase 2 load gate.

- Ammunition item post-load verification and tooltips now use native component hooks and tooltip context. Smeltery drains use the tested hand-aware interaction bridge; ducts use native clone/tooltip context and stairs resolve their supplied state during construction. Harvest/shear event constructors preserve their default result, edible break animation uses the actual item/slot, and exchanging no longer calls pre-destruction twice. Legacy tasty/severing chances resolve looting against the actual attacking entity and loot world's enchantment registry. This batch is awaiting compilation.

- Native item attributes, post-load tool verification, entity-aware use durations, tooltip contexts, tool rarity components and enchantment mapping keys now pass the independent production checks. Native tool data tests cover rarity resetting from EPIC to COMMON and independent copies. The full compile decreased through 669, 666, 651 and 648 to 637 errors. Native map IDs/frame cancellation and HUD DeltaTracker changes are being verified; runtime loading remains pending.

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

### Phase 2 — Bootstrap (complete: 2026-10-08)
Full TiC/Mantle production compilation, both jar assemblies, native mod construction, data reload and the actual in-world gate pass in run 37785258311. Resource schemas, module registration, native conditional recipes/outputs, modifier attributes and station SNBT load without data errors. No source exclusion or fake registry fallback is used.

### Phase 3 — Core tool system (complete: 2026-10-08)
Port and preserve the core material/tool/modifier systems, including MaterialId and material managers, ToolDefinition/ToolModule/ToolStack, IModifiable, Modifier/ModifierId/ModifierManager, modifier hooks/modules, and related serialization.

Pay particular attention to ItemStack data changes introduced since 1.20.1. Prefer adapters or compatibility layers where they allow existing TiC concepts and addon APIs to remain intact.

### Phase 4 — Tool construction (in progress)
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

- Complete TiC and Mantle production sources compile and both jars assemble on NeoForge 1.21.1. The imported Forge JUnit fixtures remain unported.
- Actual server world loading and the initial core-tool checks pass. Deeper runtime compatibility, client rendering and later gameplay phases remain unverified.

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


### Instrument checkpoint and native material ingredients

- Run 36995256861: Mantle, migration idempotence, ResourceId, leaf compilation (including InstrumentIngredient), loot and tag behavior passed. Declarations decreased from 314 to 308 and full compilation from 5,842 to 5,820. The fluid/instrument behavior tasks still stopped before assertions: bootstrap reached creative tabs, then failed because compile-only classpath omitted Minecraft language resources. Add actual Gradle runtime artifacts to isolated JavaExec classpaths; do not fabricate language files or count these tests as passes.
- NestedIngredient, MaterialIngredient and MaterialValueIngredient now use native custom ingredients. Deferred ingredient type registration replaces removed CraftingHelper registration. Factories return native Ingredient wrappers; shaped material recipes retrieve the typed helper through getCustomIngredient(). Material predicates, legacy flat/nested JSON and material_tag, exact/ranged values, merge behavior and material recipe lookups remain present.
- Material display uses actual material identity instead of the removed hasTag method and returns a stream for native stack/component deduplication. Remove the obsolete Forge per-ingredient caches/invalidation hooks; native recipe wrappers own display caching and are rebuilt with recipes. Material/tag reload ordering and displays still require game verification. Nested custom matching is never marked simple unless its nested ingredient also is simple.
- The loadable ingredient codec bridge carries the actual DynamicOps registry context through nested deserialization/serialization and restores its enclosing scope on failures. Material nested wire uses RegistryFriendlyByteBuf and Ingredient.CONTENTS_STREAM_CODEC. No fabricated registry provider is introduced.
- Add an independent bridge test using actual NoContainerIngredient, real vanilla registries, native nested JSON/wire and an intentional decoding failure. Material implementations remain core-coupled and await full compiler checks; runtime recipe matching, crafting and reload behavior are unverified. Phase 2 remains incomplete.


### Native material checkpoint / modifier and material loot codecs

- Run 36996102018: Mantle, migration idempotence, ResourceId, independent compilation, loot/tag behavior, instrument component/legacy matching and JSON/wire behavior all passed. The actual loadable ingredient bridge also passed registry-scope, nested JSON/wire and failure-restoration checks. TiC declarations decreased from 308 to 297 and full compilation from 5,820 to 5,760. Core material classes still have Lombok cascades; no game/runtime recipe verification yet.
- Fluid tests reached wire encoding but rejected non-synced built-in fluid registry IDs. The isolated vanilla bootstrap had not run NeoForge's registry modification lifecycle. A port-test-only class in the registry package constructs the actual native ModifyRegistriesEvent and posts it to a bus configured by NeoForgeRegistriesSetup. Its real listeners set sync flags and callbacks. No reflection, fake registry provider or replacement production implementation is used; this does not constitute a modded game startup.
- Chrysophilite and has-modifier loot conditions use native MapCodec/LootItemConditionType instances, with unchanged predicates. Attribute lookup wraps the actual registered attribute holder. Has-modifier declares its TOOL context requirement for validation.
- Tool-data and random-tool-part loot entries use native list-based condition/function fields and codecs, retaining initial damage and material selection logic. Damage percentages validate between zero and one; material loader JSON is exposed through a registry-aware codec bridge accepting its original primitive/object forms. Deferred registrations return the same native type instances.
- Add independent fluid JSON/NBT bridge and failure-restoration checks. Exact native bonus-formula codec APIs are inspected from the resolved Minecraft artifacts before porting the remaining bonus functions. Await the next CI checkpoint; phase 2 is incomplete.


### Loot checkpoint / native bonus functions and armor modifier state

- Run 37027348480: Mantle, migration idempotence, ResourceId, independent compilation, existing loot/tag, fluid contents, instrument and loadable ingredient registry-scope checks all passed. Fluid tests now cover component snapshots, JSON/wire, scaled simulation/save, constant-container behavior, old offsets and registry-aware JSON/NBT loadable bridging. TiC declarations decreased from 297 to 291 and full compilation from 5,760 to 5,734.
- No direct API/compiler diagnostics targeted AddToolDataFunction, ToolPartLootEntry or ChrysophiliteLootCondition. HasModifierLootCondition still has the same unresolved Lombok constructor cascade. Full runtime loot execution remains unverified.
- Inspect exact resolved 1.21.1 APIs: vanilla bonus formulas are exposed by a private MapCodec field named FORMULA_CODEC. A narrowly scoped verified access transformer permits reuse of that codec. Do not duplicate vanilla formula algorithms or fabricate formula holder types. The legacy allowance for missing parameters supplies an empty parameters map before native decoding.
- Both modifier/chrysophilite bonus functions use native list conditions and MapCodec/LootItemFunctionType registration, retaining native count formulas, include_base semantics and builder APIs. The chrysophilite function uses the actual registered attribute holder. Add isolated checks comparing deterministic counts with actual vanilla formulas, legacy JSON, invalid IDs and JSON/wire round trips.
- Protection, armor-level, zoom and slot-in-charge consumers accept the real transient entity data as Optional, matching the already ported capability/context helpers. Remove the old LazyOptional resolve calls; protection attribute access uses its registered holder. Slot tracking is sized from the native equipment enum.
- Armor-definition arrays include space for native BODY type without accidentally adding a body definition to legacy four-piece armor sets. Explicit BODY requests remain representable. HolyModifier uses Mantle's compiled tag-backed mob category predicate. No obsolete dummy ArmorMaterial interface replacement is claimed; the native material wrapper/item constructor port is still pending.
- Await next CI before counting bonus behavior as passed. Armor effects, priority, equipment changes, rendering and game loading remain unverified. Phase 2 is incomplete.


### Native armor registration and bonus-constructor checkpoint

- Run 37028594512: Mantle, idempotence and ResourceId passed. TiC declaration diagnostics decreased from 291 to 276 and full diagnostics from 5,734 to 5,690. The new bonus smoke test blocked independent checks because two vanilla formula constructors remained package-private; those checks must not be counted as passed. Add narrowly scoped native constructor access entries for the existing production builders and test; run 37029839246 verifies them.
- Replace the removed ArmorMaterial interface implementation with a wrapper declaring actual native ARMOR_MATERIAL registry entries. Each namespace attaches only its own DeferredRegister to its mod bus. Native material stats stay zero because tool definitions own stats, durability and repair. Texture layers retain the material ID. Equip sounds use native deferred sound-registry holders, preserving identity without requiring early sound binding.
- Initialize armor definitions during TinkerTools construction and attach its native material registry before registry events. Armor item constructors take real Holder<ArmorMaterial> values; wrapper overloads retain explicit tool definitions and texture names. Addons attach their material namespace during mod construction. Traveler and plate item registration use the original four player armor types; BODY remains opt-in.
- Sound registration uses the native RegisterEvent API. Add isolated native registry lifecycle checks covering material keys, addon bus ownership, late sound binding and actual vanilla player-armor constructors. No full armor behavior, rendering, item-data migration, modded game load or Phase 2 completion is claimed.


### Native fuel capability source migration

- Run 37029839246: Mantle, migration idempotence, independent compilation and every existing behavior check passed, including native bonus formula legacy JSON, deterministic drop counts and JSON/wire round trips. TiC declaration diagnostics remain 276; full diagnostics decreased from 5,690 to 5,686. The native bonus codec and production builders have no direct diagnostics. Armor registry changes are being checked separately in run 37030635355.
- FuelModule, SolidFuelModule and MultitankFuelModule use owner-aware native block capability sources. Server sources use actual BlockCapabilityCache and follow invalidation/absent capabilities; client gauges query the native level API. No capability is queried inside an invalidation listener. Removed owners and world changes discard caches. Multiblock rebuilds discard positions and the selected handler; native caches observe tank replacement/unload/reload and newly available handlers.
- Preserve liquid-before-solid priority, recipe-based temperatures, consumption/simulation, last-position preference, position sync and ordered aggregate transfers. GUI fluid aggregation compares native fluid components. Aggregate tank indices address local tank zero of each selected handler. Legacy compound and native array position offsets are read through the already verified TagUtil helper.
- BlockCapabilitySource is compiled independently against the exact native API. Full fuel/structure runtime verification still requires completing TiC compilation and game loading; this source batch does not satisfy that gate.

- Run 37030635355: Mantle and all existing independent behavior checks passed; declaration diagnostics decreased to 275 and full diagnostics to 5,676. The new armor check failed at compilation due to its BusBuilder import; correct it to the already verified native api package. Full root compilation still reports Lombok-generated getter cascades plus later armor API work; do not replace generated getters to hide those failures.


### Melter/alloyer/heater and inventory menu capability migration

- Register actual native block fluid handlers for the melter/alloyer, and item handlers for the melter/heater. Remove their obsolete provider overrides and LazyOptional owners. Native block entity lifecycle handles provider invalidation; existing inventories, heating rates and fuel modules remain in place. Their remaining persistence/rendering API migrations and actual game behavior are still pending.
- MixerAlloyTank uses native capability sources on the five permitted sides, retaining the ALLOYER_TANKS tag restriction and opposite-side queries. Resolve current handlers before creating an indexed view so unavailable/replaced tanks and chunk reloads recover without a Forge listener. Exclude unloaded neighbor chunks from lookups. Outputs and simulation are unchanged.
- Melter/alloyer fuel slots, single-item slots and table side inventories query actual native block item capabilities. Bucket input validation queries the native item fluid capability. SideInventoryContainer represents absent handlers with Optional and uses the native empty handler fallback; it is included in independent API compilation.
- Await CI for this batch. Bootstrap Phase 2 is still incomplete; no distributable TiC JAR or Minecraft load is claimed.

- Run 37031511247: Mantle, idempotence, independent compilation including BlockCapabilitySource, and all existing codec/behavior tests passed. Declaration diagnostics decreased from 275 to 254 and full diagnostics from 5,676 to 5,635. The armor check passed actual material events, namespace ownership, zero-stat descriptors and late sound binding, then stopped when constructing items because the isolated item registry was frozen. Open its normal mutation window too and register every fixture armor item, avoiding orphan intrusive holders. Fuel-module diagnostics left in this run are still unresolved Lombok/core-recipe cascades and the removed burn-time hook; the latter needs a native item extension call.


### Formed smeltery IO and retained capability forwarding

- Run 37032677971: Mantle, idempotence, independent API compilation including side inventory menus, all existing fluid/instrument/loadable/bonus checks, and actual native armor registration checks passed. Armor checks now verify registered material keys, addon bus ownership, late sound binding, zero-stat descriptors and four vanilla armor constructors. TiC declaration diagnostics decreased from 254 to 222 and full diagnostics from 5,635 to 5,574. Armor runtime stats/rendering and core mod loading are still unverified.
- HeatingStructureBlockEntity exposes its internal tank to IO only while formed and alive, without registering a public fluid handler on the controller. Native registration exposes melting inventories on smeltery/foundry, fluid handling on drains/ducts, and item handling on ducts/chutes. Remove the old Forge capability owners/listeners rather than substituting fake LazyOptional implementations.
- Retained IO handlers forward every transfer to the current loaded valid master. After removal or loss of formation they use native empty handlers; a replacement/reloaded master is resolved on subsequent operations. Master changes and loads invalidate the IO block's native providers. Tank filters reuse their actual parent wrapper/listeners, preserving filtered tank mapping and component-sensitive transfers. Duct item filters query actual item fluid capabilities.
- IO texture loading, drain update tags and duct item persistence/update tags use registry-aware native block entity hooks. Duct NBT reads clear its derived fluid filter and tank mapping; zero-tank filter mapping cannot query a negative index. Existing legacy master offsets remain handled by the already verified servant/TagUtil implementation.
- Add isolated checks with actual native fluid tanks and item inventories for retained proxy behavior through replacement, absence and recovery, transfer simulation, component preservation and separation from old targets. Await CI. Full multiblock/runtime verification and the remaining heating-structure persistence migration still require a compiled, loaded TiC; Phase 2 is incomplete.


### Casting, cannon, proxy tank and registry-aware persistence migration

- Run 37033930173: Mantle, idempotence, all independent compilation and every behavioral check passed. New retained IO proxy checks confirmed replacement/removal/recovery, simulations, component preservation and unchanged old targets using actual native fluid tanks and item inventories. TiC declaration diagnostics decreased from 222 to 188 and full diagnostics from 5,574 to 5,512. This is not a compiled or loaded TiC.
- Casting basin/table/tank, fluid cannon and proxy tank register native fluid/item handlers, preserving their sided casting inventories and nested-item transfer behavior. Remove old provider overrides/LazyOptional fields. Gauge sources follow neighbor rotation and use native owner-aware capability lookups. Proxy item tanks resolve actual native item capabilities for each operation, avoiding stale handlers after component/filter changes; execution still uses that operation's returned container and syncs changes.
- Casting fluid saves use native registry-aware fluid encoding and the verified StoredFluid reader for legacy/native input. Missing fluid/filter values reset previous state; invalid filter IDs safely select the empty filter. Fill/drain matching compares native fluid components. Proxy/cannon/heater item persistence passes the provided registry lookup even before level attachment. Cannon delegates tank loading to its already ported native parent. Table client update tags write inventory with the supplied lookup.
- Casting tank save/load and implicit item contents use native fluid components with registry-aware persistence, preserving per-item contents, light, redstone and inventory state. Its block interaction hooks distinguish item/empty-hand actions, retain hit-region behavior and redstone swaps, and place/clone fluid contents through native tank helpers. Its own native block codec retains the casting tank type.
- Native burn-time item extension is used by the heater slot validator, matching the fuel module. Await CI before counting this source batch as verified. Recipe lookup/holder migration, casting runtime, structure persistence and game loading remain incomplete; Phase 2 remains in progress.


### Table and chest native inventory migration

- Register the four table inventories and three chest inventories through the native block capability event. Keep configured automation restrictions, sided table inventory lookup, legacy chest block entity IDs, and actual scaling chest handler interfaces used by the GUI.
- Remove obsolete LazyOptional owners and table wrapper assignments. Neighbor inventory selection and scaling chest screens resolve actual native block item handlers, retaining the modifiable-handler restriction and empty fallback.
- Chest Items-only saves keep physical capacity independent of serialized Size and pass the supplied registry lookup to native item serialization. Chest color and station material use registry-aware save/load hooks; loading an uncolored chest clears stale custom color. Part chest duplicate matching compares native item components.
- Add isolated native inventory checks for slot limits, simulation, highest/trailing visible slots, component-preserving Items-only save/load, shrinking after extraction and empty reloads. Await CI; full tables/recipes/GUI and TiC loading remain unfinished. Phase 2 is still in progress.


### Container interactions, cannon projectile and native packet contents

- Run 37035597802: Mantle, idempotence, independent compilation and all existing behavioral checks passed. TiC declaration diagnostics decreased from 188 to 160; full diagnostics decreased from 5,512 to 5,440. Full compilation and game loading are still unfinished.
- Container filling recipes and tool tank interactions query native item/block handlers. Casting inputs expose a full FluidStack snapshot so filling preserves native component patches; legacy implementors retain their old metadata through a CUSTOM_DATA bridge. Casting wrapper initializes its fluid to EMPTY, supplies full snapshots, and exposes legacy custom data defensively.
- Dropper rails use native sided ENTITY_AUTOMATION inventory access, preserving their downward eligibility/upward transfer queries. Fluid cannon projectiles query native block inventory access, use the native synced-data builder and dimensions accessors, and save/load fluids with the entity's registry lookup. Legacy/native cannon positions use TagUtil.
- Station menus open through the native player extension. Item-slot and fluid-update payloads use native optional stream codecs and the real registry-aware play buffer provided by Mantle; absent lookup is rejected. Item-slot updates guard invalid indices and pass the actual client level/states to the renderer.
- Add independent native packet round trips for empty and component-bearing item/fluid contents, amounts, slots, positions and buffer consumption. Await CI. Container/cannon gameplay, recipes and Phase 2 completion remain pending.


### Native recipe input compatibility for retained read-only inventories

- Mantle IRecipeContainer extends the real native RecipeInput alongside Container. Its native size delegates to the existing slot-count contract, and native emptiness uses RecipeInput's normal slot scan. Existing single-stack and empty inventories retain their specialized methods and unsupported mutations.
- This allows existing casting/melting/material/station inventory views to satisfy native recipe type bounds without fake inventory adapters or deleting their read-only behavior. Native RecipeHolder lookup, provider-aware recipe methods and serializer/datagen migration still remain.
- Add independent checks for ordered multi-slot views, backing-inventory changes, empty and single-item inputs, unsupported mutations and component-preserving casting snapshots. Await CI; this is not a completed recipe or Phase 2 gate.


- Run 37036827316: Mantle, idempotence, independent compilation and existing behavioral checks passed. TiC declaration diagnostics decreased from 160 to 148; full diagnostics decreased from 5,440 to 5,407. The new scaling chest check found an existing last-slot visual-size overflow (slot+2 exceeded physical capacity); clamp expansion to the actual number of slots and rerun all persistence/shrinking checks. Do not count this new behavior check as passed before the rerun.


### Native raw translations and holder-aware crafting display/transfer

- Replace removed ForgeI18n pattern lookup with the native Language raw translation lookup in domain names, translation availability and book content. This retains literal percent signs and resource-pack translations without interpreting them as format strings. The already compiled Mantle TranslationHelper uses the same native API. Detect NeoForge by its actual neoforge mod ID.
- Book crafting display resolves native RecipeHolders to their actual values, preserves vanilla shaped dimensions and searches station extra requirements through native recipe lists. JEI 1.21.1 crafting transfer uses RecipeHolder<CraftingRecipe>, as required by its published API, retaining slot layout and inventory restrictions. Eligibility uses the native recipe dimension contract and nine-ingredient limit.
- Await CI. Native client rendering, complete book displays, JEI loading and Phase 2 game loading remain pending.


### Native entity pairing and complex spawn contents

- Run 37037426846: Mantle, independent compilation, existing behavior checks and the new native item/fluid update packet checks passed. TiC declaration diagnostics decreased from 148 to 142; full diagnostics decreased from 5,407 to 5,376. The scaling chest check still reports the already diagnosed last-slot overflow in that earlier commit; its fix is queued in the later source.
- Five custom entity spawn overrides accept the actual ServerEntity and delegate their base spawn packet to the native implementation. Keep IEntityWithComplexSpawn on legacy projectiles and custom item frames: native entity pairing appends their AdvancedAddEntityPayload through the real NeoForge extension, rather than discarding extra spawn information.
- Complex spawn methods accept RegistryFriendlyByteBuf; projectile item contents use native optional component-aware ItemStack stream codecs and the native item view. Item-frame variant, attachment position and direction retain their extra payload fields. Item-frame synced data uses the native builder and calls its parent builder hook.
- Verified the native pairing extension and server tracker patch in official NeoForge 1.21.1 source. Await compile/runtime checks. Actual entity tracking, frame rendering and multiplayer game loading remain pending.


- Run 37037816204: Mantle and the new native RecipeInput/casting snapshot checks passed; declaration diagnostics remained 142 and full diagnostics decreased from 5,376 to 5,349. The later run 37038144680 passed every independent check, including scaling chest final-slot bounds, Items-only registry-aware persistence, component/count preservation, simulation, extraction shrink and empty reload. Declaration/full diagnostics remained 142 / 5,349. Phase 2 still requires full compilation and game loading.
- Inspect public publisher metadata for available pinned native optional integrations in an independent CI job. This reads public version identifiers and filenames only; it does not install integrations into the runtime, publish artifacts, or replace missing APIs with source exclusions.


### Published native optional integration APIs

- Public publisher metadata in run 37039511274 identified the official native artifacts and source repositories. Pin compile-only Crafting Tweaks 21.1.11 (Modrinth DMu0oBKf / IhkQpEve), Immersive Engineering 12.4.2-194 (tIm2nV03 / uNRARSH2), and Dummy 1.21-2.1.2 (Adega8YN / DldwQTMc). They are not added to the runtime or bundled into TiC. Verify actual resolution/API compilation in CI.
- Official Crafting Tweaks provider methods and Dummy class/disableShield methods retain the existing integration contracts. IE native chemthrower callbacks add the actual projectile Entity parameter; pass it into the existing fluid-effect context while retaining fluid consumption, tag routing and shield disabling.
- The public Diet project currently reports no NeoForge 1.21.1 version. Its integration remains in scope; do not compile against a falsely labeled native jar or silently remove its behavior. The Json Things slug probe is corrected to jsonthings; its actual native artifact must still be resolved and ported.


- Run 37038702416: Mantle and every independent check passed. Raw Language lookup, vanilla shaped dimensions and JEI RecipeHolder crafting API were found on the exact resolved classpath. TiC declaration diagnostics decreased from 142 to 134; full diagnostics from 5,349 to 5,321.
- Run 37039085283: Mantle and every independent check passed after entity spawn migration. TiC declaration diagnostics decreased from 134 to 129; full diagnostics from 5,321 to 5,284. Actual entity tracking and multiplayer load are still unverified.
- Json Things is published on CurseForge rather than under the attempted Modrinth slugs. Its publisher file page identifies JsonThings-1.21.1-0.13.7.jar, native NeoForge 1.21.1, file 7949745, with coordinate curse.maven:json-things-541617:7949745 (https://www.curseforge.com/minecraft/mc-mods/json-things/files/7949745). Pin it compile-only and retain all integration sources. Await dependency/API validation.
- Modifier datagen uses Mantle's already ported MobType category model, matching the previously migrated Holy module. Native entity tags retain category semantics; no removed vanilla MobType import is used.

### Potion component migration (in progress)

- Bottle filling and deprecated potion transfers now use native `PotionContents`; fluid transfers retain component patches.
- `PotionData` reads legacy `Potion`, `CustomPotionEffects`, and `CustomPotionColor`, writes native components, and preserves unrelated metadata.
- Spilling and cloud effects retain their existing base-potion-only semantics and legacy predicate fields while reading native fluid contents.
- A production-helper smoke check covers item/fluid transfer, effects/color, legacy conversion, predicate snapshots, and native JSON/wire/save round trips. CI validation is pending; full root compilation and game loading remain incomplete.

- Runs 37039893207 and 37040654632 resolved the pinned optional APIs and passed Mantle plus every existing independent check. Declaration/full diagnostics decreased to 114 / 5,244 and then 85 / 5,196 respectively. No root JAR or game-load success is claimed.
- Further potion migration covers casting/display fluids, persistent tipped-tool IDs, model/tooltip colors, buckets, cauldron interactions, and JEI subtype identities. Native capabilities replace old bucket/powder-snow attachment; repeated bucket drain must return empty after container replacement.
- Tag-preference fluid outputs now accept native component patches, preserving the potion tag preference rather than pinning one fluid. Expanded production smoke validation is pending.

- Run 37041757083 passed Mantle, compilation of the potion helper, and all previous independent checks; declarations/full diagnostics decreased to 81 / 5,179. The new potion wire check stopped because its isolated vanilla bootstrap omitted NeoForge's registry modification lifecycle. Add the same real lifecycle helper used by the already passing packet tests; no codec bypass or fabricated registry is introduced.
- Food/magma bottle capabilities and JEI/generation lookups now use native item capability registration, retaining constant-container handlers and null/zero-tank handling. Native food tooltip effects and tick-rate formatting are migrated; validation is pending.

### Native advancement predicates and combat event migration

- Run 37043465493 compiled Mantle and all production leaf sources; every independent behavior check passed, including potion components, legacy effect/color migration, bucket simulation and repeated drain, JEI subtype distinction, tag-preference component output, and JSON/wire/save round trips. Declaration/full diagnostics are 58 / 5,061; Phase 2 remains incomplete.
- Tool-stack advancement conditions now use the registered native `ItemSubPredicate.Type` and vanilla `ItemPredicate.Builder.withSubPredicate`, retaining existing datagen convenience methods and tool-tag safety. No final-class access-transformer workaround is introduced.
- Native critical state/multiplier, mob-effect applicability/cures, and cancellable projectile impact replace removed Forge event results. Critical behavior validation covers airborne/grounded hits, additive boosts, earlier addon multipliers, and suppression of a vanilla critical hit.
- Explosion knockback damping now reads the native explosion-knockback resistance attribute, which is the mechanism used by native protection enchantments.
- CI runs the same independent production tests in one Gradle invocation with `--continue`; root compilation remains a separate mandatory gate and sources are not excluded. New changes await CI validation.

### Native shield, harvest, JEI widget and projectile continuation (2026-10-03)

- Runs 37099651017, 37099737128, and 37099996569 reduced declaration diagnostics from 54 to 49, 46, and 39. Mantle compiled and every independent production check passed. The new JEI widget check verifies registration, relative/exclusive hover bounds, and dynamic displayed-value callbacks.
- Hotbar rendering uses native GUI layer IDs and DeltaTracker. Shield blocking checks the native blocked state before applying tool durability because the native event also fires for unblocked attacks.
- Mining and melting effectiveness use each actual tier's incorrect-blocks-for-drops tag. Named tier persistence and comparison still require migration; this does not replace custom tiers with vanilla numeric levels.
- Pinned JEI 19.27 does not contain the newer 19.51 drawable widget builders used by the source. Production drawables now implement the supported IRecipeWidget contract, preserving animation and dynamic cooling/error tooltips without dropping them or raising the dependency blindly.
- Modifiable arrows now supply their actual tool-aware projectile through ProjectileItem.asProjectile; dispenser behavior is constructed during common setup with the registered item. Enchantment-to-modifier packets use native registry-aware codecs instead of a removed static Forge registry. These changes await their own compiler checkpoint.
- Slime armor rendering follows native material layers, NeoForge tint/model hooks, packed colors, profile components, and glint rendering. Biome modifier keys use NeoForge's actual registry key. Validation is pending.
- Phase 2 remains in progress. The whole-source compile is blocked at declaration/annotation processing; no distributable JAR or Minecraft load is claimed. Declaration counts cannot be used as a count of all remaining method-body errors.


### Native damage, optional integration and declaration gate (2026-10-03)

- Run 37102592265: Mantle and all independent production checks passed. Native damage-stage checks preserve armor/resistance ordering, custom/negative/capped protection, earlier addon reductions, zero-enchantment fallback, and exact absorption consumption. Phase 2 remains incomplete.
- Runs 37103497599, 37103778662 and 37103962078 reduced declaration blockers from 8 to 7, 4 and 2. Diet's optional published capability/tracker contract is isolated from incompatible Forge SDK jars. Tooltips use native component display interception and attribute-skip events; contextual looting is attached to actual native count/chance calculation sites, retaining projectile/offhand/armor/bleeding behavior. Runtime Mixin application and gameplay remain unverified.
- Conditional advancements now serialize native AdvancementHolder values through registry-aware codecs and NeoForge conditions. Ordered recipe alternatives decode lazily to the selected concrete recipe, preserving recipe IDs/types and fallback order; an outer OR condition disables all-false alternatives. Raw datagen retains optional mod IDs without constructing dummy runtime recipes. Current conditional kiln-unlock callers share identical advancement data; differing unlocks are explicitly rejected rather than silently chosen.
- Run 37104208183: Mantle compiled, independent production checks passed (including first-match/lazy/fallback codec checks), and the declaration/annotation-processing task passed. The mandatory full-source compile reached method bodies and reported 1,782 errors. This is a new post-annotation baseline, not comparable to the previous declaration counts. No sources are excluded. TiC compilation, JAR generation and Minecraft loading remain unfinished; Phase 2 has not passed.
- Read-only tooltip policy retains explicit legacy HideFlags from CUSTOM_DATA and preserves component snapshots. Native conditional-ops and reload tag-context checks are being expanded; do not count these new checks as passed until their CI results are verified.

### Native effects, recipe IDs and fluent vertex checkpoint (2026-10-03)

- Native effect holders/tick callbacks and stable resource IDs retain amplifier scaling. Attribute operation JSON keeps the legacy addition/multiply_base/multiply_total spellings and wire indices while accepting native aliases.
- Configured curative_items use persistent/synchronized native EffectCures, including arbitrary registered item identities. Milk/honey map to native cures; empty item lists retain totem and unrelated non-item cures. The configured MobEffectInstance is actually applied. Copy/codec identity and these cure policies pass the production effect check. In-game cancellation, save/load, milk consumption and equipment changes still require game tests.
- FinishedRecipe output now has explicit resource IDs, including casting serializers whose constructor starts with the serializer and per-level salvage suffixes. The chosen recipe remains the actual concrete runtime recipe.
- TintedVertexBuilder implements the exact native vertex API and returns its wrapper throughout fluent calls. Attribute forwarding, packed ARGB color and exactly-once RGBA tint pass the production check. Visual game verification remains pending.
- MaterialVariantId uses a native StreamCodec-backed EntityDataSerializer with the existing UTF ID representation. Deprecated potion registration remains lazy and resolves real mob-effect holders inside its supplier; neither source has direct errors in the verified 1,465-error checkpoint.
- Subsequent source batches preserve slime factories, placement conditions and compost probabilities, migrate structure/Tick/loot advancement criteria, and add a tank-content sub-predicate that reads both native fluid components and legacy saved tank data. Tank criteria retain exact fluid/amount and the old allowance for extra metadata. These later batches await their own full compile and behavioral checks.

Phase 2 remains open until full TiC compilation and a real Minecraft load succeed. No source exclusions, fake platform APIs or fixed-texture substitutions are used to advance its gate.

### Native entity synchronization builder migration (2026-10-03)

- Seven entity implementations now declare their existing synchronized fields through `SynchedEntityData.Builder`, forwarding the same builder to their superclass where required. Field IDs, serializers, defaults and later get/set operations are unchanged. Full compilation and game synchronization remain pending verification.

### Native liquid, loot, synchronization and effect policy checkpoints (2026-10-03)

- Actual full compiler totals: synchronized entity builder run 37133331246: **1,271**; modifier loot/ignition run 37133415269: **1,258**; fluid entity/container codec run 37133543923: **1,250**; item cure policy run 37133684226: **1,235**; registered effect holder/food/carry run 37133779998: **1,206**. Mantle, declaration/AP and independent checks pass at these checkpoints. No source was excluded from the full compilation gate.
- ModifierLootModifier and its deferred registry entry now use native MapCodec. Projectile and held-melee selection retain their prior branches using DIRECT_ATTACKING_ENTITY / ATTACKING_ENTITY. Remaining legacy entity ignition calls retain their existing duration rounding with igniteForSeconds.
- Entity fluid synchronization uses OPTIONAL_STREAM_CODEC and independently verifies empty values, fluid amounts, component patches and mutable snapshot isolation. Tool container fluid updates use the real registry-aware buffer; full menu/game behavior remains pending.
- Cheese removes one random milk-curable native effect holder. Plague copying and effect-duration reduction on unequip check item cures separately from totem and addon non-item cures. Revenge/strong-bones retain armor-item cure identities. Item-curable policy and retained totem/addon checks pass.
- Ten files / 77 reviewed common-tag references migrate to resolved native fields. Parser/import/owned-tag/comment/string/qualified-reference tests, exact dependency API verification and the second zero-change pass succeeded in run 37134258923; concurrent authorized branch updates rejected the bot push before compilation. Persistence now rebases its generated-source commit on the current branch with bounded retries, without force-pushing. Glass-silica and stained-glass conventions still need separate semantic review.
- Material and gadget tooltip overrides and two nested tooltip callers now use native Item.TooltipContext. Their full compile verification is pending.

### Native common tags, recipe holders, components and persistence (2026-10-03)

- Common-tag and tooltip checkpoint run 37134429115: **1,119** full errors; all independent checks pass. The reviewed common-tag migration touches 10 files / 77 references and verifies the exact resolved fields, parser ownership/import handling and second-pass idempotence. The bounded non-force persistence rebase also succeeds in run 37134614580.
- Native recipe-holder lookups checkpoint run 37134806080: **1,105** full errors; all independent checks pass. Casting, melting and worktable searches unwrap native holders only where recipe values are consumed. Alloy caches remain mutable for shuffle/iterator removal; part-builder candidates sort by native holder ID before unwrapping.
- Material item data preserves the owned Material key inside native CUSTOM_DATA. Every mutation commits a copied component through CustomData.update; reads return detached snapshots. Item and block-item post-load callbacks retain material-ID remapping through verifyComponentsAfterLoad. Dynamic material rarity remains a separate pending API adaptation.
- Material component wire verification initially lacked NeoForge's synchronized-registry initialization in the standalone harness. The existing real VanillaRegistryPortTestBootstrap fixes the harness; run 37135492610 passes read/copy isolation, preserved unrelated metadata/name, committed remaps, empty-stack protection and native item wire round trip checks.
- Block entity persistence checkpoint run 37135492610: **1,068** full errors; all established and newly added independent checks pass. Melter/alloyer native tank serialization receives the actual provided registry lookup. Five legacy block-entity loads and native save/update callbacks are migrated; three existing loadAdditional overrides now retain their superclass's public visibility. Owned multiblock/module persistence and game save/reload still require broader verification.
- Worktable and heating structure render bounds now flow through the native IBlockEntityRendererExtension callback, preserving one-/two-block bounds and multiblock interior extents. Full compile verification and visual/game validation are pending.

### Native crafting, render bounds and thrown tools (2026-10-03)

- Native renderer bounds checkpoint 37136028925: **1,062** full errors. Crafting station checkpoint 37136626470: **1,050** full errors. All independent production checks pass, including the actual native shapeless recipe and remainder slot mapping across ten trimmed/offset layouts. Recipe caching, awards and synchronization retain real RecipeHolder IDs; crafting player context is restored in finally blocks.
- Thrown-tool checkpoint 37137172718: **1,018** full errors. Native AbstractArrow owns the actual pickup stack and native persistence; TiC volatile flags are rebuilt after load. Hook-driven temporary equipment swaps restore the previous stack in finally blocks. Independent checks pass, but thrown-tool game save/reload, legacy 1.20 item import and native projectile gameplay remain unverified.
- Dispenser accessors now follow the native BlockSource record. Projectile speed, direction, stack consumption, block placement and sound behavior are retained. This source batch awaits its compiler checkpoint.

### Native projectile and material crafting checkpoint (2026-10-04)

- Full compiler totals: registered event attributes/block experience 37137611563: **1,001**; dispenser and read-only modifier data 37138186571: **985**; crystalshot and reflection/combat callbacks 37174236654: **970**; native modifiable arrow weapon/pickup/persistence 37174409004: **959**; native material crafting 37174973173: **914**. Existing independent production checks pass at these checkpoints. The earlier API inspection referred to a nonexistent IArrowItemExtension and is corrected to inspect the actual native ArrowItem callbacks. Gradle distribution download timeout is increased from 10 to 120 seconds after an observed CI timeout.
- Material shaped, deprecated shaped-material and shapeless recipes now consume CraftingInput and HolderLookup.Provider, expose native MapCodec/registry-aware streams, and leave identity in RecipeHolder. Legacy constructors retain their signatures without synthetic recipe IDs. Ordered extra materials and same-material/variant matching remain in scope.
- The new production pattern helper and actual native ingredient codecs preserve object identity between grid ingredients and ordered/repeated parts. Native JSON decode, wire round trip, repeated versus distinct ingredient objects, recreated JSON pattern data, native border trimming, invalid symbols/indices and unchanged inputs pass the independent production check. The three migrated crafting recipe classes have no direct compiler errors in checkpoint 37174973173. Full material assembly still depends on the unfinished ToolStack component writeback migration and requires game validation.
- Modifiable arrows carry the actual firing weapon into native AbstractArrow and bind their pickup item to native storage. Native registry-aware item save/load rebuilds the arrow tool cache. Crystalshot retains variant metadata, random choice, custom projectile type on native dispenser calls and infinity lookup through the real dynamic enchantment registry. Enderference uses native knockback/post-attack callbacks; reflected hurting projectiles use native scalar acceleration along their reflected motion. Old-world import and runtime projectile behavior remain unverified.
- Tool tooltip/name/attribute and melee-attribute batches are being validated next. Phase 2 remains in progress; no JAR or game load is claimed.


### 2026-10-04 native melee attack integration (phase 2, still incomplete)

Verified Actions runs 37175396419 and 37175694565: complete TiC compiler totals 879 and 856 respectively (comma-aware parsing). Mantle compilation, independent production checks, migration parser/idempotence and declaration/AP gates passed. The latter persisted 15 diagnosed native item-count copies and list-based conditions. Full TiC compilation still fails; no game load or usable JAR is claimed.

Next integration replaces ToolAttackUtil's obsolete attribute map, modifier identities, critical event and post-hurt API with native holder iteration, ResourceLocation IDs, fireCriticalHit, and victim-only doPostAttackEffectsWithItemSource using the actual hit DamageSource. Existing offensive modifier hooks remain responsible for tool effects. Temporary knockback/looting and secondary invulnerability state now restore in finally blocks. Tooltip decimal formatting uses native ItemAttributeModifiers.ATTRIBUTE_MODIFIER_FORMAT. The actual lastHurt field access transformer replaces its obsolete mapped name. This integration awaits its full build result and in-game validation.


### 2026-10-04 native combat, client buffers and scoped brewing

Full-source compiler totals, parsed including commas: tooltip 891 (37175176805), registered melee attributes 879 (37175396419), conditions/count copies 856 (37175694565), native melee integration 839 (37176203309), GUI buffer/access integration 786 (37176401220), normalized sprite UV verification 786 (37176581983), fluid/brewing integration 762 (37176872201). These are failed full builds, not completed phases. Mantle and declaration/AP gates passed in those completed runs. New brewing behavioral validation remains pending after the corrected fixture; existing production checks remained passing.

ToolAttackUtil uses actual attribute holders, ID-based replacement/removal, native critical event state, and victim-only post-attack effects with the hit's DamageSource. Temporary knockback, looting and secondary invulnerability state restore on exceptional exits. Tooltip formatting uses native ItemAttributeModifiers. GUI buffer lifecycle, record texture access, retained screen/renderer access and normalized sprite UVs now use the actual 1.21.1 methods/fields; in-game rendering is unverified.

TinkerFluids registers bottle/slime/magma brewing recipes on each actual RegisterBrewingRecipesEvent builder. BottleBrewingRecipe now requires that builder as its fifth constructor argument, and retains first-match container ingredient behavior against the actual builder's container mixes, including later addon registrations. No global or empty-registry fallback is introduced. Native cauldron maps, bucket dispenser state/level/position and fluid effect holders are migrated. Bottle output copying, component preservation, single-slot policy and cross-world isolation are covered by the new native test, pending its corrected run.

The tool-data alias audit found 27 getter sites requiring review, including InventoryModule, ModifierSetWorktableRecipe, CreativeSlotModifier and StatOverrideModifier. ToolStack still requires explicit component write-back and nested mutation handling; copying CUSTOM_DATA alone would silently lose changes. The following damage slice migrates native damageable-item predicates, Consumer<Item> break callbacks, onEquippedItemBroken notifications, and committed/read-only broken-marker access. It awaits full compiler/runtime verification. Phase 2 remains open until full compilation and actual game loading pass.


### Native tool data and enchantment checkpoint (2026-10-04)

- Full compiler totals: native damage callbacks 37177650170: **736**; native ToolStack writeback 37179465558: **724**; curse/durability callbacks 37180093884 and native cache invalidation 37180308520: **713**. The first new ToolStack smoke fixture used the CompoundTag-only parser with a Tag result; run 37179958252 corrects that fixture and passes every independent check. Do not interpret a continue-on-error workflow step as a passing test without checking its actual log.
- Mutable tool data now belongs to a working view, while attached ItemStacks receive copied native components. Damage and Unbreakable are native components; delta commits preserve unrelated external metadata. External edits to any native component keep the cached view invalid until its next refresh. Ordinary ItemStack copies and detached tool copies are independent; live attachment is explicit. Read-only data getters return snapshots, and nested inventory/worktable/creative-slot/stat/smelting writers explicitly commit edited data.
- Run 37180308520 passes Mantle, declaration/AP and all independent production checks, including native tool save/wire, immutable snapshots, nested writes, cache invalidation, copy/detach, brewing and material pattern checks. Actual Minecraft loading and gameplay remain unverified.
- The next source batch migrates enchantment hooks to real dynamic registry holders, preserves signed intermediate corrections and native level limits, and restores the exact original enchantment component after harvesting, including exception cleanup. Modifier datagen uses the GatherDataEvent lookup and registry-aware LoadableOps scope. Run 37180789528 passes the new native enchantment registry/negative correction/clamping/restoration/codec test, Mantle, declaration/AP and every independent production check; full compilation still fails with 681 errors. Phase 2 remains open until full TiC compilation and Minecraft loading succeed.

- Native attribute migration replaces the removed per-slot ItemStack callback with ItemAttributeModifiers and exact EquipmentSlotGroup values. Raw modifier hooks retain their output contract through registered attribute holders. Armor base stats keep the native per-type armor IDs; reequip comparisons use effective main-hand entries. Public native ArmorItem bytecode confirms its default attribute supplier is separate from item components, so the supported native item callback supplies tool stats without a new global event override. New attribute slot/holder/ID/JSON/wire checks and full compilation are pending.

### 2026-10-04: native block interaction, harvesting, persistence and banner assets

The full-source compiler checkpoint is 560 errors. Registry-aware melting storage, native block interaction dispatch, relative block-position NBT (including legacy compounds), overlay buffers, frame interaction and model resource identities have been migrated. Independent production checks remain green. The custom harvest bridge now runs after the actual server block-break permission event and before native block destruction; native drop-event experience is emitted once. Its Mixin application and gameplay still require the full Minecraft load gate.

Banner recipes now read native BANNER_PATTERNS components. Tool cosmetics retain native texture asset IDs and translation keys, and shield sprite generation scans actual resource-pack PNGs, including custom namespaces and nested assets, rather than the renderer's lazy pattern cache. A registry-aware native component/save test covers ordered layers, clear backgrounds, immutable input snapshots and packed dye colors. This batch is pending CI verification. Import of old 1.20 tool-banner pattern hashes and in-game banner rendering/HD resource-pack checks remain open; this checkpoint does not claim old-world compatibility or a usable JAR.

### 2026-10-04: native client geometry, screen input and advancement requirements

Full TiC compilation moved from 560 to 541 errors after banner components, then 504 after geometry baking and packed-color armor/skull rendering, and 468 after screen input and verified native layout access. All independent production checks pass through the 468-error checkpoint, including native banner component persistence and grouped advancement criteria validation/codec behavior. Actual game loading, resource reload, client rendering and harvesting Mixin application remain pending the full Phase 2 gate.

Subsequent pending-CI edits commit material/cosmetic CUSTOM_DATA changes through native immutable snapshots, let component-only tanks select their fluid model, preserve component-sensitive tank caches and native armor-trim decals, and wire the already-ported tool item sub-predicate into its actual registry event lifecycle. Bucket datagen uses the actual public native bucket content and the NeoForge resource namespace; command-generated fluid tags use the native registry tag-directory path. These pending edits are not claimed as a successful full build or usable JAR.


### Native JEI crafting holders and display continuation (2026-10-04)

- Full-source runs 37203458268, 37203677476, 37203909023 and 37204517736 decreased TiC javac totals through 433, 429 and 426 to **397**. Mantle, TiC declaration checks and all independent production behavior checks passed. Phase 2 still requires a complete TiC compile and a real Minecraft load; neither is complete.
- JEI crafting repair copies, filtered material searches, easter-egg hiding and tool inventory transfers now use actual native RecipeHolder identities. Repair display IDs come from the selected crafting holder; no placeholder server recipe ID is generated.
- Category drawables, title tooltips and transparent tooltip regions use the pinned JEI widget contract. Native loading-error guards use ModLoader.hasErrors. Their next checkpoint includes explicit widget-area bounds and tooltip snapshot checks.
- Native crafting extension registration is migrated to one registered extension per recipe class, with per-holder material renderers. Weak references avoid retaining unloaded recipe managers; same-ID recipe replacements receive new renderers. Invalid material displays retain the unhandled behavior. The new independent native-holder callback/cache check and full-source compilation are pending CI.
- Pinned JEI 19.27 ingredient visibility is read-only; context-scoped hide/unhide calls still need a supported implementation or a verified dependency migration. Do not replace them with ingredient removal that changes recipe visibility.


### Native explosion and supported JEI visibility API (2026-10-04)

- The widget checkpoint 37204945927 reports **374** full-source errors with all independent checks passing. The crafting-display checkpoint 37205277417 reports **359**; its newly added test exposed missing vanilla registry bootstrap in the test harness. The harness now uses the same actual NeoForge LoadingModList and vanilla Bootstrap sequence as the existing native registry checks, pending rerun. No complete mod compile/load is claimed.
- Custom explosions use native center(), constructor particles/sound, context-sensitive entity immunity and packet block-interaction/particle/sound metadata. Damage uses the verified native superclass damageSource through a mapped protected access change. Native detonation/start events, block rays, loot finalization, custom damage, signed knockback and invulnerability bypass remain in scope; NeoForge's knockback hook is now invoked before applying and synchronizing velocity. Full compile and in-game explosion/packet validation are pending.
- The exact compiled JEI 19.27 API has no ingredient hide/unhide mutation methods. Its replacement cannot be ingredient removal, which affects recipe visibility. Official JEI source documents scoped visibility since 19.50; the authoritative BlameJared Maven metadata in run 37205609892 confirms published 19.51.0.420 common APIs. Both modules now resolve pinned JEI 19.51.0.420 and declare an optional client dependency requiring 19.51+, preserving scoped ingredient-list filtering. Dependency resolution, exact API inspection, Mantle compatibility and native checks are pending CI. The supported widget adapters remain valid native IRecipeWidget implementations.

### Native attachment lifecycle and player gameplay (2026-10-08)

- Run 37819921112 compiles the official pinned NeoForge GameTest framework and reaches actual player login. Login exposes an unbound persistent_data attachment: the three modifier-data attachment registers and piggyback register were subscribed during CommonSetup, after the native attachment registry event.
- Move those subscriptions to their owning module constructors, retaining actual native providers, persistence/death-copy policy and login/dimension/respawn synchronization. No login event or payload synchronization is bypassed. The actual player test now requires persistent, transient modifier and piggyback capabilities.
- Extend the native world gate to actual canceled/approved sword attacks and to the real Part Builder / Tinkers Station wood-to-parts-to-pickaxe loop, including input consumption and native constructed-tool persistence. These new changes await CI; Phase 3 remains in progress.

### Actual native player and construction gate passed (2026-10-08)

- Run 37820711260 passes full TiC/Mantle assembly and the strict actual Minecraft world gate. Native player login now succeeds with the real registered persistent, modifier and piggyback capabilities. No reload/runtime ERROR remains apart from first-run server.properties.
- Actual native canceled and approved stone harvesting and sword attacks pass, including one permission event, durability synchronization, native equipped sword attributes, health changes and temporary knockback cleanup.
- Actual placed Part Builder and Tinkers Station pass the oak-planks to material parts to usable pickaxe loop, exact material/pattern/part consumption and native constructed-tool persistence. Menu/client rendering and full later-phase coverage remain open.
- Add actual station repair and incremental haste application in two transactions (9 redstone, native save/load, 36 redstone), checking modifier amounts, rebuilt speed, exact input consumption and one upgrade slot across both transactions. This extension awaits CI.

### Human armor slots and native menu transactions (2026-10-08)

- Run 37821406610 passes actual station repair and its exact input consumption, then fails at the first partial haste result. It is not marked as a modifier/gameplay gate pass. Preserve the failing behavior and report the actual selected recipe, error, free slots and contributor-tag membership in the assertion to diagnose it.
- The native armor Type enum adds animal BODY. The station's old values() loop would insert a duplicate player boots slot. Restrict this player inventory menu to the four actual humanoid armor types, retaining the existing inventory indices and icons.
- Exercise real Part Builder pattern button / native result-slot pickup and station quick-move result transfer instead of directly invoking their craft callbacks. Validate all 41 player inventory indices occur exactly once. This batch awaits CI; client screen rendering remains unverified.

### Native common-tag dependency cycles (2026-10-08)

- Run 37821855891 passes native construction menu button/pickup/quick-move actions and all 41 unique player inventory slots, then diagnoses partial haste as no matching recipe: three upgrade slots remain, but the old redstone contributor tag is empty.
- Official NeoForge's c:dusts/redstone contains actual vanilla redstone plus an optional forge:dusts/redstone compatibility import. The retained old tag imports c:dusts/redstone, forming a dependency cycle. Resolve this at the actual native TagLoader build boundary: replace only reciprocal optional common/legacy imports in canonical tags with the current reload's actual legacy entries, excluding their edge back to that canonical tag. Preserve entry order, pack source and native remove operations; leave the old tag's canonical import and all unrelated tags intact. No holders, item IDs or registry context are synthesized.
- Actual reload, common contributor membership and the two-transaction persisted modifier gate await verification.

### Actual graphical client bootstrap gate (2026-10-08)

- Add an isolated test-source-only native client run under Xvfb with software OpenGL and a null audio device. It uses the complete production mod/client/resource setup, requires the actual title screen after initial reload and verifies the real registered pickaxe, renderer, model manager and nonzero window. The test then requests normal native shutdown; CI requires its logged marker and zero client ERROR entries.
- This first client gate does not claim joined-world rendering, menu visuals, material compositing or complete Phase 4/5 coverage. Production jars do not include the check class.
- Correct the public API inspector's ItemRenderer target to the actual entity package. Its previous multi-class javap invocation returned success despite the invalid class; that item's API was not verified.

### Phase 3 complete; Phase 4 continues (2026-10-08)

- Run 37823184986 on c71ef1bb08ccc825ef0fdfe8e6d1e31c4e85e453 passes full production TiC/Mantle assembly and the strict actual world gate after the common-tag cycle fix. Native reload loads 3336 recipes and 23 station layouts, with no ERROR except first-run server.properties.
- Actual menu construction, exact consumption, native repair and partial haste (9 redstone, native save/load, 36 redstone) all pass. Haste retains amount/level, rebuilds mining speed and consumes one upgrade slot across both transactions. This completes the core system/server gate for Phase 3.
- Phase 4 now focuses on client screens, synchronized state and the full user-facing construction flow. The actual graphical client bootstrap is running separately; server-side menu success is not claimed as visual client success. Comprehensive old-world migration and later phases remain open.

### Namespaced client sprite lifecycle (2026-10-08)

- The first actual graphical client run 37823478332 fails during RegisterClientReloadListenersEvent: vanilla SpriteSources.register(String, ...) forces a minecraft namespace and rejects the namespaced shield-banner type.
- Give the sprite source a stable actual SpriteSourceType identity and register it through NeoForge's official RegisterSpriteSourceTypesEvent using the original tconstruct:shield_banner_to_modifier ID. Remove the late reload-listener registration, retaining the existing crop/resource-pack/HD sprite generation and datagen type identity. Actual client verification awaits CI.

### Native client resource migration (2026-10-08)

- Run 37824348458 gets past the sprite type lifecycle, then fails real resource reload: old Forge composite/parent models, nested shield sprite fields and Mantle's obsolete fog_distance vertex call. Its timeout is a failure, not a client pass.
- Move the three composite block loaders and 97 generated bucket/material-part model parents plus hand-authored tool parents to the real NeoForge IDs. Update the part datagen parent too. Flatten the existing shield atlas source to the native MapCodec structure, preserving all crop/placement values.
- Use native camera-relative Position for fluid fog distance and remove the obsolete view-rotation uniform. Restrict human plating/dummy plating registrations and creative iteration to the existing four human armor types; the new animal BODY was inadvertently included and had no human resource or definition.
- Install the actual narrator native library in CI, complete the real first-run accessibility onboarding in the test fixture, and bound title-screen waiting with an explicit failure. Native graphical reload verification is pending.

### Actual client bootstrap passes; construction screens continue (2026-10-08)

- Run 37827502867 passes the real title-screen/resource-reload gate with zero client ERROR entries. Pinned native public-resource inspection also verifies the new fog call and all three real NeoForge item parents.
- The bootstrap still reports missing old Forge parent warnings. Targeted inspection of 1077 owned model resources finds 47 remaining old default/default-tool parents, including shared tool templates and the shield core. Migrate those references without replacing the composited models or material overrides.
- Extend the isolated actual-client fixture to create a real native integrated world, log in its real player, compare baked wood/iron pickaxe geometry, and render native material parts. Place real construction tables, open their real screens with normal menu buffers, select patterns and layouts through actual client packets, and craft three wood parts into a pickaxe through normal inventory clicks. Verify exact materials/patterns and part consumption on the actual server, as well as usable tool data on both sides.
- CI now requires the construction-screen marker and rejects missing model/texture warnings in addition to all client ERROR entries. This expanded gate is pending; separate dedicated-server joins and full armor rendering remain open.
