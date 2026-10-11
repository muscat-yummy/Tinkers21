# This repo is currently being neglected.
# このリポジトリは現在サボっています。

**English** | [日本語](README_ja.md)

# Tinkers' Construct for Minecraft 1.21.1

An unofficial port of **Tinkers' Construct** to **Minecraft 1.21.1** using **NeoForge**.

This project aims to bring the original Tinkers' Construct experience to modern Minecraft while preserving the original mod's systems, APIs, data formats, and addon compatibility as much as reasonably possible.

> [!IMPORTANT]
> This is an **unofficial community port** and is not affiliated with or maintained by the original Tinkers' Construct developers or SlimeKnights.

## Goals

The primary goal of this project is a faithful port rather than a rewrite or reimplementation.

Where possible, the project aims to preserve:

- Materials and material stats
- Tool parts and tool construction
- Tool definitions
- Modifiers and modifier hooks
- Tinkers' Station and related crafting systems
- Smeltery and Foundry systems
- Melting and casting
- Fluid systems
- Slimes and other Tinkers' Construct content
- Existing resource and data formats
- Tinkers' Construct APIs used by addons

Minecraft and NeoForge APIs have changed significantly since Minecraft 1.20.1. Some internal changes are therefore unavoidable, particularly around item data, networking, rendering, registries, and other platform APIs.

Where an API must change, compatibility with existing Tinkers' Construct concepts and addons is preferred over introducing unnecessary new abstractions.

## Target

| Component | Version |
| --- | --- |
| Minecraft | 1.21.1 |
| Mod Loader | NeoForge |
| Java | 21 |
| Original Tinkers' Construct | 1.20.1 |

## Status

🚧 **Work in progress**

This port is currently under active development.

Do not expect all features to work correctly yet. Builds produced during development may contain missing functionality, crashes, broken rendering, incompatible data, or other issues.

The rough development order is:

1. Build system and NeoForge initialization
2. Registries and core infrastructure
3. Materials, tools and modifiers
4. Tool construction and Tinkers' Station
5. Tool and armor rendering
6. Smeltery, melting, casting and fluids
7. World content and remaining features
8. Addon compatibility

## Addon Compatibility

Addon compatibility is an important goal of this project.

The port attempts to retain the original Tinkers' Construct API structure wherever practical so that existing addons can also be ported to Minecraft 1.21.1 without being completely rewritten.

Compatibility is not guaranteed while development is ongoing.

Once the core port becomes stable, additional work may be done to port or support existing Tinkers' Construct addons.

## Contributing

Contributions, testing, bug reports, and compatibility fixes are welcome.

When contributing, please avoid replacing existing Tinkers' Construct systems with unrelated implementations merely to make the project compile.

Changes should preferably preserve existing behavior and APIs unless Minecraft 1.21.1 or NeoForge makes that impractical.

For major architectural changes, please explain why the original implementation cannot reasonably be preserved.

## Issues

When reporting an issue, please include:

- Minecraft version
- NeoForge version
- Port version or commit
- Installed mods
- Relevant logs or crash reports
- Steps to reproduce the problem

Please check whether the issue is already known before submitting a duplicate report.

## Upstream Project

This project is based on the original **Tinkers' Construct** project by **SlimeKnights**.

Tinkers' Construct and its original source code can be found on GitHub:

SlimeKnights/TinkersConstruct

Please report issues specific to this port **here**, not to the upstream Tinkers' Construct developers.

## License

This project is distributed under the **MIT License**.

Portions of this project are derived from Tinkers' Construct. Original copyright notices and license information are retained as required by the MIT License.

See `LICENSE` for details.

## Disclaimer

This project is an independent, unofficial port.

It is not an official Minecraft product, and it is not approved by or associated with Mojang Studios, Microsoft, SlimeKnights, or the original Tinkers' Construct maintainers.
