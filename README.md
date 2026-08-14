
# Zap's Model Only Resources (ZMOR)

Zap's Model Only Resources (ZMOR) is a client-side Fabric mod for Minecraft that tries to do cosmetics using resource packs, that gives you complete control over how custom resource packs and item textures are rendered. It allows you to selectively apply custom resource pack armor, items, and models to your local character while keeping clean default textures for other players, mobs, armor stands, and item frames.

---

## Prerequisites
- Fabric API
- Mod Menu (For Configuration Of the Mod)
---
## Features
- Local-Only Texture Isolation
- Item Whitelist & Catalog
- Per-Item Resource Pack Assignment
- Main Override & Fallback Base Pack Selectors
- Peer Texture Synchronization (Experimental)

---

## Supported Versions

- 1.21.11
- 26.1.x
- 26.2
---

## Configuration

Access the configuration interface in-game via **Mod Menu** -> **ZMOR**.

### Configuration Options
* **Other Players**: Enable or disable custom textures rendering on other players.
* **Mobs & Armor Stands**: Enable or disable custom armor on mobs and armor stands.
* **Item Frames**: Enable or disable custom textures in item frames.
* **Sync Peer Packs**: Enable or disable receiving custom pack textures from peers.
* **Item Whitelist Filter**: Manage which items are affected by custom texture isolation.
* **Main Override Pack**: Choose the primary custom resource pack source.
* **Fallback Base Pack**: Choose the fallback texture pack for non-local entities.

---

## License

Distributed under the [**LGPLv3 License**](https://github.com/Zapaxe/Z/blob/main/LICENSE). Created by [**Zapaxe**](https://github.com/Zapaxe).
