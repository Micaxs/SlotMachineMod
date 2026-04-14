package net.micaxs.slotmachine.sound;

import net.micaxs.slotmachine.SlotMachineMod;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModSounds {

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, SlotMachineMod.MOD_ID);

    /** Looping reel-spinning sound played while the slot machine is active. */
    public static final RegistryObject<SoundEvent> SPINNING = SOUND_EVENTS.register("spinning",
            () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(SlotMachineMod.MOD_ID, "spinning")));

    /** Short click/thunk played each time an individual reel stops. */
    public static final RegistryObject<SoundEvent> REEL_STOP = SOUND_EVENTS.register("reel_stop",
            () -> SoundEvent.createVariableRangeEvent(
                    new ResourceLocation(SlotMachineMod.MOD_ID, "reel_stop")));

    public static void register(IEventBus eventBus) {
        SOUND_EVENTS.register(eventBus);
    }
}

