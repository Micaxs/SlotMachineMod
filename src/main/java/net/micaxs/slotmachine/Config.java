package net.micaxs.slotmachine;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = SlotMachineMod.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class Config
{
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();

    // TODO: In future release update this to a list and support multiple items somehow.
    private static final ForgeConfigSpec.ConfigValue<String> VALID_GAMBLING_ITEM = BUILDER
            .comment("A list of items to allow being gambled with.")
            .define("valid_gambling_item", "minecraft:emerald", Config::validateItemName);

    private static final ForgeConfigSpec.ConfigValue<Double> WIN_CHANCE_2 = BUILDER
            .comment("Percentage that 2 of the same will occur in the slot machine [0.1 = 10%, 1.0 = 100%] (Range: 0.0 - 1.0)")
            .define("double_win_chance", 0.2, Config::validateDouble);

    private static final ForgeConfigSpec.ConfigValue<Double> WIN_CHANCE_3 = BUILDER
            .comment("Percentage that 3 of the same will occur in the slot machine [0.1 = 10%, 1.0 = 100%] (Range: 0.0 - 1.0)")
            .define("triple_win_chance", 0.1, Config::validateDouble);

    private static final ForgeConfigSpec.ConfigValue<Integer> DOUBLE_PAYOUT_AMOUNT = BUILDER
            .comment("Set the amount of items you return when you have 2 of the same in the slots machine. (Valid Range: 1 - 8)")
            .define("double_payout_amount", 2, Config::validateIntegerMax8);

    private static final ForgeConfigSpec.ConfigValue<Integer> TRIPLE_PAYOUT_AMOUNT = BUILDER
            .comment("Set the amount of items you return when you have 3 of the same in the slots machine. Note that setting this will prevent spinning the slot machine when the output slot + this value is more then the stack limit! Also keep this value higher than double_payout_amount (Valid Range: 1 - 16)")
            .define("triple_payout_amount", 3, Config::validateIntegerMax16);


    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static Item validBetItem;
    public static double tripleWinChance;
    public static double doubleWinChance;
    public static int triplePayoutAmount;
    public static int doublePayoutAmount;

    private static boolean validateItemName(final Object obj)
    {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(new ResourceLocation(itemName));
    }

    private static boolean validateDouble(final Object obj)
    {
        return obj instanceof final Double d && d >= 0.0 && d <= 1.0;
    }

    private static boolean validateIntegerMax8(final Object obj)
    {
        return obj instanceof final Integer d && d >= 1 && d <= 8;
    }

    private static boolean validateIntegerMax16(final Object obj)
    {
        return obj instanceof final Integer d && d >= 1 && d <= 16;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event)
    {
        validBetItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(VALID_GAMBLING_ITEM.get()));
        tripleWinChance = WIN_CHANCE_3.get();
        doubleWinChance = WIN_CHANCE_2.get();
        triplePayoutAmount = TRIPLE_PAYOUT_AMOUNT.get();
        doublePayoutAmount = DOUBLE_PAYOUT_AMOUNT.get();
    }
}
