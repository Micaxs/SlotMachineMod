package net.micaxs.slotmachine.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.micaxs.slotmachine.SlotMachineMod;
import net.micaxs.slotmachine.network.PacketHandler;
import net.micaxs.slotmachine.network.packet.SlotsC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class SlotMachineOwnerScreen extends AbstractContainerScreen<SlotMachineOwnerMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/slot_machine_admin_gui.png");

    public SlotMachineOwnerScreen(SlotMachineOwnerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    private int[] results = new int[3];
    private Button spinButton;
    private Button stopButton;
    private String message = "";

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        results = new int[]{6,6,6};
    }


    @Override
    protected void renderBg(GuiGraphics guiGraphics, float pPartialTick, int pMouseX, int pMouseY) {
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, TEXTURE);

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        guiGraphics.blit(TEXTURE, x, y, 0, 0, this.imageWidth, this.imageHeight);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);

        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);
    }
}
