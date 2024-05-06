package net.micaxs.slotmachine.block.entity;

import net.micaxs.slotmachine.SlotMachineMod;
import net.micaxs.slotmachine.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities {

    public static DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, SlotMachineMod.MOD_ID);

    public static final RegistryObject<BlockEntityType<SlotMachineBlockEntity>> SLOT_MACHINE_BE =
            BLOCK_ENTITIES.register("slot_machine_be",
                    () -> BlockEntityType.Builder.of(SlotMachineBlockEntity::new, ModBlocks.SLOT_MACHINE.get()).build(null));

    public static final RegistryObject<BlockEntityType<BJMachineBlockEntity>> BJ_MACHINE_BE =
            BLOCK_ENTITIES.register("bj_machine_be",
                    () -> BlockEntityType.Builder.of(BJMachineBlockEntity::new, ModBlocks.BJ_MACHINE.get()).build(null));

    public static void register(IEventBus eventBus) {
        BLOCK_ENTITIES.register(eventBus);
    }
}
