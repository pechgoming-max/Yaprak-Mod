package com.kanki.yaprakmod;

import com.kanki.yaprakmod.CustomShearsItem.Tier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.item.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YaprakMod implements ModInitializer {

    public static final String MOD_ID = "yaprakmod";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    // Ağ paketleri
    public static final Identifier BALANCE_PACKET = new Identifier(MOD_ID, "balance");        // Sunucu -> İstemci
    public static final Identifier PC_REQUEST_PACKET = new Identifier(MOD_ID, "pc_request");  // İstemci -> Sunucu
    public static final Identifier PC_ACTION_PACKET = new Identifier(MOD_ID, "pc_action");    // İstemci -> Sunucu

    // PC eylemleri: 0 = hepsini sat, 1..N = ilgili seviyedeki makası satın al
    public static final int ACTION_SELL_ALL = 0;

    // Eşyalar
    public static final Item TAHTA_MAKAS = registerItem(Tier.TAHTA.id, new CustomShearsItem(Tier.TAHTA));
    public static final Item DEMIR_MAKAS = registerItem(Tier.DEMIR.id, new CustomShearsItem(Tier.DEMIR));
    public static final Item ALTIN_MAKAS = registerItem(Tier.ALTIN.id, new CustomShearsItem(Tier.ALTIN));
    public static final Item ELMAS_MAKAS = registerItem(Tier.ELMAS.id, new CustomShearsItem(Tier.ELMAS));
    public static final Item EFSANEVI_MAKAS = registerItem(Tier.EFSANEVI.id, new CustomShearsItem(Tier.EFSANEVI));
    public static final Item PC_ITEM = registerItem("pc_item",
            new PcItem(new Item.Settings().maxCount(1).rarity(Rarity.UNCOMMON)));

    private static Item registerItem(String name, Item item) {
        return Registry.register(Registries.ITEM, new Identifier(MOD_ID, name), item);
    }

    @Override
    public void onInitialize() {
        registerItemGroup();
        registerServerNetworking();

        // İstemci tarafı paket alıcısı yalnızca istemcide kaydedilir
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            PcScreen.registerClientReceivers();
        }

        LOGGER.info("Yaprak-Mod yüklendi!");
    }

    private static void registerItemGroup() {
        Registry.register(Registries.ITEM_GROUP, new Identifier(MOD_ID, "yaprakmod"),
                FabricItemGroup.builder()
                        .displayName(Text.translatable("itemGroup." + MOD_ID))
                        .icon(() -> new ItemStack(ELMAS_MAKAS))
                        .entries((context, entries) -> {
                            entries.add(TAHTA_MAKAS);
                            entries.add(DEMIR_MAKAS);
                            entries.add(ALTIN_MAKAS);
                            entries.add(ELMAS_MAKAS);
                            entries.add(EFSANEVI_MAKAS);
                            entries.add(PC_ITEM);
                        })
                        .build());
    }

    private static void registerServerNetworking() {
        // PC ekranı açıldığında bakiye isteği
        ServerPlayNetworking.registerGlobalReceiver(PC_REQUEST_PACKET,
                (server, player, handler, buf, responseSender) -> MoneyManager.sync(player, "", 0L));

        // PC ekranındaki buton eylemleri
        ServerPlayNetworking.registerGlobalReceiver(PC_ACTION_PACKET,
                (server, player, handler, buf, responseSender) -> {
                    int action = buf.readInt();
                    handlePcAction(player, action);
                });
    }

    private static boolean holdsPc(ServerPlayerEntity player) {
        return player.getMainHandStack().isOf(PC_ITEM) || player.getOffHandStack().isOf(PC_ITEM);
    }

    private static void handlePcAction(ServerPlayerEntity player, int action) {
        if (!holdsPc(player)) {
            MoneyManager.sync(player, "no_pc", 0L);
            return;
        }

        Tier[] tiers = Tier.values();
        if (action == ACTION_SELL_ALL) {
            long earned = MoneyManager.sellAllLeaves(player);
            MoneyManager.sync(player, earned > 0 ? "sold" : "no_leaves", earned);
        } else if (action >= 1 && action <= tiers.length) {
            Tier tier = tiers[action - 1];
            if (MoneyManager.buyShears(player, tier)) {
                MoneyManager.sync(player, "bought", tier.price);
            } else {
                MoneyManager.sync(player, "not_enough", tier.price);
            }
        } else {
            MoneyManager.sync(player, "", 0L);
        }
    }
}
