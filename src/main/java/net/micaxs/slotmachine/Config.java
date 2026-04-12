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

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> VALID_GAMBLING_ITEM = BUILDER
            .comment("Global fallback: items accepted as bets when no per-machine bet item is configured. Max 9 entries.")
            .defineListAllowEmpty("valid_gambling_items", List.of("minecraft:emerald"), Config::validateItemName);

    private static final ForgeConfigSpec.ConfigValue<Double> WIN_CHANCE_3 = BUILDER
            .comment("Probability of a 3-in-a-row win (e.g. 0.05 = 5%). Range: 0.0 – 1.0")
            .define("triple_win_chance", 0.05, Config::validateDouble);

    private static final ForgeConfigSpec.ConfigValue<Double> WIN_CHANCE_2 = BUILDER
            .comment("Probability of a 2-in-a-row win (e.g. 0.15 = 15%). Range: 0.0 – 1.0")
            .define("double_win_chance", 0.15, Config::validateDouble);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static Set<Item> validBetItems;
    public static double tripleWinChance;
    public static double doubleWinChance;

    private static boolean validateItemName(final Object obj) {
        return obj instanceof final String itemName && ForgeRegistries.ITEMS.containsKey(new ResourceLocation(itemName));
    }

    private static boolean validateDouble(final Object obj) {
        return obj instanceof final Double d && d >= 0.0 && d <= 1.0;
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        validBetItems = VALID_GAMBLING_ITEM.get().stream()
                .map(itemName -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName)))
                .limit(9)
                .collect(Collectors.toSet());
        tripleWinChance = WIN_CHANCE_3.get();
        doubleWinChance = WIN_CHANCE_2.get();
    }
}
