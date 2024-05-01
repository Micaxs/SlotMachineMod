package net.micaxs.slotmachine.screen;

import net.micaxs.slotmachine.block.entity.SlotMachineBlockEntity;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.SimpleContainerData;

import javax.annotation.Nullable;

public class SlotMachineOwnerMenuProvider implements MenuProvider {
    private final SlotMachineBlockEntity slotMachineBlockEntity;

    public SlotMachineOwnerMenuProvider(SlotMachineBlockEntity slotMachineBlockEntity) {
        this.slotMachineBlockEntity = slotMachineBlockEntity;
    }

    @Override
    public Component getDisplayName() {
        return Component.literal("Owner Menu");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        // Create a SimpleContainerData object
        SimpleContainerData data = new SimpleContainerData(2);

        // Return your SlotMachineOwnerMenu here
        return new SlotMachineOwnerMenu(i, inventory, slotMachineBlockEntity, data);
    }
}