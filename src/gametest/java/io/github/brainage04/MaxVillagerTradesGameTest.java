package io.github.brainage04;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.fabricmc.fabric.api.gametest.v1.GameTest;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerData;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;

import java.util.Optional;

public final class MaxVillagerTradesGameTest {
	@GameTest(maxTicks = 40)
	public void villagerLevelUpGeneratesMaxedBookAndItemTrades(GameTestHelper helper) {
		helper.runAtTickTime(1, () -> {
			ServerLevel level = helper.getLevel();

			Int2ObjectMap<VillagerTrades.ItemListing[]> librarianTrades = VillagerTrades.TRADES.get(VillagerProfession.LIBRARIAN);
			Int2ObjectMap<VillagerTrades.ItemListing[]> weaponsmithTrades = VillagerTrades.TRADES.get(VillagerProfession.WEAPONSMITH);
			Int2ObjectMap<VillagerTrades.ItemListing[]> librarianExperimentalTrades = VillagerTrades.EXPERIMENTAL_TRADES.get(VillagerProfession.LIBRARIAN);
			Int2ObjectMap<VillagerTrades.ItemListing[]> weaponsmithExperimentalTrades = VillagerTrades.EXPERIMENTAL_TRADES.get(VillagerProfession.WEAPONSMITH);

			VillagerTrades.ItemListing[] originalLibrarianLevel2 = librarianTrades.get(2);
			VillagerTrades.ItemListing[] originalWeaponsmithLevel5 = weaponsmithTrades.get(5);
			VillagerTrades.ItemListing[] originalExperimentalLibrarianLevel2 = librarianExperimentalTrades == null ? null : librarianExperimentalTrades.get(2);
			VillagerTrades.ItemListing[] originalExperimentalWeaponsmithLevel5 = weaponsmithExperimentalTrades == null ? null : weaponsmithExperimentalTrades.get(5);

			VillagerTrades.ItemListing[] librarianLevelUpTrade = new VillagerTrades.ItemListing[]{createLevelUpBookTrade(level)};
			VillagerTrades.ItemListing[] weaponsmithLevelUpTrade = new VillagerTrades.ItemListing[]{createLevelUpItemTrade(level)};

			librarianTrades.put(2, librarianLevelUpTrade);
			weaponsmithTrades.put(5, weaponsmithLevelUpTrade);

			if (librarianExperimentalTrades != null) librarianExperimentalTrades.put(2, librarianLevelUpTrade);
			if (weaponsmithExperimentalTrades != null) weaponsmithExperimentalTrades.put(5, weaponsmithLevelUpTrade);

			MaxVillagerTrades.overrideVillagerTradeOffers(VillagerTrades.TRADES);
			MaxVillagerTrades.overrideVillagerTradeOffers(VillagerTrades.EXPERIMENTAL_TRADES);

			assertLibrarianLevelUpTradeIsMaxed(helper, level);
			assertWeaponsmithLevelUpTradeIsMaxed(helper, level);

			librarianTrades.put(2, originalLibrarianLevel2);
			weaponsmithTrades.put(5, originalWeaponsmithLevel5);

			if (librarianExperimentalTrades != null) librarianExperimentalTrades.put(2, originalExperimentalLibrarianLevel2);
			if (weaponsmithExperimentalTrades != null) weaponsmithExperimentalTrades.put(5, originalExperimentalWeaponsmithLevel5);

			helper.succeed();
		});
	}

	private static VillagerTrades.ItemListing createLevelUpBookTrade(ServerLevel level) {
		Holder<Enchantment> sharpness = level.registryAccess()
				.lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(Enchantments.SHARPNESS);
		ItemStack book = EnchantmentHelper.createBook(new EnchantmentInstance(sharpness, 1));
		return (serverLevel, entity, random) -> new MerchantOffer(
				new ItemCost(Items.EMERALD, 12),
				Optional.of(new ItemCost(Items.BOOK)),
				book.copy(),
				12,
				1,
				0.2F
		);
	}

	private static VillagerTrades.ItemListing createLevelUpItemTrade(ServerLevel level) {
		Holder<Enchantment> sharpness = level.registryAccess()
				.lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(Enchantments.SHARPNESS);
		return (serverLevel, entity, random) -> {
			ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
			sword.enchant(sharpness, 1);
			return new MerchantOffer(new ItemCost(Items.EMERALD, 40), sword, 3, 30, 0.05F);
		};
	}

	private static void assertLibrarianLevelUpTradeIsMaxed(GameTestHelper helper, ServerLevel level) {
		Holder<Enchantment> sharpness = level.registryAccess()
				.lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(Enchantments.SHARPNESS);
		Villager villager = createVillager(helper, level, VillagerProfession.LIBRARIAN, 1, 1, 2, 1);

		villager.getOffers();
		villager.increaseMerchantCareer(level);

		boolean foundMaxedBook = villager.getOffers().stream().anyMatch(offer -> {
			ItemEnchantments stored = offer.getResult().getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
			return stored.getLevel(sharpness) == sharpness.value().getMaxLevel();
		});

		helper.assertTrue(villager.getVillagerData().level() == 2, "Librarian should level up to apprentice");
		helper.assertTrue(foundMaxedBook, "Expected level-up to add a maxed enchanted book trade");
	}

	private static void assertWeaponsmithLevelUpTradeIsMaxed(GameTestHelper helper, ServerLevel level) {
		Holder<Enchantment> sharpness = level.registryAccess()
				.lookupOrThrow(Registries.ENCHANTMENT)
				.getOrThrow(Enchantments.SHARPNESS);
		Villager villager = createVillager(helper, level, VillagerProfession.WEAPONSMITH, 4, 3, 2, 1);

		villager.getOffers();
		villager.increaseMerchantCareer(level);

		boolean foundMaxedSword = villager.getOffers().stream().anyMatch(offer -> {
			ItemStack result = offer.getResult();
			ItemEnchantments enchants = result.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
			return result.is(Items.DIAMOND_SWORD) && enchants.getLevel(sharpness) == sharpness.value().getMaxLevel();
		});

		helper.assertTrue(villager.getVillagerData().level() == 5, "Weaponsmith should level up to master");
		helper.assertTrue(foundMaxedSword, "Expected level-up to add a maxed enchanted item trade");
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
