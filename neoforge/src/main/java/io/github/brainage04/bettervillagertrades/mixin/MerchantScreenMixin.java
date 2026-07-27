package io.github.brainage04.bettervillagertrades.mixin;

import io.github.brainage04.bettervillagertrades.TradeFilter;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.trading.MerchantOffers;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Optional client controls; vanilla clients continue to use the emerald interaction and commands. */
@Mixin(MerchantScreen.class)
public abstract class MerchantScreenMixin extends AbstractContainerScreen<MerchantMenu> {
	@Shadow
	private int shopItem;

	private MerchantScreenMixin(MerchantMenu menu, Inventory inventory, Component title) {
		super(menu, inventory, title);
	}

	@Inject(method = "init", at = @At("TAIL"))
	private void betterVillagerTrades$addControls(CallbackInfo ci) {
		int controlsY = topPos + imageHeight + 4;
		addRenderableWidget(Button.builder(
				Component.literal("Reroll trades"),
				button -> betterVillagerTrades$sendCommand("bettervillagertrades reroll")
		).bounds(leftPos + 5, controlsY, 96, 20).build());
		addRenderableWidget(Button.builder(
				Component.literal("Toggle filter"),
				button -> betterVillagerTrades$toggleSelectedFilter()
		).bounds(leftPos + 105, controlsY, 96, 20).build());
	}

	@Unique
	private void betterVillagerTrades$toggleSelectedFilter() {
		MerchantOffers offers = menu.getOffers();
		if (shopItem < 0 || shopItem >= offers.size()) {
			return;
		}
		TradeFilter filter = TradeFilter.fromStack(offers.get(shopItem).getResult());
		betterVillagerTrades$sendCommand("bettervillagertrades filter toggle " + filter.commandArguments());
	}

	@Unique
	private void betterVillagerTrades$sendCommand(String command) {
		if (minecraft != null && minecraft.player != null && minecraft.player.connection != null) {
			minecraft.player.connection.sendCommand(command);
		}
	}
}
