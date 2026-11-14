package com.kanki.yaprakmod;

import net.minecraft.client.item.TooltipContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ShearsItem;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class CustomShearsItem extends ShearsItem {
    private final int rewardAmount;

    public CustomShearsItem(Settings settings, int rewardAmount) {
        super(settings);
        this.rewardAmount = rewardAmount;
    }

    public int getRewardAmount() {
        return rewardAmount;
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.literal("Yaprak başına: +" + rewardAmount + " Para").formatted(Formatting.GREEN));
        super.appendTooltip(stack, world, tooltip, context);
    }
}
