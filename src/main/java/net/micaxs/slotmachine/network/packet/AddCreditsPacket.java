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
    private boolean insert;

    public AddCreditsPacket(BlockPos pos, int credits, boolean insert) {
        this.pos = pos;
        this.credits = credits;
        this.insert = insert;
    }

    public static void encode(AddCreditsPacket packet, FriendlyByteBuf buffer) {
        buffer.writeBlockPos(packet.pos);
        buffer.writeInt(packet.credits);
        buffer.writeBoolean(packet.insert);
    }

    public static AddCreditsPacket decode(FriendlyByteBuf buffer) {
        BlockPos pos = buffer.readBlockPos();
        int credits = buffer.readInt();
        boolean insert = buffer.readBoolean();
        return new AddCreditsPacket(pos, credits, insert);
    }

    public static void handle(AddCreditsPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerLevel serverLevel = (ServerLevel) context.getSender().level();
            BlockEntity blockEntity = serverLevel.getBlockEntity(packet.pos);
            if (blockEntity instanceof BJMachineBlockEntity) {
                ((BJMachineBlockEntity) blockEntity).addCredits(packet.credits, packet.insert);
            }
        });
        context.setPacketHandled(true);
    }
}