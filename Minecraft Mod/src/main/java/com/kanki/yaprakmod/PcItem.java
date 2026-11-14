package com.kanki.yaprakmod;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.item.TooltipContext;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class PcItem extends Item {

    public PcItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);
        if (world.isClient) {
            ClientHelper.openScreen();
        }
        return TypedActionResult.success(stack, world.isClient);
    }

    @Override
    public void appendTooltip(ItemStack stack, @Nullable World world, List<Text> tooltip, TooltipContext context) {
        tooltip.add(Text.translatable("tooltip.yaprakmod.pc.line1").formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.yaprakmod.pc.line2").formatted(Formatting.DARK_GRAY));
        super.appendTooltip(stack, world, tooltip, context);
    }

    /** İstemci sınıflarına yalnızca burada dokunulur; sunucuda bu sınıf hiç yüklenmez. */
    private static final class ClientHelper {
        private static void openScreen() {
            MinecraftClient.getInstance().setScreen(new PcScreen());
        }
    }
}
