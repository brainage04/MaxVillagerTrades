package io.github.brainage04;

import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaxLibrarianTradesTest {
	private static HolderLookup.Provider vanillaLookup;

	@BeforeAll
	static void bootstrapMinecraft() {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		vanillaLookup = VanillaRegistries.createLookup();
	}

	@Test
	void maximizesStoredEnchantmentsForNoviceLibrarianTrade() {
		Holder<Enchantment> sharpness = enchantment(Enchantments.SHARPNESS);
		ItemStack book = EnchantmentHelper.createBook(new EnchantmentInstance(sharpness, 1));
		MerchantOffer offer = new MerchantOffer(
				new ItemCost(Items.EMERALD, 12),
				Optional.of(new ItemCost(Items.BOOK)),
				book,
				12,
				1,
				0.2F
		);

		MaxLibrarianTrades.TradeModification modification = MaxLibrarianTrades.maximizeTradeOffer(
				offer,
				new MaxLibrarianTrades.TradeContext(VillagerProfession.LIBRARIAN, 1, 2)
		);

		ItemEnchantments stored = modification.offer().getResult().getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		assertEquals(sharpness.value().getMaxLevel(), stored.getLevel(sharpness));
		assertEquals(
				"Modified enchants for Trade 2 with Novice Librarian Villager - minecraft:sharpness 1 -> minecraft:sharpness 5",
				MaxLibrarianTrades.buildTradeModificationLog(
						new MaxLibrarianTrades.TradeContext(VillagerProfession.LIBRARIAN, 1, 2),
						modification.changes()
				)
		);
	}

	@Test
	void maximizesRegularEnchantmentsForMasterSmithTrades() {
		assertRegularEnchantedTradeIsMaxed(VillagerProfession.WEAPONSMITH, Items.DIAMOND_SWORD, Enchantments.SHARPNESS);
		assertRegularEnchantedTradeIsMaxed(VillagerProfession.ARMORER, Items.DIAMOND_CHESTPLATE, Enchantments.PROTECTION);
		assertRegularEnchantedTradeIsMaxed(VillagerProfession.TOOLSMITH, Items.DIAMOND_PICKAXE, Enchantments.EFFICIENCY);
	}

	@Test
	void vanillaTradePoolsStillContainExpectedBookAndSmithListings() throws ReflectiveOperationException {
		assertTrue(
				Arrays.stream(VillagerTrades.TRADES.get(VillagerProfession.LIBRARIAN).get(1))
						.anyMatch(listing -> listing.getClass().getSimpleName().equals("EnchantBookForEmeralds")),
				"Novice librarians should still have an enchanted book listing"
		);

		assertHasEnchantedEquipmentListing(VillagerProfession.WEAPONSMITH);
		assertHasEnchantedEquipmentListing(VillagerProfession.ARMORER);
		assertHasEnchantedEquipmentListing(VillagerProfession.TOOLSMITH);
	}

	@Test
	void leavesAlreadyMaxedOffersUnchanged() {
		Holder<Enchantment> efficiency = enchantment(Enchantments.EFFICIENCY);
		ItemStack tool = new ItemStack(Items.DIAMOND_PICKAXE);
		tool.enchant(efficiency, efficiency.value().getMaxLevel());
		MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 32), tool, 3, 15, 0.05F);

		MaxLibrarianTrades.TradeModification modification = MaxLibrarianTrades.maximizeTradeOffer(
				offer,
				new MaxLibrarianTrades.TradeContext(VillagerProfession.TOOLSMITH, 5, 1)
		);

		assertFalse(modification.modified());
		assertTrue(modification.changes().isEmpty());
		assertEquals(offer.getResult().getEnchantments(), modification.offer().getResult().getEnchantments());
	}

	private static void assertRegularEnchantedTradeIsMaxed(
			ResourceKey<VillagerProfession> profession,
			net.minecraft.world.item.Item item,
			ResourceKey<Enchantment> enchantmentKey
	) {
		Holder<Enchantment> enchantment = enchantment(enchantmentKey);
		ItemStack itemStack = new ItemStack(item);
		itemStack.enchant(enchantment, 1);
		MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 40), itemStack, 3, 30, 0.05F);

		MaxLibrarianTrades.TradeModification modification = MaxLibrarianTrades.maximizeTradeOffer(
				offer,
				new MaxLibrarianTrades.TradeContext(profession, 5, 1)
		);

		ItemEnchantments applied = modification.offer().getResult().getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		assertEquals(enchantment.value().getMaxLevel(), applied.getLevel(enchantment));
		assertFalse(modification.changes().isEmpty());
	}

	private static void assertHasEnchantedEquipmentListing(ResourceKey<VillagerProfession> profession) throws ReflectiveOperationException {
		List<VillagerTrades.ItemListing> listings = List.of(VillagerTrades.TRADES.get(profession).get(5));
		boolean found = false;
		for (VillagerTrades.ItemListing listing : listings) {
			String simpleName = listing.getClass().getSimpleName();
			if (simpleName.equals("EnchantedItemForEmeralds")) {
				found = true;
				break;
			}

			if (!simpleName.equals("ItemsForEmeralds")) {
				continue;
			}

			Field enchantmentProvider = listing.getClass().getDeclaredField("enchantmentProvider");
			enchantmentProvider.setAccessible(true);
			Optional<?> provider = assertInstanceOf(Optional.class, enchantmentProvider.get(listing));
			if (provider.isEmpty()) {
				continue;
			}

			Field itemStackField = listing.getClass().getDeclaredField("itemStack");
			itemStackField.setAccessible(true);
			ItemStack itemStack = assertInstanceOf(ItemStack.class, itemStackField.get(listing));
			found = itemStack.is(Items.DIAMOND_CHESTPLATE) || itemStack.is(Items.DIAMOND_SWORD) || itemStack.is(Items.DIAMOND_PICKAXE);
			if (found) {
				break;
			}
		}

		assertTrue(found, () -> "Expected an enchanted diamond equipment trade for " + profession.identifier());
	}

	private static Holder<Enchantment> enchantment(ResourceKey<Enchantment> enchantmentKey) {
		return vanillaLookup.lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).getOrThrow(enchantmentKey);
	}
}
