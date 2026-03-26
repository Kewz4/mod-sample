package com.punchy;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.item.ItemStack;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public class PunchyConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static File configFile;

    // ── Rendering / animation settings ──────────────────────────────────────
    public boolean renderArmorArmsFP             = false;
    public boolean bettercombatCompat            = false;
    public float   animationSpeed                = 5.5f;
    public boolean enableMod                     = true;
    public boolean enableTuning                  = false;
    public boolean disableResourcePackModelParts = false;
    public boolean disableArmPhysics             = false;
    public boolean disableNativeItemPhysics      = false;
    public boolean disableBoatMinecartRaftModels = false;
    public boolean disablePistonModels           = false;
    public boolean disableChestModels            = false;
    public boolean disableEnchantingTableModels  = false;
    public boolean disableBoatFirstPersonAnimations = false;
    public boolean disableEnhancedFireArmEffects = false;

    // ── Item blacklist ───────────────────────────────────────────────────────
    /**
     * List of item IDs or mod IDs for which Punchy animations are disabled.
     *
     * Supported formats:
     *   modid:item_id          → disables one specific item
     *   modid                  → disables every item from that mod
     *   modid:*_sword          → glob wildcard – disables all items whose ID ends with _sword
     *   minecraft:.*axe        → raw regex is also accepted
     *
     * To find an item's ID in-game, press F3 + H and hover over the item.
     */
    public List<String> itemBlacklist = new ArrayList<>();

    // Runtime cache – not serialised.
    private transient List<Pattern> blacklistPatterns = new ArrayList<>();

    public static PunchyConfig instance;

    // ── Load / save ──────────────────────────────────────────────────────────

    public static void load(File file) {
        configFile = file;
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                instance = GSON.fromJson(reader, PunchyConfig.class);
                if (instance == null) instance = new PunchyConfig();
            } catch (IOException e) {
                e.printStackTrace();
                instance = new PunchyConfig();
            }
        } else {
            instance = new PunchyConfig();
            // Sensible demo defaults
            instance.itemBlacklist.add("minecraft:lantern");
            instance.itemBlacklist.add("minecraft");
            instance.save();
        }
        instance.compilePatterns();
    }

    public void save() {
        try (FileWriter writer = new FileWriter(configFile)) {
            GSON.toJson(this, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
        compilePatterns();
    }

    // ── Pattern compilation ──────────────────────────────────────────────────

    /**
     * Converts each blacklist entry to a compiled {@link Pattern}.
     *
     * Conversion rules:
     * <ol>
     *   <li>If the entry contains no colon it is treated as a mod ID and expanded
     *       to {@code modid:.*} (matches all items from that mod).</li>
     *   <li>All regex special characters are escaped EXCEPT {@code *}, which is
     *       treated as a glob wildcard and expanded to {@code .*}.</li>
     *   <li>If the entry already contains {@code .*} it is used as-is (raw regex).</li>
     * </ol>
     */
    private void compilePatterns() {
        if (blacklistPatterns == null) blacklistPatterns = new ArrayList<>();
        blacklistPatterns.clear();

        for (String entry : itemBlacklist) {
            if (entry == null || entry.isBlank()) continue;
            try {
                String regex = entryToRegex(entry);
                blacklistPatterns.add(Pattern.compile(regex));
            } catch (Exception e) {
                System.err.println("[Punchy] Invalid blacklist entry (skipping): " + entry);
            }
        }
    }

    private static String entryToRegex(String entry) {
        // Raw regex passthrough — user wrote .* or explicit regex syntax
        if (entry.contains(".*")) return entry;

        if (!entry.contains(":")) {
            if (entry.contains("*")) {
                // Path-only glob like *_leggings → matches any namespace:path_ending
                return "^[^:]++:" + globToRegex(entry) + "$";
            } else {
                // Plain mod ID like "cobblemon" → match all items from that mod
                return "^" + globToRegex(entry) + ":.*$";
            }
        }

        // Has colon — split namespace:path and glob-convert each part separately
        int colon = entry.indexOf(':');
        return "^" + globToRegex(entry.substring(0, colon))
                   + ":" + globToRegex(entry.substring(colon + 1)) + "$";
    }

    private static String globToRegex(String glob) {
        StringBuilder sb = new StringBuilder();
        for (char c : glob.toCharArray()) {
            switch (c) {
                case '*'  -> sb.append(".*");
                case '.'  -> sb.append("\\.");
                case '+'  -> sb.append("\\+");
                case '?'  -> sb.append("\\?");
                case '^'  -> sb.append("\\^");
                case '$'  -> sb.append("\\$");
                case '{'  -> sb.append("\\{");
                case '}'  -> sb.append("\\}");
                case '('  -> sb.append("\\(");
                case ')'  -> sb.append("\\)");
                case '['  -> sb.append("\\[");
                case ']'  -> sb.append("\\]");
                case '\\' -> sb.append("\\\\");
                case '|'  -> sb.append("\\|");
                default   -> sb.append(c);
            }
        }
        return sb.toString();
    }

    // ── Public API ───────────────────────────────────────────────────────────

    public boolean isBlacklisted(ItemStack stack) {
        if (instance == null || blacklistPatterns == null) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for (Pattern p : blacklistPatterns) {
            if (p.matcher(id).matches()) return true;
        }
        return false;
    }
}
