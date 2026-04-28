package io.github.brainage04.maxvillagertrades;

import net.minecraft.SharedConstants;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.level.gamerules.GameRules;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaxVillagerTradesTest {
	@BeforeAll
	static void bootstrapMinecraft() throws ClassNotFoundException {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Class.forName(MaxVillagerTrades.class.getName());
	}

	@Test
	void registersCustomGameRulesWithEnabledDefaults() {
		GameRules rules = new GameRules(FeatureFlags.DEFAULT_FLAGS);
		assertTrue(rules.get(MaxVillagerTrades.MAX_ENCHANTED_BOOK_TRADES));
		assertTrue(rules.get(MaxVillagerTrades.MAX_ENCHANTED_ITEM_TRADES));
	}

	@Test
	void formatsTradeModificationLog() {
		assertEquals(
				"Modified enchants for Trade 2 with Novice Librarian Villager - minecraft:sharpness 1 -> minecraft:sharpness 5",
				MaxVillagerTrades.buildTradeModificationLog(
						new MaxVillagerTrades.TradeContext(VillagerProfession.LIBRARIAN, 1, 2),
						List.of(new MaxVillagerTrades.EnchantmentChange("minecraft:sharpness", 1, 5))
				)
		);
	}
}
