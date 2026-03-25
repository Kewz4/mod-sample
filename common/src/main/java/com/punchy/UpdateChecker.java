package com.punchy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.SharedConstants;

import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {
    /**
     * The version this build thinks it is — intentionally set to an older version
     * to demonstrate the update-checker flow. In a real release this matches the
     * Modrinth version_number of the published JAR (e.g. "2.4").
     */
    public static final String MOD_VERSION = "2.3";
    public static final String PROJECT_ID  = "punchy-fpa";

    public static volatile boolean updateAvailable = false;
    public static volatile boolean popupShown      = false;
    public static volatile String  downloadUrl     = "";
    public static volatile String  latestVersion   = "";

    private static volatile boolean checkStarted = false;

    /**
     * Kick off an async update check against the Modrinth API.
     *
     * @param loader  "fabric", "forge", or "neoforge"
     */
    public static void checkForUpdates(String loader) {
        if (checkStarted) return;
        checkStarted = true;

        // Capture the running MC version before entering the async thread.
        // SharedConstants is bootstrapped before mod init, so it is safe here.
        final String mcVersion;
        try {
            mcVersion = SharedConstants.getCurrentVersion().getName();
        } catch (Exception e) {
            System.err.println("[Punchy] Could not determine MC version – skipping update check.");
            return;
        }

        System.out.println("[Punchy] Starting update check | loader=" + loader + " | mc=" + mcVersion);

        CompletableFuture.runAsync(() -> {
            try {
                // Fetch up to 11 versions (the total the project publishes across all loaders/MC versions).
                String endpoint = "https://api.modrinth.com/v2/project/" + PROJECT_ID
                        + "/version?limit=11";
                URL url = new URL(endpoint);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                conn.setRequestProperty("User-Agent", "PunchyMod/UpdateChecker");
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                int code = conn.getResponseCode();
                System.out.println("[Punchy] Modrinth API response: " + code);

                if (code != 200) {
                    System.err.println("[Punchy] Update check failed – HTTP " + code);
                    return;
                }

                try (InputStreamReader reader = new InputStreamReader(
                        conn.getInputStream(), StandardCharsets.UTF_8)) {

                    JsonArray versions = JsonParser.parseReader(reader).getAsJsonArray();

                    for (JsonElement element : versions) {
                        JsonObject obj = element.getAsJsonObject();

                        // --- filter by MC version ---
                        boolean mcMatch = false;
                        for (JsonElement gv : obj.getAsJsonArray("game_versions")) {
                            if (gv.getAsString().equals(mcVersion)) {
                                mcMatch = true;
                                break;
                            }
                        }
                        if (!mcMatch) continue;

                        // --- filter by loader ---
                        boolean loaderMatch = false;
                        for (JsonElement l : obj.getAsJsonArray("loaders")) {
                            if (l.getAsString().equalsIgnoreCase(loader)) {
                                loaderMatch = true;
                                break;
                            }
                        }
                        if (!loaderMatch) continue;

                        // First matching entry is the latest version for this loader+MC combo.
                        String latest = obj.get("version_number").getAsString();
                        System.out.println("[Punchy] Latest version on Modrinth: " + latest
                                + " (running: " + MOD_VERSION + ")");

                        if (!latest.equals(MOD_VERSION)) {
                            updateAvailable = true;
                            latestVersion   = latest;
                            // Prefer direct-download file URL when available, fall back to version page.
                            String fileUrl = "";
                            JsonArray files = obj.getAsJsonArray("files");
                            for (JsonElement f : files) {
                                JsonObject file = f.getAsJsonObject();
                                if (file.has("primary") && file.get("primary").getAsBoolean()) {
                                    fileUrl = file.get("url").getAsString();
                                    break;
                                }
                            }
                            if (fileUrl.isEmpty() && files.size() > 0) {
                                fileUrl = files.get(0).getAsJsonObject().get("url").getAsString();
                            }
                            // Fall back to the version page if no direct file URL was found.
                            downloadUrl = fileUrl.isEmpty()
                                    ? "https://modrinth.com/mod/" + PROJECT_ID
                                      + "/version/" + obj.get("id").getAsString()
                                    : fileUrl;
                            System.out.println("[Punchy] Update available! Download: " + downloadUrl);
                        } else {
                            System.out.println("[Punchy] Mod is up to date.");
                        }
                        return; // stop after finding the first (latest) match
                    }
                    System.out.println("[Punchy] No matching version found for " + loader + " / " + mcVersion);
                }
            } catch (Exception e) {
                System.err.println("[Punchy] Error during update check:");
                e.printStackTrace();
            }
        });
    }
}
