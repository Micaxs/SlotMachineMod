package net.micaxs.slotmachine.network.packet;

import net.micaxs.slotmachine.screen.BJMachineScreen;
import net.micaxs.slotmachine.screen.SlotMachineScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BJS2CPacket {

    private final BlockPos pos;
    private final int results;

    public BJS2CPacket(BlockPos pos, int results) {
        this.pos = pos;
        this.results = results;
    }

    public BJS2CPacket(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.results = buffer.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeVarInt(results);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            Minecraft.getInstance().execute(() -> {
                Screen screen = Minecraft.getInstance().screen;
                if (screen instanceof BJMachineScreen) {
                    ((BJMachineScreen) screen).updateCredits(this.results);
                }
            });
        });
        return true;
    }
}