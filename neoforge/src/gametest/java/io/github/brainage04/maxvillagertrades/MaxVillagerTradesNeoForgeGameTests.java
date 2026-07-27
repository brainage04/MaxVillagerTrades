package io.github.brainage04.maxvillagertrades;

import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

public final class MaxVillagerTradesNeoForgeGameTests {
	public void generatedVillagerTradesAreMaxed(GameTestHelper helper) {
		helper.runAtTickTime(1, () -> {
			ServerLevel level = helper.getLevel();
			boolean foundBookTrade = generatedTradesHaveOnlyMaxedEnchantments(helper, level, VillagerProfession.LIBRARIAN, 1, DataComponents.STORED_ENCHANTMENTS);
			boolean foundItemTrade = generatedTradesHaveOnlyMaxedEnchantments(helper, level, VillagerProfession.WEAPONSMITH, 5, DataComponents.ENCHANTMENTS);
			helper.assertTrue(foundBookTrade, "Expected at least one generated enchanted book trade");
			helper.assertTrue(foundItemTrade, "Expected at least one generated enchanted item trade");
			helper.succeed();
		});
	}

	public void gamerulesIndependentlyControlBookAndItemTradeOutputs(GameTestHelper helper) {
		ServerLevel level = helper.getLevel();
		var enchantmentRegistry = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
		Holder<Enchantment> sharpness = enchantmentRegistry.wrapAsHolder(enchantmentRegistry.getValueOrThrow(Enchantments.SHARPNESS));
		ItemStack book = enchantedStack(new ItemStack(Items.ENCHANTED_BOOK), DataComponents.STORED_ENCHANTMENTS, sharpness);
		ItemStack sword = enchantedStack(new ItemStack(Items.DIAMOND_SWORD), DataComponents.ENCHANTMENTS, sharpness);
		MerchantOffer bookOffer = new MerchantOffer(new ItemCost(Items.EMERALD), book, 1, 1, 0.0F);
		MerchantOffer swordOffer = new MerchantOffer(new ItemCost(Items.EMERALD), sword, 1, 1, 0.0F);
		MaxVillagerTrades.maximizeTradeOffer(bookOffer, false, true);
		MaxVillagerTrades.maximizeTradeOffer(swordOffer, true, false);
		helper.assertTrue(enchantmentLevel(bookOffer, DataComponents.STORED_ENCHANTMENTS, sharpness) == 1, "The book gamerule must leave enchanted books unchanged when disabled");
		helper.assertTrue(enchantmentLevel(swordOffer, DataComponents.ENCHANTMENTS, sharpness) == 1, "The item gamerule must leave enchanted items unchanged when disabled");
		MaxVillagerTrades.maximizeTradeOffer(bookOffer, true, false);
		MaxVillagerTrades.maximizeTradeOffer(swordOffer, false, true);
		helper.assertTrue(enchantmentLevel(bookOffer, DataComponents.STORED_ENCHANTMENTS, sharpness) == sharpness.value().getMaxLevel(), "The book gamerule must maximize enchanted books when enabled");
		helper.assertTrue(enchantmentLevel(swordOffer, DataComponents.ENCHANTMENTS, sharpness) == sharpness.value().getMaxLevel(), "The item gamerule must maximize enchanted items when enabled");
		helper.succeed();
	}

	private static ItemStack enchantedStack(ItemStack stack, DataComponentType<ItemEnchantments> componentType, Holder<Enchantment> enchantment) {
		ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
		enchantments.set(enchantment, 1);
		stack.set(componentType, enchantments.toImmutable());
		return stack;
	}

	private static int enchantmentLevel(MerchantOffer offer, DataComponentType<ItemEnchantments> componentType, Holder<Enchantment> enchantment) {
		return offer.getResult().getOrDefault(componentType, ItemEnchantments.EMPTY).getLevel(enchantment);
	}

	private static boolean generatedTradesHaveOnlyMaxedEnchantments(GameTestHelper helper, ServerLevel level, net.minecraft.resources.ResourceKey<VillagerProfession> profession, int professionLevel, DataComponentType<ItemEnchantments> componentType) {
		boolean foundEnchantments = false;
		for (int i = 0; i < 20; i++) {
			Villager villager = createVillager(helper, level, profession, professionLevel, 1 + (i % 5), 2, 1 + (i / 5));
			for (MerchantOffer offer : villager.getOffers()) {
				ItemEnchantments enchantments = offer.getResult().getOrDefault(componentType, ItemEnchantments.EMPTY);
				if (enchantments.isEmpty()) continue;
				foundEnchantments = true;
				for (Holder<Enchantment> enchantment : enchantments.keySet()) helper.assertTrue(enchantments.getLevel(enchantment) == enchantment.value().getMaxLevel(), "Generated villager trade enchantment should be max level");
			}
		}
		return foundEnchantments;
	}

	private static Villager createVillager(GameTestHelper helper, ServerLevel level, net.minecraft.resources.ResourceKey<VillagerProfession> profession, int levelNumber, int x, int y, int z) {
		Villager villager = helper.spawn(EntityTypes.VILLAGER, x, y, z);
		VillagerData villagerData = villager.getVillagerData().withProfession(level.registryAccess(), profession).withLevel(levelNumber);
		villager.setVillagerData(villagerData);
		return villager;
	}
}
