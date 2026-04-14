package net.micaxs.slotmachine.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.micaxs.slotmachine.Config;
import net.micaxs.slotmachine.SlotMachineMod;
import net.micaxs.slotmachine.block.entity.ServerSlotMachineBlockEntity;
import net.micaxs.slotmachine.network.PacketHandler;
import net.micaxs.slotmachine.network.packet.ServerClaimPayoutC2SPacket;
import net.micaxs.slotmachine.network.packet.ServerSlotsC2SPacket;
import net.micaxs.slotmachine.sound.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ServerSlotMachineScreen extends AbstractContainerScreen<ServerSlotMachineMenu> {

    private static final long COOLDOWN = 1000;
    /** Delay (ms) before the first reel stops after receiving server results. */
    private static final long INITIAL_REVEAL_DELAY = 300;
    /** Delay (ms) between each successive reel stopping. */
    private static final long REEL_STOP_DELAY = 600;

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_machine_gui.png");

    private long lastClickTime = 0;
    private int[] results = new int[3];
    /** Non-null while the sequential stop animation is in progress. */
    private int[] finalResults = null;
    private long revealStartTime = 0;
    /** True from the moment the stop button is clicked until the server result arrives. */
    private boolean stopPressed = false;
    /** The currently-playing looping spin sound; null when not spinning. */
    private SoundInstance spinningSound = null;
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
        this.finalResults = null;
        this.stopPressed = false;
        stopSpinningSound(); // clean up if screen was resized while spinning
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
                pButton -> {
                    stopPressed = true;
                    PacketHandler.sendToServer(new ServerSlotsC2SPacket(ServerSlotMachineMenu.blockEntity.getBlockPos(), false));
                }
        ).pos(x + 64, y + 68).size(44, 11).build());
    }

    public void updateResults(int[] newResults) {
        // {0,0,0} = server confirming the spin started; already showing spinning reels.
        boolean allSpinning = newResults.length == 3
                && newResults[0] == ServerSlotMachineBlockEntity.RESULT_SPINNING
                && newResults[1] == ServerSlotMachineBlockEntity.RESULT_SPINNING
                && newResults[2] == ServerSlotMachineBlockEntity.RESULT_SPINNING;
        if (allSpinning) {
            // Server confirmed the bet was valid — now it is safe to start the sound.
            startSpinningSound();
            return;
        }

        // Any real result clears the stop-pressed guard.
        this.stopPressed = false;

        // Special / service-state codes: show immediately with no animation.
        if (getSpecialResultCode(newResults) != -1) {
            this.finalResults = null;
            this.results = newResults;
            this.message = getResultMessage();
            this.outOfService = isServiceStateResult(newResults);
        } else {
            // Normal fruit-symbol result: start sequential stop animation.
            this.finalResults = Arrays.copyOf(newResults, newResults.length);
            this.revealStartTime = System.currentTimeMillis();
            this.results = new int[]{
                    ServerSlotMachineBlockEntity.RESULT_SPINNING,
                    ServerSlotMachineBlockEntity.RESULT_SPINNING,
                    ServerSlotMachineBlockEntity.RESULT_SPINNING};
            this.message = Component.empty();
            this.outOfService = false;
        }
    }

    /** Advances the sequential stop animation each render frame. */
    private void tickRevealAnimation() {
        if (finalResults == null) return;
        long elapsed = System.currentTimeMillis() - revealStartTime;

        if (elapsed >= INITIAL_REVEAL_DELAY
                && results[0] == ServerSlotMachineBlockEntity.RESULT_SPINNING) {
            results[0] = finalResults[0];
            playReelStopSound();
        }
        if (elapsed >= INITIAL_REVEAL_DELAY + REEL_STOP_DELAY
                && results[1] == ServerSlotMachineBlockEntity.RESULT_SPINNING) {
            results[1] = finalResults[1];
            playReelStopSound();
        }
        if (elapsed >= INITIAL_REVEAL_DELAY + REEL_STOP_DELAY * 2
                && results[2] == ServerSlotMachineBlockEntity.RESULT_SPINNING) {
            results[2] = finalResults[2];
            playReelStopSound();
            stopSpinningSound();
            this.message = getResultMessage();
            this.outOfService = isServiceStateResult(results);
            this.finalResults = null;
            // Tell the server the animation is done so it moves the prize into the output slot.
            PacketHandler.sendToServer(new ServerClaimPayoutC2SPacket(ServerSlotMachineMenu.blockEntity.getBlockPos()));
        }
    }

    // -------------------------------------------------------------------------
    // Sound helpers
    // -------------------------------------------------------------------------

    /** Starts playing the looping spinning.ogg. Stops any previous instance first. */
    private void startSpinningSound() {
        stopSpinningSound();
        spinningSound = new SimpleSoundInstance(
                ModSounds.SPINNING.get().getLocation(),
                SoundSource.MASTER,
                1.0f, 1.0f,
                RandomSource.create(),
                true,  // loop
                0,
                SoundInstance.Attenuation.NONE,
                0.0, 0.0, 0.0,
                true   // relative (non-positional)
        );
        Minecraft.getInstance().getSoundManager().play(spinningSound);
    }

    /** Stops the looping spinning sound if one is playing. */
    private void stopSpinningSound() {
        if (spinningSound != null) {
            Minecraft.getInstance().getSoundManager().stop(spinningSound);
            spinningSound = null;
        }
    }

    /** Plays the short stop.ogg reel-click sound once. */
    private void playReelStopSound() {
        Minecraft.getInstance().getSoundManager().play(
                SimpleSoundInstance.forUI(ModSounds.REEL_STOP.get(), 1.0f));
    }

    @Override
    public void onClose() {
        stopSpinningSound();
        super.onClose();
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

        // Don't interrupt an active spin or reveal animation with a service-state check.
        if (isSpinning() || finalResults != null) return;

        boolean wasOutOfService = outOfService;
        int serviceState = menu.getServiceState();
        outOfService = serviceState != ServerSlotMachineBlockEntity.SERVICE_READY;
        if (outOfService) {
            // Cancel any ongoing reveal animation and show the service state immediately.
            this.finalResults = null;
            this.stopPressed = false;
            stopSpinningSound();
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
            // Each reel is rendered individually: still spinning → random image, stopped → final symbol.
            renderReel(guiGraphics, x + 59, y + 21, results[0]);
            renderReel(guiGraphics, x + 78, y + 21, results[1]);
            renderReel(guiGraphics, x + 97, y + 21, results[2]);
        }
    }

    /** Renders a single reel: a random spinning image if still spinning, or the final symbol. */
    private void renderReel(GuiGraphics guiGraphics, int x, int y, int result) {
        if (result == ServerSlotMachineBlockEntity.RESULT_SPINNING) {
            drawRandomImage(guiGraphics, x, y);
        } else {
            drawSlotImageInSlot(guiGraphics, x, y, result);
        }
    }

    private void drawSlotImageInSlot(GuiGraphics guiGraphics, int x, int y, int result) {
        ResourceLocation image = switch (result) {
            case 1 -> new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_banana.png");
            case 2 -> new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_bar.png");
            case 3 -> new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_cherry.png");
            case 4 -> new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_orange.png");
            case 5 -> new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_strawberry.png");
            default -> new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_banana.png");
        };
        drawImage(guiGraphics, image, x, y);
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

    /** Formats a chance value for display: whole numbers show without decimals, fractions keep up to 2 decimal places. */
    private static String formatChance(double chance) {
        if (chance == Math.floor(chance)) {
            return (int) chance + "%";
        }
        // Strip trailing zeros (e.g. 2.50 → "2.5%", 0.25 → "0.25%")
        return String.valueOf(Math.round(chance * 100.0) / 100.0) + "%";
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

        ItemStack tooltipStack = null;
        int tooltipX = 0, tooltipY = 0;
        for (int i = 0; i < prizes.size(); i++) {
            int iconScreenY = panelY + Math.round((13 + i * rowH) * SCALE);
            if (mouseX >= panelX && mouseX < panelX + 8
                    && mouseY >= iconScreenY && mouseY < iconScreenY + 8) {
                tooltipStack = prizes.get(i).toStack(prizes.get(i).amount());
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
            ItemStack stack = prize.toStack(prize.amount());
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
            g.drawString(font, name,                          19, rowY + 2,  0xFFFFFF, false);
            g.drawString(font, formatChance(prize.chance()),  19, rowY + 11, 0x55FF55, false);
        }

        pose.popPose();

        if (tooltipStack != null) {
            g.renderTooltip(font, tooltipStack, tooltipX, tooltipY);
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);
        tickRevealAnimation();
        refreshServiceMessage();

        boolean isRevealing = finalResults != null;
        boolean outputFull = getSpecialResultCode(results) == ServerSlotMachineBlockEntity.RESULT_TAKE_OUT_WIN;
        stopButton.visible = !outOfService && !outputFull && !isRevealing && !stopPressed && isSpinning();
        spinButton.visible = !outOfService && !outputFull && !isRevealing && !stopPressed && !stopButton.visible;

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

