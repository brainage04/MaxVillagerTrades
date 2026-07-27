package io.github.brainage04.maxvillagertrades.neoforge;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.serialization.Codec;
import io.github.brainage04.maxvillagertrades.MaxVillagerTrades;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

@Mod(MaxVillagerTrades.MOD_ID)
public final class MaxVillagerTradesNeoForge {
	private static final DeferredRegister<GameRule<?>> GAME_RULES = DeferredRegister.create(Registries.GAME_RULE, MaxVillagerTrades.MOD_ID);
	private static final DeferredHolder<GameRule<?>, GameRule<Boolean>> MAX_ENCHANTED_BOOK_TRADES = registerBoolean("max_enchanted_book_trades");
	private static final DeferredHolder<GameRule<?>, GameRule<Boolean>> MAX_ENCHANTED_ITEM_TRADES = registerBoolean("max_enchanted_item_trades");

	public MaxVillagerTradesNeoForge(IEventBus modEventBus) {
		GAME_RULES.register(modEventBus);
		modEventBus.addListener((FMLCommonSetupEvent event) -> MaxVillagerTrades.initialize(this::gameRule));
	}

	private static DeferredHolder<GameRule<?>, GameRule<Boolean>> registerBoolean(String name) {
		return GAME_RULES.register(name, () -> new GameRule<>(
				GameRuleCategory.MISC,
				GameRuleType.BOOL,
				BoolArgumentType.bool(),
				(visitor, rule) -> visitor.visitBoolean(rule),
				Codec.BOOL,
				value -> value ? 1 : 0,
				true,
				FeatureFlagSet.of()
		));
	}

	private GameRule<Boolean> gameRule(String name, boolean defaultValue) {
		return switch (name) {
			case "max_enchanted_book_trades" -> MAX_ENCHANTED_BOOK_TRADES.get();
			case "max_enchanted_item_trades" -> MAX_ENCHANTED_ITEM_TRADES.get();
			default -> throw new IllegalArgumentException("Unknown MaxVillagerTrades gamerule: " + name);
		};
	}
}
