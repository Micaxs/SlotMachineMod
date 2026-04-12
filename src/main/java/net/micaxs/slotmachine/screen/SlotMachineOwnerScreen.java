package net.micaxs.slotmachine.screen;

import net.micaxs.slotmachine.SlotMachineMod;
import net.micaxs.slotmachine.block.entity.SlotMachineBlockEntity;
import net.micaxs.slotmachine.network.PacketHandler;
import net.micaxs.slotmachine.network.packet.AdminConfigC2SPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

public class SlotMachineOwnerScreen extends AbstractContainerScreen<SlotMachineOwnerMenu> {

    // ── dimensions ──────────────────────────────────────────────────────────
    private static final int PANEL_WIDTH  = 176;
    private static final int PANEL_HEIGHT = 248;

    // ── background texture ───────────────────────────────────────────────────
    // 256×256 PNG; UI content occupies the top-left 176×248 region.
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            SlotMachineMod.MOD_ID, "textures/gui/slot_machine_owner_ui_main.png");

    // ── slot-frame positions (relative to panel top-left) ───────────────────
    // Remove any drawSlotFrame call in renderBg() if your texture already
    // includes the slot-background graphic for that position.
    private static final int BET_SLOT_FRAME_X  = 11;
    private static final int BET_SLOT_FRAME_Y  = 14;
    private static final int PRIZE_SLOT_X      = 11;
    private static final int PRIZE_SLOT_Y_BASE = 40;  // first prize row
    private static final int PRIZE_SLOT_STEP   = 20;  // vertical spacing per row

    // ── text positions (relative to panel top-left) ─────────────────────────
    // Adjust each pair to line up with your texture's label areas.
    private static final int TEXT_BET_LABEL_X    = 32;  private static final int TEXT_BET_LABEL_Y    = 15;
    private static final int TEXT_BET_VALUE_X    = 32;  private static final int TEXT_BET_VALUE_Y    = 24;
    private static final int TEXT_PRIZE_HDR_X    = 105;   private static final int TEXT_PRIZE_HDR_Y    = 32;
    private static final int TEXT_PRIZE_NAME_X   = 32;
    private static final int TEXT_PRIZE_ROW_Y    = 42;  // rowTopY = TEXT_PRIZE_ROW_Y + i * PRIZE_SLOT_STEP
    private static final int TEXT_PRIZE_CHANCE_X = 130; // chance % right-aligned to this x
    private static final int TEXT_INVENTORY_X    = 6;   private static final int TEXT_INVENTORY_Y    = 161;

    // ── button positions (relative to panel top-left) ───────────────────────
    // ± buttons per prize row (size 14 × 12)
    private static final int BTN_DEC_X    = 132;
    private static final int BTN_INC_X    = 148;
    private static final int BTN_OFFSET_Y = 2;   // added to rowTopY
    private static final int BTN_PM_W     = 14;
    private static final int BTN_PM_H     = 12;
    // Navigation buttons at bottom of prize table (size 76 × 14)
    private static final int BTN_NAV_Y        = 141;
    private static final int BTN_STOCK_X      = 8;   private static final int BTN_STOCK_W      = 76;
    private static final int BTN_BET_STORE_X  = 92;  private static final int BTN_BET_STORE_W  = 76;
    private static final int BTN_NAV_H        = 14;

    // ── colours (shared with sub-screens) ───────────────────────────────────
    static final int COL_BG           = 0xFF2B2D36;
    static final int COL_PANEL        = 0xFF383B47;
    static final int COL_HEADER       = 0xFF1C1E27;
    static final int COL_SEPARATOR    = 0xFF555870;
    static final int COL_LABEL        = 0xFFCCCCCC;
    static final int COL_CHANCE       = 0xFF88FF88;
    static final int COL_HINT         = 0xFF888888;
    static final int COL_SLOT         = 0xFF20232B;
    static final int COL_SLOT_BORDER  = 0xFF6A7084;
    // Button colours
    static final int COL_BTN_BG      = 0xFF4A4D5C;
    static final int COL_BTN_HOVER   = 0xFF5E6278;
    static final int COL_BTN_BORDER  = 0xFF6A7084;
    static final int COL_BTN_TEXT    = 0xFFFFFFFF;

    private final Inventory playerInventory;

    public SlotMachineOwnerScreen(SlotMachineOwnerMenu pMenu, Inventory pPlayerInventory, Component pTitle) {
        super(pMenu, pPlayerInventory, pTitle);
        this.playerInventory = pPlayerInventory;
        this.imageWidth  = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY     = 10000;
        menu.setGhostSlotsActive(true);
        menu.setStorageSlotsActive(false, false);
        // Buttons are drawn manually in render() and handled in mouseClicked().
        // No addRenderableWidget() calls needed for custom buttons.
    }

    // ── background ──────────────────────────────────────────────────────────

    @Override
    protected void renderBg(GuiGraphics g, float pPartialTick, int pMouseX, int pMouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // ── Slot frames ───────────────────────────────────────────────────────
        // Remove any call below if your texture already includes slot backgrounds.
       // drawSlotFrame(g, x + BET_SLOT_FRAME_X, y + BET_SLOT_FRAME_Y);
      //  for (int i = 0; i < SlotMachineBlockEntity.NUM_PRIZES; i++) {
      //      drawSlotFrame(g, x + PRIZE_SLOT_X, y + PRIZE_SLOT_Y_BASE + i * PRIZE_SLOT_STEP);
       // }
    }

    static void drawSlotFrame(GuiGraphics g, int x, int y) {
       // g.fill(x,     y,     x + 18, y + 18, COL_SLOT_BORDER);
       // g.fill(x + 1, y + 1, x + 17, y + 17, COL_SLOT);
    }

    // ── render ──────────────────────────────────────────────────────────────

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g);
        // Draw the background texture before super.render() so slot items and
        // custom buttons are always painted on top.
        // Texture is 256×256; UI content occupies the top-left 176×248 region.
        g.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        super.render(g, mouseX, mouseY, delta);
        renderTooltip(g, mouseX, mouseY);

        int x = this.leftPos;
        int y = this.topPos;

        // Title – adjust TEXT_TITLE_X / TEXT_TITLE_Y
//        g.drawString(font, Component.translatable("slots.admin.title"),
//                x + TEXT_TITLE_X, y + TEXT_TITLE_Y, COL_LABEL, false);


        // Bet item – adjust TEXT_BET_LABEL_* / TEXT_BET_VALUE_*
        ItemStack betStack = menu.getConfiguredBetItem();
        g.drawString(font, Component.translatable("slots.admin.bet_item"),
                x + TEXT_BET_LABEL_X, y + TEXT_BET_LABEL_Y, COL_LABEL, false);
        if (betStack.isEmpty()) {
            g.drawString(font, Component.translatable("slots.admin.bet_hint"),
                    x + TEXT_BET_VALUE_X, y + TEXT_BET_VALUE_Y, COL_HINT, false);
        } else {
            g.drawString(font, betStack.getHoverName(),
                    x + TEXT_BET_VALUE_X, y + TEXT_BET_VALUE_Y, 0xFFFFFF, false);
        }


        // Prize table header – adjust TEXT_PRIZE_HDR_X / TEXT_PRIZE_HDR_Y
        g.drawString(font, Component.translatable("slots.admin.prize_table"),
                x + TEXT_PRIZE_HDR_X, y + TEXT_PRIZE_HDR_Y, COL_LABEL, false);


        for (int i = 0; i < SlotMachineBlockEntity.NUM_PRIZES; i++) {
            // Row Y: adjust TEXT_PRIZE_ROW_Y (base) and PRIZE_SLOT_STEP (spacing)
            int rowTopY = y + TEXT_PRIZE_ROW_Y + i * PRIZE_SLOT_STEP;
            int textY   = rowTopY + 4;

            ItemStack prizeTemplate = menu.getPrizeTemplate(i);
            Component prizeLabel = prizeTemplate.isEmpty()
                    ? Component.translatable("slots.admin.prize_label", i + 1)
                    : prizeTemplate.getHoverName();
            g.drawString(font, prizeLabel, x + TEXT_PRIZE_NAME_X, textY, COL_LABEL, false);

            String chStr = menu.getPrizeChance(i) + "%";
            // Chance % right-aligned to TEXT_PRIZE_CHANCE_X – adjust as needed
            g.drawString(font, chStr,
                    x + TEXT_PRIZE_CHANCE_X - font.width(chStr), textY, COL_CHANCE, false);

            // ± buttons – drawn manually so they are always visible regardless of
            // Minecraft's widget rendering pipeline.  Adjust BTN_DEC_X / BTN_INC_X /
            // BTN_OFFSET_Y / BTN_PM_W / BTN_PM_H to match your texture.
            drawButton(g, x + BTN_DEC_X, rowTopY + BTN_OFFSET_Y, BTN_PM_W, BTN_PM_H,
                    "-", mouseX, mouseY);
            drawButton(g, x + BTN_INC_X, rowTopY + BTN_OFFSET_Y, BTN_PM_W, BTN_PM_H,
                    "+", mouseX, mouseY);
        }

        // Navigation buttons – adjust BTN_STOCK_X / BTN_BET_STORE_X / BTN_NAV_Y /
        // BTN_STOCK_W / BTN_BET_STORE_W / BTN_NAV_H to match your texture.
        drawButton(g, x + BTN_STOCK_X, y + BTN_NAV_Y, BTN_STOCK_W, BTN_NAV_H,
                Component.translatable("slots.admin.prize_stock").getString(), mouseX, mouseY);
        drawButton(g, x + BTN_BET_STORE_X, y + BTN_NAV_Y, BTN_BET_STORE_W, BTN_NAV_H,
                Component.translatable("slots.admin.bet_storage").getString(), mouseX, mouseY);

        // "Inventory" label – adjust TEXT_INVENTORY_X / TEXT_INVENTORY_Y
        g.drawString(font, Component.translatable("slots.admin.inventory"),
                x + TEXT_INVENTORY_X, y + TEXT_INVENTORY_Y, COL_HINT, false);
    }

    /** Draws a custom button rectangle with centred label and hover highlight. */
    private void drawButton(GuiGraphics g, int bx, int by, int bw, int bh,
                            String label, int mouseX, int mouseY) {
        boolean hovered = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
        // Border
        g.fill(bx, by, bx + bw, by + bh, COL_BTN_BORDER);
        // Background (hover-aware)
        g.fill(bx + 1, by + 1, bx + bw - 1, by + bh - 1, hovered ? COL_BTN_HOVER : COL_BTN_BG);
        // Centred label
        int tx = bx + (bw - font.width(label)) / 2;
        int ty = by + (bh - 8) / 2;
        g.drawString(font, label, tx, ty, COL_BTN_TEXT, false);
    }

    // ── click handling ───────────────────────────────────────────────────────

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = this.leftPos;
        int y = this.topPos;

        for (int i = 0; i < SlotMachineBlockEntity.NUM_PRIZES; i++) {
            int rowTopY = y + TEXT_PRIZE_ROW_Y + i * PRIZE_SLOT_STEP;
            // − button
            if (inBounds(mouseX, mouseY, x + BTN_DEC_X, rowTopY + BTN_OFFSET_Y, BTN_PM_W, BTN_PM_H)) {
                PacketHandler.sendToServer(new AdminConfigC2SPacket(
                        SlotMachineOwnerMenu.blockEntity.getBlockPos(), i, -1));
                return true;
            }
            // + button
            if (inBounds(mouseX, mouseY, x + BTN_INC_X, rowTopY + BTN_OFFSET_Y, BTN_PM_W, BTN_PM_H)) {
                PacketHandler.sendToServer(new AdminConfigC2SPacket(
                        SlotMachineOwnerMenu.blockEntity.getBlockPos(), i, 1));
                return true;
            }
        }

        // Prize Stock nav button
        if (inBounds(mouseX, mouseY, x + BTN_STOCK_X, y + BTN_NAV_Y, BTN_STOCK_W, BTN_NAV_H)) {
            this.minecraft.setScreen(new SlotMachineStockScreen(this.menu, this.playerInventory, this.title));
            return true;
        }
        // Bet Storage nav button
        if (inBounds(mouseX, mouseY, x + BTN_BET_STORE_X, y + BTN_NAV_Y, BTN_BET_STORE_W, BTN_NAV_H)) {
            this.minecraft.setScreen(new SlotMachineBetStorageScreen(this.menu, this.playerInventory, this.title));
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    private static boolean inBounds(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }
}
