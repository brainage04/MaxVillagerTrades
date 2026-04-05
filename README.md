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

# Gamerules

Two boolean gamerules control the behavior:

- `max_enchanted_book_trades` toggles max-level stored enchantments on enchanted books.
- `max_enchanted_item_trades` toggles max-level regular enchantments on enchanted items.

Both default to `true`.
