# Third-Party Licenses

This document lists all third-party dependencies used in Just Coordinates and their respective licenses.

## Runtime Dependencies

### Minecraft & Core Framework

#### Minecraft
- **Project**: Minecraft Java Edition
- **Versions**: 1.21.1, 1.20.1
- **Developer**: Mojang Studios
- **License**: Minecraft EULA
- **URL**: https://www.minecraft.net/

#### Fabric Loader (Fabric)
- **Project**: Fabric Loader
- **Version**: 0.16.10
- **Organization**: FabricMC
- **License**: Apache License 2.0
- **URL**: https://github.com/FabricMC/fabric-loader
- **License URL**: https://github.com/FabricMC/fabric-loader/blob/master/LICENSE

#### Fabric API (Fabric)
- **Project**: Fabric API
- **Versions**: 0.116.7+1.21.1, 0.92.6+1.20.1
- **Organization**: FabricMC
- **License**: Apache License 2.0
- **URL**: https://github.com/FabricMC/fabric
- **License URL**: https://github.com/FabricMC/fabric/blob/master/LICENSE

#### NeoForge (NeoForge, 1.21.1)
- **Project**: NeoForge
- **Version**: 21.1.219
- **Organization**: NeoForged
- **License**: LGPL-2.1-only
- **URL**: https://github.com/neoforged/NeoForge
- **License URL**: https://github.com/neoforged/NeoForge/blob/1.21.x/LICENSE.txt
- **Note**: Projects using NeoForge's APIs are not required to be licensed under LGPL-2.1

#### MinecraftForge (Forge, 1.20.1)
- **Project**: MinecraftForge
- **Version**: 1.20.1-47.4.10
- **Organization**: MinecraftForge
- **License**: LGPL-2.1-only
- **URL**: https://github.com/MinecraftForge/MinecraftForge
- **License URL**: https://github.com/MinecraftForge/MinecraftForge/blob/1.20.x/LICENSE.txt
- **Note**: Projects using MinecraftForge's APIs are not required to be licensed under LGPL-2.1

## License Summaries

### Apache License 2.0
Permissive license that allows commercial use, modification, distribution, and private use. Requires preservation of copyright and license notices.

**Used by**: Fabric Loader, Fabric API

### LGPL-2.1-only
Copyleft license that requires derivative works to be licensed under LGPL-2.1. However, linking to libraries licensed under LGPL-2.1 does not require the linking code to be licensed under LGPL-2.1.

**Used by**: NeoForge, MinecraftForge

## Notes

- All dependencies are used in compliance with their respective licenses
- Runtime dependencies are not bundled with Just Coordinates; users must install them separately
- NeoForge and MinecraftForge's LGPL-2.1 license does not affect Just Coordinates' license due to linking exception

## License Compliance

Just Coordinates is licensed under the LGPL-3.0-only License. All dependencies are compatible with this license:
- **Permissive licenses** (Apache 2.0): Fully compatible
- **LGPL licenses** (LGPL-2.1): Compatible due to dynamic linking (no license propagation)

For questions about licensing, please contact the project maintainer.

---

Last updated: 2026-02-10
