package dev.rosewood.rosechat.manager;

import dev.rosewood.rosechat.RoseChat;
import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.manager.Manager;
import org.bukkit.Bukkit;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.LinkedList;

public class DebugManager extends Manager {

    private boolean enabled;
    private final LinkedList<String> messages;

    public DebugManager(RosePlugin rosePlugin) {
        super(rosePlugin);
        this.messages = new LinkedList<>();
    }

    public void addMessage(String message) {
        LocalDateTime now = LocalDateTime.now();
        String prefix = "[" + now.getHour() + ":" + now.getMinute() + ":" + now.getSecond() + ":" + System.currentTimeMillis() + "]";
        this.messages.add(prefix + " " + message + "\n");
    }

    @Override
    public void reload() {

    }

    @Override
    public void disable() {

    }

    public void save() {
        Bukkit.getScheduler().runTaskAsynchronously(RoseChat.getInstance(), () -> {
            long time = System.currentTimeMillis();
            File debugFolder = new File(this.rosePlugin.getDataFolder(), "debug");
            debugFolder.mkdirs();
            File debugFile = new File(this.rosePlugin.getDataFolder() + "/debug", "debug-" + time + ".log");

            try {
                if (!debugFile.createNewFile()) return;

                Writer writer = Files.newBufferedWriter(Paths.get(debugFile.getAbsolutePath()));
                for (String message : this.messages) {
                    writer.write(message);
                }

                writer.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            this.messages.clear();
        });
    }

    public LinkedList<String> getMessages() {
        return this.messages;
    }

    public boolean isEnabled() {
        return this.enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
