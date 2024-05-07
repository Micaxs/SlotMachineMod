package net.micaxs.slotmachine.network.packet;

import net.micaxs.slotmachine.block.entity.BJMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AddCreditsPacket {
    private final BlockPos pos;
    private final int credits;

    public AddCreditsPacket(BlockPos pos, int credits) {
        this.pos = pos;
        this.credits = credits;
    }

    public static void encode(AddCreditsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeInt(packet.credits);
    }

    public static AddCreditsPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        int credits = buffer.readInt();
        return new AddCreditsPacket(pos, credits);
    }

    public static void handle(AddCreditsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerLevel serverLevel = (ServerLevel) context.getSender().level();
            BlockEntity blockEntity = serverLevel.getBlockEntity(packet.pos);
            if (blockEntity instanceof BJMachineBlockEntity) {
                ((BJMachineBlockEntity) blockEntity).addCredits(packet.credits);
            }
        });
        context.setPacketHandled(true);
    }
}