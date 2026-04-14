package net.micaxs.slotmachine.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.micaxs.slotmachine.Config;
import net.micaxs.slotmachine.SlotMachineMod;
import net.micaxs.slotmachine.block.entity.SlotMachineBlockEntity;
import net.micaxs.slotmachine.network.PacketHandler;
import net.micaxs.slotmachine.network.packet.ClaimPayoutC2SPacket;
import net.micaxs.slotmachine.network.packet.SlotsC2SPacket;
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SlotMachineScreen extends AbstractContainerScreen<SlotMachineMenu> {

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

    public SlotMachineScreen(SlotMachineMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        this.results = new int[]{SlotMachineBlockEntity.RESULT_INVALID_BET,
                SlotMachineBlockEntity.RESULT_INVALID_BET,
                SlotMachineBlockEntity.RESULT_INVALID_BET};
        this.finalResults = null;
        this.stopPressed = false;
        stopSpinningSound(); // clean up if screen was resized while spinning
        refreshServiceMessage();

        this.spinButton = this.addRenderableWidget(new Button.Builder(
                Component.translatable("slots.gui.spin"),
                pButton -> {
                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastClickTime < COOLDOWN) {
                        return;
                    }
                    lastClickTime = currentTime;
                    PacketHandler.sendToServer(new SlotsC2SPacket(SlotMachineMenu.blockEntity.getBlockPos(), true));
                    results = new int[]{SlotMachineBlockEntity.RESULT_SPINNING,
                            SlotMachineBlockEntity.RESULT_SPINNING,
                            SlotMachineBlockEntity.RESULT_SPINNING};
                    message = Component.empty();
                }).pos(x + 64, y + 68).size(44, 11).build());

        this.stopButton = this.addRenderableWidget(new Button.Builder(
                Component.translatable("slots.gui.stop"),
                pButton -> {
                    stopPressed = true;
                    PacketHandler.sendToServer(new SlotsC2SPacket(SlotMachineMenu.blockEntity.getBlockPos(), false));
                }
        ).pos(x + 64, y + 68).size(44, 11).build());
    }

    public void updateResults(int[] newResults) {
        // {0,0,0} = server confirming the spin started; client is already showing spinning reels.
        // Do NOT start the reveal animation — just stay in the spinning state.
        boolean allSpinning = newResults.length == 3
                && newResults[0] == SlotMachineBlockEntity.RESULT_SPINNING
                && newResults[1] == SlotMachineBlockEntity.RESULT_SPINNING
                && newResults[2] == SlotMachineBlockEntity.RESULT_SPINNING;
        if (allSpinning) {
            // Server confirmed the bet was valid — now it is safe to start the sound.
            startSpinningSound();
            return;
        }

        // Any real result clears the stop-pressed guard.
        this.stopPressed = false;

        // Special / service-state codes (all three identical, value >= RESULT_INVALID_BET):
        // show immediately with no animation.
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
                    SlotMachineBlockEntity.RESULT_SPINNING,
                    SlotMachineBlockEntity.RESULT_SPINNING,
                    SlotMachineBlockEntity.RESULT_SPINNING};
            this.message = Component.empty();
            this.outOfService = false;
        }
    }

    /** Advances the sequential stop animation each render frame. */
    private void tickRevealAnimation() {
        if (finalResults == null) return;
        long elapsed = System.currentTimeMillis() - revealStartTime;

        if (elapsed >= INITIAL_REVEAL_DELAY
                && results[0] == SlotMachineBlockEntity.RESULT_SPINNING) {
            results[0] = finalResults[0];
            playReelStopSound();
        }
        if (elapsed >= INITIAL_REVEAL_DELAY + REEL_STOP_DELAY
                && results[1] == SlotMachineBlockEntity.RESULT_SPINNING) {
            results[1] = finalResults[1];
            playReelStopSound();
        }
        if (elapsed >= INITIAL_REVEAL_DELAY + REEL_STOP_DELAY * 2
                && results[2] == SlotMachineBlockEntity.RESULT_SPINNING) {
            results[2] = finalResults[2];
            playReelStopSound();
            stopSpinningSound();
            // All reels stopped – finalise the message and clear the pending data.
            this.message = getResultMessage();
            this.outOfService = isServiceStateResult(results);
            this.finalResults = null;
            // Tell the server the animation is done so it moves the prize into the output slot.
            PacketHandler.sendToServer(new ClaimPayoutC2SPacket(SlotMachineMenu.blockEntity.getBlockPos()));
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
        // If we're showing "take out win" and the player has since collected the item, reset.
        if (getSpecialResultCode(results) == SlotMachineBlockEntity.RESULT_TAKE_OUT_WIN) {
            if (!menu.hasOutputItem()) {
                results = repeatedResult(SlotMachineBlockEntity.RESULT_INVALID_BET);
                message = Component.empty();
                outOfService = false;
            }
            return; // don't apply normal service-state refresh while in this state
        }

        // Don't interrupt an active spin or reveal animation with a service-state check.
        // The bet was already accepted; service state may change mid-spin (e.g. last prize used)
        // and that is expected — show the new state only once the machine is idle.
        if (isSpinning() || finalResults != null) return;

        boolean wasOutOfService = outOfService;
        int serviceState = menu.getServiceState();
        outOfService = serviceState != SlotMachineBlockEntity.SERVICE_READY;
        if (outOfService) {
            // Cancel any ongoing reveal animation and show the service state immediately.
            this.finalResults = null;
            stopSpinningSound();
            this.results = resultForServiceState(serviceState);
            this.message = getResultMessage();
        } else if (wasOutOfService) {
            this.results = repeatedResult(SlotMachineBlockEntity.RESULT_INVALID_BET);
            this.message = Component.empty();
        }
    }

    private boolean isSpinning() {
        return results[0] == SlotMachineBlockEntity.RESULT_SPINNING
                && results[1] == SlotMachineBlockEntity.RESULT_SPINNING
                && results[2] == SlotMachineBlockEntity.RESULT_SPINNING;
    }

    private int[] resultForServiceState(int serviceState) {
        return switch (serviceState) {
            case SlotMachineBlockEntity.SERVICE_NOT_CONFIGURED -> repeatedResult(SlotMachineBlockEntity.RESULT_NOT_CONFIGURED);
            case SlotMachineBlockEntity.SERVICE_OUT_OF_STOCK -> repeatedResult(SlotMachineBlockEntity.RESULT_OUT_OF_STOCK);
            case SlotMachineBlockEntity.SERVICE_OUT_OF_ORDER -> repeatedResult(SlotMachineBlockEntity.RESULT_OUT_OF_ORDER);
            default -> repeatedResult(SlotMachineBlockEntity.RESULT_INVALID_BET);
        };
    }

    private int[] repeatedResult(int result) {
        return new int[]{result, result, result};
    }

    private boolean isServiceStateResult(int[] currentResults) {
        return getSpecialResultCode(currentResults) == SlotMachineBlockEntity.RESULT_NOT_CONFIGURED
                || getSpecialResultCode(currentResults) == SlotMachineBlockEntity.RESULT_OUT_OF_STOCK
                || getSpecialResultCode(currentResults) == SlotMachineBlockEntity.RESULT_OUT_OF_ORDER;
    }

    private int getSpecialResultCode(int[] currentResults) {
        if (currentResults.length == 3
                && currentResults[0] == currentResults[1]
                && currentResults[1] == currentResults[2]
                && currentResults[0] >= SlotMachineBlockEntity.RESULT_INVALID_BET) {
            return currentResults[0];
        }
        return -1;
    }

    private Component getResultMessage() {
        int specialCode = getSpecialResultCode(results);
        if (specialCode == SlotMachineBlockEntity.RESULT_TAKE_OUT_WIN) {
            return Component.translatable("slots.gui.take_out_win");
        }
        if (specialCode == SlotMachineBlockEntity.RESULT_NOT_CONFIGURED) {
            return Component.translatable("slots.gui.not_configured");
        }
        if (specialCode == SlotMachineBlockEntity.RESULT_OUT_OF_STOCK) {
            return Component.translatable("slots.gui.out_of_stock");
        }
        if (specialCode == SlotMachineBlockEntity.RESULT_OUT_OF_ORDER) {
            return Component.translatable("slots.gui.out_of_order");
        }
        if (specialCode == SlotMachineBlockEntity.RESULT_INVALID_BET) {
            return Component.translatable("slots.gui.invalid_bet");
        }
        // Triple match: all three the same fruit symbol
        if (results[0] == results[1] && results[1] == results[2] && results[0] >= 1 && results[0] <= 5) {
            return Component.translatable("slots.gui.you_won");
        }
        // Double match: exactly two of the three show the same fruit symbol
        boolean allFruits = results[0] >= 1 && results[0] <= 5
                && results[1] >= 1 && results[1] <= 5
                && results[2] >= 1 && results[2] <= 5;
        boolean twoSame = (results[0] == results[1] || results[1] == results[2] || results[0] == results[2])
                && !(results[0] == results[1] && results[1] == results[2]);
        if (allFruits && twoSame) {
            return Component.translatable("slots.gui.you_won");
        }
        // All different: loss
        if (results[0] != results[1] && results[1] != results[2] && results[0] != results[2]) {
            return Component.translatable("slots.gui.you_lost");
        }
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
        if (result == SlotMachineBlockEntity.RESULT_SPINNING) {
            drawRandomImage(guiGraphics, x, y);
        } else {
            drawSlotImageInSlot(guiGraphics, x, y, result);
        }
    }

    private void drawRandomImage(GuiGraphics guiGraphics, int x, int y) {
        List<ResourceLocation> images = Arrays.asList(
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_banana.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_bar.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_cherry.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_orange.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_strawberry.png")
        );

        ResourceLocation image = images.get(new Random().nextInt(images.size()));
        drawImage(guiGraphics, image, x, y);
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

    private void drawImage(GuiGraphics guiGraphics, ResourceLocation image, int x, int y) {
        guiGraphics.blit(image, x, y, 0, 0, 16, 43, 16, 43);
    }

    /** Renders the "Prizes" side-panel to the right of the main GUI at 50 % scale. */
    private void renderPrizesPanel(GuiGraphics g, int mouseX, int mouseY) {
        // Collect prizes that are configured (non-empty template + chance > 0)
        List<ItemStack> stacks  = new ArrayList<>();
        List<Integer>   chances = new ArrayList<>();
        for (int i = 0; i < SlotMachineBlockEntity.NUM_PRIZES; i++) {
            ItemStack template = menu.getPrizeTemplate(i);
            int chance = menu.getPrizeChance(i);
            if (!template.isEmpty() && chance > 0) {
                stacks.add(template);
                chances.add(chance);
            }
        }
        if (stacks.isEmpty()) return;

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        // Anchor point in screen space; all rendering below is done at 0.5× scale.
        final int panelX = x + this.imageWidth + 8;
        final int panelY = y + 4;
        final float SCALE = 0.5f;

        // Layout constants in "logical" units (scale 0.5 → ×0.5 on screen).
        final int rowH   = 20;   // 10 screen px per row
        final int panelW = 90;   // 45 screen px wide

        // Tooltip hit detection happens in screen coordinates (before any transform).
        ItemStack tooltipStack = null;
        int tooltipX = 0, tooltipY = 0;
        for (int i = 0; i < stacks.size(); i++) {
            // Icon occupies 16 logical units → 8 screen px.
            int iconScreenY = panelY + Math.round((13 + i * rowH) * SCALE);
            if (mouseX >= panelX && mouseX < panelX + 8
                    && mouseY >= iconScreenY && mouseY < iconScreenY + 8) {
                tooltipStack = stacks.get(i);
                tooltipX = mouseX;
                tooltipY = mouseY;
            }
        }

        // Push a 0.5× scale matrix anchored at (panelX, panelY).
        var pose = g.pose();
        pose.pushPose();
        pose.translate(panelX, panelY, 0);
        pose.scale(SCALE, SCALE, 1.0f);

        // "Prizes" header
        g.drawString(font, Component.translatable("slots.gui.prizes"), 0, 0, 0xFFFFFF, false);

        for (int i = 0; i < stacks.size(); i++) {
            ItemStack stack = stacks.get(i);
            int chance = chances.get(i);
            int rowY = 13 + i * rowH;

            // Item icon with stack-count decoration
            g.renderItem(stack, 0, rowY);
            g.renderItemDecorations(font, stack, 0, rowY);

            // Item name, truncated to panel width
            String name = stack.getHoverName().getString();
            int maxTextW = panelW - 22;
            if (font.width(name) > maxTextW) {
                while (name.length() > 1 && font.width(name + "..") > maxTextW) {
                    name = name.substring(0, name.length() - 1);
                }
                name += "..";
            }
            g.drawString(font, name,      19, rowY + 2,  0xFFFFFF, false);
            g.drawString(font, chance + "%", 19, rowY + 11, 0x55FF55, false);
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
        boolean outputFull = getSpecialResultCode(results) == SlotMachineBlockEntity.RESULT_TAKE_OUT_WIN;
        // stopButton: visible only while actively spinning and before stop was pressed / results arrived.
        stopButton.visible = !outOfService && !outputFull && !isRevealing && !stopPressed && isSpinning();
        // spinButton: hidden while spinning, while waiting for stop result, and during the reveal animation.
        spinButton.visible = !outOfService && !outputFull && !isRevealing && !stopPressed && !stopButton.visible;

        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
        if (Config.showPrizePanel) {
            renderPrizesPanel(guiGraphics, mouseX, mouseY);
        }

        if (!message.getString().isEmpty()) {
            int messageWidth = Minecraft.getInstance().font.width(this.message);
            int messageX = (int) ((this.width - messageWidth) / 2f);
            int messageY = (int) (this.height / 2f) - (outOfService ? 45 : 75);
            int color = outOfService ? 0xFF5555
                    : (outputFull ? 0xFFAA00 : 0xFFFFFF);
            guiGraphics.drawString(Minecraft.getInstance().font, this.message, messageX, messageY, color);
        }
    }
}
