package net.micaxs.slotmachine.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.micaxs.slotmachine.SlotMachineMod;
import net.micaxs.slotmachine.network.PacketHandler;
import net.micaxs.slotmachine.network.packet.SlotsC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.client.gui.components.Button;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Random;

public class SlotMachineScreen extends AbstractContainerScreen<SlotMachineMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_machine_gui.png");

    public SlotMachineScreen(SlotMachineMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    private int[] results = new int[3];
    private Button spinButton;
    private Button stopButton;
    private String message = "";

    private boolean outOfService;

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        results = new int[]{6,6,6};

        if (SlotMachineMenu.blockEntity.isSlotMachineInventoryFull()) {
            this.outOfService = true;
            this.message = "Out of Order";
        } else {
            this.outOfService = false;
            this.message = "";
        }


        if (!outOfService) {
            this.spinButton = new Button.Builder(Component.translatable("slots.gui.spin"), pButton -> {
                PacketHandler.sendToServer(new SlotsC2SPacket(SlotMachineMenu.blockEntity.getBlockPos(), true));
                results = new int[]{0, 0, 0}; // Add this line
            }).pos(x + 64, y + 68).size(44, 11).build();

            this.stopButton = new Button.Builder(Component.translatable("slots.gui.stop"), pButton -> {
                PacketHandler.sendToServer(new SlotsC2SPacket(SlotMachineMenu.blockEntity.getBlockPos(), false));
            }).pos(x + 64, y + 68).size(44, 11).build();

            this.addRenderableWidget(spinButton);
            this.addRenderableWidget(stopButton);
        }
    }

    public void updateResults(int[] newResults) {
        this.results = newResults;
        this.message = getResultMessage();
    }

    private String getResultMessage() {
        if (results[0] == results[1] && results[1] == results[2]) {
            if (results[0] != 6 && results[0] != 0) {
                return "You won!";
            } else {
                return "";
            }
        } else if (results[0] != results[1] && results[1] != results[2] && results[0] != results[2]) {
            return "You lost!";
        } else {
            return "2 out of 3!";
        }
    }

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);

        if (!outOfService) {
            if (results[0] != 0 && results[1] != 0 && results[2] != 0) {
                drawSlotImageInSlot(guiGraphics, x + 59, y + 21, results[0]);  // Slot 1
                drawSlotImageInSlot(guiGraphics, x + 78, y + 21, results[1]);  // Slot 2
                drawSlotImageInSlot(guiGraphics, x + 97, y + 21, results[2]);  // Slot 3
            } else {
                renderSlotWheels(guiGraphics, x, y);
            }
        }

    }

    private ResourceLocation[] lastImages = new ResourceLocation[3];
    private void renderSlotWheels(GuiGraphics guiGraphics, int x, int y) {
        SlotMachineMenu menu = this.menu;

        if (!areResutlsInYet()) {
            drawRandomImage(guiGraphics, x + 59, y + 21);  // Slot 1
            drawRandomImage(guiGraphics, x + 78, y + 21);  // Slot 2
            drawRandomImage(guiGraphics, x + 97, y + 21);  // Slot 3
        }
    }

    private ResourceLocation drawSlotImageInSlot(GuiGraphics guiGraphics, int x, int y, int slotImage) {
        List<ResourceLocation> images = Arrays.asList(
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_banana.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_bar.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_cherry.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_orange.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_strawberry.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_empty.png")
        );

        ResourceLocation image = images.get(slotImage - 1);
        drawImage(guiGraphics, image, x, y);

        return image;
    }

    private ResourceLocation drawRandomImage(GuiGraphics guiGraphics, int x, int y) {
        List<ResourceLocation> images = Arrays.asList(
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_banana.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_bar.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_cherry.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_orange.png"),
                new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_strawberry.png")
        );

        int randomIndex = new Random().nextInt(images.size());
        ResourceLocation image = images.get(randomIndex);

        drawImage(guiGraphics, image, x, y);

        return image;
    }

    private void drawImage(GuiGraphics guiGraphics, ResourceLocation image, int x, int y) {
        if (image != null) {
            //RenderSystem.setShaderTexture(0, image);
            guiGraphics.blit(image, x, y, 0, 0, 16, 43, 16, 43);
        }
    }

    private boolean areResutlsInYet() {
        return results[0] != 0 && results[1] != 0 && results[2] != 0;
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);

        if (!outOfService) {
            stopButton.visible = !areResutlsInYet();
            spinButton.visible = !stopButton.visible;

            // draw a filled rectangle of 100x50 in center of the window
            guiGraphics.fill(this.width / 2 - 50, this.height / 2 - 25, this.width / 2 + 50, this.height / 2 + 25, 0xFF00FF00); // 0xFF00FF00 is the color green

        }

        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        int messageWidth = Minecraft.getInstance().font.width(this.message);

        if (Objects.equals(this.message, "Out of Order")) {
            guiGraphics.drawString(Minecraft.getInstance().font, this.message, (int) ((this.width - messageWidth) / 2f), (int) (this.height / 2f) - 45, 0xFF0000);
        } else {
            guiGraphics.drawString(Minecraft.getInstance().font, this.message, (int) ((this.width - messageWidth) / 2f), (int) (this.height / 2f) - 75, 0xFFFFFF);
        }
    }
}
