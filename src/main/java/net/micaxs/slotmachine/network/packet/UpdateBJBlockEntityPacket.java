package net.micaxs.slotmachine.network.packet;

import net.micaxs.slotmachine.block.entity.BJMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class UpdateBJBlockEntityPacket {
    private final BlockPos pos;
    private final int value;

    public UpdateBJBlockEntityPacket(BlockPos pos, int value) {
        this.pos = pos;
        this.value = value;
    }

    public static void encode(UpdateBJBlockEntityPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeInt(packet.value);
    }

    public static UpdateBJBlockEntityPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        int value = buffer.readInt();
        return new UpdateBJBlockEntityPacket(pos, value);
    }

    public static void handle(UpdateBJBlockEntityPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerLevel serverLevel = (ServerLevel) context.getSender().level();
            BlockEntity blockEntity = serverLevel.getBlockEntity(packet.pos);
            if (blockEntity instanceof BJMachineBlockEntity) {
                BJMachineBlockEntity bjBlockEntity = (BJMachineBlockEntity) blockEntity;
                bjBlockEntity.checkStatus(packet.value);
            }
        });
        context.setPacketHandled(true);
    }
}