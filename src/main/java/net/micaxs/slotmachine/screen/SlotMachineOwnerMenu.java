package net.micaxs.slotmachine.screen;

import net.micaxs.slotmachine.block.ModBlocks;
import net.micaxs.slotmachine.block.entity.SlotMachineBlockEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.SlotItemHandler;

public class SlotMachineOwnerMenu extends AbstractContainerMenu {

    public static final int VANILLA_SLOT_COUNT = 36;
    public static final int CONFIG_SLOT_COUNT = 1 + SlotMachineBlockEntity.NUM_PRIZES;
    public static final int STOCK_SLOT_COUNT = SlotMachineBlockEntity.PRIZE_STOCK_SLOTS;
    public static final int BET_STORAGE_SLOT_COUNT = SlotMachineBlockEntity.BET_STORAGE_SLOTS;

    public static final int BET_CONFIG_SLOT = VANILLA_SLOT_COUNT;
    public static final int PRIZE_TEMPLATE_START = BET_CONFIG_SLOT + 1;
    public static final int PRIZE_STOCK_START = PRIZE_TEMPLATE_START + SlotMachineBlockEntity.NUM_PRIZES;
    public static final int BET_STORAGE_START = PRIZE_STOCK_START + STOCK_SLOT_COUNT;
    // Storage-grid origin (item area, not frame).
    // Adjust STORAGE_SLOT_Y so it equals (your texture's slot-grid frame top + 1).
    // Must stay in sync with SLOT_GRID_Y in SlotMachineStockScreen /
    // SlotMachineBetStorageScreen  (STORAGE_SLOT_Y = SLOT_GRID_Y + 1).
    private static final int STORAGE_SLOT_X = 8;   // x offset of first slot column
    private static final int STORAGE_SLOT_Y = 34;  // y offset of first slot row (was 100)

    public static SlotMachineBlockEntity blockEntity;

    private final Level level;
    private final ContainerData data;

    // Actual slot counts recorded during construction – may differ from the
    // compile-time constants if an old save had fewer slots (e.g. 27 instead of 54).
    private int actualPrizeStockCount = 0;
    private int actualBetStorageStart = BET_STORAGE_START; // safe default
    private int actualBetStorageCount = 0;

    public SlotMachineOwnerMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv,
                inv.player.level().getBlockEntity(extraData.readBlockPos()),
                new SimpleContainerData(SlotMachineBlockEntity.DATA_COUNT));
    }

    public SlotMachineOwnerMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(ModMenuTypes.SLOT_MACHINE_OWNER_MENU.get(), pContainerId);
        blockEntity = (SlotMachineBlockEntity) entity;
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        // Bet Item Slot
        blockEntity.getBetItemHandler().ifPresent(handler ->
                this.addSlot(new GhostSlot(handler, 0, 12, 15)));

        blockEntity.getPrizeItemHandler().ifPresent(handler -> {
            for (int i = 0; i < SlotMachineBlockEntity.NUM_PRIZES; i++) {
                this.addSlot(new GhostSlot(handler, i, 12, 41 + i * 20));
            }
        });

        blockEntity.getPrizeStockHandler().ifPresent(handler -> {
            // Iterates all slots in the handler (PRIZE_STOCK_SLOTS wide, 9 per row).
            actualPrizeStockCount = handler.getSlots();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                int row = slot / 9;
                int col = slot % 9;

                // TODO: Adjust this to be correct
                this.addSlot(new ToggleableSlot(handler, slot,
                        STORAGE_SLOT_X + col * 18, STORAGE_SLOT_Y + row * 18, true));
            }
        });

        blockEntity.getBetStorageHandler().ifPresent(handler -> {
            // Iterates all slots in the handler (BET_STORAGE_SLOTS wide, 9 per row).
            actualBetStorageStart = PRIZE_STOCK_START + actualPrizeStockCount;
            actualBetStorageCount = handler.getSlots();
            for (int slot = 0; slot < handler.getSlots(); slot++) {
                int row = slot / 9;
                int col = slot % 9;

                // TODO: Adjust this to be correct
                this.addSlot(new ToggleableSlot(handler, slot,
                        STORAGE_SLOT_X + col * 18, STORAGE_SLOT_Y + row * 18, false));
            }
        });

        addDataSlots(data);
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (isGhostSlot(slotId)) {
            Slot slot = this.slots.get(slotId);
            ItemStack carried = getCarried();
            if (carried.isEmpty()) {
                slot.set(ItemStack.EMPTY);
            } else {
                ItemStack template = carried.copy();
                template.setCount(1); // ghost slots never hold stacks > 1
                slot.set(template);
            }
            broadcastChanges();
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    public int getPrizeChance(int prizeIndex) {
        return data.get(prizeIndex + 1);
    }

    public ItemStack getPrizeTemplate(int prizeIndex) {
        return blockEntity.getPrizeTemplate(prizeIndex);
    }

    public ItemStack getConfiguredBetItem() {
        return blockEntity.getConfiguredBetItem();
    }

    private boolean isGhostSlot(int slotId) {
        return slotId >= BET_CONFIG_SLOT && slotId < BET_CONFIG_SLOT + CONFIG_SLOT_COUNT;
    }

    @Override
    public ItemStack quickMoveStack(Player playerIn, int pIndex) {
        Slot sourceSlot = slots.get(pIndex);
        if (sourceSlot == null || !sourceSlot.hasItem()) {
            return ItemStack.EMPTY;
        }
        // Ghost config slots never participate in shift-click
        if (pIndex >= BET_CONFIG_SLOT && pIndex < BET_CONFIG_SLOT + CONFIG_SLOT_COUNT) {
            return ItemStack.EMPTY;
        }

        ItemStack stackInSlot = sourceSlot.getItem();
        ItemStack original    = stackInSlot.copy();
        boolean moved;

        if (pIndex >= BET_STORAGE_START && pIndex < BET_STORAGE_START + BET_STORAGE_SLOT_COUNT) {
            // Bet storage → player inventory
            moved = moveItemStackTo(stackInSlot, 0, VANILLA_SLOT_COUNT, true);

        } else if (pIndex >= PRIZE_STOCK_START && pIndex < PRIZE_STOCK_START + STOCK_SLOT_COUNT) {
            // Prize stock → player inventory
            moved = moveItemStackTo(stackInSlot, 0, VANILLA_SLOT_COUNT, true);

        } else if (pIndex < VANILLA_SLOT_COUNT) {
            // Player inventory → prize stock (bets are machine-managed, not manually deposited)
            moved = moveItemStackTo(stackInSlot,
                    PRIZE_STOCK_START, PRIZE_STOCK_START + STOCK_SLOT_COUNT, false);

        } else {
            return ItemStack.EMPTY;
        }

        if (!moved) {
            return ItemStack.EMPTY;
        }

        if (stackInSlot.isEmpty()) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }

        sourceSlot.onTake(playerIn, stackInSlot);
        return original;
    }

    @Override
    public boolean stillValid(Player pPlayer) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()),
                pPlayer, ModBlocks.SLOT_MACHINE.get());
    }

    private void addPlayerInventory(Inventory playerInventory) {
        for (int row = 0; row < 3; ++row) {
            for (int col = 0; col < 9; ++col) {
                this.addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 166 + row * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory playerInventory) {
        for (int col = 0; col < 9; ++col) {
            this.addSlot(new Slot(playerInventory, col, 8 + col * 18, 224));
        }
    }

    private static class GhostSlot extends SlotItemHandler {
        private boolean active = true;

        GhostSlot(net.minecraftforge.items.IItemHandler itemHandler, int index, int x, int y) {
            super(itemHandler, index, x, y);
        }

        public void setSlotActive(boolean active) {
            this.active = active;
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public boolean mayPickup(Player player) {
            return false;
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return true;
        }
    }

    // -------------------------------------------------------------------------
    // Helpers called by sub-screens when switching views
    // -------------------------------------------------------------------------

    /** Show or hide the ghost config slots (bet-item + prize templates). */
    public void setGhostSlotsActive(boolean active) {
        ((GhostSlot) slots.get(BET_CONFIG_SLOT)).setSlotActive(active);
        for (int i = 0; i < SlotMachineBlockEntity.NUM_PRIZES; i++) {
            ((GhostSlot) slots.get(PRIZE_TEMPLATE_START + i)).setSlotActive(active);
        }
    }

    /** Independently activate/deactivate the prize-stock and bet-storage grids. */
    public void setStorageSlotsActive(boolean prizeStock, boolean betStorage) {
        // Use actual registered counts (not compile-time constants) so old saves
        // that were loaded with fewer slots don't cause an IndexOutOfBoundsException.
        for (int i = 0; i < actualPrizeStockCount; i++) {
            ((ToggleableSlot) slots.get(PRIZE_STOCK_START + i)).setActive(prizeStock);
        }
        for (int i = 0; i < actualBetStorageCount; i++) {
            ((ToggleableSlot) slots.get(actualBetStorageStart + i)).setActive(betStorage);
        }
    }

    public static class ToggleableSlot extends SlotItemHandler {
        private boolean active;

        ToggleableSlot(net.minecraftforge.items.IItemHandler itemHandler, int index, int x, int y, boolean active) {
            super(itemHandler, index, x, y);
            this.active = active;
        }

        public void setActive(boolean active) {
            this.active = active;
        }

        @Override
        public boolean isActive() {
            return active;
        }

        @Override
        public boolean mayPickup(Player player) {
            // Do NOT gate on `active` — that flag is only synced to the client.
            // isActive() controls client-side rendering/click-detection; the server
            // must always allow extraction so the player can take items out when the
            // bet-storage or prize-stock sub-screen is open.
            return super.mayPickup(player);
        }

        @Override
        public boolean mayPlace(ItemStack stack) {
            return active && super.mayPlace(stack);
        }
    }
}
