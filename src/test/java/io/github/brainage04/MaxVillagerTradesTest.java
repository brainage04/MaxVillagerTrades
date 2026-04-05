package io.github.brainage04;

import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.data.registries.VanillaRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.Bootstrap;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.gamerules.GameRules;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MaxVillagerTradesTest {
	private static HolderLookup.Provider vanillaLookup;

	@BeforeAll
	static void bootstrapMinecraft() throws ClassNotFoundException {
		SharedConstants.tryDetectVersion();
		Bootstrap.bootStrap();
		Class.forName(MaxVillagerTrades.class.getName());
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

		MaxVillagerTrades.TradeModification modification = MaxVillagerTrades.maximizeTradeOffer(
				offer,
				new MaxVillagerTrades.TradeContext(VillagerProfession.LIBRARIAN, 1, 2)
		);

		ItemEnchantments stored = modification.offer().getResult().getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		assertEquals(sharpness.value().getMaxLevel(), stored.getLevel(sharpness));
		assertEquals(
				"Modified enchants for Trade 2 with Novice Librarian Villager - minecraft:sharpness 1 -> minecraft:sharpness 5",
				MaxVillagerTrades.buildTradeModificationLog(
						new MaxVillagerTrades.TradeContext(VillagerProfession.LIBRARIAN, 1, 2),
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
	void registersCustomGameRulesWithEnabledDefaults() {
		GameRules rules = new GameRules(FeatureFlags.DEFAULT_FLAGS);
		assertTrue(rules.get(MaxVillagerTrades.MAX_ENCHANTED_BOOK_TRADES));
		assertTrue(rules.get(MaxVillagerTrades.MAX_ENCHANTED_ITEM_TRADES));
	}

	@Test
	void canDisableBookAndItemMaxingIndependently() {
		Holder<Enchantment> sharpness = enchantment(Enchantments.SHARPNESS);

		ItemStack book = EnchantmentHelper.createBook(new EnchantmentInstance(sharpness, 1));
		MerchantOffer bookOffer = new MerchantOffer(
				new ItemCost(Items.EMERALD, 12),
				Optional.of(new ItemCost(Items.BOOK)),
				book,
				12,
				1,
				0.2F
		);

		MaxVillagerTrades.TradeModification bookUnchanged = MaxVillagerTrades.maximizeTradeOffer(
				bookOffer,
				new MaxVillagerTrades.TradeContext(VillagerProfession.LIBRARIAN, 1, 2),
				false,
				true
		);
		assertEquals(1, bookUnchanged.offer().getResult().getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(sharpness));

		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
		sword.enchant(sharpness, 1);
		MerchantOffer swordOffer = new MerchantOffer(new ItemCost(Items.EMERALD, 40), sword, 3, 30, 0.05F);

		MaxVillagerTrades.TradeModification itemUnchanged = MaxVillagerTrades.maximizeTradeOffer(
				swordOffer,
				new MaxVillagerTrades.TradeContext(VillagerProfession.WEAPONSMITH, 5, 1),
				true,
				false
		);
		assertEquals(1, itemUnchanged.offer().getResult().getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY).getLevel(sharpness));
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

		MaxVillagerTrades.TradeModification modification = MaxVillagerTrades.maximizeTradeOffer(
				offer,
				new MaxVillagerTrades.TradeContext(VillagerProfession.TOOLSMITH, 5, 1)
		);

		assertFalse(modification.modified());
		assertTrue(modification.changes().isEmpty());
		assertEquals(offer.getResult().getEnchantments(), modification.offer().getResult().getEnchantments());
	}

	@Test
	void wrapsTradesForAnyProfessionPresentInTheTradeMap() {
		ResourceKey<VillagerProfession> customProfession = ResourceKey.create(
				net.minecraft.core.registries.Registries.VILLAGER_PROFESSION,
				Identifier.parse("testmod:sage")
		);
		MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 1), new ItemStack(Items.BOOK), 12, 1, 0.05F);
		VillagerTrades.ItemListing listing = (level, entity, random) -> offer;
		Int2ObjectOpenHashMap<VillagerTrades.ItemListing[]> levels = new Int2ObjectOpenHashMap<>();
		levels.put(2, new VillagerTrades.ItemListing[]{listing});

		Map<ResourceKey<VillagerProfession>, it.unimi.dsi.fastutil.ints.Int2ObjectMap<VillagerTrades.ItemListing[]>> trades = new LinkedHashMap<>();
		trades.put(customProfession, levels);

		MaxVillagerTrades.overrideVillagerTradeOffers(trades);

		VillagerTrades.ItemListing wrapped = trades.get(customProfession).get(2)[0];
		assertEquals("WrappedTradeListing", wrapped.getClass().getSimpleName());

		MaxVillagerTrades.overrideVillagerTradeOffers(trades);
		assertEquals("WrappedTradeListing", trades.get(customProfession).get(2)[0].getClass().getSimpleName());
	}

	@Test
	void maximizesCustomStoredEnchantmentsForCustomProfessionTrade() {
		ResourceKey<VillagerProfession> customProfession = ResourceKey.create(
				net.minecraft.core.registries.Registries.VILLAGER_PROFESSION,
				Identifier.parse("testmod:sage")
		);
		Holder<Enchantment> customEnchantment = customEnchantment("sage_wisdom", 4, Items.BOOK);
		ItemStack book = EnchantmentHelper.createBook(new EnchantmentInstance(customEnchantment, 1));
		MerchantOffer offer = new MerchantOffer(
				new ItemCost(Items.EMERALD, 18),
				Optional.of(new ItemCost(Items.BOOK)),
				book,
				12,
				1,
				0.2F
		);

		MaxVillagerTrades.TradeModification modification = MaxVillagerTrades.maximizeTradeOffer(
				offer,
				new MaxVillagerTrades.TradeContext(customProfession, 3, 1)
		);

		ItemEnchantments stored = modification.offer().getResult().getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
		assertEquals(customEnchantment.value().getMaxLevel(), stored.getLevel(customEnchantment));
		assertTrue(modification.modified());
	}

	@Test
	void maximizesCustomItemEnchantmentsForCustomProfessionTrade() {
		ResourceKey<VillagerProfession> customProfession = ResourceKey.create(
				net.minecraft.core.registries.Registries.VILLAGER_PROFESSION,
				Identifier.parse("testmod:sage")
		);
		Holder<Enchantment> customEnchantment = customEnchantment("sage_edge", 4, Items.DIAMOND_SWORD);
		ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
		sword.enchant(customEnchantment, 1);
		MerchantOffer offer = new MerchantOffer(new ItemCost(Items.EMERALD, 40), sword, 3, 30, 0.05F);

		MaxVillagerTrades.TradeModification modification = MaxVillagerTrades.maximizeTradeOffer(
				offer,
				new MaxVillagerTrades.TradeContext(customProfession, 5, 1)
		);

		ItemEnchantments applied = modification.offer().getResult().getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY);
		assertEquals(customEnchantment.value().getMaxLevel(), applied.getLevel(customEnchantment));
		assertTrue(modification.modified());
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

		MaxVillagerTrades.TradeModification modification = MaxVillagerTrades.maximizeTradeOffer(
				offer,
				new MaxVillagerTrades.TradeContext(profession, 5, 1)
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

	private static Holder<Enchantment> customEnchantment(String path, int maxLevel, Item... supportedItems) {
		Enchantment enchantment = new Enchantment(
				Component.literal("Test " + path),
				Enchantment.definition(
						HolderSet.direct(Item::builtInRegistryHolder, supportedItems),
						1,
						maxLevel,
						Enchantment.constantCost(1),
						Enchantment.constantCost(1),
						1,
						EquipmentSlotGroup.ANY
				),
				HolderSet.empty(),
				DataComponentMap.EMPTY
		);
		return Holder.direct(enchantment);
	}
}
