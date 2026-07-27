package io.github.brainage04.maxvillagertrades.neoforge;

import io.github.brainage04.maxvillagertrades.MaxVillagerTrades;
import io.github.brainage04.maxvillagertrades.MaxVillagerTradesNeoForgeGameTests;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.registries.RegisterEvent;

@EventBusSubscriber(modid = MaxVillagerTrades.MOD_ID)
public final class MaxVillagerTradesNeoForgeGameTestRegistration {
	private MaxVillagerTradesNeoForgeGameTestRegistration() {
	}

	@SubscribeEvent
	public static void registerTestFunctions(RegisterEvent event) {
		MaxVillagerTradesNeoForgeGameTests tests = new MaxVillagerTradesNeoForgeGameTests();
		event.register(BuiltInRegistries.TEST_FUNCTION.key(), Identifier.fromNamespaceAndPath(MaxVillagerTrades.MOD_ID, "generated_villager_trades_are_maxed"), () -> tests::generatedVillagerTradesAreMaxed);
		event.register(BuiltInRegistries.TEST_FUNCTION.key(), Identifier.fromNamespaceAndPath(MaxVillagerTrades.MOD_ID, "gamerules_independently_control_trade_outputs"), () -> tests::gamerulesIndependentlyControlBookAndItemTradeOutputs);
	}
}
