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
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
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

public class SlotMachineBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            if (!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0 -> stack.getItem().equals(Config.validBetItem);
                case 1 -> false; // Don't put stuff in output slot you dum dum.
                default -> super.isItemValid(slot, stack);
            };
        }
    };
    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    public ItemStack BET_ITEM;

    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();

    protected final ContainerData data;

    private int slot1;
    private int slot2;
    private int slot3;
    private int stopped;


    public SlotMachineBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.SLOT_MACHINE_BE.get(), pPos, pBlockState);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> SlotMachineBlockEntity.this.stopped;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> SlotMachineBlockEntity.this.stopped = pValue;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }


    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
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
        return Component.translatable("block.slotmachinemod.slot_machine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new SlotMachineMenu(pContainerId, pPlayerInventory,this, this.data);
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

    public void tick(Level pLevel, BlockPos pPos, BlockState pState, BlockEntity pBlockEntity) {
        setChanged(pLevel, pPos, pState);
    }


    private void payout(int slot1, int slot2, int slot3) {
        // TODO: Add A Jackpot system (everytime someone loses the jackpot increases by 1 and it should be displayed in the UI somewhere)

        // 3 the same -> x2
        if (slot1 == slot2 && slot2 == slot3) {
            ItemStack betItem = this.itemHandler.extractItem(INPUT_SLOT, 1, false);
            betItem.setCount(Config.triplePayoutAmount);

            ItemStack outputSlotItem = this.itemHandler.getStackInSlot(OUTPUT_SLOT);
            if (outputSlotItem.isEmpty()) {
                this.itemHandler.setStackInSlot(OUTPUT_SLOT, betItem);
            } else if (outputSlotItem.getItem() == betItem.getItem()) {
                outputSlotItem.setCount(outputSlotItem.getCount() + Config.triplePayoutAmount);
                this.itemHandler.setStackInSlot(OUTPUT_SLOT, outputSlotItem);
            } else {
                // You screwed as you dun fucked up somehow.
            }
        } else if (slot1 == slot2 || slot2 == slot3 || slot1 == slot3) {
            ItemStack betItem = this.itemHandler.extractItem(INPUT_SLOT, 1, false);
            betItem.setCount(Config.doublePayoutAmount);

            ItemStack outputSlotItem = this.itemHandler.getStackInSlot(OUTPUT_SLOT);
            if (outputSlotItem.isEmpty()) {
                this.itemHandler.setStackInSlot(OUTPUT_SLOT, betItem);
            } else if (outputSlotItem.getItem() == betItem.getItem()) {
                outputSlotItem.setCount(outputSlotItem.getCount() + Config.doublePayoutAmount);
                this.itemHandler.setStackInSlot(OUTPUT_SLOT, outputSlotItem);
            } else {
                // You screwed as you dun fucked up somehow.
            }
        } else {
            this.itemHandler.extractItem(INPUT_SLOT, 1, false);
        }
        setChanged();
    }


    private boolean hasCoin() {
        return this.itemHandler.getStackInSlot(INPUT_SLOT).getItem() == Items.DIAMOND;
    }

    private boolean canInsertItemIntoOutputSlot(Item item) {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty() || this.itemHandler.getStackInSlot(OUTPUT_SLOT).is(item);
    }

    private boolean canInsertAmountIntoOutputSlot(int count) {
        return this.itemHandler.getStackInSlot(OUTPUT_SLOT).getCount() + count <= this.itemHandler.getStackInSlot(OUTPUT_SLOT).getMaxStackSize();
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

    public int[] stopSpin() {
        double twoSameProbability = Config.doubleWinChance;
        double threeSameProbability = Config.tripleWinChance;

        double random = Math.random();

        if (random < threeSameProbability) {
            slot1 = slot2 = slot3 = (int) (Math.random() * 5 + 1);
        } else if (random < twoSameProbability + threeSameProbability) {
            slot1 = slot2 = (int) (Math.random() * 5 + 1);
            do {
                slot3 = (int) (Math.random() * 5 + 1);
            } while ((slot3 == slot1 || slot3 == slot2) && (slot1 != slot2));
        } else {
            do {
                slot1 = (int) (Math.random() * 5 + 1);
                slot2 = (int) (Math.random() * 5 + 1);
                slot3 = (int) (Math.random() * 5 + 1);
            } while (slot1 == slot2 || slot2 == slot3 || slot1 == slot3);
        }

        // Call payout directly when the spin stops
        payout(slot1, slot2, slot3);

        setChanged();

        return new int[]{slot1, slot2, slot3};
    }


    public int[] startSpin() {
        BET_ITEM = this.itemHandler.getStackInSlot(INPUT_SLOT);
        ItemStack itemInOutputSlot = this.itemHandler.getStackInSlot(OUTPUT_SLOT);
        if (BET_ITEM.getItem().equals(Config.validBetItem)) {
            if (itemInOutputSlot.isEmpty() || itemInOutputSlot.getItem() == BET_ITEM.getItem() && itemInOutputSlot.getCount() + Config.triplePayoutAmount < itemInOutputSlot.getMaxStackSize()) {
                // We good to go...
                return new int[]{0, 0, 0};
            } else {
                return new int[]{6, 6, 6};
            }
        } else {
            return new int[]{6, 6, 6};
        }
    }
}
