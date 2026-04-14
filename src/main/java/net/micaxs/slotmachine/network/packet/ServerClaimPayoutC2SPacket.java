package net.micaxs.slotmachine.network.packet;

import net.micaxs.slotmachine.block.entity.ServerSlotMachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

/**
 * Sent by the client when the server slot machine's reel-stop animation finishes.
 * The server calls {@link ServerSlotMachineBlockEntity#releasePayout()} to move any pending prize
 * from the temporary staging handler into the player-facing output slot.
 */
public class ServerClaimPayoutC2SPacket {

    private final BlockPos pos;

    public ServerClaimPayoutC2SPacket(BlockPos pos) {
        this.pos = pos;
    }

    public ServerClaimPayoutC2SPacket(FriendlyByteBuf buffer) {
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
            if (be instanceof ServerSlotMachineBlockEntity slotMachine) {
                slotMachine.releasePayout();
            }
        });
        return true;
    }
}

