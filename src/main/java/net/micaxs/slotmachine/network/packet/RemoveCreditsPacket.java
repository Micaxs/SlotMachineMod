package net.micaxs.slotmachine.network.packet;

import net.micaxs.slotmachine.block.entity.BJMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RemoveCreditsPacket {
    private final BlockPos pos;

    public RemoveCreditsPacket(BlockPos pos) {
        this.pos = pos;
    }

    public static void encode(RemoveCreditsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
    }

    public static RemoveCreditsPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        return new RemoveCreditsPacket(pos);
    }

    public static void handle(RemoveCreditsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerLevel serverLevel = (ServerLevel) context.getSender().level();
            BlockEntity blockEntity = serverLevel.getBlockEntity(packet.pos);
            if (blockEntity instanceof BJMachineBlockEntity) {
                ((BJMachineBlockEntity) blockEntity).removeCredits(true);
            }
        });
        context.setPacketHandled(true);
    }
}