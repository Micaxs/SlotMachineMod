package net.micaxs.slotmachine.network.packet;

import net.micaxs.slotmachine.block.entity.BJMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class DeductCreditsPacket {
    private final BlockPos pos;
    private final int amount;

    public DeductCreditsPacket(BlockPos pos, int amount) {
        this.pos = pos;
        this.amount = amount;
    }

    public static void encode(DeductCreditsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeInt(packet.amount);
    }

    public static DeductCreditsPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        int amount = buffer.readInt();
        return new DeductCreditsPacket(pos, amount);
    }

    public static void handle(DeductCreditsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerLevel serverLevel = (ServerLevel) context.getSender().level();
            BlockEntity blockEntity = serverLevel.getBlockEntity(packet.pos);
            if (blockEntity instanceof BJMachineBlockEntity) {
                ((BJMachineBlockEntity) blockEntity).deductCredits(packet.amount);
            }
        });
        context.setPacketHandled(true);
    }
}