package net.micaxs.slotmachine.network.packet;

import net.micaxs.slotmachine.block.entity.BJMachineBlockEntity;
import net.micaxs.slotmachine.network.PacketHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class BJC2SPacket {

    private final BlockPos pos;
    private final boolean spinState; // true for start, false for stop

    public BJC2SPacket(BlockPos pos, boolean spinState) {
        this.pos = pos;
        this.spinState = spinState;
    }

    public BJC2SPacket(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.spinState = buffer.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeBoolean(spinState);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
//        context.enqueueWork(() -> {
//            ServerPlayer player = context.getSender();
//            ServerLevel level = player.serverLevel();
//            BlockEntity be = level.getBlockEntity(pos);
//            if (be instanceof BJMachineBlockEntity) {
//                int[] results;
//                if (spinState) {
//                    results = ((BJMachineBlockEntity) be).startSpin();
//                } else {
//                    results = ((BJMachineBlockEntity) be).stopSpin();
//                }
//                PacketHandler.sendToPlayer(new SlotsS2CPacket(pos, results), player);
//            }
//        });
        return true;
    }
}
