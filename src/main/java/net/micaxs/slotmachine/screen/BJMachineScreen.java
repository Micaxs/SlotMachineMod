package net.micaxs.slotmachine.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.micaxs.slotmachine.SlotMachineMod;
import net.micaxs.slotmachine.network.PacketHandler;
import net.micaxs.slotmachine.network.packet.AddCreditsPacket;
import net.micaxs.slotmachine.network.packet.RemoveCreditsPacket;
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

public class BJMachineScreen extends AbstractContainerScreen<BJMachineMenu> {

    private static final ResourceLocation TEXTURE =
            new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/bj_machine_gui.png");

    public BJMachineScreen(BJMachineMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    private Button addCreditsButton;
    private Button retrieveCreditsButton;
    private String message = "";
    private int credits = 0;

    private boolean outOfService;

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;

        credits = BJMachineMenu.blockEntity.getCredits();

        this.addCreditsButton = new Button.Builder(Component.translatable("bj.gui.add_credits"), pButton -> {
            this.menu.addCredits();
            PacketHandler.sendToServer(new AddCreditsPacket(BJMachineMenu.blockEntity.getBlockPos(), 1));
        }).pos(x + 8, y + 29).size(18, 10).build();

        this.retrieveCreditsButton = new Button.Builder(Component.translatable("bj.gui.retrieve_credits"), pButton -> {
            this.menu.removeCredits();
            PacketHandler.sendToServer(new RemoveCreditsPacket(BJMachineMenu.blockEntity.getBlockPos()));
        }).pos(x + 150, y + 29).size(18, 10).build();

        this.addRenderableWidget(addCreditsButton);
        this.addRenderableWidget(retrieveCreditsButton);

    }

    public void updateCredits(int newCredits) {
        this.credits = newCredits;
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

        // Draw the current Credits Text!
        int messageWidth = Minecraft.getInstance().font.width("Credits: " + BJMachineMenu.blockEntity.getCredits());
        guiGraphics.drawString(Minecraft.getInstance().font, "Credits: " + BJMachineMenu.blockEntity.getCredits(), (int) ((this.width - messageWidth) / 2f), (int) (this.height / 2f) - 92, 0xFFFFFF);
    }
}
