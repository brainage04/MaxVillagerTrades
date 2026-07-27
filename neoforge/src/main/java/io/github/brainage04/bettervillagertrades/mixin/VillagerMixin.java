package io.github.brainage04.bettervillagertrades.mixin;

import io.github.brainage04.bettervillagertrades.BetterVillagerTrades;
import io.github.brainage04.bettervillagertrades.VillagerTradeReroller;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public abstract class VillagerMixin implements VillagerTradeReroller {
	@Shadow
	protected abstract void updateTrades(ServerLevel level);

	@Unique
	private int betterVillagerTrades$offerCountBeforeUpdate;

	@Inject(method = "updateTrades", at = @At("HEAD"))
	private void betterVillagerTrades$captureOfferCount(ServerLevel level, CallbackInfo ci) {
		betterVillagerTrades$offerCountBeforeUpdate = ((Villager) (Object) this).getOffers().size();
	}

	@Inject(method = "updateTrades", at = @At("TAIL"))
	private void betterVillagerTrades$maximizeNewOffers(ServerLevel level, CallbackInfo ci) {
		BetterVillagerTrades.maximizeNewVillagerOffers((Villager) (Object) this, level, betterVillagerTrades$offerCountBeforeUpdate);
	}

	@Override
	public void betterVillagerTrades$rerollTrades(ServerLevel level) {
		((Villager) (Object) this).getOffers().clear();
		updateTrades(level);
	}
}
