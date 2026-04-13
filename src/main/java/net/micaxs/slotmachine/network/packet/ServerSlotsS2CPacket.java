package net.micaxs.slotmachine.network.packet;

import net.micaxs.slotmachine.screen.ServerSlotMachineScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class ServerSlotsS2CPacket {

    private final BlockPos pos;
    private final int[] results;

    public ServerSlotsS2CPacket(BlockPos pos, int[] results) {
        this.pos = pos;
        this.results = results;
    }

    public ServerSlotsS2CPacket(FriendlyByteBuf buffer) {
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
            Minecraft.getInstance().execute(() -> {
                Screen screen = Minecraft.getInstance().screen;
                if (screen instanceof ServerSlotMachineScreen sss) {
                    sss.updateResults(this.results);
                }
            });
        });
        return true;
    }
}

