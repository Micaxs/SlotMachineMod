package net.micaxs.slotmachine.network;

import net.micaxs.slotmachine.SlotMachineMod;
import net.micaxs.slotmachine.network.packet.SlotsC2SPacket;
import net.micaxs.slotmachine.network.packet.SlotsS2CPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

public class PacketHandler {
    private static SimpleChannel INSTANCE;

    private static int packetId = 0;

    private static int id() {
        return packetId++;
    }

    public static void register() {
        SimpleChannel net = NetworkRegistry.ChannelBuilder.named(new ResourceLocation(SlotMachineMod.MOD_ID, "main"))
                .networkProtocolVersion(() -> "1.0")
                .clientAcceptedVersions(s -> true)
                .serverAcceptedVersions(s -> true)
                .simpleChannel();

        INSTANCE = net;

        // Register Packet
        net.messageBuilder(SlotsC2SPacket.class, id(), NetworkDirection.PLAY_TO_SERVER).decoder(SlotsC2SPacket::new).encoder(SlotsC2SPacket::toBytes).consumerMainThread(SlotsC2SPacket::handle).add();
        net.messageBuilder(SlotsS2CPacket.class, id(), NetworkDirection.PLAY_TO_CLIENT).decoder(SlotsS2CPacket::new).encoder(SlotsS2CPacket::toBytes).consumerMainThread(SlotsS2CPacket::handle).add();
    }

    public static <MSG> void sendToServer(MSG msg) {
        INSTANCE.sendToServer(msg);
    }

    public static <MSG> void sendToPlayer(MSG msg, ServerPlayer player) {
        INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), msg);
    }

    public static <MSG> void serverToAllClients(MSG msg) {
        INSTANCE.send(PacketDistributor.ALL.noArg(), msg);
    }

}
