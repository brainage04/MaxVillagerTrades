package io.github.brainage04.bettervillagertrades;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/** Persistent, per-player desired-trade filters. */
public final class PlayerTradeFilters extends SavedData {
	private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
	private static final Codec<PlayerTradeFilters> CODEC = RecordCodecBuilder.create(instance -> instance.group(
			Codec.unboundedMap(UUID_CODEC, TradeFilter.CODEC.listOf())
					.optionalFieldOf("filters", Map.of())
					.forGetter(PlayerTradeFilters::snapshot)
	).apply(instance, PlayerTradeFilters::new));

	public static final SavedDataType<PlayerTradeFilters> TYPE = new SavedDataType<>(
			BetterVillagerTrades.of("player_trade_filters"),
			PlayerTradeFilters::new,
			CODEC,
			DataFixTypes.SAVED_DATA_MAP_DATA
	);

	private final Map<UUID, List<TradeFilter>> filters = new HashMap<>();

	public PlayerTradeFilters() {
	}

	private PlayerTradeFilters(Map<UUID, List<TradeFilter>> filters) {
		filters.forEach((player, entries) -> this.filters.put(player, unique(entries)));
	}

	public static PlayerTradeFilters get(MinecraftServer server) {
		return server.overworld().getDataStorage().computeIfAbsent(TYPE);
	}

	public List<TradeFilter> get(ServerPlayer player) {
		return get(player.getUUID());
	}

	List<TradeFilter> get(UUID player) {
		return List.copyOf(filters.getOrDefault(player, List.of()));
	}

	public boolean add(ServerPlayer player, TradeFilter filter) {
		return add(player.getUUID(), filter);
	}

	boolean add(UUID player, TradeFilter filter) {
		List<TradeFilter> playerFilters = filters.computeIfAbsent(player, ignored -> new ArrayList<>());
		if (playerFilters.contains(filter)) {
			return false;
		}
		playerFilters.add(filter);
		setDirty();
		return true;
	}

	public boolean remove(ServerPlayer player, TradeFilter filter) {
		return remove(player.getUUID(), filter);
	}

	boolean remove(UUID player, TradeFilter filter) {
		List<TradeFilter> playerFilters = filters.get(player);
		if (playerFilters == null || !playerFilters.remove(filter)) {
			return false;
		}
		if (playerFilters.isEmpty()) {
			filters.remove(player);
		}
		setDirty();
		return true;
	}

	public boolean toggle(ServerPlayer player, TradeFilter filter) {
		return remove(player, filter) ? false : add(player, filter);
	}

	public boolean clear(ServerPlayer player) {
		return clear(player.getUUID());
	}

	boolean clear(UUID player) {
		if (filters.remove(player) == null) {
			return false;
		}
		setDirty();
		return true;
	}

	public Optional<TradeFilter> protectedBy(ServerPlayer player, MerchantOffers offers) {
		return protectedBy(player.getUUID(), offers);
	}

	Optional<TradeFilter> protectedBy(UUID player, MerchantOffers offers) {
		for (TradeFilter filter : get(player)) {
			if (offers.stream().anyMatch(offer -> filter.matches(offer.getResult()))) {
				return Optional.of(filter);
			}
		}
		return Optional.empty();
	}

	private Map<UUID, List<TradeFilter>> snapshot() {
		Map<UUID, List<TradeFilter>> copy = new HashMap<>();
		filters.forEach((player, entries) -> copy.put(player, List.copyOf(entries)));
		return Map.copyOf(copy);
	}

	private static List<TradeFilter> unique(List<TradeFilter> entries) {
		return new ArrayList<>(new LinkedHashSet<>(entries));
	}
}
