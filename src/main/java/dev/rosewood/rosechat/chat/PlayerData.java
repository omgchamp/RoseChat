package dev.rosewood.rosechat.chat;

import dev.rosewood.rosechat.RoseChat;
import dev.rosewood.rosechat.api.RoseChatAPI;
import dev.rosewood.rosechat.manager.DataManager;
import dev.rosewood.rosechat.manager.PlayerDataManager;
import dev.rosewood.rosechat.message.MessageLog;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class PlayerData {

    private final UUID uuid;
    private final MessageLog messageLog;
    private String replyTo;
    private boolean messageSpy;
    private boolean channelSpy;
    private boolean groupSpy;
    private boolean canBeMessaged;
    private boolean tagSounds;
    private boolean messageSounds;
    private boolean emojis;
    private ChatChannel currentChannel;
    private String color;
    private String nickname;
    private final List<GroupChat> groupInvites;
    private final List<UUID> ignoringPlayers;

    private long muteTime;
    private MuteTask activeMuteTask;

    /**
     * Creates a new PlayerData for a specific player.
     * @param uuid The UUID of the player.
     */
    public PlayerData(UUID uuid) {
        this.uuid = uuid;
        this.messageLog = new MessageLog(uuid);
        this.canBeMessaged = true;
        this.tagSounds = true;
        this.messageSounds = true;
        this.emojis = true;
        this.color = "";
        this.currentChannel = RoseChatAPI.getInstance().getChannelManager().getDefaultChannel();
        this.groupInvites = new ArrayList<>();
        this.ignoringPlayers = new ArrayList<>();
    }

    /**
     * Saves player data to the database.
     */
    public void save() {
        RoseChat.getInstance().getManager(PlayerDataManager.class).updatePlayerData(this);
    }

    /**
     * @return The UUID of the player who owns this player data.
     */
    public UUID getUUID() {
        return this.uuid;
    }

    /**
     * @return The message log.
     */
    public MessageLog getMessageLog() {
        return this.messageLog;
    }

    /**
     * @return The player to reply to.
     */
    public String getReplyTo() {
        return this.replyTo;
    }

    /**
     * Sets who the player is replying to.
     * @param replyTo The player to reply to.
     */
    public void setReplyTo(String replyTo) {
        this.replyTo = replyTo;
    }

    /**
     * @return True if the player has message spy enabled.
     */
    public boolean hasMessageSpy() {
        return this.messageSpy;
    }

    /**
     * @param messageSpy Whether the player has message spy enabled.
     */
    public void setMessageSpy(boolean messageSpy) {
        this.messageSpy = messageSpy;
    }

    /**
     * @return True if the player has channel spy enabled.
     */
    public boolean hasChannelSpy() {
        return this.channelSpy;
    }

    /**
     * @param channelSpy Whether the player has channel spy enabled.
     */
    public void setChannelSpy(boolean channelSpy) {
        this.channelSpy = channelSpy;
    }

    /**
     * @return True if the player has channel spy enabled.
     */
    public boolean hasGroupSpy() {
        return this.groupSpy;
    }

    /**
     * @param groupSpy Whether the player has group spy enabled.
     */
    public void setGroupSpy(boolean groupSpy) {
        this.groupSpy = groupSpy;
    }

    /**
     * @return True if the player can be messaged.
     */
    public boolean canBeMessaged() {
        return this.canBeMessaged;
    }

    /**
     * @param canBeMessaged Whether the player can be messaged.
     */
    public void setCanBeMessaged(boolean canBeMessaged) {
        this.canBeMessaged = canBeMessaged;
    }

    /**
     * @return True if the player has tag sounds enabled.
     */
    public boolean hasTagSounds() {
        return this.tagSounds;
    }

    /**
     * @param tagSounds Whether the player has tag sounds enabled.
     */
    public void setTagSounds(boolean tagSounds) {
        this.tagSounds = tagSounds;
    }

    /**
     * @return True if the player has message sounds enabled.
     */
    public boolean hasMessageSounds() {
        return this.messageSounds;
    }

    /**
     * @param messageSounds Whether the player has message sounds enabled.
     */
    public void setMessageSounds(boolean messageSounds) {
        this.messageSounds = messageSounds;
    }

    /**
     * @return True if the player has emojis enabled.
     */
    public boolean hasEmojis() {
        return this.emojis;
    }

    /**
     * @param emojis Whether the player has emojis enabled.
     */
    public void setEmojis(boolean emojis) {
        this.emojis = emojis;
    }

    /**
     * @return The current channel the player is in.
     */
    public ChatChannel getCurrentChannel() {
        return this.currentChannel;
    }

    /**
     * Sets the current channel that the player is in.
     * @param currentChannel The channel to use.
     */
    public void setCurrentChannel(ChatChannel currentChannel) {
        this.currentChannel = currentChannel;
    }

    /**
     * @return The current chat color of the player.
     */
    public String getColor() {
        return this.color;
    }

    /**
     * @param color The chat color for the player.
     */
    public void setColor(String color) {
        this.color = color;
    }

    /**
     * Gets the current nickname of the player.
     * This includes gradients, emojis, tags, etc.
     * Use {@link Player#getDisplayName} for the current formatted name.
     * {@link RoseChatAPI#parse} this to get the 'live' name, e.g. with placeholders for things like coords.
     *
     * @return The current nickname of the player.
     */
    public String getNickname() {
        return this.nickname;
    }

    /**
     * Sets the current nickname of the player.
     * @param nickname The nickname to use.
     */
    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    /**
     * @return The invites the player has received.
     */
    public List<GroupChat> getGroupInvites() {
        return this.groupInvites;
    }

    /**
     * Adds to the group invites list.
     * @param groupChat The group chat to add.
     */
    public void inviteToGroup(GroupChat groupChat) {
        this.groupInvites.add(groupChat);
    }

    /**
     * @return A list of UUIDs of players that the player is ignoring.
     */
    public List<UUID> getIgnoringPlayers() {
        return this.ignoringPlayers;
    }

    /**
     * Ignores a player.
     * @param target The player to ignore.
     */
    public void ignore(UUID target) {
        RoseChatAPI api = RoseChatAPI.getInstance();
        this.ignoringPlayers.add(target);
        api.getPlayerDataManager().addIgnore(this.getUUID(), target);
    }

    /**
     * Stops ignoring a player.
     * @param target The player to stop ignoring.
     */
    public void unignore(UUID target) {
        RoseChatAPI api = RoseChatAPI.getInstance();
        this.ignoringPlayers.remove(target);
        api.getPlayerDataManager().removeIgnore(this.getUUID(), target);
    }

    /**
     * Mutes the player from being able to send messages.
     * @param expirationTime The timestamp of when the mute expires.
     */
    public void mute(long expirationTime) {
        if (this.activeMuteTask != null)
            this.activeMuteTask.cancel();

        this.muteTime = expirationTime;
        if (expirationTime != -1)
            this.activeMuteTask = new MuteTask(this);
    }

    /**
     * Unmutes the player.
     */
    public void unmute() {
        if (this.activeMuteTask != null) {
            this.activeMuteTask.cancel();
            this.activeMuteTask = null;
        }
        this.muteTime = 0;
    }

    /**
     * @return true if the player is muted
     */
    public boolean isMuted() {
        return this.muteTime == -1 || this.activeMuteTask != null;
    }

    /**
     * @return true if the mute is expired, or false if it doesn't exist or hasn't expired yet
     */
    public boolean isMuteExpired() {
        return this.muteTime > 0 && this.muteTime < System.currentTimeMillis();
    }

    /**
     * @return The timestamp of when the mute expires.
     */
    public long getMuteExpirationTime() {
        return this.muteTime;
    }

}
