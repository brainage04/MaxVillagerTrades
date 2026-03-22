package io.github.brainage04;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

public class MaxLibrarianTrades implements ModInitializer {
	public static final String MOD_ID = "maxlibrariantrades";
	public static final String MOD_NAME = "MaxLibrarianTrades";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private static VillagerTrades.ItemListing createMaxEnchWrapper(VillagerTrades.ItemListing original) {
		return (level, entity, random) -> {
			MerchantOffer offer = original.getOffer(level, entity, random);
			if (offer == null) return null;

			ItemStack result = offer.getResult().copy();
			if (result.getItem() == Items.ENCHANTED_BOOK) {
				ItemEnchantments before = result.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY);
				if (before.isEmpty()) return offer;

				ItemEnchantments.Mutable enchantments = new ItemEnchantments.Mutable(before);
				for (Holder<Enchantment> enchantment : before.keySet()) {
					enchantments.set(enchantment, enchantment.value().getMaxLevel());
				}

				result.set(DataComponents.STORED_ENCHANTMENTS, enchantments.toImmutable());

				offer = new MerchantOffer(
						offer.getItemCostA(),
						offer.getItemCostB(),
						result,
						offer.getUses(),
						offer.getMaxUses(),
						offer.getXp(),
						offer.getPriceMultiplier(),
						offer.getDemand()
				);
			}

			return offer;
		};
	}

	private static void overrideVillagerTradeOffers() {
		Map<ResourceKey<VillagerProfession>, Int2ObjectMap<VillagerTrades.ItemListing[]>> tradesMap = VillagerTrades.TRADES;

		Int2ObjectMap<VillagerTrades.ItemListing[]> librarianLevels = tradesMap.get(VillagerProfession.LIBRARIAN);
		if (librarianLevels == null) return;

		for (int level : librarianLevels.keySet()) {
			VillagerTrades.ItemListing[] originalFactories = librarianLevels.get(level);
			VillagerTrades.ItemListing[] wrappedFactories = new VillagerTrades.ItemListing[originalFactories.length];

			for (int i = 0; i < originalFactories.length; i++) {
				VillagerTrades.ItemListing original = originalFactories[i];
				wrappedFactories[i] = createMaxEnchWrapper(original);
			}

			librarianLevels.put(level, wrappedFactories);
		}
	}
	
	@Override
	public void onInitialize() {
		LOGGER.info("{} initializing...", MOD_NAME);

		overrideVillagerTradeOffers();

		LOGGER.info("{} initialized.", MOD_NAME);
	}
}
