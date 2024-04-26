package net.micaxs.slotmachine.network.packet;

import net.micaxs.slotmachine.block.entity.SlotMachineBlockEntity;
import net.micaxs.slotmachine.network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class SlotsC2SPacket {

    private final BlockPos pos;
    private final boolean spinState; // true for start, false for stop

    public SlotsC2SPacket(BlockPos pos, boolean spinState) {
        this.pos = pos;
        this.spinState = spinState;
    }

    public SlotsC2SPacket(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.spinState = buffer.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeBoolean(spinState);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SlotMachineBlockEntity) {
                int[] results;
                if (spinState) {
                    results = ((SlotMachineBlockEntity) be).startSpin();
                } else {
                    results = ((SlotMachineBlockEntity) be).stopSpin();
                }
                PacketHandler.sendToPlayer(new SlotsS2CPacket(pos, results), player);
            }
        });
        return true;
    }
}
