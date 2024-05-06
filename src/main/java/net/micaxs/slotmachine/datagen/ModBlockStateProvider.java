package net.micaxs.slotmachine.datagen;

import net.micaxs.slotmachine.SlotMachineMod;
import net.micaxs.slotmachine.block.ModBlocks;
import net.minecraft.data.PackOutput;
import net.minecraftforge.client.model.generators.BlockStateProvider;
import net.minecraftforge.client.model.generators.ModelFile;
import net.minecraftforge.common.data.ExistingFileHelper;

public class ModBlockStateProvider extends BlockStateProvider {
    public ModBlockStateProvider(PackOutput output, ExistingFileHelper exFileHelper) {
        super(output, SlotMachineMod.MOD_ID, exFileHelper);
    }

    @Override
    protected void registerStatesAndModels() {
        horizontalBlock(ModBlocks.SLOT_MACHINE.get(),
                new ModelFile.UncheckedModelFile(modLoc("block/slot_machine")));
        horizontalBlock(ModBlocks.BJ_MACHINE.get(),
                new ModelFile.UncheckedModelFile(modLoc("block/bj_machine")));
    }


}