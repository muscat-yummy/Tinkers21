# Tinkers' Construct 1.21.1 NeoForge Porting Notes

This document tracks the port of Tinkers' Construct from Minecraft 1.20.1 / Forge to Minecraft 1.21.1 / NeoForge / Java 21.

## Current checkpoint (2026-09-26)

- The repository started with documentation only. A NeoForge 1.21.1 / Java 21 Gradle scaffold and wrapper are now present.
- Upstream source and resources have **not** been imported. The build deliberately fails before compilation until `src/main/java/slimeknights/tconstruct/TConstruct.java` is present. A successful empty JAR is not a valid port.
- Pinned upstream source candidate: [SlimeKnights/TinkersConstruct 1.20.1 at 2785bc3](https://github.com/SlimeKnights/TinkersConstruct/commit/2785bc3b5e7777f9f1fd77a243887171ea2c3c68). Import the original `src` tree with its notices intact, then replace the Forge build configuration with the scaffold here. Keep a separate provenance commit.
- Tinkers' Construct 1.20.1 depends on Mantle 1.20.1. Its official source repository currently has a 1.20 branch and no 1.21.1 branch. Resolve a compatible NeoForge Mantle port before trying to compile the full TiC source; never remove Mantle-backed features just to make the build green.
- Build configuration and metadata are preparatory only. No gameplay, runtime load, or API compatibility has been verified.

### Next coherent slice

1. Import upstream source/assets/data from the pinned commit, keeping package names and attribution.
2. Port Mantle (or identify a compatible, source-audited port) and wire it into the 1.21.1 build.
3. Run one compile pass, classify the first failures as A/B/C, and migrate initialization and registries without stubbing subsystems.
4. At the Phase 2 boundary, run one meaningful Minecraft load test.

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

- Source and resources are not imported; compilation is intentionally blocked.
- Mantle compatibility for NeoForge 1.21.1 is unresolved.

## Upstream

This is an unofficial community port based on Tinkers' Construct by SlimeKnights. Port-specific issues should be reported to this project rather than to the upstream maintainers.
