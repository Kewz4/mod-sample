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

    public List<String> itemBlacklist = new ArrayList<>();

    // Runtime cache for regex patterns
    private transient List<Pattern> blacklistPatterns = new ArrayList<>();

    public static PunchyConfig instance;

    public static void load(File file) {
        configFile = file;
        if (file.exists()) {
            try (FileReader reader = new FileReader(file)) {
                instance = GSON.fromJson(reader, PunchyConfig.class);
            } catch (IOException e) {
                e.printStackTrace();
                instance = new PunchyConfig();
            }
        } else {
            instance = new PunchyConfig();
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

    private void compilePatterns() {
        blacklistPatterns.clear();
        for (String entry : itemBlacklist) {
            String regex = entry;
            // Basic glob to regex conversion
            // If no colon, assume it's a mod id and match all items in it
            if (!regex.contains(":")) {
                regex = regex + ":.*";
            }

            // Escape special regex characters if they are not meant to be wildcards?
            // Prompt implies specific wildcard usage.
            // Let's just replace * with .* and be careful.
            // A more robust solution would escape everything except *, then replace * with .*
            // But for this demo, simple replacement is okay.
            // We should ensure we don't break existing regex syntax if the user intends to use it.
            // "Make blacklisting items have regex/regex like capabilities"

            // If the user inputs ".*sword", it works as regex.
            // If "mod:*sword", we replace * -> .*. "mod:.*sword".

            // To be safe, let's just replace * with .* if it's not already .*
            // Actually, simply replacing * with .* is standard "glob-like".

            if (!regex.contains(".*")) {
                 regex = regex.replace("*", ".*");
            }

            try {
                blacklistPatterns.add(Pattern.compile(regex));
            } catch (Exception e) {
                System.err.println("Invalid regex in blacklist: " + entry);
            }
        }
    }

    public boolean isBlacklisted(ItemStack stack) {
        if (instance == null) return false;
        String id = BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
        for (Pattern p : blacklistPatterns) {
            if (p.matcher(id).matches()) {
                return true;
            }
        }
        return false;
    }
}
