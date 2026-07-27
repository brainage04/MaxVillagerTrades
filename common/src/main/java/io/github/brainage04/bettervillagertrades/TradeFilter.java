package io.github.brainage04.bettervillagertrades;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import java.util.Optional;

/** A player-owned filter that protects matching villager trade outputs from rerolls. */
public record TradeFilter(Identifier item, Optional<Identifier> enchantment) {
	public static final Codec<TradeFilter> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Identifier.CODEC.fieldOf("item").forGetter(TradeFilter::item),
			Identifier.CODEC.optionalFieldOf("enchantment").forGetter(TradeFilter::enchantment)
	).apply(instance, TradeFilter::new));

	public static TradeFilter fromStack(ItemStack stack) {
		Identifier item = BuiltInRegistries.ITEM.getKey(stack.getItem());
		Optional<Identifier> enchantment = firstEnchantment(
				stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY)
		).or(() -> firstEnchantment(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY)));
		return new TradeFilter(item, enchantment);
	}

	public boolean matches(ItemStack stack) {
		if (!item.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()))) {
			return false;
		}
		return enchantment.isEmpty()
				|| containsEnchantment(stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS, ItemEnchantments.EMPTY), enchantment.get())
				|| containsEnchantment(stack.getOrDefault(DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY), enchantment.get());
	}

	public String commandArguments() {
		return item + enchantment.map(id -> " " + id).orElse("");
	}

	public String description() {
		return enchantment.map(id -> item + " with " + id).orElse(item.toString());
	}

	private static Optional<Identifier> firstEnchantment(ItemEnchantments enchantments) {
		return enchantments.keySet().stream()
				.map(Holder<Enchantment>::unwrapKey)
				.flatMap(Optional::stream)
				.map(key -> key.identifier())
				.sorted()
				.findFirst();
	}

	private static boolean containsEnchantment(ItemEnchantments enchantments, Identifier expected) {
		return enchantments.keySet().stream()
				.map(Holder<Enchantment>::unwrapKey)
				.flatMap(Optional::stream)
				.anyMatch(key -> key.identifier().equals(expected));
	}
}
