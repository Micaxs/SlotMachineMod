package net.micaxs.slotmachine.block.entity;

import net.micaxs.slotmachine.Config;
import net.micaxs.slotmachine.screen.SlotMachineMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class SlotMachineBlockEntity extends BlockEntity implements MenuProvider {

    public static final int NUM_PRIZES = 5;
    // 6 rows × 9 cols – adjust here if you want a different grid size;
    // the matching STORAGE_SLOT_Y in SlotMachineOwnerMenu and
    // STORAGE_ROWS in the sub-screens must stay in sync.
    public static final int PRIZE_STOCK_SLOTS = 54;
    public static final int BET_STORAGE_SLOTS = 54;
    public static final int DATA_COUNT = 6;

    public static final int SERVICE_READY = 0;
    public static final int SERVICE_NOT_CONFIGURED = 1;
    public static final int SERVICE_OUT_OF_STOCK = 2;
    public static final int SERVICE_OUT_OF_ORDER = 3;

    public static final int RESULT_SPINNING = 0;
    public static final int RESULT_INVALID_BET = 6;
    public static final int RESULT_OUT_OF_STOCK = 7;
    public static final int RESULT_OUT_OF_ORDER = 8;
    public static final int RESULT_NOT_CONFIGURED = 9;
    public static final int RESULT_TAKE_OUT_WIN = 10;

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            markInventoryChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case INPUT_SLOT -> isValidBetItem(stack);
                case OUTPUT_SLOT -> false;
                default -> super.isItemValid(slot, stack);
            };
        }
    };

    private final ItemStackHandler betItemHandler = new ItemStackHandler(1) {
        @Override
        protected void onContentsChanged(int slot) {
            markInventoryChanged();
        }

        @Override
        public int getSlotLimit(int slot) {
            return 1;
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }
    };

    private final ItemStackHandler prizeItemHandler = new ItemStackHandler(NUM_PRIZES) {
        @Override
        protected void onContentsChanged(int slot) {
            markInventoryChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }
    };

    private final ItemStackHandler ownerPrizeStockHandler = new ItemStackHandler(PRIZE_STOCK_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            markInventoryChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }

        /**
         * Prevent NBT from resizing this handler below its declared size.
         * Old saves stored 27 slots; we keep the new size (54) and migrate the
         * existing items into the first N slots automatically.
         * The injected "Size" also forces the parent to reset the stacks array on
         * each sync, preventing stale client-side items after the stock runs out.
         */
        @Override
        public void deserializeNBT(net.minecraft.nbt.CompoundTag nbt) {
            net.minecraft.nbt.CompoundTag adjusted = nbt.copy();
            adjusted.putInt("Size", getSlots());
            super.deserializeNBT(adjusted);
        }
    };

    private final ItemStackHandler ownerBetStorageHandler = new ItemStackHandler(BET_STORAGE_SLOTS) {
        @Override
        protected void onContentsChanged(int slot) {
            markInventoryChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return true;
        }

        /**
         * Same fix as ownerPrizeStockHandler: force the declared size so the parent
         * resets the stacks array on every sync, preventing stale client-side data.
         */
        @Override
        public void deserializeNBT(net.minecraft.nbt.CompoundTag nbt) {
            net.minecraft.nbt.CompoundTag adjusted = nbt.copy();
            adjusted.putInt("Size", getSlots());
            super.deserializeNBT(adjusted);
        }
    };

    /**
     * Temporarily holds won prize items after {@link #stopSpin()} until the client's reel-stop
     * animation finishes and sends a {@code ClaimPayoutC2SPacket}.  Items are then moved to the
     * player-facing {@link #OUTPUT_SLOT} by {@link #releasePayout()}.
     */
    private final ItemStackHandler pendingPayoutHandler = new ItemStackHandler(1) {
        @Override
        public int getSlotLimit(int slot) {
            return Integer.MAX_VALUE; // temporary staging – no stack-size restriction needed
        }
    };

    private final int[] prizeChances = new int[NUM_PRIZES];

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IItemHandler> lazyBetItemHandler = LazyOptional.empty();
    private LazyOptional<IItemHandler> lazyPrizeItemHandler = LazyOptional.empty();
    private LazyOptional<IItemHandler> lazyPrizeStockHandler = LazyOptional.empty();
    private LazyOptional<IItemHandler> lazyBetStorageHandler = LazyOptional.empty();

    private int stopped = 1;
    private UUID ownerUUID;

    protected final ContainerData data;

    public SlotMachineBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.SLOT_MACHINE_BE.get(), pPos, pBlockState);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> SlotMachineBlockEntity.this.stopped;
                    case 1 -> SlotMachineBlockEntity.this.prizeChances[0];
                    case 2 -> SlotMachineBlockEntity.this.prizeChances[1];
                    case 3 -> SlotMachineBlockEntity.this.prizeChances[2];
                    case 4 -> SlotMachineBlockEntity.this.prizeChances[3];
                    case 5 -> SlotMachineBlockEntity.this.prizeChances[4];
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                switch (index) {
                    case 0 -> SlotMachineBlockEntity.this.stopped = value;
                    case 1 -> SlotMachineBlockEntity.this.prizeChances[0] = value;
                    case 2 -> SlotMachineBlockEntity.this.prizeChances[1] = value;
                    case 3 -> SlotMachineBlockEntity.this.prizeChances[2] = value;
                    case 4 -> SlotMachineBlockEntity.this.prizeChances[3] = value;
                    case 5 -> SlotMachineBlockEntity.this.prizeChances[4] = value;
                    default -> {
                    }
                }
            }

            @Override
            public int getCount() {
                return DATA_COUNT;
            }
        };
    }

    private void markInventoryChanged() {
        setChanged();
        if (level != null && !level.isClientSide()) {
            level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
        }
    }

    public void setOwner(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public Object getOwner() {
        return ownerUUID;
    }

    public ContainerData getData() {
        return data;
    }

    public void adjustPrizeChance(int prizeIndex, int delta) {
        if (prizeIndex < 0 || prizeIndex >= NUM_PRIZES) {
            return;
        }
        prizeChances[prizeIndex] = Math.max(0, Math.min(100, prizeChances[prizeIndex] + delta));
        markInventoryChanged();
    }

    public int getPrizeChance(int prizeIndex) {
        if (prizeIndex < 0 || prizeIndex >= NUM_PRIZES) {
            return 0;
        }
        return prizeChances[prizeIndex];
    }

    public ItemStack getConfiguredBetItem() {
        return betItemHandler.getStackInSlot(0).copy();
    }

    public ItemStack getPrizeTemplate(int prizeIndex) {
        if (prizeIndex < 0 || prizeIndex >= NUM_PRIZES) {
            return ItemStack.EMPTY;
        }
        return prizeItemHandler.getStackInSlot(prizeIndex).copy();
    }

    public ItemStack getDisplayBetItem() {
        ItemStack configured = getConfiguredBetItem();
        if (!configured.isEmpty()) {
            configured.setCount(1);
            return configured;
        }

        if (Config.validBetItems.size() == 1) {
            Item item = Config.validBetItems.iterator().next();
            return new ItemStack(item);
        }

        return ItemStack.EMPTY;
    }

    public boolean isConfigured() {
        boolean hasBetItem = !betItemHandler.getStackInSlot(0).isEmpty() || !Config.validBetItems.isEmpty();
        if (!hasBetItem) {
            return false;
        }

        for (int i = 0; i < NUM_PRIZES; i++) {
            if (!prizeItemHandler.getStackInSlot(i).isEmpty() && prizeChances[i] > 0) {
                return true;
            }
        }

        return false;
    }

    public int getServiceState() {
        if (!isConfigured()) {
            return SERVICE_NOT_CONFIGURED;
        }
        if (!hasRequiredPrizeStock()) {
            return SERVICE_OUT_OF_STOCK;
        }
        if (!canAcceptAnyCollectedBet()) {
            return SERVICE_OUT_OF_ORDER;
        }
        return SERVICE_READY;
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    public LazyOptional<IItemHandler> getBetItemHandler() {
        return lazyBetItemHandler;
    }

    public LazyOptional<IItemHandler> getPrizeItemHandler() {
        return lazyPrizeItemHandler;
    }

    public LazyOptional<IItemHandler> getPrizeStockHandler() {
        return lazyPrizeStockHandler;
    }

    public LazyOptional<IItemHandler> getBetStorageHandler() {
        return lazyBetStorageHandler;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        lazyBetItemHandler = LazyOptional.of(() -> betItemHandler);
        lazyPrizeItemHandler = LazyOptional.of(() -> prizeItemHandler);
        lazyPrizeStockHandler = LazyOptional.of(() -> ownerPrizeStockHandler);
        lazyBetStorageHandler = LazyOptional.of(() -> ownerBetStorageHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyBetItemHandler.invalidate();
        lazyPrizeItemHandler.invalidate();
        lazyPrizeStockHandler.invalidate();
        lazyBetStorageHandler.invalidate();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.slotmachinemod.slot_machine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        // Release any pending payout so the player always sees their items when the screen opens.
        releasePayout();
        return new SlotMachineMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.put("betItem", betItemHandler.serializeNBT());
        pTag.put("prizeItems", prizeItemHandler.serializeNBT());
        pTag.put("prizeStock", ownerPrizeStockHandler.serializeNBT());
        pTag.put("betStorage", ownerBetStorageHandler.serializeNBT());
        pTag.put("pendingPayout", pendingPayoutHandler.serializeNBT());
        pTag.putIntArray("prizeChances", prizeChances);
        pTag.putInt("slot_machine.stopped", stopped);
        if (ownerUUID != null) {
            pTag.putUUID("ownerUUID", ownerUUID);
        }
        super.saveAdditional(pTag);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        betItemHandler.deserializeNBT(pTag.getCompound("betItem"));
        prizeItemHandler.deserializeNBT(pTag.getCompound("prizeItems"));
        ownerPrizeStockHandler.deserializeNBT(pTag.getCompound("prizeStock"));
        ownerBetStorageHandler.deserializeNBT(pTag.getCompound("betStorage"));
        if (pTag.contains("pendingPayout")) {
            pendingPayoutHandler.deserializeNBT(pTag.getCompound("pendingPayout"));
        }
        if (pTag.contains("prizeChances")) {
            int[] saved = pTag.getIntArray("prizeChances");
            for (int i = 0; i < NUM_PRIZES && i < saved.length; i++) {
                prizeChances[i] = Math.max(0, Math.min(100, saved[i]));
            }
        }
        stopped = pTag.getInt("slot_machine.stopped");
        if (pTag.contains("ownerUUID")) {
            ownerUUID = pTag.getUUID("ownerUUID");
        }
    }

    @Override
    public CompoundTag getUpdateTag() {
        return saveWithoutMetadata();
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void setChanged() {
        super.setChanged();
        if (this.level != null && !this.level.isClientSide) {
            this.level.sendBlockUpdated(this.getBlockPos(), this.getBlockState(), this.getBlockState(), 3);
        }
    }

    public void tick(Level pLevel, BlockPos pPos, BlockState pState, BlockEntity pBlockEntity) {
        setChanged(pLevel, pPos, pState);
    }

    public int[] startSpin() {
        // Ensure any un-claimed payout from a previous spin is visible before the TAKE_OUT_WIN check.
        releasePayout();

        ItemStack potentialBet = itemHandler.getStackInSlot(INPUT_SLOT);
        int serviceState = getServiceState();

        if (serviceState != SERVICE_READY) {
            stopped = 1;
            return resultForServiceState(serviceState);
        }

        // Block spin while a previous win is still sitting in the output slot
        if (!itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
            stopped = 1;
            return blockedResult(RESULT_TAKE_OUT_WIN);
        }

        if (potentialBet.isEmpty() || !isValidBetItem(potentialBet)) {
            stopped = 1;
            return blockedResult(RESULT_INVALID_BET);
        }

        ItemStack wager = potentialBet.copy();
        wager.setCount(1);
        if (!storeCollectedBet(wager)) {
            stopped = 1;
            return resultForServiceState(SERVICE_OUT_OF_ORDER);
        }

        itemHandler.extractItem(INPUT_SLOT, 1, false);
        stopped = 0;
        markInventoryChanged();
        return new int[]{RESULT_SPINNING, RESULT_SPINNING, RESULT_SPINNING};
    }

    public int[] stopSpin() {
        double roll = Math.random();
        boolean tripleMatch = roll < Config.tripleWinChance;
        boolean doubleMatch = !tripleMatch && roll < Config.tripleWinChance + Config.doubleWinChance;

        if (tripleMatch || doubleMatch) {
            int selectedPrize = selectWeightedPrize();
            if (selectedPrize >= 0) {
                ItemStack prizeTemplate = prizeItemHandler.getStackInSlot(selectedPrize);
                int desiredCount = (Config.doubleTriplePayout && tripleMatch) ? 2 : 1;
                int available = countMatchingItems(ownerPrizeStockHandler, prizeTemplate);
                int actualCount = Math.min(desiredCount, available);
                if (actualCount > 0) {
                    ItemStack payout = prizeTemplate.copy();
                    payout.setCount(actualCount);
                    extractPrizeFromStock(payout);
                    // Park the prize; it is moved to the player output slot only after the
                    // client animation completes (via ClaimPayoutC2SPacket → releasePayout()).
                    pendingPayoutHandler.setStackInSlot(0, payout.copy());
                }
                stopped = 1;
                markInventoryChanged();
                return createCosmeticReels(true, tripleMatch);
            }
        }

        stopped = 1;
        markInventoryChanged();
        return createCosmeticReels(false, false);
    }

    /**
     * Selects a prize index by weighted random using each prize's configured chance as its weight.
     * Returns -1 if no prizes are enabled.
     */
    private int selectWeightedPrize() {
        int totalWeight = 0;
        for (int i = 0; i < NUM_PRIZES; i++) {
            if (!prizeItemHandler.getStackInSlot(i).isEmpty() && prizeChances[i] > 0) {
                totalWeight += prizeChances[i];
            }
        }
        if (totalWeight <= 0) return -1;

        int rand = (int) (Math.random() * totalWeight);
        int cumulative = 0;
        for (int i = 0; i < NUM_PRIZES; i++) {
            if (!prizeItemHandler.getStackInSlot(i).isEmpty() && prizeChances[i] > 0) {
                cumulative += prizeChances[i];
                if (rand < cumulative) {
                    return i;
                }
            }
        }
        return -1;
    }

    /**
     * @param anyWin     true if the player won (double or triple match)
     * @param tripleMatch true if all three reels should match (triple win); false = two-reel match
     */
    private int[] createCosmeticReels(boolean anyWin, boolean tripleMatch) {
        if (tripleMatch) {
            // All three reels show the same symbol
            int sym = (int) (Math.random() * 5 + 1);
            return new int[]{sym, sym, sym};
        } else if (anyWin) {
            // Two reels the same, one different (double match)
            int sym = (int) (Math.random() * 5 + 1);
            int other;
            do {
                other = (int) (Math.random() * 5 + 1);
            } while (other == sym);
            return new int[]{sym, sym, other};
        } else {
            // All three different (loss)
            int r1 = (int) (Math.random() * 5 + 1);
            int r2;
            do { r2 = (int) (Math.random() * 5 + 1); } while (r2 == r1);
            int r3;
            do { r3 = (int) (Math.random() * 5 + 1); } while (r3 == r1 || r3 == r2);
            return new int[]{r1, r2, r3};
        }
    }

    /** Returns true when the player-output slot contains an item. */
    public boolean hasOutputItem() {
        return !itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty();
    }

    /**
     * Moves any item in {@link #pendingPayoutHandler} into the player-facing output slot
     * (or drops it at the block if the slot is occupied).  Safe to call multiple times;
     * does nothing when the pending handler is empty.
     */
    public void releasePayout() {
        ItemStack pending = pendingPayoutHandler.getStackInSlot(0);
        if (!pending.isEmpty()) {
            pendingPayoutHandler.setStackInSlot(0, ItemStack.EMPTY);
            awardPrize(pending);
            markInventoryChanged();
        }
    }

    private void awardPrize(ItemStack prize) {
        ItemStack outputSlot = itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (outputSlot.isEmpty()) {
            itemHandler.setStackInSlot(OUTPUT_SLOT, prize);
            return;
        }

        if (ItemStack.isSameItemSameTags(outputSlot, prize)
                && outputSlot.getCount() + prize.getCount() <= outputSlot.getMaxStackSize()) {
            outputSlot.grow(prize.getCount());
            itemHandler.setStackInSlot(OUTPUT_SLOT, outputSlot);
            return;
        }

        if (level != null) {
            ItemEntity entity = new ItemEntity(level,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.0,
                    worldPosition.getZ() + 0.5,
                    prize);
            level.addFreshEntity(entity);
        }
    }

    private boolean isValidBetItem(ItemStack stack) {
        ItemStack configured = betItemHandler.getStackInSlot(0);
        if (!configured.isEmpty()) {
            return stack.getItem() == configured.getItem();
        }
        return Config.validBetItems.contains(stack.getItem());
    }

    private boolean canAcceptAnyCollectedBet() {
        ItemStack configured = betItemHandler.getStackInSlot(0);
        if (!configured.isEmpty()) {
            ItemStack singleBet = configured.copy();
            singleBet.setCount(1);
            return canInsertFully(ownerBetStorageHandler, singleBet);
        }

        for (Item item : Config.validBetItems) {
            if (canInsertFully(ownerBetStorageHandler, new ItemStack(item))) {
                return true;
            }
        }

        return false;
    }

    private boolean hasRequiredPrizeStock() {
        for (ItemStack requiredPrize : getRequiredPrizeStock()) {
            if (countMatchingItems(ownerPrizeStockHandler, requiredPrize) < requiredPrize.getCount()) {
                return false;
            }
        }
        return true;
    }

    private List<ItemStack> getRequiredPrizeStock() {
        List<ItemStack> requiredPrizes = new ArrayList<>();
        for (int i = 0; i < NUM_PRIZES; i++) {
            ItemStack prizeTemplate = prizeItemHandler.getStackInSlot(i);
            if (!prizeTemplate.isEmpty() && prizeChances[i] > 0) {
                mergeMatchingStack(requiredPrizes, prizeTemplate.copy());
            }
        }
        return requiredPrizes;
    }

    private void mergeMatchingStack(List<ItemStack> stacks, ItemStack stackToMerge) {
        for (ItemStack existing : stacks) {
            if (ItemStack.isSameItemSameTags(existing, stackToMerge)) {
                existing.grow(stackToMerge.getCount());
                return;
            }
        }
        stacks.add(stackToMerge);
    }

    private int countMatchingItems(ItemStackHandler handler, ItemStack template) {
        int total = 0;
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            ItemStack stack = handler.getStackInSlot(slot);
            if (ItemStack.isSameItemSameTags(stack, template)) {
                total += stack.getCount();
            }
        }
        return total;
    }

    private boolean extractPrizeFromStock(ItemStack template) {
        if (countMatchingItems(ownerPrizeStockHandler, template) < template.getCount()) {
            return false;
        }

        int remaining = template.getCount();
        for (int slot = 0; slot < ownerPrizeStockHandler.getSlots() && remaining > 0; slot++) {
            ItemStack stockStack = ownerPrizeStockHandler.getStackInSlot(slot);
            if (!ItemStack.isSameItemSameTags(stockStack, template)) {
                continue;
            }

            int toExtract = Math.min(remaining, stockStack.getCount());
            ownerPrizeStockHandler.extractItem(slot, toExtract, false);
            remaining -= toExtract;
        }

        return remaining == 0;
    }

    private boolean canInsertFully(ItemStackHandler handler, ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < handler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = handler.insertItem(slot, remaining, true);
        }
        return remaining.isEmpty();
    }

    private boolean storeCollectedBet(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = 0; slot < ownerBetStorageHandler.getSlots() && !remaining.isEmpty(); slot++) {
            remaining = ownerBetStorageHandler.insertItem(slot, remaining, false);
        }
        return remaining.isEmpty();
    }

    private int[] resultForServiceState(int serviceState) {
        return switch (serviceState) {
            case SERVICE_NOT_CONFIGURED -> blockedResult(RESULT_NOT_CONFIGURED);
            case SERVICE_OUT_OF_STOCK -> blockedResult(RESULT_OUT_OF_STOCK);
            case SERVICE_OUT_OF_ORDER -> blockedResult(RESULT_OUT_OF_ORDER);
            default -> blockedResult(RESULT_INVALID_BET);
        };
    }

    private int[] blockedResult(int result) {
        return new int[]{result, result, result};
    }

    public void drops() {
        dropItemHandlerContents(itemHandler);
    }

    public void dropAdminItems() {
        dropItemHandlerContents(betItemHandler);
        dropItemHandlerContents(prizeItemHandler);
        dropItemHandlerContents(ownerPrizeStockHandler);
        dropItemHandlerContents(ownerBetStorageHandler);
    }

    private void dropItemHandlerContents(ItemStackHandler handler) {
        if (level == null || level.isClientSide) {
            return;
        }

        SimpleContainer inventory = new SimpleContainer(handler.getSlots());
        for (int slot = 0; slot < handler.getSlots(); slot++) {
            inventory.setItem(slot, handler.getStackInSlot(slot));
        }
        Containers.dropContents(level, worldPosition, inventory);
    }

    public void dropSlotsMachine(Player player, BlockPos pos) {
        drops();
        dropAdminItems();
        level.addFreshEntity(new ItemEntity(level,
                player.position().x, player.position().y, player.position().z,
                new ItemStack(player.level().getBlockState(pos).getBlock().asItem()).copy()));
        player.level().removeBlock(pos, true);
    }
}
