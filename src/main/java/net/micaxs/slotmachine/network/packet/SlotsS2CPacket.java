package net.micaxs.slotmachine.network.packet;

import net.micaxs.slotmachine.screen.SlotMachineScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SlotsS2CPacket {

    private final BlockPos pos;
    private final int[] results;

    public SlotsS2CPacket(BlockPos pos, int[] results) {
        this.pos = pos;
        this.results = results;
    }

    public SlotsS2CPacket(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.results = buffer.readVarIntArray();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeVarIntArray(results);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            // Handle the packet on the client side
            // Update the results array in the SlotMachineScreen class
            Minecraft.getInstance().execute(() -> {
                Screen screen = Minecraft.getInstance().screen;
                if (screen instanceof SlotMachineScreen) {
                    ((SlotMachineScreen) screen).updateResults(this.results);
                }
            });
        });
        return true;
    }
}