package net.micaxs.slotmachine;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
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

    private static final ForgeConfigSpec.ConfigValue<Boolean> DOUBLE_TRIPLE_PAYOUT = BUILDER
            .comment("When true: triple match pays out 2 items, double match pays out 1 item.\n" +
                     "When false: all wins pay out exactly 1 item regardless of match type.")
            .define("double_triple_payout", true);

    private static final ForgeConfigSpec.ConfigValue<Boolean> SHOW_PRIZE_PANEL = BUILDER
            .comment("Show the prize pool side-panel next to the player-owned slot machine GUI.")
            .define("show_prize_panel", true);

    // ── Server Slot Machine ──────────────────────────────────────────────────

    private static final ForgeConfigSpec.ConfigValue<String> SERVER_BET_ITEM = BUILDER
            .comment("Item required as a bet on the server slot machine (e.g. 'minecraft:emerald').")
            .define("server_bet_item", "minecraft:emerald", Config::validateItemName);

    private static final ForgeConfigSpec.ConfigValue<List<? extends String>> SERVER_PRIZES = BUILDER
            .comment("Prizes for the server slot machine.\n" +
                     "Format: 'namespace:item|amount|chance'\n" +
                     "  amount – number of items awarded on a win (1–32; max 32 so a double payout returns 64, filling one full stack)\n" +
                     "  chance – win weight 0–100, decimals supported (e.g. 0.25, 2.50)\n" +
                     "Example: [\"minecraft:diamond|16|10\", \"minecraft:emerald|8|0.25\"]\n" +
                     "Legacy 2-part format 'namespace:item|chance' is also accepted and defaults amount to 1.")
            .defineListAllowEmpty("server_prizes",
                    List.of("minecraft:gold_ingot|2|5", "minecraft:diamond|1|5"),
                    Config::validatePrizeEntry);

    private static final ForgeConfigSpec.ConfigValue<Double> SERVER_WIN_CHANCE_3 = BUILDER
            .comment("Server slot machine: probability of a 3-in-a-row win. Range: 0.0 – 1.0")
            .define("server_triple_win_chance", 0.05, Config::validateDouble);

    private static final ForgeConfigSpec.ConfigValue<Double> SERVER_WIN_CHANCE_2 = BUILDER
            .comment("Server slot machine: probability of a 2-in-a-row win. Range: 0.0 – 1.0")
            .define("server_double_win_chance", 0.15, Config::validateDouble);

    private static final ForgeConfigSpec.ConfigValue<Boolean> SERVER_SHOW_PRIZE_PANEL = BUILDER
            .comment("Show the prize pool side-panel next to the server slot machine GUI.")
            .define("server_show_prize_panel", true);

    static final ForgeConfigSpec SPEC = BUILDER.build();

    public static Set<Item> validBetItems;
    public static double tripleWinChance;
    public static double doubleWinChance;
    public static boolean doubleTriplePayout;
    public static boolean showPrizePanel;

    public static Item serverBetItem;
    public static List<ServerPrize> serverPrizes;
    public static double serverTripleWinChance;
    public static double serverDoubleWinChance;
    public static boolean serverShowPrizePanel;

    /**
     * Represents one prize entry for the server slot machine.
     *
     * @param amount base number of items awarded on a win (1–32).
     *               Max is 32 so that a double-payout triple match returns 64 (one full stack).
     * @param chance relative win weight (0.0–100.0); supports decimals e.g. {@code 0.25}, {@code 2.50}.
     */
    public record ServerPrize(Item item, int amount, double chance) {
        public ItemStack toStack(int count) {
            return new ItemStack(item, count);
        }
    }

    private static boolean validateItemName(final Object obj) {
        if (!(obj instanceof String itemName)) return false;
        try {
            new ResourceLocation(itemName);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean validateDouble(final Object obj) {
        return obj instanceof final Double d && d >= 0.0 && d <= 1.0;
    }

    private static boolean validatePrizeEntry(final Object obj) {
        if (!(obj instanceof String s)) return false;
        String[] parts = s.split("\\|");
        if (parts.length != 2 && parts.length != 3) return false;
        try {
            new ResourceLocation(parts[0].trim()); // validate ResourceLocation format only
        } catch (Exception e) {
            return false;
        }
        if (parts.length == 3) {
            // New format: item|amount|chance
            try {
                int amount = Integer.parseInt(parts[1].trim());
                if (amount < 1 || amount > 32) return false; // max 32 so double payout fits in a stack of 64
            } catch (NumberFormatException e) {
                return false;
            }
            try {
                double chance = Double.parseDouble(parts[2].trim());
                return chance >= 0.0 && chance <= 100.0;
            } catch (NumberFormatException e) {
                return false;
            }
        } else {
            // Legacy format: item|chance (amount defaults to 1)
            try {
                double chance = Double.parseDouble(parts[1].trim());
                return chance >= 0.0 && chance <= 100.0;
            } catch (NumberFormatException e) {
                return false;
            }
        }
    }

    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        validBetItems = VALID_GAMBLING_ITEM.get().stream()
                .map(itemName -> ForgeRegistries.ITEMS.getValue(new ResourceLocation(itemName)))
                .limit(9)
                .collect(Collectors.toSet());
        tripleWinChance = WIN_CHANCE_3.get();
        doubleWinChance = WIN_CHANCE_2.get();
        doubleTriplePayout = DOUBLE_TRIPLE_PAYOUT.get();
        showPrizePanel = SHOW_PRIZE_PANEL.get();

        serverBetItem = ForgeRegistries.ITEMS.getValue(new ResourceLocation(SERVER_BET_ITEM.get()));
        serverPrizes = SERVER_PRIZES.get().stream()
                .map(entry -> {
                    String[] parts = entry.split("\\|");
                    Item item = ForgeRegistries.ITEMS.getValue(new ResourceLocation(parts[0].trim()));
                    if (parts.length == 3) {
                        // New format: item|amount|chance
                        int amount = Math.min(32, Math.max(1, Integer.parseInt(parts[1].trim())));
                        double chance = Double.parseDouble(parts[2].trim());
                        return new ServerPrize(item, amount, chance);
                    } else {
                        // Legacy format: item|chance (amount defaults to 1)
                        double chance = Double.parseDouble(parts[1].trim());
                        return new ServerPrize(item, 1, chance);
                    }
                })
                .filter(p -> p.item() != null && p.chance() > 0.0)
                .collect(Collectors.toList());
        serverTripleWinChance = SERVER_WIN_CHANCE_3.get();
        serverDoubleWinChance = SERVER_WIN_CHANCE_2.get();
        serverShowPrizePanel = SERVER_SHOW_PRIZE_PANEL.get();
    }
}
