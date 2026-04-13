package net.micaxs.slotmachine.block.entity;

import net.micaxs.slotmachine.Config;
import net.micaxs.slotmachine.screen.ServerSlotMachineMenu;
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

import java.util.List;

public class ServerSlotMachineBlockEntity extends BlockEntity implements MenuProvider {

    public static final int DATA_COUNT = 1;

    public static final int SERVICE_READY = 0;
    public static final int SERVICE_NOT_CONFIGURED = 1;

    public static final int RESULT_SPINNING      = 0;
    public static final int RESULT_INVALID_BET   = 6;
    public static final int RESULT_NOT_CONFIGURED = 9;
    public static final int RESULT_TAKE_OUT_WIN  = 10;

    private static final int INPUT_SLOT  = 0;
    private static final int OUTPUT_SLOT = 1;

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            markInventoryChanged();
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case INPUT_SLOT  -> isValidBetItem(stack);
                case OUTPUT_SLOT -> false;
                default -> super.isItemValid(slot, stack);
            };
        }
    };

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    private int stopped = 1;

    protected final ContainerData data;

    public ServerSlotMachineBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.SERVER_SLOT_MACHINE_BE.get(), pPos, pBlockState);
        this.data = new ContainerData() {
            @Override
            public int get(int index) {
                return index == 0 ? ServerSlotMachineBlockEntity.this.stopped : 0;
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) ServerSlotMachineBlockEntity.this.stopped = value;
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

    public ContainerData getData() {
        return data;
    }

    public boolean isConfigured() {
        if (Config.serverBetItem == null) return false;
        List<Config.ServerPrize> prizes = Config.serverPrizes;
        return prizes != null && !prizes.isEmpty();
    }

    public int getServiceState() {
        return isConfigured() ? SERVICE_READY : SERVICE_NOT_CONFIGURED;
    }

    private boolean isValidBetItem(ItemStack stack) {
        return Config.serverBetItem != null && stack.getItem() == Config.serverBetItem;
    }

    public boolean hasOutputItem() {
        return !itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty();
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.slotmachinemod.server_slot_machine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new ServerSlotMachineMenu(pContainerId, pPlayerInventory, this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.putInt("slot_machine.stopped", stopped);
        super.saveAdditional(pTag);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        stopped = pTag.getInt("slot_machine.stopped");
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
        ItemStack potentialBet = itemHandler.getStackInSlot(INPUT_SLOT);

        if (!isConfigured()) {
            stopped = 1;
            return blockedResult(RESULT_NOT_CONFIGURED);
        }

        if (!itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
            stopped = 1;
            return blockedResult(RESULT_TAKE_OUT_WIN);
        }

        if (potentialBet.isEmpty() || !isValidBetItem(potentialBet)) {
            stopped = 1;
            return blockedResult(RESULT_INVALID_BET);
        }

        // Consume the bet (discard it, don't store)
        itemHandler.extractItem(INPUT_SLOT, 1, false);
        stopped = 0;
        markInventoryChanged();
        return new int[]{RESULT_SPINNING, RESULT_SPINNING, RESULT_SPINNING};
    }

    public int[] stopSpin() {
        double roll = Math.random();
        boolean tripleMatch = roll < Config.serverTripleWinChance;
        boolean doubleMatch = !tripleMatch && roll < Config.serverTripleWinChance + Config.serverDoubleWinChance;

        if (tripleMatch || doubleMatch) {
            int selectedPrize = selectWeightedPrize();
            if (selectedPrize >= 0) {
                Config.ServerPrize prize = Config.serverPrizes.get(selectedPrize);
                int count = (Config.doubleTriplePayout && tripleMatch) ? 2 : 1;
                ItemStack payout = prize.toStack(count);
                awardPrize(payout);
                stopped = 1;
                markInventoryChanged();
                return createCosmeticReels(true, tripleMatch);
            }
        }

        stopped = 1;
        markInventoryChanged();
        return createCosmeticReels(false, false);
    }

    private int selectWeightedPrize() {
        if (Config.serverPrizes == null || Config.serverPrizes.isEmpty()) return -1;

        int totalWeight = 0;
        for (Config.ServerPrize prize : Config.serverPrizes) {
            totalWeight += prize.chance();
        }
        if (totalWeight <= 0) return -1;

        int rand = (int) (Math.random() * totalWeight);
        int cumulative = 0;
        for (int i = 0; i < Config.serverPrizes.size(); i++) {
            cumulative += Config.serverPrizes.get(i).chance();
            if (rand < cumulative) {
                return i;
            }
        }
        return -1;
    }

    private int[] createCosmeticReels(boolean anyWin, boolean tripleMatch) {
        if (tripleMatch) {
            int sym = (int) (Math.random() * 5 + 1);
            return new int[]{sym, sym, sym};
        } else if (anyWin) {
            int sym = (int) (Math.random() * 5 + 1);
            int other;
            do { other = (int) (Math.random() * 5 + 1); } while (other == sym);
            return new int[]{sym, sym, other};
        } else {
            int r1 = (int) (Math.random() * 5 + 1);
            int r2;
            do { r2 = (int) (Math.random() * 5 + 1); } while (r2 == r1);
            int r3;
            do { r3 = (int) (Math.random() * 5 + 1); } while (r3 == r1 || r3 == r2);
            return new int[]{r1, r2, r3};
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

    private int[] blockedResult(int result) {
        return new int[]{result, result, result};
    }

    /** Drop items currently in the input/output slots. */
    public void drops() {
        if (level == null || level.isClientSide) return;
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int slot = 0; slot < itemHandler.getSlots(); slot++) {
            inventory.setItem(slot, itemHandler.getStackInSlot(slot));
        }
        Containers.dropContents(level, worldPosition, inventory);
    }
}

