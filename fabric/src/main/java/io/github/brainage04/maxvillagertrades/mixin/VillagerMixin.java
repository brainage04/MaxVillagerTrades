package io.github.brainage04.maxvillagertrades.mixin;

import io.github.brainage04.maxvillagertrades.MaxVillagerTrades;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.npc.villager.Villager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Villager.class)
public class VillagerMixin {
	@Unique
	private int maxVillagerTrades$offerCountBeforeUpdate;

	@Inject(method = "updateTrades", at = @At("HEAD"))
	private void maxVillagerTrades$captureOfferCount(ServerLevel level, CallbackInfo ci) {
		maxVillagerTrades$offerCountBeforeUpdate = ((Villager) (Object) this).getOffers().size();
	}

	@Inject(method = "updateTrades", at = @At("TAIL"))
	private void maxVillagerTrades$maximizeNewOffers(ServerLevel level, CallbackInfo ci) {
		MaxVillagerTrades.maximizeNewVillagerOffers(
				(Villager) (Object) this,
				level,
				maxVillagerTrades$offerCountBeforeUpdate
		);
	}
}
