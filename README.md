# About

This mod modifies villager trade factories so that all enchantments contained
within villager trade outputs will always be max level.

This applies to both stored enchantments on Enchanted Books and regular
enchantments on enchanted equipment. Vanilla trades and enchantments added by
datapacks/mods are supported, assuming the added enchantments are implemented
correctly.

Existing trades that have been used at least once will not be affected,
only newly generated villager trades and new trades unlocked when villagers
level up.


## Loaders

Fabric and NeoForge builds are provided. This is a server-side mod, so vanilla clients can join servers that use it.

## Migrating from the Fabric-only release

Install exactly one matching JAR: the Fabric JAR on Fabric (with Fabric API), or the NeoForge JAR on NeoForge. Remove the previous JAR before switching loaders; do not install both variants together. The mod ID remains `maxvillagertrades`, so its gamerules and existing world data keep their established paths. Install it only on the server—clients, including vanilla clients, do not need a mod JAR. Building the root project emits both loader artifacts under `build/libs`.

# Gamerules

Two boolean gamerules control the behavior:

- `max_enchanted_book_trades` toggles max-level stored enchantments on enchanted books.
- `max_enchanted_item_trades` toggles max-level regular enchantments on enchanted items.

Both default to `true`.
