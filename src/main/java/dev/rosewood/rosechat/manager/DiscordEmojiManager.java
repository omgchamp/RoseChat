package dev.rosewood.rosechat.manager;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.config.CommentedFileConfiguration;
import dev.rosewood.rosegarden.manager.Manager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class DiscordEmojiManager extends Manager {

    private Map<String, List<String>> discordEmojis;

    public DiscordEmojiManager(RosePlugin rosePlugin) {
        super(rosePlugin);
        this.discordEmojis = new HashMap<>();
    }

    @Override
    public void reload() {
        File emojiFile = new File(this.rosePlugin.getDataFolder(), "discord-emoji.json");
        if (!emojiFile.exists()) this.rosePlugin.saveResource("discord-emoji.json", false);

        try {
            Reader reader = Files.newBufferedReader(Paths.get(emojiFile.getAbsolutePath()));
            JsonParser parser = new JsonParser();
            JsonElement root = parser.parse(reader);

            JsonObject categories = root.getAsJsonObject();
            Set<Map.Entry<String, JsonElement>> categoryEntries = categories.entrySet();
            for (Map.Entry<String, JsonElement> categoryEntry : categoryEntries) {
                JsonArray emojis = categoryEntry.getValue().getAsJsonArray();

                for (JsonElement emoji : emojis) {
                    JsonObject emojiObject = emoji.getAsJsonObject();
                    JsonArray namesArray = emojiObject.get("names").getAsJsonArray();
                    String surrogates = emojiObject.get("surrogates").getAsString();

                    List<String> names = new ArrayList<>();
                    for (JsonElement name : namesArray) names.add(name.getAsString());
                    this.discordEmojis.put(surrogates, names);
                }
            }

        } catch (IOException ignored) {}
    }

    @Override
    public void disable() {

    }

    public String unformatUnicode(String message) {
        for (String unicode : this.discordEmojis.keySet()) {
            if (message.contains(unicode))
                message = message.replace(unicode, ":" + this.discordEmojis.get(unicode).get(0) + ":");
        }

        return message;
    }

    public List<String> getNames(String unicode) {
        return this.discordEmojis.get(unicode);
    }

    public Map<String, List<String>> getDiscordEmojis() {
        return this.discordEmojis;
    }
}
