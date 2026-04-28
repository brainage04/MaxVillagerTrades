package io.github.brainage04.maxvillagertrades;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class MaxVillagerTrades implements ModInitializer {
	public static final String MOD_ID = "maxvillagertrades";
	public static final String MOD_NAME = "MaxVillagerTrades";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

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

	static TradeModification maximizeTradeOffer(MerchantOffer offer) {
		return maximizeTradeOffer(offer, true, true);
	}

	static TradeModification maximizeTradeOffer(MerchantOffer offer, boolean maxBookTrades, boolean maxItemTrades) {
		if (offer == null) {
			return new TradeModification(null, List.of());
		}

		ItemStack result = offer.getResult();
		List<EnchantmentChange> changes = new ArrayList<>();

		if (maxBookTrades) {
			maximizeEnchantments(result, DataComponents.STORED_ENCHANTMENTS, changes);
		}
		if (maxItemTrades) {
			maximizeEnchantments(result, DataComponents.ENCHANTMENTS, changes);
		}

		return new TradeModification(offer, List.copyOf(changes));
	}

	static String buildTradeModificationLog(TradeContext context, List<EnchantmentChange> changes) {
		return "Modified enchants for Trade " + context.tradeIndex()
				+ " with " + context.villagerLevelName() + " " + context.professionName() + " Villager - "
				+ changes.stream().map(EnchantmentChange::describe).collect(Collectors.joining(", "));
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
			if (oldLevel == maxLevel) continue;

			mutable.set(enchantment, maxLevel);
			changes.add(new EnchantmentChange(enchantment.getRegisteredName(), oldLevel, maxLevel));
			modified = true;
		}

		if (modified) {
			stack.set(componentType, mutable.toImmutable());
		}
	}

	public static void maximizeNewVillagerOffers(Villager villager, ServerLevel level, int firstOfferIndex) {
		MerchantOffers offers = villager.getOffers();
		ResourceKey<VillagerProfession> profession = villager.getVillagerData()
				.profession()
				.unwrapKey()
				.orElse(VillagerProfession.NONE);
		int villagerLevel = villager.getVillagerData().level();

		for (int i = Math.max(0, firstOfferIndex); i < offers.size(); i++) {
			TradeModification modification = maximizeTradeOffer(
					offers.get(i),
					level.getGameRules().get(MAX_ENCHANTED_BOOK_TRADES),
					level.getGameRules().get(MAX_ENCHANTED_ITEM_TRADES)
			);
			if (modification.modified()) {
				LOGGER.info(buildTradeModificationLog(new TradeContext(profession, villagerLevel, i + 1), modification.changes()));
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

		LOGGER.info("{} initialized.", MOD_NAME);
	}
}
