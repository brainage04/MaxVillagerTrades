package io.github.brainage04.bettervillagertrades;

import io.github.brainage04.bettervillagertrades.fabric.BetterVillagerTradesFabric;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class BetterVillagerTradesTest {
	@BeforeAll
	static void bootstrapMinecraft() throws ClassNotFoundException {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		new BetterVillagerTradesFabric().onInitialize();
	}

	@Test
	void registersCustomGameRulesWithEnabledDefaults() {
		GameRules rules = new GameRules(FeatureFlags.DEFAULT_FLAGS);
		assertTrue(rules.get(BetterVillagerTrades.MAX_ENCHANTED_BOOK_TRADES));
		assertTrue(rules.get(BetterVillagerTrades.MAX_ENCHANTED_ITEM_TRADES));
	}

	@Test
	void formatsTradeModificationLog() {
		assertEquals(
				"Modified enchants for Trade 2 with Novice Librarian Villager - minecraft:sharpness 1 -> minecraft:sharpness 5",
				BetterVillagerTrades.buildTradeModificationLog(
						new BetterVillagerTrades.TradeContext(VillagerProfession.LIBRARIAN, 1, 2),
						List.of(new BetterVillagerTrades.EnchantmentChange("minecraft:sharpness", 1, 5))
				)
		);
	}

	@Test
	void keepsTradeFiltersIsolatedPerPlayer() {
		PlayerTradeFilters filters = new PlayerTradeFilters();
		TradeFilter mendingBook = new TradeFilter(
				BuiltInRegistries.ITEM.getKey(Items.ENCHANTED_BOOK),
				Optional.of(BetterVillagerTrades.of("mending"))
		);
		UUID firstPlayer = UUID.randomUUID();
		UUID secondPlayer = UUID.randomUUID();

		assertTrue(filters.add(firstPlayer, mendingBook));
		assertFalse(filters.add(firstPlayer, mendingBook));
		assertEquals(List.of(mendingBook), filters.get(firstPlayer));
		assertTrue(filters.get(secondPlayer).isEmpty());
		assertTrue(filters.remove(firstPlayer, mendingBook));
		assertTrue(filters.get(firstPlayer).isEmpty());
	}
}
