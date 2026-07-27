package io.github.brainage04.bettervillagertrades;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.Optional;

/** Validates and performs player-requested villager trade rerolls. */
public final class TradeRerollService {
	private TradeRerollService() {
	}

	public static InteractionResult interact(Player player, Entity target, InteractionHand hand) {
		if (!(target instanceof Villager villager)
				|| !player.isShiftKeyDown()
				|| !player.getItemInHand(hand).is(Items.EMERALD)) {
			return InteractionResult.PASS;
		}
		if (player.level().isClientSide()) {
			return InteractionResult.SUCCESS;
		}
		if (player instanceof ServerPlayer serverPlayer) {
			reroll(serverPlayer, villager, true);
		}
		return InteractionResult.SUCCESS;
	}

	public static int rerollTradingVillager(ServerPlayer player) {
		Optional<Villager> villager = player.level().getEntitiesOfClass(
				Villager.class,
				player.getBoundingBox().inflate(8.0),
				candidate -> candidate.getTradingPlayer() == player
		).stream().findFirst();
		if (villager.isEmpty()) {
			player.sendSystemMessage(Component.literal("Open a villager trading screen before using this reroll command."));
			return 0;
		}
		return reroll(player, villager.get(), true) ? 1 : 0;
	}

	public static boolean reroll(ServerPlayer player, Villager villager, boolean sendFeedback) {
		Component denial = eligibilityFailure(villager);
		if (denial != null) {
			if (sendFeedback) player.sendSystemMessage(denial);
			return false;
		}

		MerchantOffers offers = villager.getOffers();
		Optional<TradeFilter> protectedBy = PlayerTradeFilters.get(player.level().getServer()).protectedBy(player, offers);
		if (protectedBy.isPresent()) {
			if (sendFeedback) {
				player.sendSystemMessage(Component.literal(
						"Reroll blocked: this villager offers your protected trade " + protectedBy.get().description() + "."
				));
			}
			return false;
		}

		if (!(villager instanceof VillagerTradeReroller reroller)) {
			BetterVillagerTrades.LOGGER.error("Villager reroll mixin was not applied.");
			if (sendFeedback) player.sendSystemMessage(Component.literal("Trade rerolling is unavailable because the server mixin did not load."));
			return false;
		}

		reroller.betterVillagerTrades$rerollTrades(player.level());
		if (player.containerMenu instanceof MerchantMenu menu && villager.getTradingPlayer() == player) {
			menu.setOffers(villager.getOffers());
			player.sendMerchantOffers(
					menu.containerId,
					villager.getOffers(),
					villager.getVillagerData().level(),
					villager.getVillagerXp(),
					villager.showProgressBar(),
					villager.canRestock()
			);
		}
		if (sendFeedback) player.sendSystemMessage(Component.literal("Villager trades rerolled."));
		return true;
	}

	private static Component eligibilityFailure(Villager villager) {
		if (villager.isBaby()) {
			return Component.literal("Baby villagers do not have trades to reroll.");
		}
		if (villager.getVillagerData().profession().unwrapKey().orElse(VillagerProfession.NONE) == VillagerProfession.NONE) {
			return Component.literal("This villager needs a profession before its trades can be rerolled.");
		}
		if (villager.getVillagerData().level() != 1 || villager.getVillagerXp() != 0) {
			return Component.literal("Only novice villagers with no trade experience can be rerolled.");
		}
		if (villager.getOffers().stream().anyMatch(offer -> offer.getUses() > 0)) {
			return Component.literal("This villager is locked because one of its trades has already been used.");
		}
		return null;
	}
}
