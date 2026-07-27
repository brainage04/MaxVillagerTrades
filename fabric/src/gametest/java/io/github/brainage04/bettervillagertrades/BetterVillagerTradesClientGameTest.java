package io.github.brainage04.bettervillagertrades;

import io.github.brainage04.fabricmoddingconventions.ClientGameTestRecorder;
import io.github.brainage04.fabricmoddingconventions.ClientGameTestServers;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestDedicatedServerContext;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.MerchantScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.npc.villager.VillagerProfession;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

import java.util.List;
import java.util.Properties;
import java.util.UUID;

@SuppressWarnings("UnstableApiUsage")
public final class BetterVillagerTradesClientGameTest implements FabricClientGameTest {
	private static UUID librarianId;
	private static UUID equipmentSmithId;

	@Override
	public void runTest(ClientGameTestContext context) {
		Properties serverProperties = ClientGameTestServers.flatServerProperties();
		try (TestDedicatedServerContext server = context.worldBuilder().createServer(serverProperties)) {
			ClientGameTestServers.connectToDedicatedServer(context, server, "Better Villager Trades visual GameTest");
			try {
				server.runOnServer(BetterVillagerTradesClientGameTest::prepareTrades);
				ClientGameTestServers.assertClientWorldAndPlayerAvailable(context);
				context.waitTicks(20);

				ClientGameTestRecorder.startRecording(context);
				ClientGameTestRecorder.showStep(
						context,
						"librarian-max-book",
						"Generated max-level enchanted book",
						"A real librarian offer result is staged in the visible inventory with every enchantment at its maximum level"
				);
				openInventoryScreen(context);
				assertClientInventoryTrade(context, 0, DataComponents.STORED_ENCHANTMENTS);
				context.waitTicks(60);

				openMerchantScreen(context);
				ClientGameTestRecorder.showStep(
						context,
						"trade-reroll-controls",
						"Per-player reroll controls",
						"The optional client adds Reroll trades and Toggle filter controls beneath the standard villager trading screen"
				);
				assertMerchantControls(context);
				context.waitTicks(60);

				assertClientVillagerStaged(context, equipmentSmithId);
				ClientGameTestRecorder.showStep(
						context,
						"equipment-max-item",
						"Generated max-level equipment",
						"A real weaponsmith offer result is staged in the visible inventory with every enchantment at its maximum level"
				);
				openInventoryScreen(context);
				assertClientInventoryTrade(context, 1, DataComponents.ENCHANTMENTS);
				context.waitTicks(60);
			} finally {
				server.runOnServer(BetterVillagerTradesClientGameTest::cleanupTrades);
				ClientGameTestServers.disconnectFromDedicatedServer(context);
			}
		}
	}

	private static void prepareTrades(MinecraftServer server) {
		ServerLevel level = server.overworld();
		server.getGameRules().set(BetterVillagerTrades.MAX_ENCHANTED_BOOK_TRADES, true, server);
		server.getGameRules().set(BetterVillagerTrades.MAX_ENCHANTED_ITEM_TRADES, true, server);
		ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
		player.teleportTo(level, 2.5, -60.0, 4.0, java.util.Set.of(), 180.0F, 0.0F, false);

		Villager librarian = generatedVillager(level, VillagerProfession.LIBRARIAN, 1, true, 2.5, -60.0, 2.5);
		Villager equipmentSmith = generatedVillager(level, VillagerProfession.WEAPONSMITH, 5, false, 5.5, -60.0, 2.5);
		player.getInventory().setItem(0, firstEnchantedTrade(librarian, true));
		player.getInventory().setItem(1, firstEnchantedTrade(equipmentSmith, false));
		librarianId = librarian.getUUID();
		equipmentSmithId = equipmentSmith.getUUID();
	}

	private static Villager generatedVillager(
			ServerLevel level, net.minecraft.resources.ResourceKey<VillagerProfession> profession, int professionLevel,
			boolean bookTrade, double x, double y, double z
	) {
		for (int attempt = 0; attempt < 40; attempt++) {
			Villager villager = EntityTypes.VILLAGER.create(level, EntitySpawnReason.COMMAND);
			if (villager == null) throw new AssertionError("Expected villager creation to succeed.");
			villager.setPos(x, y, z);
			villager.setVillagerData(villager.getVillagerData().withProfession(level.registryAccess(), profession).withLevel(professionLevel));
			level.addFreshEntity(villager);
			if (hasMaxedGeneratedTrade(villager, bookTrade)) return villager;
			villager.discard();
		}
		throw new AssertionError("Expected a generated " + (bookTrade ? "librarian book" : "equipment") + " trade with enchantments.");
	}

	private static boolean hasMaxedGeneratedTrade(Villager villager, boolean bookTrade) {
		for (MerchantOffer offer : villager.getOffers()) {
			ItemEnchantments enchantments = offer.getResult().getOrDefault(
					bookTrade ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS, ItemEnchantments.EMPTY
			);
			if (!enchantments.isEmpty()) {
				for (Holder<Enchantment> enchantment : enchantments.keySet()) {
					if (enchantments.getLevel(enchantment) != enchantment.value().getMaxLevel()) return false;
				}
				return true;
			}
		}
		return false;
	}

	private static net.minecraft.world.item.ItemStack firstEnchantedTrade(Villager villager, boolean bookTrade) {
		net.minecraft.core.component.DataComponentType<ItemEnchantments> componentType =
				bookTrade ? DataComponents.STORED_ENCHANTMENTS : DataComponents.ENCHANTMENTS;
		for (MerchantOffer offer : villager.getOffers()) {
			if (!offer.getResult().getOrDefault(componentType, ItemEnchantments.EMPTY).isEmpty()) return offer.getResult().copy();
		}
		throw new AssertionError("Expected a generated enchanted trade result.");
	}

	private static void openMerchantScreen(ClientGameTestContext context) {
		context.runOnClient(client -> {
			MerchantMenu menu = new MerchantMenu(0, client.player.getInventory());
			MerchantOffers offers = new MerchantOffers();
			offers.add(new MerchantOffer(
					new ItemCost(Items.EMERALD),
					client.player.getInventory().getItem(0).copy(),
					1,
					1,
					0.0F
			));
			menu.setOffers(offers);
			client.gui.setScreen(new MerchantScreen(menu, client.player.getInventory(), Component.literal("Librarian")));
		});
	}

	private static void assertMerchantControls(ClientGameTestContext context) {
		context.runOnClient(client -> {
			if (!(client.gui.screen() instanceof MerchantScreen screen)) {
				throw new AssertionError("Expected the villager trading screen to be open.");
			}
			List<String> buttonLabels = screen.children().stream()
					.filter(Button.class::isInstance)
					.map(Button.class::cast)
					.map(button -> button.getMessage().getString())
					.toList();
			if (!buttonLabels.contains("Reroll trades") || !buttonLabels.contains("Toggle filter")) {
				throw new AssertionError("Expected both BetterVillagerTrades controls in the trading screen.");
			}
		});
	}

	private static void openInventoryScreen(ClientGameTestContext context) {
		context.runOnClient(client -> {
			if (client.player.containerMenu instanceof MerchantMenu) {
				client.player.closeContainer();
			}
			client.gui.setScreen(new InventoryScreen(client.player));
		});
	}

	private static void assertClientInventoryTrade(ClientGameTestContext context, int slot,
			net.minecraft.core.component.DataComponentType<ItemEnchantments> componentType) {
		context.runOnClient(client -> {
			ItemEnchantments enchantments = client.player.getInventory().getItem(slot)
					.getOrDefault(componentType, ItemEnchantments.EMPTY);
			if (enchantments.isEmpty()) throw new AssertionError("Expected the generated trade result in the visible inventory.");
			for (Holder<Enchantment> enchantment : enchantments.keySet()) {
				if (enchantments.getLevel(enchantment) != enchantment.value().getMaxLevel()) {
					throw new AssertionError("Expected every enchantment in the visible generated trade result to be maximum level.");
				}
			}
		});
	}

	private static void assertClientVillagerStaged(ClientGameTestContext context, UUID villagerId) {
		context.runOnClient(client -> {
			if (!(client.level.getEntity(villagerId) instanceof Villager)) {
				throw new AssertionError("Expected staged villager on the client.");
			}
		});
	}

	private static void cleanupTrades(MinecraftServer server) {
		ServerLevel level = server.overworld();
		if (librarianId != null) {
			Entity librarian = level.getEntity(librarianId);
			if (librarian != null) librarian.discard();
		}
		if (equipmentSmithId != null) {
			Entity equipmentSmith = level.getEntity(equipmentSmithId);
			if (equipmentSmith != null) equipmentSmith.discard();
		}
		librarianId = null;
		equipmentSmithId = null;
	}
}
