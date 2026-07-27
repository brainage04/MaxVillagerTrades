package io.github.brainage04.bettervillagertrades.neoforge;

import io.github.brainage04.bettervillagertrades.BetterVillagerTrades;
import io.github.brainage04.bettervillagertrades.BetterVillagerTradesNeoForgeGameTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = BetterVillagerTrades.MOD_ID)
public final class BetterVillagerTradesNeoForgeGameTestRegistration {
	private BetterVillagerTradesNeoForgeGameTestRegistration() {
	}

	@SubscribeEvent
	public static void registerTestFunctions(RegisterEvent event) {
		BetterVillagerTradesNeoForgeGameTests tests = new BetterVillagerTradesNeoForgeGameTests();
		event.register(BuiltInRegistries.TEST_FUNCTION.key(), Identifier.fromNamespaceAndPath(BetterVillagerTrades.MOD_ID, "generated_villager_trades_are_maxed"), () -> tests::generatedVillagerTradesAreMaxed);
		event.register(BuiltInRegistries.TEST_FUNCTION.key(), Identifier.fromNamespaceAndPath(BetterVillagerTrades.MOD_ID, "gamerules_independently_control_trade_outputs"), () -> tests::gamerulesIndependentlyControlBookAndItemTradeOutputs);
		event.register(BuiltInRegistries.TEST_FUNCTION.key(), Identifier.fromNamespaceAndPath(BetterVillagerTrades.MOD_ID, "rerolls_untraded_villagers_without_protected_offers"), () -> tests::rerollsUntradedVillagersWithoutProtectedOffers);
	}
}
