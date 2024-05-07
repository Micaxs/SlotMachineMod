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
    private final boolean buttonState; // true for start, false for stop

    public BJC2SPacket(BlockPos pos, boolean buttonState) {
        this.pos = pos;
        this.buttonState = buttonState;
    }

    public BJC2SPacket(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.buttonState = buffer.readBoolean();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeBoolean(buttonState);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
//        context.enqueueWork(() -> {
//            ServerPlayer player = context.getSender();
//            ServerLevel level = player.serverLevel();
//            BlockEntity be = level.getBlockEntity(pos);
//            if (be instanceof BJMachineBlockEntity) {
//                int results;
//                if (buttonState) {
//                    results = ((BJMachineBlockEntity) be).addCredits();
//                } else {
//                    results = ((BJMachineBlockEntity) be).removeCredits();
//                }
//                PacketHandler.sendToPlayer(new BJS2CPacket(pos, results), player);
//            }
//        });
        return true;
    }
}
