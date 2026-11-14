package com.kanki.yaprakmod;

import net.minecraft.block.BlockState;
import net.minecraft.block.Block;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Shearable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.registry.Registries;
import net.minecraft.registry.tag.BlockTags;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.Rarity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CustomShearsItem extends ShearsItem {

    public enum Tier {
        //      id               dayanıklılık hız   şans   azami  YP/yaprak  fiyat    nadirlik        renk
        TAHTA("tahta_makas",         120, 1.0f, 0.10f, 1,  0L,     50L, Rarity.COMMON,   Formatting.GOLD),
        DEMIR("demir_makas",         238, 1.5f, 0.20f, 1,  1L,    250L, Rarity.COMMON,   Formatting.WHITE),
        ALTIN("altin_makas",          64, 2.5f, 0.35f, 2,  2L,   1000L, Rarity.UNCOMMON, Formatting.YELLOW),
        ELMAS("elmas_makas",        1561, 3.0f, 0.50f, 2,  4L,   5000L, Rarity.RARE,     Formatting.AQUA),
        EFSANEVI("efsanevi_makas",  4000, 4.0f, 0.75f, 3, 10L,  25000L, Rarity.EPIC,     Formatting.LIGHT_PURPLE);

        public final String id;
        public final int durability;
        public final float speedMultiplier;
        public final float bonusChance;
        public final int maxBonus;
        public final long moneyPerLeaf;
        public final long price;
        public final Rarity rarity;
        public final Formatting color;

        Tier(String id, int durability, float speedMultiplier, float bonusChance, int maxBonus,
             long moneyPerLeaf, long price, Rarity rarity, Formatting color) {
            this.id = id;
            this.durability = durability;
            this.speedMultiplier = speedMultiplier;
            this.bonusChance = bonusChance;
            this.maxBonus = maxBonus;
            this.moneyPerLeaf = moneyPerLeaf;
            this.price = price;
            this.rarity = rarity;
            this.color = color;
        }

        /** Kayıtlı eşyayı döner (eşyalar YaprakMod'da kaydedildikten sonra çağrılmalıdır). */
        public Item item() {
            return Registries.ITEM.get(new Identifier(YaprakMod.MOD_ID, id));
        }
    }

    private final Tier tier;

    public CustomShearsItem(Tier tier) {
        super(buildSettings(tier));
        this.tier = tier;
    }

    private static Item.Settings buildSettings(Tier tier) {
        Item.Settings settings = new Item.Settings().maxDamage(tier.durability).rarity(tier.rarity);
        if (tier == Tier.EFSANEVI) {
            settings.fireproof();
        }
        return settings;
    }

    public Tier getTier() {
        return tier;
    }

    @Override
    public float getMiningSpeedMultiplier(ItemStack stack, BlockState state) {
        float base = super.getMiningSpeedMultiplier(stack, state);
        // Sadece makasın zaten hızlı olduğu bloklarda (yaprak, ağ, yün...) hızı artır
        return base > 1.0f ? base * tier.speedMultiplier : base;
    }

    @Override
    public boolean postMine(ItemStack stack, World world, BlockState state, BlockPos pos, LivingEntity miner) {
        boolean result = super.postMine(stack, world, state, pos, miner);

        if (!world.isClient && state.isIn(BlockTags.LEAVES)
                && !(miner instanceof PlayerEntity player && player.isCreative())) {
            int count = 1;
            if (world.random.nextFloat() < tier.bonusChance) {
                count += 1 + world.random.nextInt(tier.maxBonus);
            }

            // Vanilla yaprak tablosu sadece orijinal makası tanıdığı için yaprağı kendimiz düşürürüz
            Block.dropStack(world, pos, new ItemStack(state.getBlock().asItem(), count));

            if (tier.moneyPerLeaf > 0 && miner instanceof ServerPlayerEntity serverPlayer) {
                long reward = tier.moneyPerLeaf * count;
                MoneyManager.addMoney(serverPlayer, reward);
                serverPlayer.sendMessage(
                        Text.translatable("message.yaprakmod.leaf_bonus", reward).formatted(Formatting.GOLD), true);
            }
        }
        return result;
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (entity instanceof Shearable shearable && shearable.isShearable()) {
            World world = user.getWorld();
            if (!world.isClient) {
                shearable.sheared(SoundCategory.PLAYERS);
                stack.damage(1, user, p -> p.sendToolBreakStatus(hand));
            }
            return ActionResult.success(world.isClient);
        }
        return ActionResult.PASS;
    }

    @Override
    public boolean hasGlint(ItemStack stack) {
        return tier == Tier.EFSANEVI || super.hasGlint(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.yaprakmod.shears.desc").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.yaprakmod.shears.speed", tier.speedMultiplier)
                .formatted(tier.color));
        tooltip.add(Text.translatable("tooltip.yaprakmod.shears.chance", (int) (tier.bonusChance * 100) + "%")
                .formatted(Formatting.GREEN));
        tooltip.add(Text.translatable("tooltip.yaprakmod.shears.max_bonus", tier.maxBonus)
                .formatted(Formatting.GREEN));
        tooltip.add(Text.translatable("tooltip.yaprakmod.shears.money", tier.moneyPerLeaf)
                .formatted(Formatting.GOLD));
        if (tier == Tier.EFSANEVI) {
            tooltip.add(Text.translatable("tooltip.yaprakmod.shears.fireproof").formatted(Formatting.RED));
            tooltip.add(Text.translatable("tooltip.yaprakmod.shears.legendary")
                    .formatted(Formatting.LIGHT_PURPLE, Formatting.ITALIC));
        }
        super.appendTooltip(stack, world, tooltip, context);
    }
}
