package io.github.brainage04;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.entity.npc.villager.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class MaxVillagerTrades implements ModInitializer {
	public static final String MOD_ID = "maxvillagertrades";
	public static final String MOD_NAME = "MaxVillagerTrades";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static final GameRule<Boolean> MAX_ENCHANTED_BOOK_TRADES = registerBooleanGameRule("max_enchanted_book_trades", true);
	public static final GameRule<Boolean> MAX_ENCHANTED_ITEM_TRADES = registerBooleanGameRule("max_enchanted_item_trades", true);

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

	private record WrappedTradeListing(VillagerTrades.ItemListing original,
	                                   TradeContext context) implements VillagerTrades.ItemListing {

		@Override
			public MerchantOffer getOffer(ServerLevel level, Entity entity, RandomSource random) {
				TradeModification modification = maximizeTradeOffer(
						original.getOffer(level, entity, random),
						context,
						level.getGameRules().get(MAX_ENCHANTED_BOOK_TRADES),
						level.getGameRules().get(MAX_ENCHANTED_ITEM_TRADES)
				);
				if (modification.modified()) {
					LOGGER.info(buildTradeModificationLog(context, modification.changes()));
				}
				return modification.offer();
			}
		}

	static TradeModification maximizeTradeOffer(MerchantOffer offer, TradeContext context) {
		return maximizeTradeOffer(offer, context, true, true);
	}

	static TradeModification maximizeTradeOffer(MerchantOffer offer, TradeContext context, boolean maxBookTrades, boolean maxItemTrades) {
		if (offer == null) {
			return new TradeModification(null, List.of());
		}

		ItemStack result = offer.getResult().copy();
		List<EnchantmentChange> changes = new ArrayList<>();

		if (maxBookTrades) {
			maximizeEnchantments(result, DataComponents.STORED_ENCHANTMENTS, changes);
		}
		if (maxItemTrades) {
			maximizeEnchantments(result, DataComponents.ENCHANTMENTS, changes);
		}

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

	private static GameRule<Boolean> registerBooleanGameRule(String name, boolean defaultValue) {
		return GameRuleBuilder
				.forBoolean(defaultValue)
				.category(GameRuleCategory.MISC)
				.buildAndRegister(Identifier.fromNamespaceAndPath(MOD_ID, name));
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

	static void overrideVillagerTradeOffers(Map<ResourceKey<VillagerProfession>, Int2ObjectMap<VillagerTrades.ItemListing[]>> tradesMap) {
		for (Map.Entry<ResourceKey<VillagerProfession>, Int2ObjectMap<VillagerTrades.ItemListing[]>> professionEntry : tradesMap.entrySet()) {
			ResourceKey<VillagerProfession> profession = professionEntry.getKey();
			Int2ObjectMap<VillagerTrades.ItemListing[]> professionLevels = professionEntry.getValue();
			if (professionLevels == null) {
				continue;
			}

			for (int level : professionLevels.keySet()) {
				VillagerTrades.ItemListing[] originalFactories = professionLevels.get(level);
				if (originalFactories == null) {
					continue;
				}
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
		// todo: add support for wandering trader trades

		LOGGER.info("{} initialized.", MOD_NAME);
	}
}
