package dev.rosewood.rosechat.chat;

import dev.rosewood.rosechat.RoseChat;
import dev.rosewood.rosechat.api.RoseChatAPI;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class MuteTask extends BukkitRunnable {

    private final RoseChatAPI api;
    private final PlayerData data;

    public MuteTask(PlayerData data) {
        this.api = RoseChatAPI.getInstance();
        this.data = data;
        this.runTaskTimerAsynchronously(RoseChat.getInstance(), 0L, 5L * 20L);
    }

    @Override
    public void run() {
        if (this.data == null) this.cancel();
        Player player = Bukkit.getPlayer(this.data.getUUID());
        if (player == null || this.data.getMuteTime() < 0) this.cancel();
        if (this.data.getMuteTime() < System.currentTimeMillis()) {
            this.data.setMuteTime(0);
            this.api.getLocaleManager().sendComponentMessage(player, "command-mute-unmuted");
            this.api.getDataManager().getMuteTasks().remove(this.data.getUUID());
            this.data.save();
            this.cancel();
        }
    }

}
