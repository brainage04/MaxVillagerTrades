package io.github.brainage04.bettervillagertrades;

import net.minecraft.server.level.ServerLevel;

/** Implemented on villagers by each loader's mixin. */
public interface VillagerTradeReroller {
	void betterVillagerTrades$rerollTrades(ServerLevel level);
}
