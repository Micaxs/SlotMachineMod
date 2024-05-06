package net.micaxs.slotmachine;

import com.mojang.logging.LogUtils;
import net.micaxs.slotmachine.block.ModBlocks;
import net.micaxs.slotmachine.block.ModItems;
import net.micaxs.slotmachine.block.entity.ModBlockEntities;
import net.micaxs.slotmachine.network.PacketHandler;
import net.micaxs.slotmachine.screen.*;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.renderer.entity.EntityRenderers;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.BuildCreativeModeTabContentsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

// The value here should match an entry in the META-INF/mods.toml file
@Mod(SlotMachineMod.MOD_ID)
public class SlotMachineMod
{
    public static final String MOD_ID = "slotmachinemod";
    private static final Logger LOGGER = LogUtils.getLogger();


    public SlotMachineMod() {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
        ModItems.register(modEventBus);
        ModBlocks.register(modEventBus);
        ModBlockEntities.register(modEventBus);
        ModMenuTypes.register(modEventBus);
        modEventBus.addListener(this::commonSetup);
        MinecraftForge.EVENT_BUS.register(this);
        modEventBus.addListener(this::addCreative);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(final FMLCommonSetupEvent event) {
        PacketHandler.register();
    }

    private void addCreative(BuildCreativeModeTabContentsEvent event) {
        if (event.getTabKey() == CreativeModeTabs.TOOLS_AND_UTILITIES) {
            event.accept(ModBlocks.SLOT_MACHINE);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
    }


    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents
    {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event)
        {
            // Slots Machine GUI
            MenuScreens.register(ModMenuTypes.SLOT_MACHINE_MENU.get(), SlotMachineScreen::new);
            MenuScreens.register(ModMenuTypes.SLOT_MACHINE_OWNER_MENU.get(), SlotMachineOwnerScreen::new);

            // Blackjack Machine GUI
            MenuScreens.register(ModMenuTypes.BJ_MACHINE_MENU.get(), BJMachineScreen::new);
        }
    }
}
