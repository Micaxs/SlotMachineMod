package net.micaxs.slotmachine.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.micaxs.slotmachine.Config;
import net.micaxs.slotmachine.SlotMachineMod;
import net.micaxs.slotmachine.block.entity.ServerSlotMachineBlockEntity;
import net.micaxs.slotmachine.network.PacketHandler;
import net.micaxs.slotmachine.network.packet.ServerSlotsC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ServerSlotMachineScreen extends AbstractContainerScreen<ServerSlotMachineMenu> {

    private static final long COOLDOWN = 1000;
    private static final ResourceLocation TEXTURE =
            new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_machine_gui.png");

    private long lastClickTime = 0;
    private int[] results = new int[3];
    private Button spinButton;
    private Button stopButton;
    private Component message = Component.empty();
    private boolean outOfService;

    public ServerSlotMachineScreen(ServerSlotMachineMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        this.results = new int[]{
                ServerSlotMachineBlockEntity.RESULT_INVALID_BET,
                ServerSlotMachineBlockEntity.RESULT_INVALID_BET,
                ServerSlotMachineBlockEntity.RESULT_INVALID_BET};
        refreshServiceMessage();

        this.spinButton = this.addRenderableWidget(new Button.Builder(
                Component.translatable("slots.gui.spin"),
                pButton -> {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastClickTime < COOLDOWN) return;
                    lastClickTime = currentTime;
                    PacketHandler.sendToServer(new ServerSlotsC2SPacket(ServerSlotMachineMenu.blockEntity.getBlockPos(), true));
                    results = new int[]{
                            ServerSlotMachineBlockEntity.RESULT_SPINNING,
                            ServerSlotMachineBlockEntity.RESULT_SPINNING,
                            ServerSlotMachineBlockEntity.RESULT_SPINNING};
                    message = Component.empty();
                }).pos(x + 64, y + 68).size(44, 11).build());

        this.stopButton = this.addRenderableWidget(new Button.Builder(
                Component.translatable("slots.gui.stop"),
                pButton -> PacketHandler.sendToServer(new ServerSlotsC2SPacket(ServerSlotMachineMenu.blockEntity.getBlockPos(), false))
        ).pos(x + 64, y + 68).size(44, 11).build());
    }

    public void updateResults(int[] newResults) {
        this.results = newResults;
        this.message = getResultMessage();
        this.outOfService = isServiceStateResult(newResults);
    }

    private void refreshServiceMessage() {
        if (getSpecialResultCode(results) == ServerSlotMachineBlockEntity.RESULT_TAKE_OUT_WIN) {
            if (!menu.hasOutputItem()) {
                results = repeatedResult(ServerSlotMachineBlockEntity.RESULT_INVALID_BET);
                message = Component.empty();
                outOfService = false;
            }
            return;
        }

        boolean wasOutOfService = outOfService;
        int serviceState = menu.getServiceState();
        outOfService = serviceState != ServerSlotMachineBlockEntity.SERVICE_READY;
        if (outOfService) {
            this.results = resultForServiceState(serviceState);
            this.message = getResultMessage();
        } else if (wasOutOfService) {
            this.results = repeatedResult(ServerSlotMachineBlockEntity.RESULT_INVALID_BET);
            this.message = Component.empty();
        }
    }

    private boolean isSpinning() {
        return results[0] == ServerSlotMachineBlockEntity.RESULT_SPINNING
                && results[1] == ServerSlotMachineBlockEntity.RESULT_SPINNING
                && results[2] == ServerSlotMachineBlockEntity.RESULT_SPINNING;
    }

    private int[] resultForServiceState(int serviceState) {
        return switch (serviceState) {
            case ServerSlotMachineBlockEntity.SERVICE_NOT_CONFIGURED ->
                    repeatedResult(ServerSlotMachineBlockEntity.RESULT_NOT_CONFIGURED);
            default -> repeatedResult(ServerSlotMachineBlockEntity.RESULT_INVALID_BET);
        };
    }

    private int[] repeatedResult(int result) {
        return new int[]{result, result, result};
    }

    private boolean isServiceStateResult(int[] currentResults) {
        return getSpecialResultCode(currentResults) == ServerSlotMachineBlockEntity.RESULT_NOT_CONFIGURED;
    }

    private int getSpecialResultCode(int[] currentResults) {
        if (currentResults.length == 3
                && currentResults[0] == currentResults[1]
                && currentResults[1] == currentResults[2]
                && currentResults[0] >= ServerSlotMachineBlockEntity.RESULT_INVALID_BET) {
            return currentResults[0];
        }
        return -1;
    }

    private Component getResultMessage() {
        int specialCode = getSpecialResultCode(results);
        if (specialCode == ServerSlotMachineBlockEntity.RESULT_TAKE_OUT_WIN)
            return Component.translatable("slots.gui.take_out_win");
        if (specialCode == ServerSlotMachineBlockEntity.RESULT_NOT_CONFIGURED)
            return Component.translatable("slots.gui.not_configured");
        if (specialCode == ServerSlotMachineBlockEntity.RESULT_INVALID_BET)
            return Component.translatable("slots.gui.invalid_bet");
        // Triple match
        if (results[0] == results[1] && results[1] == results[2] && results[0] >= 1 && results[0] <= 5)
            return Component.translatable("slots.gui.you_won");
        // Double match
        boolean allFruits = results[0] >= 1 && results[0] <= 5
                && results[1] >= 1 && results[1] <= 5
                && results[2] >= 1 && results[2] <= 5;
        boolean twoSame = (results[0] == results[1] || results[1] == results[2] || results[0] == results[2])
                && !(results[0] == results[1] && results[1] == results[2]);
        if (allFruits && twoSame) return Component.translatable("slots.gui.you_won");
        // Loss
        if (results[0] != results[1] && results[1] != results[2] && results[0] != results[2])
            return Component.translatable("slots.gui.you_lost");
        return Component.empty();
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        ItemStack displayBetItem = menu.getDisplayBetItem();
        if (!displayBetItem.isEmpty()) {
            var pose = guiGraphics.pose();
            pose.pushPose();
            pose.translate(x + 22, y + 16, 0.0);
            pose.scale(0.75f, 0.75f, 1.0f);
            guiGraphics.renderItem(displayBetItem, 0, 0);
            pose.popPose();
        }

        if (!outOfService) {
            if (!isSpinning()) {
                drawSlotImageInSlot(guiGraphics, x + 59, y + 21, results[0]);
                drawSlotImageInSlot(guiGraphics, x + 78, y + 21, results[1]);
                drawSlotImageInSlot(guiGraphics, x + 97, y + 21, results[2]);
            } else {
                renderSlotWheels(guiGraphics, x, y);
            }
        }
    }

    private void renderSlotWheels(GuiGraphics guiGraphics, int x, int y) {
        drawRandomImage(guiGraphics, x + 59, y + 21);
        drawRandomImage(guiGraphics, x + 78, y + 21);
        drawRandomImage(guiGraphics, x + 97, y + 21);
    }

    private void drawSlotImageInSlot(GuiGraphics guiGraphics, int x, int y, int slotImage) {
        List<ResourceLocation> images = Arrays.asList(
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_banana.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_bar.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_cherry.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_orange.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_strawberry.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_empty.png")
        );
        int imageIndex = Math.max(1, Math.min(slotImage, images.size())) - 1;
        drawImage(guiGraphics, images.get(imageIndex), x, y);
    }

    private void drawRandomImage(GuiGraphics guiGraphics, int x, int y) {
        List<ResourceLocation> images = Arrays.asList(
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_banana.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_bar.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_cherry.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_orange.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_strawberry.png")
        );
        drawImage(guiGraphics, images.get(new Random().nextInt(images.size())), x, y);
    }

    private void drawImage(GuiGraphics guiGraphics, ResourceLocation image, int x, int y) {
        guiGraphics.blit(image, x, y, 0, 0, 16, 43, 16, 43);
    }

    /** Renders the "Prizes" side-panel (from config). */
    private void renderPrizesPanel(GuiGraphics g, int mouseX, int mouseY) {
        List<Config.ServerPrize> prizes = menu.getServerPrizes();
        if (prizes.isEmpty()) return;

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        final int panelX = x + this.imageWidth + 8;
        final int panelY = y + 4;
        final float SCALE = 0.5f;
        final int rowH = 20;
        final int panelW = 90;

        // Tooltip hit detection in screen coordinates
        ItemStack tooltipStack = null;
        int tooltipX = 0, tooltipY = 0;
        for (int i = 0; i < prizes.size(); i++) {
            int iconScreenY = panelY + Math.round((13 + i * rowH) * SCALE);
            if (mouseX >= panelX && mouseX < panelX + 8
                    && mouseY >= iconScreenY && mouseY < iconScreenY + 8) {
                tooltipStack = prizes.get(i).toStack(1);
                tooltipX = mouseX;
                tooltipY = mouseY;
            }
        }

        var pose = g.pose();
        pose.pushPose();
        pose.translate(panelX, panelY, 0);
        pose.scale(SCALE, SCALE, 1.0f);

        g.drawString(font, Component.translatable("slots.gui.prizes"), 0, 0, 0xFFFFFF, false);

        for (int i = 0; i < prizes.size(); i++) {
            Config.ServerPrize prize = prizes.get(i);
            ItemStack stack = prize.toStack(1);
            int rowY = 13 + i * rowH;

            g.renderItem(stack, 0, rowY);
            g.renderItemDecorations(font, stack, 0, rowY);

            String name = stack.getHoverName().getString();
            int maxTextW = panelW - 22;
            if (font.width(name) > maxTextW) {
                while (name.length() > 1 && font.width(name + "..") > maxTextW) {
                    name = name.substring(0, name.length() - 1);
                }
                name += "..";
            }
            g.drawString(font, name,                19, rowY + 2,  0xFFFFFF, false);
            g.drawString(font, prize.chance() + "%", 19, rowY + 11, 0x55FF55, false);
        }

        pose.popPose();

        if (tooltipStack != null) {
            g.renderTooltip(font, tooltipStack, tooltipX, tooltipY);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        refreshServiceMessage();

        boolean outputFull = getSpecialResultCode(results) == ServerSlotMachineBlockEntity.RESULT_TAKE_OUT_WIN;
        stopButton.visible = !outOfService && !outputFull && isSpinning();
        spinButton.visible = !outOfService && !outputFull && !stopButton.visible;

        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
        if (Config.serverShowPrizePanel) {
            renderPrizesPanel(guiGraphics, mouseX, mouseY);
        }

        if (!message.getString().isEmpty()) {
            int messageWidth = Minecraft.getInstance().font.width(this.message);
            int messageX = (int) ((this.width - messageWidth) / 2f);
            int messageY = (int) (this.height / 2f) - (outOfService ? 45 : 75);
            int color = outOfService ? 0xFF5555 : (outputFull ? 0xFFAA00 : 0xFFFFFF);
            guiGraphics.drawString(Minecraft.getInstance().font, this.message, messageX, messageY, color);
        }
    }
}

