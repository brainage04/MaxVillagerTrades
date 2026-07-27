package io.github.brainage04.maxvillagertrades.fabric;

import io.github.brainage04.maxvillagertrades.MaxVillagerTrades;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRuleCategory;

public final class MaxVillagerTradesFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		MaxVillagerTrades.initialize((name, defaultValue) -> GameRuleBuilder.forBoolean(defaultValue)
				.category(GameRuleCategory.MISC)
				.buildAndRegister(Identifier.fromNamespaceAndPath(MaxVillagerTrades.MOD_ID, name)));
	}
}
