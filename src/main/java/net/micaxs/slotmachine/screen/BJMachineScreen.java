package net.micaxs.slotmachine.screen;

import com.mojang.blaze3d.systems.RenderSystem;
import net.micaxs.slotmachine.SlotMachineMod;
import net.micaxs.slotmachine.network.PacketHandler;
import net.micaxs.slotmachine.network.packet.AddCreditsPacket;
import net.micaxs.slotmachine.network.packet.DeductCreditsPacket;
import net.micaxs.slotmachine.network.packet.RemoveCreditsPacket;
import net.micaxs.slotmachine.network.packet.UpdateBJBlockEntityPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class BJMachineScreen extends AbstractContainerScreen<BJMachineMenu> {

    private ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);
    private ScheduledFuture<?> futureTask = null;
    private boolean condition = false;


    private static final ResourceLocation TEXTURE =
            new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/bj_machine_gui.png");

    private static final ResourceLocation SCORE_TEXTURE =
            new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/score.png");

    private static final ResourceLocation C1 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/c1.png");
    private static final ResourceLocation C2 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/c2.png");
    private static final ResourceLocation C3 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/c3.png");
    private static final ResourceLocation C4 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/c4.png");
    private static final ResourceLocation C5 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/c5.png");
    private static final ResourceLocation C6 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/c6.png");
    private static final ResourceLocation C7 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/c7.png");
    private static final ResourceLocation C8 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/c8.png");
    private static final ResourceLocation C9 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/c9.png");
    private static final ResourceLocation C10 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/c10.png");
    private static final ResourceLocation CJ = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/cj.png");
    private static final ResourceLocation CQ = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/cq.png");
    private static final ResourceLocation CK = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/ck.png");

    private static final ResourceLocation H1 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/h1.png");
    private static final ResourceLocation H2 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/h2.png");
    private static final ResourceLocation H3 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/h3.png");
    private static final ResourceLocation H4 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/h4.png");
    private static final ResourceLocation H5 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/h5.png");
    private static final ResourceLocation H6 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/h6.png");
    private static final ResourceLocation H7 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/h7.png");
    private static final ResourceLocation H8 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/h8.png");
    private static final ResourceLocation H9 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/h9.png");
    private static final ResourceLocation H10 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/h10.png");
    private static final ResourceLocation HJ = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/hj.png");
    private static final ResourceLocation HQ = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/hq.png");
    private static final ResourceLocation HK = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/hk.png");

    private static final ResourceLocation D1 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/d1.png");
    private static final ResourceLocation D2 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/d2.png");
    private static final ResourceLocation D3 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/d3.png");
    private static final ResourceLocation D4 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/d4.png");
    private static final ResourceLocation D5 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/d5.png");
    private static final ResourceLocation D6 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/d6.png");
    private static final ResourceLocation D7 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/d7.png");
    private static final ResourceLocation D8 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/d8.png");
    private static final ResourceLocation D9 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/d9.png");
    private static final ResourceLocation D10 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/d10.png");
    private static final ResourceLocation DJ = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/dj.png");
    private static final ResourceLocation DQ = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/dq.png");
    private static final ResourceLocation DK = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/dk.png");

    private static final ResourceLocation S1 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/s1.png");
    private static final ResourceLocation S2 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/s2.png");
    private static final ResourceLocation S3 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/s3.png");
    private static final ResourceLocation S4 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/s4.png");
    private static final ResourceLocation S5 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/s5.png");
    private static final ResourceLocation S6 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/s6.png");
    private static final ResourceLocation S7 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/s7.png");
    private static final ResourceLocation S8 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/s8.png");
    private static final ResourceLocation S9 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/s9.png");
    private static final ResourceLocation S10 = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/s10.png");
    private static final ResourceLocation SJ = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/sj.png");
    private static final ResourceLocation SQ = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/sq.png");
    private static final ResourceLocation SK = new ResourceLocation(SlotMachineMod.MOD_ID, "textures/gui/cards/sk.png");


    public BJMachineScreen(BJMachineMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
    }

    private Button addCreditsButton;
    private Button retrieveCreditsButton;

    private Button dealHandButton;
    private Button hitButton;
    private Button standButton;

    public int credits = 0;
    public boolean payoutSent = false;

    public boolean cancelTask = false;

    public void addCredits(int amount, boolean insert) {
        credits += amount;
        PacketHandler.sendToServer(new AddCreditsPacket(BJMachineMenu.blockEntity.getBlockPos(), amount, insert));
    }
    public void removeAllCredits() {
        credits = 0;
        PacketHandler.sendToServer(new RemoveCreditsPacket(BJMachineMenu.blockEntity.getBlockPos()));
    }
    public void removeCredits(int amount) {
        credits -= amount;
        PacketHandler.sendToServer(new DeductCreditsPacket(BJMachineMenu.blockEntity.getBlockPos(), amount));
    }


    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY = 10000;

        int x = (this.width - this.imageWidth) / 2;
        int y = (this.height - this.imageHeight) / 2;


        this.addCreditsButton = new Button.Builder(Component.translatable("bj.gui.add_credits"), pButton -> {
            Item itemInSlot = BJMachineMenu.blockEntity.getInventory().getStackInSlot(0).getItem();
            if (itemInSlot == Items.DIAMOND) {
                this.addCredits(1, true);
            }
        }).pos(x + 8, y + 29).size(18, 10).build();

        this.retrieveCreditsButton = new Button.Builder(Component.translatable("bj.gui.retrieve_credits"), pButton -> {
            this.removeAllCredits();
        }).pos(x + 150, y + 29).size(18, 10).build();

        this.dealHandButton = new Button.Builder(Component.translatable("bj.gui.deal"), pButton -> {
            if (futureTask != null) {
                futureTask.cancel(false);
            }
            isResetting = false;
            this.menu.dealNewHand();
            this.removeCredits(1);
            this.payoutSent = false;
        }).pos(x + 58, y + 69).size(60, 12).build();

        this.hitButton = new Button.Builder(Component.translatable("bj.gui.hit"), pButton -> {
            this.menu.hit();
        }).pos(x + 50, y + 69).size(30, 12).build();

        this.standButton = new Button.Builder(Component.translatable("bj.gui.stand"), pButton -> {
            this.menu.stand();
        }).pos(x + 82, y + 69).size(35, 12).build();


        this.addRenderableWidget(addCreditsButton);
        this.addRenderableWidget(retrieveCreditsButton);
        this.addRenderableWidget(dealHandButton);
        this.addRenderableWidget(hitButton);
        this.addRenderableWidget(standButton);

        dealHandButton.visible = false;
    }

    public void updateCredits(int newCredits) {
        this.credits = newCredits;
    }

    private ResourceLocation getCardResourceLocation(String card) {
        switch (card) {
            case "C1":
                return C1;
            case "C2":
                return C2;
            case "C3":
                return C3;
            case "C4":
                return C4;
            case "C5":
                return C5;
            case "C6":
                return C6;
            case "C7":
                return C7;
            case "C8":
                return C8;
            case "C9":
                return C9;
            case "C10":
                return C10;
            case "CJ":
                return CJ;
            case "CQ":
                return CQ;
            case "CK":
                return CK;
            case "H1":
                return H1;
            case "H2":
                return H2;
            case "H3":
                return H3;
            case "H4":
                return H4;
            case "H5":
                return H5;
            case "H6":
                return H6;
            case "H7":
                return H7;
            case "H8":
                return H8;
            case "H9":
                return H9;
            case "H10":
                return H10;
            case "HJ":
                return HJ;
            case "HQ":
                return HQ;
            case "HK":
                return HK;
            case "D1":
                return D1;
            case "D2":
                return D2;
            case "D3":
                return D3;
            case "D4":
                return D4;
            case "D5":
                return D5;
            case "D6":
                return D6;
            case "D7":
                return D7;
            case "D8":
                return D8;
            case "D9":
                return D9;
            case "D10":
                return D10;
            case "DJ":
                return DJ;
            case "DQ":
                return DQ;
            case "DK":
                return DK;
            case "S1":
                return S1;
            case "S2":
                return S2;
            case "S3":
                return S3;
            case "S4":
                return S4;
            case "S5":
                return S5;
            case "S6":
                return S6;
            case "S7":
                return S7;
            case "S8":
                return S8;
            case "S9":
                return S9;
            case "S10":
                return S10;
            case "SJ":
                return SJ;
            case "SQ":
                return SQ;
            case "SK":
                return SK;
            default:
                throw new IllegalArgumentException("Invalid card: " + card);
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

        if (BJMachineMenu.blockEntity.isPlaying()) {
            String[] dealerCards = this.menu.getDealerCards();
            String[] playerCards = this.menu.getPlayerCards();

            drawDealerCards(guiGraphics, dealerCards);
            drawPlayerCards(guiGraphics, playerCards);

        }

    }

    private ResourceLocation drawCard(GuiGraphics guiGraphics, int x, int y, String card) {
        // Get the image for the card
        ResourceLocation cardImage = getCardResourceLocation(card);

        // Draw the image at the specified coordinates
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, cardImage);
        guiGraphics.blit(cardImage, x, y, 0, 0, 15, 22, 15, 22);

        // Return the image that was drawn
        return cardImage;
    }


    private void drawPlayerCards(GuiGraphics guiGraphics, String[] playerCards) {
        int x = (this.width - this.imageWidth) / 2;
        int cardWidth = 15;
        int nonNullCards = 0;
        for (String card : playerCards) {
            if (card != null) {
                nonNullCards++;
            }
        }
        int totalWidth = nonNullCards * cardWidth + (nonNullCards - 1) * 2;
        int center = x + 176 / 2;
        int startX = center - totalWidth / 2;
        int startY = (this.height - this.imageHeight) / 2 + 40;
        for (int i = 0; i < playerCards.length; i++) {
            if (playerCards[i] != null) {
                int cardX = startX + i * (cardWidth + 2);
                drawCard(guiGraphics, cardX, startY, playerCards[i]);
            }
        }
    }

    private void drawDealerCards(GuiGraphics guiGraphics, String[] dealerCards) {
        int x = (this.width - this.imageWidth) / 2;
        int cardWidth = 15;
        int nonNullCards = 0;
        for (String card : dealerCards) {
            if (card != null) {
                nonNullCards++;
            }
        }
        int totalWidth = nonNullCards * cardWidth + (nonNullCards - 1) * 2;
        int center = x + 176 / 2;
        int startX = center - totalWidth / 2;
        int startY = (this.height - this.imageHeight) / 2 + 7;
        for (int i = 0; i < dealerCards.length; i++) {
            if (dealerCards[i] != null) {
                int cardX = startX + i * (cardWidth + 2);
                drawCard(guiGraphics, cardX, startY, dealerCards[i]);
            }
        }
    }

    private void sendPayoutPacket(int amount) {
        if (!payoutSent) {
            this.addCredits(amount, false);
            payoutSent = true;
        }
    }

    @Override
    public void removed() {
        super.removed();
        BJMachineMenu.blockEntity.resetGame();
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float delta) {
        renderBackground(guiGraphics);

        if (BJMachineMenu.blockEntity.isPlaying()) {
            dealHandButton.visible = false;
            hitButton.visible = true;
            standButton.visible = true;

            addCreditsButton.visible = false;
            retrieveCreditsButton.visible = false;
        } else {
            if (credits == 0) {
                dealHandButton.visible = false;
            } else {
                dealHandButton.visible = true;
            }

            hitButton.visible = false;
            standButton.visible = false;
            addCreditsButton.visible = true;
            retrieveCreditsButton.visible = true;
        }

        super.render(guiGraphics, mouseX, mouseY, delta);
        renderTooltip(guiGraphics, mouseX, mouseY);

        // Draw the current Credits Text!
        drawCredits(guiGraphics);

        if (BJMachineMenu.blockEntity.playerBusted) {
            drawDealerCards(guiGraphics, this.menu.getDealerCards());
            drawPlayerCards(guiGraphics, this.menu.getPlayerCards());
            drawScore(guiGraphics);
            int bustedWidth = Minecraft.getInstance().font.width("Player Busted!");
            guiGraphics.drawString(Minecraft.getInstance().font, "Player Busted!", (int) ((this.width - bustedWidth) / 2f), (int) (this.height / 2f) - 53, 0xFF0000);
            resetGameAfterDelay();
        } else if (BJMachineMenu.blockEntity.dealerBusted) {
            drawDealerCards(guiGraphics, this.menu.getDealerCards());
            drawPlayerCards(guiGraphics, this.menu.getPlayerCards());
            drawScore(guiGraphics);
            sendPayoutPacket(2);
            int bustedWidth = Minecraft.getInstance().font.width("Player Wins!");
            guiGraphics.drawString(Minecraft.getInstance().font, "Player Wins!", (int) ((this.width - bustedWidth) / 2f), (int) (this.height / 2f) - 53, 0x00FF00);
            resetGameAfterDelay();
        } else if (BJMachineMenu.blockEntity.dealerWins) {
            drawDealerCards(guiGraphics, this.menu.getDealerCards());
            drawPlayerCards(guiGraphics, this.menu.getPlayerCards());
            drawScore(guiGraphics);
            int bustedWidth = Minecraft.getInstance().font.width("Dealer Wins!");
            guiGraphics.drawString(Minecraft.getInstance().font, "Dealer Wins!", (int) ((this.width - bustedWidth) / 2f), (int) (this.height / 2f) - 53, 0xFF0000);
            resetGameAfterDelay();
        } else if (BJMachineMenu.blockEntity.playerWins) {
            drawDealerCards(guiGraphics, this.menu.getDealerCards());
            drawPlayerCards(guiGraphics, this.menu.getPlayerCards());
            drawScore(guiGraphics);
            sendPayoutPacket(2);
            int bustedWidth = Minecraft.getInstance().font.width("Player Wins!");
            guiGraphics.drawString(Minecraft.getInstance().font, "Player Wins!", (int) ((this.width - bustedWidth) / 2f), (int) (this.height / 2f) - 53, 0x00FF00);
            resetGameAfterDelay();
        } else if (BJMachineMenu.blockEntity.isDraw) {
            drawDealerCards(guiGraphics, this.menu.getDealerCards());
            drawPlayerCards(guiGraphics, this.menu.getPlayerCards());
            drawScore(guiGraphics);
            sendPayoutPacket(1);
            int bustedWidth = Minecraft.getInstance().font.width("Push!");
            guiGraphics.drawString(Minecraft.getInstance().font, "Push!", (int) ((this.width - bustedWidth) / 2f), (int) (this.height / 2f) - 53, 0xFFFFFF);
            resetGameAfterDelay();
        } else if (BJMachineMenu.blockEntity.playerBlackjack) {
            drawDealerCards(guiGraphics, this.menu.getDealerCards());
            drawPlayerCards(guiGraphics, this.menu.getPlayerCards());
            drawScore(guiGraphics);
            sendPayoutPacket(3);
            int bustedWidth = Minecraft.getInstance().font.width("Blackjack!");
            guiGraphics.drawString(Minecraft.getInstance().font, "Blackjack!", (int) ((this.width - bustedWidth) / 2f), (int) (this.height / 2f) - 53, 0xFF9900);
            resetGameAfterDelay();
        }

        if (BJMachineMenu.blockEntity.isPlaying()) {
            // Draw the card value
            drawScore(guiGraphics);
        }

    }

    private void drawCredits(GuiGraphics guiGraphics) {
        int messageWidth = Minecraft.getInstance().font.width("Credits: " + credits);
        guiGraphics.drawString(Minecraft.getInstance().font, "Credits: " + credits, (int) ((this.width - messageWidth) / 2f), (int) (this.height / 2f) - 92, 0xFFFFFF);
    }

    private void drawScore(GuiGraphics guiGraphics) {
        int x = (this.width - this.imageWidth) / 2 + 168;
        int y = (this.height - this.imageHeight) / 2 + 50;

        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.setShaderTexture(0, SCORE_TEXTURE);
        guiGraphics.blit(SCORE_TEXTURE, x, y, 0, 0, 85, 33, 85, 33);

        int dealerCardValueText = Minecraft.getInstance().font.width("Dealer: " + BJMachineMenu.blockEntity.dealerHand);
        guiGraphics.drawString(Minecraft.getInstance().font, "Dealer: " + BJMachineMenu.blockEntity.dealerHand, x + 10, y + 4, 0xFFFFFF);
        int playerCardValueText = Minecraft.getInstance().font.width("Player: " + BJMachineMenu.blockEntity.playerHand);
        guiGraphics.drawString(Minecraft.getInstance().font, "Player: " + BJMachineMenu.blockEntity.playerHand, x + 10, y + 20, 0xFFFFFF);
    }

    private boolean isResetting = false;
    public void resetGameAfterDelay() {
        if (isResetting) {
            return;
        }

        Runnable task = () -> {
            if (!cancelTask) {
                BJMachineMenu.blockEntity.resetGame();
                futureTask.cancel(false);
            }
            isResetting = false;
        };

        futureTask = executorService.schedule(task, 3, TimeUnit.SECONDS);
        isResetting = true;
    }
}
