package io.github.brainage04;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.fabricmc.api.ModInitializer;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class MaxLibrarianTrades implements ModInitializer {
	public static final String MOD_ID = "maxlibrariantrades";
	public static final String MOD_NAME = "MaxLibrarianTrades";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
	private static final List<ResourceKey<VillagerProfession>> TRACKED_PROFESSIONS = List.of(
			VillagerProfession.LIBRARIAN,
			VillagerProfession.ARMORER,
			VillagerProfession.TOOLSMITH,
			VillagerProfession.WEAPONSMITH
	);

	record TradeContext(ResourceKey<VillagerProfession> profession, int villagerLevel, int tradeIndex) {
		private String professionName() {
			return titleCase(profession.identifier());
		}

		private String villagerLevelName() {
			return switch (villagerLevel) {
				case 1 -> "Novice";
				case 2 -> "Apprentice";
				case 3 -> "Journeyman";
				case 4 -> "Expert";
				case 5 -> "Master";
				default -> "Level " + villagerLevel;
			};
		}
	}

	record EnchantmentChange(String enchantmentName, int oldLevel, int newLevel) {
		private String describe() {
			return enchantmentName + " " + oldLevel + " -> " + enchantmentName + " " + newLevel;
		}
	}

	record TradeModification(MerchantOffer offer, List<EnchantmentChange> changes) {
		boolean modified() {
			return !changes.isEmpty();
		}
	}

	private static final class WrappedTradeListing implements VillagerTrades.ItemListing {
		private final VillagerTrades.ItemListing original;
		private final TradeContext context;

		private WrappedTradeListing(VillagerTrades.ItemListing original, TradeContext context) {
			this.original = original;
			this.context = context;
		}

		@Override
		public MerchantOffer getOffer(net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity entity, net.minecraft.util.RandomSource random) {
			TradeModification modification = maximizeTradeOffer(original.getOffer(level, entity, random), context);
			if (modification.modified()) {
				LOGGER.info(buildTradeModificationLog(context, modification.changes()));
			}
			return modification.offer();
		}
	}

	static TradeModification maximizeTradeOffer(MerchantOffer offer, TradeContext context) {
		if (offer == null) {
			return new TradeModification(null, List.of());
		}

		ItemStack result = offer.getResult().copy();
		List<EnchantmentChange> changes = new ArrayList<>();

		maximizeEnchantments(result, DataComponents.STORED_ENCHANTMENTS, changes);
		maximizeEnchantments(result, DataComponents.ENCHANTMENTS, changes);

		if (changes.isEmpty()) {
			return new TradeModification(offer, List.of());
		}

		return new TradeModification(copyOfferWithResult(offer, result), List.copyOf(changes));
	}

	static String buildTradeModificationLog(TradeContext context, List<EnchantmentChange> changes) {
		return "Modified enchants for Trade " + context.tradeIndex()
				+ " with " + context.villagerLevelName() + " " + context.professionName() + " Villager - "
				+ changes.stream().map(EnchantmentChange::describe).collect(Collectors.joining(", "));
	}

	private static MerchantOffer copyOfferWithResult(MerchantOffer offer, ItemStack result) {
		return new MerchantOffer(
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

	private static void maximizeEnchantments(
			ItemStack stack,
			DataComponentType<ItemEnchantments> componentType,
			List<EnchantmentChange> changes
	) {
		ItemEnchantments enchantments = stack.getOrDefault(componentType, ItemEnchantments.EMPTY);
		if (enchantments.isEmpty()) {
			return;
		}

		ItemEnchantments.Mutable mutable = new ItemEnchantments.Mutable(enchantments);
		boolean modified = false;
		for (Holder<Enchantment> enchantment : enchantments.keySet()) {
			int oldLevel = enchantments.getLevel(enchantment);
			int maxLevel = enchantment.value().getMaxLevel();
			if (oldLevel == maxLevel) {
				continue;
			}

			mutable.set(enchantment, maxLevel);
			changes.add(new EnchantmentChange(enchantment.getRegisteredName(), oldLevel, maxLevel));
			modified = true;
		}

		if (modified) {
			stack.set(componentType, mutable.toImmutable());
		}
	}

	private static void overrideVillagerTradeOffers(Map<ResourceKey<VillagerProfession>, Int2ObjectMap<VillagerTrades.ItemListing[]>> tradesMap) {
		for (ResourceKey<VillagerProfession> profession : TRACKED_PROFESSIONS) {
			Int2ObjectMap<VillagerTrades.ItemListing[]> professionLevels = tradesMap.get(profession);
			if (professionLevels == null) {
				continue;
			}

			for (int level : professionLevels.keySet()) {
				VillagerTrades.ItemListing[] originalFactories = professionLevels.get(level);
				VillagerTrades.ItemListing[] wrappedFactories = Arrays.copyOf(originalFactories, originalFactories.length);

				for (int i = 0; i < originalFactories.length; i++) {
					VillagerTrades.ItemListing original = originalFactories[i];
					if (original instanceof WrappedTradeListing) {
						continue;
					}

					wrappedFactories[i] = new WrappedTradeListing(original, new TradeContext(profession, level, i + 1));
				}

				professionLevels.put(level, wrappedFactories);
			}
		}
	}

	private static String titleCase(Identifier identifier) {
		String[] words = identifier.getPath().split("_");
		return Arrays.stream(words)
				.map(word -> word.substring(0, 1).toUpperCase(Locale.ROOT) + word.substring(1))
				.collect(Collectors.joining(" "));
	}
	
	@Override
	public void onInitialize() {
		LOGGER.info("{} initializing...", MOD_NAME);

		overrideVillagerTradeOffers(VillagerTrades.TRADES);
		overrideVillagerTradeOffers(VillagerTrades.EXPERIMENTAL_TRADES);

		LOGGER.info("{} initialized.", MOD_NAME);
	}
}
