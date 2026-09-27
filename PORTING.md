# Tinkers' Construct 1.21.1 NeoForge Porting Notes

This document tracks the port of Tinkers' Construct from Minecraft 1.20.1 / Forge to Minecraft 1.21.1 / NeoForge / Java 21.

## Current checkpoint (2026-09-27)

- Imported the full upstream `src` tree, `lombok.config`, and `.editorconfig` from [SlimeKnights/TinkersConstruct 1.20.1 at 2785bc3](https://github.com/SlimeKnights/TinkersConstruct/commit/2785bc3b5e7777f9f1fd77a243887171ea2c3c68) in a separate provenance commit. The repository tree now contains 1,988 Java files and 10,734 paths under `src/main/resources` (including directories).
- The NeoForge 1.21.1 / Java 21 Gradle scaffold successfully reached Java compilation. The [first compile run](https://github.com/muscat-yummy/Tinkers21/actions/runs/36289026918) failed with the compiler's initial 100 reported errors: missing Forge namespaces, absent Mantle API, and absent Lombok. The first two are expected migration work; Lombok has now been added to the compile and annotation processing classpaths, pending verification at the next meaningful phase boundary.
- Verified one-to-one NeoForge import changes touched 71 Java files ([migration run](https://github.com/muscat-yummy/Tinkers21/actions/runs/36289800563)). The entrypoint now receives the mod event bus through constructor injection and uses the 1.21 ResourceLocation factory. These are source migration steps, not a successful load test.
- The original MissingMappingsEvent flow and Mantle RegistrationHelper call in the entrypoint need a deliberate replacement; this event is absent in the NeoForge 1.21.1 source. Keep old mapping behavior in scope rather than deleting it.
- The official Mantle source has no 1.21.1 branch at this checkpoint. A compatible NeoForge Mantle dependency or port is required before TiC can compile. Do not stub or delete Mantle-backed systems to hide this dependency.
- No gameplay, runtime load, or addon compatibility has been verified. The imported source is still Forge 1.20.1 code; this branch is not a usable 1.21.1 mod.

### Next coherent slice

1. Port or source-audit a 1.21.1 NeoForge Mantle dependency and wire it into Gradle.
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

### Phase 1 — Build environment
Move Gradle/build configuration to Minecraft 1.21.1, NeoForge, and Java 21. Resolve dependencies and reach a useful compile state.

### Phase 2 — Bootstrap
Port mod initialization, registries, configuration, resource/data loading, and enough infrastructure for Minecraft to load the mod.

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
| Networking | TBD | TBD | TBD |
| Registries | TBD | TBD | TBD |
| Rendering | TBD | TBD | TBD |
| Fluids/capabilities | TBD | TBD | TBD |
| Recipes | TBD | TBD | TBD |

## Known issues

- The imported 1.20.1 source still references Forge APIs and cannot compile against NeoForge 1.21.1.
- Mantle compatibility for NeoForge 1.21.1 is unresolved.

## Upstream

This is an unofficial community port based on Tinkers' Construct by SlimeKnights. Port-specific issues should be reported to this project rather than to the upstream maintainers.
