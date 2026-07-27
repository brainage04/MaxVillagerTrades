# BetterVillagerTrades

BetterVillagerTrades improves villager trading without requiring modded clients:

- Newly generated enchanted-book and enchanted-item outputs use each enchantment's maximum level.
- Players can reroll an eligible villager's offers.
- Persistent, per-player filters prevent rerolls when the current offers contain a desired item or enchantment.

Vanilla and modded trade factories are supported as long as their outputs use standard item enchantment components.

## Rerolling trades

A villager can be rerolled only when all of these conditions are true:

- It is an adult with a profession.
- It is still a novice with zero trade experience.
- None of its offers have been used.
- None of its current offers match one of the requesting player's filters.

Vanilla clients can sneak-right-click an eligible villager while holding an emerald. The emerald is not consumed. Players with BetterVillagerTrades installed on the client also receive **Reroll trades** and **Toggle filter** buttons beneath the standard trading screen.

## Per-player filters

The trading-screen filter button toggles protection for the selected offer. Enchanted outputs are protected by item and enchantment; other outputs are protected by item.

The same filters can be managed without a client mod:

```text
/bettervillagertrades filter add <item> [enchantment]
/bettervillagertrades filter remove <item> [enchantment]
/bettervillagertrades filter toggle <item> [enchantment]
/bettervillagertrades filter list
/bettervillagertrades filter clear
```

For example, `/bettervillagertrades filter add minecraft:enchanted_book minecraft:mending` protects any Mending book offered by the villager. Filters are stored per player in the world save; one player's filters never become global rules.

`/bettervillagertrades reroll` is the command used by the optional trading-screen button and requires an open villager trading session.

## Gamerules

Two boolean gamerules retain independent control of maximum-level outputs:

- `max_enchanted_book_trades` controls stored enchantments on enchanted books.
- `max_enchanted_item_trades` controls regular enchantments on enchanted items.

Both default to `true`.

## Loaders and installation

Fabric and NeoForge builds are provided for Minecraft 26.2.

Install exactly one matching server JAR: the Fabric JAR with Fabric API, or the NeoForge JAR with NeoForge. Vanilla clients can join. Installing the matching mod on a client is optional and only adds the trading-screen controls.

This release replaces MaxVillagerTrades. Remove the old JAR before installing BetterVillagerTrades; the new mod ID is `bettervillagertrades`.

Run `./gradlew build` to emit both loader artifacts under `build/libs`.
