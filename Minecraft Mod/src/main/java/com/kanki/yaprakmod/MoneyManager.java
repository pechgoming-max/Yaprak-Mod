package com.kanki.yaprakmod;

import com.kanki.yaprakmod.CustomShearsItem.Tier;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.PersistentState;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Oyuncu bakiyelerini sunucu tarafında kalıcı olarak tutan ekonomi sistemi.
 * Para birimi: Yaprak Parası (YP).
 */
public class MoneyManager extends PersistentState {

    public static final long LEAF_SELL_PRICE = 2L;
    public static final long MAX_BALANCE = 1_000_000_000_000L;

    private static final String STATE_ID = YaprakMod.MOD_ID + "_money";
    private static final String BALANCES_KEY = "balances";

    private final Map<UUID, Long> balances = new HashMap<>();

    // ---------------------------------------------------------------- Kalıcılık

    public static MoneyManager get(MinecraftServer server) {
        return server.getOverworld().getPersistentStateManager()
                .getOrCreate(MoneyManager::fromNbt, MoneyManager::new, STATE_ID);
    }

    public static MoneyManager fromNbt(NbtCompound nbt) {
        MoneyManager manager = new MoneyManager();
        NbtCompound players = nbt.getCompound(BALANCES_KEY);
        for (String key : players.getKeys()) {
            try {
                manager.balances.put(UUID.fromString(key), players.getLong(key));
            } catch (IllegalArgumentException ignored) {
                YaprakMod.LOGGER.warn("Geçersiz UUID bakiye kaydı atlandı: {}", key);
            }
        }
        return manager;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt) {
        NbtCompound players = new NbtCompound();
        balances.forEach((uuid, value) -> players.putLong(uuid.toString(), value));
        nbt.put(BALANCES_KEY, players);
        return nbt;
    }

    private static MoneyManager of(ServerPlayerEntity player) {
        return get(Objects.requireNonNull(player.getServer()));
    }

    // ---------------------------------------------------------------- Bakiye işlemleri

    public static long getBalance(ServerPlayerEntity player) {
        return of(player).balances.getOrDefault(player.getUuid(), 0L);
    }

    public static void setBalance(ServerPlayerEntity player, long amount) {
        MoneyManager manager = of(player);
        manager.balances.put(player.getUuid(), Math.max(0L, Math.min(MAX_BALANCE, amount)));
        manager.markDirty();
    }

    public static void addMoney(ServerPlayerEntity player, long amount) {
        if (amount <= 0) {
            return;
        }
        setBalance(player, getBalance(player) + amount);
    }

    public static boolean hasEnough(ServerPlayerEntity player, long amount) {
        return getBalance(player) >= amount;
    }

    /** Yeterli bakiye varsa parayı düşer ve true döner. */
    public static boolean removeMoney(ServerPlayerEntity player, long amount) {
        if (amount < 0 || !hasEnough(player, amount)) {
            return false;
        }
        setBalance(player, getBalance(player) - amount);
        return true;
    }

    // ---------------------------------------------------------------- Market işlemleri

    /** Envanterdeki tüm yaprak bloklarını satar, kazanılan parayı döner. */
    public static long sellAllLeaves(ServerPlayerEntity player) {
        PlayerInventory inventory = player.getInventory();
        long earned = 0L;
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && stack.isIn(ItemTags.LEAVES)) {
                earned += (long) stack.getCount() * LEAF_SELL_PRICE;
                inventory.setStack(i, ItemStack.EMPTY);
            }
        }
        if (earned > 0) {
            addMoney(player, earned);
        }
        return earned;
    }

    /** Parası yetiyorsa makası satın aldırır. */
    public static boolean buyShears(ServerPlayerEntity player, Tier tier) {
        if (!removeMoney(player, tier.price)) {
            return false;
        }
        player.getInventory().offerOrDrop(new ItemStack(tier.item()));
        return true;
    }

    // ---------------------------------------------------------------- Senkronizasyon

    /** Bakiyeyi ve bir durum mesajını istemciye gönderir. */
    public static void sync(ServerPlayerEntity player, String status, long arg) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeLong(getBalance(player));
        buf.writeString(status);
        buf.writeLong(arg);
        ServerPlayNetworking.send(player, YaprakMod.BALANCE_PACKET, buf);
    }
}
