package net.micaxs.slotmachine.screen;

import net.micaxs.slotmachine.block.entity.SlotMachineBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

import javax.annotation.Nullable;

public class SlotMachineOwnerMenuProvider implements MenuProvider {
    private final SlotMachineBlockEntity slotMachineBlockEntity;

    public SlotMachineOwnerMenuProvider(SlotMachineBlockEntity slotMachineBlockEntity) {
        this.slotMachineBlockEntity = slotMachineBlockEntity;
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("slots.admin.title");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        // Pass the block entity's own ContainerData so prize chances are synced via it
        return new SlotMachineOwnerMenu(i, inventory, slotMachineBlockEntity,
                slotMachineBlockEntity.getData());
    }
}
