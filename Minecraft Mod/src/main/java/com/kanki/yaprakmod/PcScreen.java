package com.kanki.yaprakmod;

import com.kanki.yaprakmod.CustomShearsItem.Tier;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

@Environment(EnvType.CLIENT)
public class PcScreen extends Screen {

    private static final int BUTTON_W = 200;
    private static final int BUTTON_H = 20;
    private static final int PANEL_HALF_W = 114;

    // Sunucudan gelen son durum
    private static long balance = 0L;
    private static String statusKey = "";
    private static long statusArg = 0L;

    private int top;

    public PcScreen() {
        super(Text.translatable("screen.yaprakmod.pc.title"));
        statusKey = "";
        statusArg = 0L;
    }

    /** YaprakMod.onInitialize içinden yalnızca istemcide çağrılır. */
    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(YaprakMod.BALANCE_PACKET,
                (client, handler, buf, responseSender) -> {
                    long newBalance = buf.readLong();
                    String newStatus = buf.readString();
                    long newArg = buf.readLong();
                    client.execute(() -> {
                        balance = newBalance;
                        statusKey = newStatus;
                        statusArg = newArg;
                    });
                });
    }

    @Override
    protected void init() {
        top = this.height / 2 - 106;
        int x = this.width / 2 - BUTTON_W / 2;

        // Tüm yaprakları sat
        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gui.yaprakmod.pc.sell_all", MoneyManager.LEAF_SELL_PRICE),
                        button -> sendAction(YaprakMod.ACTION_SELL_ALL))
                .dimensions(x, top + 48, BUTTON_W, BUTTON_H)
                .build());

        // Makas satın alma butonları
        int y = top + 78;
        for (Tier tier : Tier.values()) {
            final int action = tier.ordinal() + 1;
            this.addDrawableChild(ButtonWidget.builder(
                            Text.translatable("gui.yaprakmod.pc.buy", tier.item().getName(), tier.price),
                            button -> sendAction(action))
                    .dimensions(x, y, BUTTON_W, BUTTON_H)
                    .build());
            y += 22;
        }

        // Kapat
        this.addDrawableChild(ButtonWidget.builder(
                        Text.translatable("gui.yaprakmod.pc.close"),
                        button -> this.close())
                .dimensions(x, y + 4, BUTTON_W, BUTTON_H)
                .build());

        requestBalance();
    }

    private void requestBalance() {
        ClientPlayNetworking.send(YaprakMod.PC_REQUEST_PACKET, PacketByteBufs.create());
    }

    private void sendAction(int action) {
        PacketByteBuf buf = PacketByteBufs.create();
        buf.writeInt(action);
        ClientPlayNetworking.send(YaprakMod.PC_ACTION_PACKET, buf);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        int cx = this.width / 2;
        // Panel arka planı ve çerçevesi
        context.fill(cx - PANEL_HALF_W - 1, top - 11, cx + PANEL_HALF_W + 1, top + 223, 0xFF55FF55);
        context.fill(cx - PANEL_HALF_W, top - 10, cx + PANEL_HALF_W, top + 222, 0xEE101810);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, cx, top, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("gui.yaprakmod.pc.balance", balance), cx, top + 16, 0xFFFFD700);

        if (!statusKey.isEmpty()) {
            boolean success = statusKey.equals("sold") || statusKey.equals("bought");
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.translatable("gui.yaprakmod.pc.status." + statusKey, statusArg),
                    cx, top + 32, success ? 0xFF55FF55 : 0xFFFF5555);
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
