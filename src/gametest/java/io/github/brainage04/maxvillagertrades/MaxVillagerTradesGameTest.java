package io.github.brainage04.maxvillagertrades;

import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;

public final class MaxVillagerTradesGameTest {
	@GameTest(maxTicks = 40)
	public void generatedVillagerTradesAreMaxed(GameTestHelper helper) {
		helper.runAtTickTime(1, () -> {
			ServerLevel level = helper.getLevel();

			boolean foundBookTrade = generatedTradesHaveOnlyMaxedEnchantments(
					helper,
					level,
					VillagerProfession.LIBRARIAN,
					1,
					DataComponents.STORED_ENCHANTMENTS
			);
			boolean foundItemTrade = generatedTradesHaveOnlyMaxedEnchantments(
					helper,
					level,
					VillagerProfession.WEAPONSMITH,
					5,
					DataComponents.ENCHANTMENTS
			);

			helper.assertTrue(foundBookTrade, "Expected at least one generated enchanted book trade");
			helper.assertTrue(foundItemTrade, "Expected at least one generated enchanted item trade");
			helper.succeed();
		});
	}

	private static boolean generatedTradesHaveOnlyMaxedEnchantments(
			GameTestHelper helper,
			ServerLevel level,
			net.minecraft.resources.ResourceKey<VillagerProfession> profession,
			int professionLevel,
			net.minecraft.core.component.DataComponentType<ItemEnchantments> componentType
	) {
		boolean foundEnchantments = false;

		for (int i = 0; i < 20; i++) {
			Villager villager = createVillager(helper, level, profession, professionLevel, 1 + (i % 5), 2, 1 + (i / 5));

			for (MerchantOffer offer : villager.getOffers()) {
				ItemEnchantments enchantments = offer.getResult().getOrDefault(componentType, ItemEnchantments.EMPTY);
				if (enchantments.isEmpty()) {
					continue;
				}

				foundEnchantments = true;
				for (Holder<Enchantment> enchantment : enchantments.keySet()) {
					helper.assertTrue(
							enchantments.getLevel(enchantment) == enchantment.value().getMaxLevel(),
							"Generated villager trade enchantment should be max level"
					);
				}
			}
		}

		return foundEnchantments;
	}

	private static Villager createVillager(
			GameTestHelper helper,
			ServerLevel level,
			net.minecraft.resources.ResourceKey<VillagerProfession> profession,
			int levelNumber,
			int x,
			int y,
			int z
	) {
		Villager villager = helper.spawn(EntityType.VILLAGER, x, y, z);
		VillagerData villagerData = villager.getVillagerData()
				.withProfession(level.registryAccess(), profession)
				.withLevel(levelNumber);
		villager.setVillagerData(villagerData);
		return villager;
	}
}
