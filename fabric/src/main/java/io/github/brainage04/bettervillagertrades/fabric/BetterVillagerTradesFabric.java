package io.github.brainage04.bettervillagertrades.fabric;

import io.github.brainage04.bettervillagertrades.BetterVillagerTrades;
import io.github.brainage04.bettervillagertrades.BetterVillagerTradesCommands;
import io.github.brainage04.bettervillagertrades.TradeRerollService;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.gamerules.GameRuleCategory;

public final class BetterVillagerTradesFabric implements ModInitializer {
	@Override
	public void onInitialize() {
		BetterVillagerTrades.initialize((name, defaultValue) -> GameRuleBuilder.forBoolean(defaultValue)
				.category(GameRuleCategory.MISC)
				.buildAndRegister(Identifier.fromNamespaceAndPath(BetterVillagerTrades.MOD_ID, name)));
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				BetterVillagerTradesCommands.register(dispatcher));
		UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) ->
				TradeRerollService.interact(player, entity, hand));
	}
}
