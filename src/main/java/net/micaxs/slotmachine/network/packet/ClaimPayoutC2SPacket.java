package net.micaxs.slotmachine.network.packet;

import net.micaxs.slotmachine.block.entity.SlotMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent by the client when the reel-stop animation finishes.
 * The server calls {@link SlotMachineBlockEntity#releasePayout()} to move any pending prize
 * from the temporary staging handler into the player-facing output slot.
 */
public class ClaimPayoutC2SPacket {

    private final BlockPos pos;

    public ClaimPayoutC2SPacket(BlockPos pos) {
        this.pos = pos;
    }

    public ClaimPayoutC2SPacket(FriendlyByteBuf buffer) {
        this.pos = buffer.readBlockPos();
    }

    public void toBytes(FriendlyByteBuf buffer) {
        buffer.writeBlockPos(pos);
    }

    public boolean handle(Supplier<NetworkEvent.Context> supplier) {
        NetworkEvent.Context context = supplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            ServerLevel level = player.serverLevel();
            BlockEntity be = level.getBlockEntity(pos);
            if (be instanceof SlotMachineBlockEntity slotMachine) {
                slotMachine.releasePayout();
            }
        });
        return true;
    }
}

