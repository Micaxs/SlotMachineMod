package net.micaxs.slotmachine.network.packet;

import net.micaxs.slotmachine.block.entity.SlotMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class AdminConfigC2SPacket {

    private final BlockPos pos;
    private final int prizeIndex;
    private final int delta;

    public AdminConfigC2SPacket(BlockPos pos, int prizeIndex, int delta) {
        this.pos = pos;
        this.prizeIndex = prizeIndex;
        this.delta = delta;
    }

    public AdminConfigC2SPacket(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
        this.prizeIndex = buffer.readVarInt();
        this.delta = buffer.readVarInt();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
        buffer.writeVarInt(prizeIndex);
        buffer.writeVarInt(delta);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) {
                return;
            }

            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SlotMachineBlockEntity smbe && player.getUUID().equals(smbe.getOwnerUUID())) {
                smbe.adjustPrizeChance(prizeIndex, delta);
            }
        });
        return true;
    }
}
