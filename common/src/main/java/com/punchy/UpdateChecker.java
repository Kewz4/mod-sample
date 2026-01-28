package com.punchy;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;

public class UpdateChecker {
    public static final String MOD_VERSION = "2.1";
    public static final String PROJECT_ID = "punchy-fpa";
    public static boolean updateAvailable = false;
    public static boolean popupShown = false;
    public static String downloadUrl = "";
    public static String latestVersion = "";

    public static void checkForUpdates(String loader) {
        CompletableFuture.runAsync(() -> {
            try {
                URL url = new URL("https://api.modrinth.com/v2/project/" + PROJECT_ID + "/version");
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod("GET");
                connection.setRequestProperty("User-Agent", "PunchyMod/UpdateChecker");

                if (connection.getResponseCode() == 200) {
                    InputStreamReader reader = new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8);
                    JsonArray versions = JsonParser.parseReader(reader).getAsJsonArray();

                    for (JsonElement element : versions) {
                        JsonObject versionObj = element.getAsJsonObject();
                        JsonArray gameVersions = versionObj.getAsJsonArray("game_versions");
                        JsonArray loaders = versionObj.getAsJsonArray("loaders");

                        boolean gameVersionMatch = false;
                        for (JsonElement gv : gameVersions) {
                            if (gv.getAsString().equals("1.21.1")) {
                                gameVersionMatch = true;
                                break;
                            }
                        }

                        boolean loaderMatch = false;
                        for (JsonElement l : loaders) {
                            if (l.getAsString().equalsIgnoreCase(loader)) {
                                loaderMatch = true;
                                break;
                            }
                        }

                        if (gameVersionMatch && loaderMatch) {
                            String verNum = versionObj.get("version_number").getAsString();
                            // If version is different (and presumably newer since list is sorted by date desc usually), flag update
                            // The prompt says "if it doesnt match... pop up"
                            if (!verNum.equals(MOD_VERSION)) {
                                updateAvailable = true;
                                latestVersion = verNum;
                                downloadUrl = "https://modrinth.com/mod/punchy-fpa/version/" + versionObj.get("id").getAsString();
                            }
                            // We found the latest for this loader/game version, so stop searching
                            break;
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }
}
