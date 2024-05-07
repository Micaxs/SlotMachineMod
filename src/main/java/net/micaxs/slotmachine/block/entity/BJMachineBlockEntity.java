package net.micaxs.slotmachine.block.entity;

import net.micaxs.slotmachine.Config;
import net.micaxs.slotmachine.screen.BJMachineMenu;
import net.micaxs.slotmachine.utils.DeckHandler;
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
    private int credits = 0;
    private final DeckHandler cardDeck = new DeckHandler();

    public int dealerHand = 0;
    public int playerHand = 0;


    private UUID ownerUUID;

    public BJMachineBlockEntity(BlockPos pPos, BlockState pBlockState) {
        super(ModBlockEntities.BJ_MACHINE_BE.get(), pPos, pBlockState);

        this.data = new ContainerData() {
            @Override
            public int get(int pIndex) {
                return switch (pIndex) {
                    case 0 -> BJMachineBlockEntity.this.playing;
                    case 1 -> BJMachineBlockEntity.this.credits;
                    default -> 0;
                };
            }

            @Override
            public void set(int pIndex, int pValue) {
                switch (pIndex) {
                    case 0 -> BJMachineBlockEntity.this.playing = pValue;
                    case 1 -> BJMachineBlockEntity.this.credits = pValue;
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


    public void payout(int winnings) {
        System.out.println("Payout");
        System.out.println("Winnings: " + winnings);
        credits += winnings;
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

    public Object getOwner() {
        return ownerUUID;
    }

    public int getCredits() {
        return credits;
    }

    public void addCredits(int amount, boolean inserting) {
        if (inserting) {
            if (credits < 32) {
                if (!itemHandler.getStackInSlot(0).isEmpty() && itemHandler.getStackInSlot(0).getItem() == Items.DIAMOND) {
                    credits += amount;
                    itemHandler.getStackInSlot(0).shrink(amount);
                }
            }
        } else {
            credits += amount;
        }
    }

    public void deductCredits(int amount) {
        credits -= amount;
    }

    public void removeCredits(boolean all) {
        if (credits > 0) {
            // check if output slot contains an item if it does add the credits to it
            if (itemHandler.getStackInSlot(OUTPUT_SLOT).isEmpty()) {
                itemHandler.setStackInSlot(OUTPUT_SLOT, new ItemStack(Items.DIAMOND, credits));
            } else {
                itemHandler.getStackInSlot(OUTPUT_SLOT).grow(credits);
            }
            credits = 0;
        }
    }

    public boolean isPlaying() {
        return playing == 1;
    }

    public String[] dealerCards = new String[7];
    public String[] playerCards = new String[7];

    private DeckHandler deck;
    public boolean playerBusted = false;
    public boolean dealerBusted = false;
    public boolean dealerWins = false;
    public boolean playerWins = false;
    public boolean isDraw = false;
    public boolean playerBlackjack = false;

    public void resetGame() {
        playing = 0;
        dealerHand = 0;
        playerHand = 0;
        playerCards = new String[7];
        dealerCards = new String[7];
        playerBusted = false;
        dealerBusted = false;
        dealerWins = false;
        playerWins = false;
        isDraw = false;
        playerBlackjack = false;
    }

    public void dealNewHand() {
        // Reset Game.
        resetGame();
        playing = 1;

        deck = new DeckHandler();
        deck.createDeck();
        deck.shuffleDeck();

        String dealerCard1 = deck.drawCard();
        String playerCard1 = deck.drawCard();
        String playerCard2 = deck.drawCard();

        System.out.println("Dealer card: " + dealerCard1);
        System.out.println("Player card 1: " + playerCard1);
        System.out.println("Player card 2: " + playerCard2);

        dealerCards[0] = dealerCard1;
        playerCards[0] = playerCard1;
        playerCards[1] = playerCard2;

        dealerHand = deck.getHandValue(dealerCards);
        playerHand = deck.getHandValue(playerCards);

        if (playerHand == 21) {
            // Player wins
            dealerWins = false;
            playerBlackjack = true;
            playing = 0;
        }
    }


    public IItemHandler getInventory() {
        return lazyItemHandler.orElseThrow(() -> new RuntimeException("Inventory not present"));
    }

    public void finishDealerCards() {
        while (dealerHand < 17) {
            String dealerCard = deck.drawCard();
            // Find the next available index in dealerCards
            int nextIndex = 0;
            for (int i = 0; i < dealerCards.length; i++) {
                if (dealerCards[i] == null || dealerCards[i].isEmpty()) {
                    nextIndex = i;
                    break;
                }
            }
            // Add the new card at the next available index
            dealerCards[nextIndex] = dealerCard;

            dealerHand = deck.getHandValue(dealerCards);

            if (dealerHand > 21) {
                dealerBusted = true;
                playing = 0;
            }
        }
    }

    public void hit() {
        if (playing == 1) {
            String playerCard = deck.drawCard();
            // Find the next available index in playerCards
            int nextIndex = 0;
            for (int i = 0; i < playerCards.length; i++) {
                if (playerCards[i] == null || playerCards[i].isEmpty()) {
                    nextIndex = i;
                    break;
                }
            }
            // Add the new card at the next available index
            playerCards[nextIndex] = playerCard;

            playerHand = deck.getHandValue(playerCards);

            if (playerHand == 21) {
                // Player wins
                dealerWins = false;
                playerBlackjack = true;
                playing = 0;
            }

            if (playerHand > 21) {
                playerBusted = true;
                displayNextDealerCard();
                playing = 0;
            }
        }
    }

    private void displayNextDealerCard() {
        String dealerCard = deck.drawCard();
        // Find the next available index in dealerCards
        int nextIndex = 0;
        for (int i = 0; i < dealerCards.length; i++) {
            if (dealerCards[i] == null || dealerCards[i].isEmpty()) {
                nextIndex = i;
                break;
            }
        }
        // Add the new card at the next available index
        dealerCards[nextIndex] = dealerCard;
        dealerHand = deck.getHandValue(dealerCards);
    }

    public void stand() {
        // Do stand logic stuff
        finishDealerCards();
        if (!dealerBusted) {
            if (dealerHand > playerHand) {
                // Dealer wins
                dealerWins = true;
                playerWins = false;
            } else if (dealerHand < playerHand) {
                // Player wins
                dealerWins = false;
                playerWins = true;
            } else {
                // Draw
                dealerWins = false;
                playerWins = false;
                isDraw = true;
            }
        }

        playing = 0;
    }

    public void checkStatus(int value) {
        if (!isPlaying()) {
            if (value == 0) {
                this.payout(0);
            } else if (value == 1) {
                this.payout(2);
            } else if (value == 2) {
                this.payout(1);
            } else if (value == 3) {
                this.payout(3);
            }
        }
    }
}
