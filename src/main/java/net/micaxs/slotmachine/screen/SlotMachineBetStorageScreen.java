package net.micaxs.slotmachine.screen;

import net.micaxs.slotmachine.SlotMachineMod;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/**
 * Separate screen for the owner to collect stored player bets.
 * Opened from {@link SlotMachineOwnerScreen} via the "Collected Bets" button.
 * Uses the same {@link SlotMachineOwnerMenu} instance so the container
 * stays open on the server when navigating between sub-screens.
 *
 * Layout mirrors {@link SlotMachineStockScreen} (176 × 248 px); only the
 * active slot set and label text differ.
 */
public class SlotMachineBetStorageScreen extends AbstractContainerScreen<SlotMachineOwnerMenu> {

    private static final int PANEL_WIDTH  = 176;
    private static final int PANEL_HEIGHT = 248;

    // ── Background texture ────────────────────────────────────────────────────
    // Shared with the Prize Stock screen. 256×256 PNG; UI occupies top-left 176×248.
    private static final ResourceLocation TEXTURE = new ResourceLocation(
            SlotMachineMod.MOD_ID, "textures/gui/slot_machine_owner_storage_gui.png");

    // ── Slot-grid layout ──────────────────────────────────────────────────────
    // Adjust SLOT_GRID_X / SLOT_GRID_Y so the slot frames line up with your texture.
    // SLOT_GRID_Y must equal (SlotMachineOwnerMenu.STORAGE_SLOT_Y - 1) so the
    // interactive slot area sits 1 px inside the drawn frame.
    private static final int SLOT_GRID_X = 7;   // x offset of the frame's left edge
    private static final int SLOT_GRID_Y = 47;  // y offset of the frame's top edge (slot item area = SLOT_GRID_Y + 1)
    private static final int STORAGE_ROWS = 6;  // rows in the grid (6 rows × 9 cols = 54 slots)

    // ── Text positions ────────────────────────────────────────────────────────
    // Adjust each pair to match label areas in your texture.
    private static final int TEXT_TITLE_X     = 6;  private static final int TEXT_TITLE_Y     = 3;
    private static final int TEXT_HINT_X      = 8;  private static final int TEXT_HINT_Y      = 36;
    private static final int TEXT_INVENTORY_X = 7;  private static final int TEXT_INVENTORY_Y = 155;

    // ── Back-button position ──────────────────────────────────────────────────
    // Adjust BTN_BACK_X / BTN_BACK_Y to move the ← Back button.
    private static final int BTN_BACK_X = 7;  private static final int BTN_BACK_Y = 10;
    private static final int BTN_BACK_W = 60; private static final int BTN_BACK_H = 14;

    private final Inventory playerInventory;

    public SlotMachineBetStorageScreen(SlotMachineOwnerMenu menu, Inventory playerInventory, Component title) {
        super(menu, playerInventory, title);
        this.playerInventory = playerInventory;
        this.imageWidth  = PANEL_WIDTH;
        this.imageHeight = PANEL_HEIGHT;
    }

    @Override
    protected void init() {
        super.init();
        this.inventoryLabelY = 10000;
        this.titleLabelY     = 10000;
        menu.setGhostSlotsActive(false);
        menu.setStorageSlotsActive(false, true);
        // ← Back button is drawn manually in render() and handled in mouseClicked().
    }

    @Override
    protected void renderBg(GuiGraphics g, float pPartialTick, int pMouseX, int pMouseY) {
        int x = this.leftPos;
        int y = this.topPos;

        // NOTE: the background texture is blitted in render() *before* super.render().

        // ── Slot frames for the 6×9 storage grid ─────────────────────────────
        // Remove these if your texture already includes slot backgrounds.
//        for (int row = 0; row < STORAGE_ROWS; row++) {
//            for (int col = 0; col < 9; col++) {
//                SlotMachineOwnerScreen.drawSlotFrame(g,
//                        x + SLOT_GRID_X + col * 18,
//                        y + SLOT_GRID_Y + row * 18);
//            }
//        }
    }

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float delta) {
        renderBackground(g);
        g.blit(TEXTURE, this.leftPos, this.topPos, 0, 0, imageWidth, imageHeight, 256, 256);
        super.render(g, mouseX, mouseY, delta);
        renderTooltip(g, mouseX, mouseY);

        int x = this.leftPos;
        int y = this.topPos;

        // Title text – adjust TEXT_TITLE_X / TEXT_TITLE_Y
//        g.drawString(font, Component.translatable("slots.admin.bet_storage"),
//                x + TEXT_TITLE_X, y + TEXT_TITLE_Y, SlotMachineOwnerScreen.COL_LABEL, false);
//
//        // Hint text – adjust TEXT_HINT_X / TEXT_HINT_Y
//        g.drawString(font, Component.translatable("slots.admin.bet_storage_hint"),
//                x + TEXT_HINT_X, y + TEXT_HINT_Y, SlotMachineOwnerScreen.COL_HINT, false);

        // "Inventory" label – adjust TEXT_INVENTORY_X / TEXT_INVENTORY_Y
        g.drawString(font, Component.translatable("slots.admin.inventory"),
                x + TEXT_INVENTORY_X, y + TEXT_INVENTORY_Y, SlotMachineOwnerScreen.COL_HINT, false);

        // ← Back button – adjust BTN_BACK_X / BTN_BACK_Y / BTN_BACK_W / BTN_BACK_H
        drawButton(g, x + BTN_BACK_X, y + BTN_BACK_Y, BTN_BACK_W, BTN_BACK_H,
                Component.translatable("slots.admin.back").getString(), mouseX, mouseY);
    }

    private void drawButton(GuiGraphics g, int bx, int by, int bw, int bh,
                            String label, int mouseX, int mouseY) {
        boolean hovered = mouseX >= bx && mouseX < bx + bw && mouseY >= by && mouseY < by + bh;
        g.fill(bx, by, bx + bw, by + bh, SlotMachineOwnerScreen.COL_BTN_BORDER);
        g.fill(bx + 1, by + 1, bx + bw - 1, by + bh - 1,
                hovered ? SlotMachineOwnerScreen.COL_BTN_HOVER : SlotMachineOwnerScreen.COL_BTN_BG);
        int tx = bx + (bw - font.width(label)) / 2;
        int ty = by + (bh - 8) / 2;
        g.drawString(font, label, tx, ty, SlotMachineOwnerScreen.COL_BTN_TEXT, false);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int x = this.leftPos;
        int y = this.topPos;
        if (mouseX >= x + BTN_BACK_X && mouseX < x + BTN_BACK_X + BTN_BACK_W
                && mouseY >= y + BTN_BACK_Y && mouseY < y + BTN_BACK_Y + BTN_BACK_H) {
            this.minecraft.setScreen(new SlotMachineOwnerScreen(this.menu, this.playerInventory, this.title));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }
}
