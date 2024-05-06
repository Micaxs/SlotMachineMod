package net.micaxs.slotmachine.block.entity;

import net.micaxs.slotmachine.Config;
import net.micaxs.slotmachine.screen.BJMachineMenu;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class BJMachineBlockEntity extends BlockEntity implements MenuProvider {

    private final ItemStackHandler itemHandler = new ItemStackHandler(2) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            assert level != null;
            if (!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0 -> Config.validBetItems.contains(stack.getItem());
                case 1 -> false; // Don't put stuff in output slot you dum dum.
                default -> super.isItemValid(slot, stack);
            };
        }
    };

    private final ItemStackHandler ownerItemHandler = new ItemStackHandler(9) {
        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
            assert level != null;
            if (!level.isClientSide()) {
                level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
            }
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack) {
            return switch (slot) {
                case 0,1,2,3,4,5,6,7,8,9 -> Config.validBetItems.contains(stack.getItem());
                default -> super.isItemValid(slot, stack);
            };
        }
    };

    private static final int INPUT_SLOT = 0;
    private static final int OUTPUT_SLOT = 1;

    public ItemStack BET_ITEM;
    private LazyOptional<IItemHandler> lazyItemHandler = LazyOptional.empty();
    private LazyOptional<IItemHandler> lazyOwnerItemHandler = LazyOptional.empty();

    protected final ContainerData data;

    private int playing = 0;
    private UUID ownerUUID;

    public BJMachineBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.BJ_MACHINE_BE.get(), pPos, pBlockState);
        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> BJMachineBlockEntity.this.playing;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> BJMachineBlockEntity.this.playing = pValue;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    public void setOwner(UUID ownerUUID) {
        this.ownerUUID = ownerUUID;
    }

    public UUID getOwnerUUID() {
        return ownerUUID;
    }

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i = 0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    public void dropOwnerItems() {
        if (level != null && !level.isClientSide) {
            for (int i = 0; i < ownerItemHandler.getSlots(); i++) {
                ItemStack stack = ownerItemHandler.getStackInSlot(i);
                if (!stack.isEmpty()) {
                    ItemEntity itemEntity = new ItemEntity(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), stack);
                    level.addFreshEntity(itemEntity);
                }
            }
        }
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
        if (cap == ForgeCapabilities.ITEM_HANDLER) {
            return lazyItemHandler.cast();
        }
        return super.getCapability(cap, side);
    }

    public LazyOptional<IItemHandler> getOwnerItemHandler() {
        return lazyOwnerItemHandler;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        lazyItemHandler = LazyOptional.of(() -> itemHandler);
        lazyOwnerItemHandler = LazyOptional.of(() -> ownerItemHandler);
    }

    @Override
    public void invalidateCaps() {
        super.invalidateCaps();
        lazyItemHandler.invalidate();
        lazyOwnerItemHandler.invalidate();
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.slotmachinemod.bj_machine");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int pContainerId, Inventory pPlayerInventory, Player pPlayer) {
        return new BJMachineMenu(pContainerId, pPlayerInventory,this, this.data);
    }

    @Override
    protected void saveAdditional(CompoundTag pTag) {
        pTag.put("inventory", itemHandler.serializeNBT());
        pTag.put("ownerInventory", ownerItemHandler.serializeNBT());
        pTag.putInt("bj_machine.playing", playing);

        if (ownerUUID != null) {
            pTag.putUUID("ownerUUID", ownerUUID);
        }

        super.saveAdditional(pTag);
    }

    @Override
    public void load(CompoundTag pTag) {
        super.load(pTag);
        itemHandler.deserializeNBT(pTag.getCompound("inventory"));
        ownerItemHandler.deserializeNBT(pTag.getCompound("ownerInventory"));
        playing = pTag.getInt("bj_machine.playing");
        if (pTag.contains("ownerUUID")) {
            ownerUUID = pTag.getUUID("ownerUUID");
        }
    }

    public void tick(Level pLevel, BlockPos pPos, BlockState pState, BlockEntity pBlockEntity) {
        setChanged(pLevel, pPos, pState);
    }

    private void addToOwnerInventory() {
        ItemStack betItemOwner = new ItemStack(BET_ITEM.getItem()); // Get the item that was actually bet
        for (int i = 0; i < ownerItemHandler.getSlots(); i++) {
            ItemStack stackInSlot = ownerItemHandler.getStackInSlot(i);
            if (stackInSlot.isEmpty()) {
                ownerItemHandler.setStackInSlot(i, betItemOwner);
                break;
            } else if (Config.validBetItems.contains(stackInSlot.getItem()) && stackInSlot.getCount() < stackInSlot.getMaxStackSize() && stackInSlot.getItem() == betItemOwner.getItem()) {
                stackInSlot.setCount(stackInSlot.getCount() + 1);
                ownerItemHandler.setStackInSlot(i, stackInSlot);
                break;
            }
        }
    }

    private void removeFromOwnerInventory(int amount) {
        Item betItem = BET_ITEM.getItem(); // Get the item that was actually bet
        for (int i = ownerItemHandler.getSlots() - 1; i >= 0 && amount > 0; i--) {
            ItemStack stackInSlot = ownerItemHandler.getStackInSlot(i);
            if (!stackInSlot.isEmpty() && stackInSlot.getItem() == betItem) {
                int itemsInSlot = stackInSlot.getCount();
                if (itemsInSlot >= amount) {
                    stackInSlot.setCount(itemsInSlot - amount);
                    ownerItemHandler.setStackInSlot(i, stackInSlot);
                    break;
                } else {
                    amount -= itemsInSlot;
                    ownerItemHandler.setStackInSlot(i, ItemStack.EMPTY);
                }
            }
        }
    }

    public boolean isBJMachineInventoryFull() {
        for (int i = 0; i < ownerItemHandler.getSlots(); i++) {
            ItemStack stackInSlot = ownerItemHandler.getStackInSlot(i);
            if (stackInSlot.isEmpty() || stackInSlot.getCount() < stackInSlot.getMaxStackSize()) {
                return false;
            }
        }
        return true;
    }

    private void payout(int slot1, int slot2, int slot3) {
        // TODO: Do payout logic for BJ machine...
        addToOwnerInventory();

        // 3 the same -> x2
        if (slot1 == slot2 && slot2 == slot3) {
            // Remove 3 from the ownerItemHandler
            removeFromOwnerInventory(Config.triplePayoutAmount);

            ItemStack betItem = BET_ITEM;
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
            removeFromOwnerInventory(Config.doublePayoutAmount);

            ItemStack betItem = BET_ITEM;
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

//    public int[] stopSpin() {
//        double twoSameProbability = Config.doubleWinChance;
//        double threeSameProbability = Config.tripleWinChance;
//
//        double random = Math.random();
//
//        if (random < threeSameProbability) {
//            slot1 = slot2 = slot3 = (int) (Math.random() * 5 + 1);
//        } else if (random < twoSameProbability + threeSameProbability) {
//            slot1 = slot2 = (int) (Math.random() * 5 + 1);
//            do {
//                slot3 = (int) (Math.random() * 5 + 1);
//            } while ((slot3 == slot1 || slot3 == slot2) && (slot1 != slot2));
//        } else {
//            do {
//                slot1 = (int) (Math.random() * 5 + 1);
//                slot2 = (int) (Math.random() * 5 + 1);
//                slot3 = (int) (Math.random() * 5 + 1);
//            } while (slot1 == slot2 || slot2 == slot3 || slot1 == slot3);
//        }
//
//        // Casually just cheezing a loss if we don't have enough to payout the player!
//        int[] slots = checkBetItemAndPayout(slot1, slot2, slot3);
//        slot1 = slots[0];
//        slot2 = slots[1];
//        slot3 = slots[2];
//
//        // Call payout directly when the spin stops
//        payout(slot1, slot2, slot3);
//
//        setChanged();
//
//        return new int[]{slot1, slot2, slot3};
//    }
//
//    public int[] checkBetItemAndPayout(int slot1, int slot2, int slot3) {
//        Map<Item, Integer> itemCounts = new HashMap<>();
//        for (int i = 0; i < ownerItemHandler.getSlots(); i++) {
//            ItemStack stackInSlot = ownerItemHandler.getStackInSlot(i);
//            itemCounts.put(stackInSlot.getItem(), itemCounts.getOrDefault(stackInSlot.getItem(), 0) + stackInSlot.getCount());
//        }
//
//        if (itemCounts.containsKey(BET_ITEM.getItem()) && itemCounts.get(BET_ITEM.getItem()) >= Config.triplePayoutAmount) {
//            return new int[]{slot1, slot2, slot3};
//        } else {
//            int[] result = new int[3];
//            result[0] = (int) (Math.random() * 5 + 1);
//            do {
//                result[1] = (int) (Math.random() * 5 + 1);
//            } while (result[1] == result[0]);
//            do {
//                result[2] = (int) (Math.random() * 5 + 1);
//            } while (result[2] == result[0] || result[2] == result[1]);
//            return result;
//        }
//    }


//    public int[] startSpin() {
//        BET_ITEM = this.itemHandler.getStackInSlot(INPUT_SLOT);
//        ItemStack itemInOutputSlot = this.itemHandler.getStackInSlot(OUTPUT_SLOT);
//
//        // Fixes the ability to start spinning and take out the betItem to cheeze the machine.
//        this.itemHandler.extractItem(INPUT_SLOT, 1, false);
//
//        if (Config.validBetItems.contains(BET_ITEM.getItem())) {
//            if (itemInOutputSlot.isEmpty() || itemInOutputSlot.getItem() == BET_ITEM.getItem() && itemInOutputSlot.getCount() + Config.triplePayoutAmount < itemInOutputSlot.getMaxStackSize()) {
//                // We good to go...
//                return new int[]{0, 0, 0};
//            } else {
//                return new int[]{6, 6, 6};
//            }
//        } else {
//            return new int[]{6, 6, 6};
//        }
//
//    }

    public Object getOwner() {
        return ownerUUID;
    }
}
