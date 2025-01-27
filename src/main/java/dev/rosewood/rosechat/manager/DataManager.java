package dev.rosewood.rosechat.manager;

import dev.rosewood.rosechat.chat.ChatChannel;
import dev.rosewood.rosechat.chat.GroupChat;
import dev.rosewood.rosechat.chat.PlayerData;
import dev.rosewood.rosechat.database.migrations._1_Create_Tables_Data;
import dev.rosewood.rosegarden.RosePlugin;
import dev.rosewood.rosegarden.database.DataMigration;
import dev.rosewood.rosegarden.manager.AbstractDataManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

public class DataManager extends AbstractDataManager {

    private final ChannelManager channelManager;

    public DataManager(RosePlugin rosePlugin) {
        super(rosePlugin);
        this.channelManager = rosePlugin.getManager(ChannelManager.class);
    }

    @Override
    public List<Class<? extends DataMigration>> getDataMigrations() {
        return Collections.singletonList(
                _1_Create_Tables_Data.class
        );
    }

    public PlayerData getPlayerData(UUID uuid) {
        AtomicReference<PlayerData> value = new AtomicReference<>(null);
        this.databaseConnector.connect(connection -> {
            String dataQuery = "SELECT * FROM " + this.getTablePrefix() + "player_data WHERE uuid = ?";
            try (PreparedStatement statement = connection.prepareStatement(dataQuery)) {
                statement.setString(1, uuid.toString());
                ResultSet result = statement.executeQuery();

                if (result.next()) {
                    boolean messageSpy = result.getBoolean("has_message_spy");
                    boolean channelSpy = result.getBoolean("has_channel_spy");
                    boolean groupSpy = result.getBoolean("has_group_spy");
                    boolean canBeMessaged = result.getBoolean("can_be_messaged");
                    boolean hasTagSounds = result.getBoolean("has_tag_sounds");
                    boolean hasMessageSounds = result.getBoolean("has_message_sounds");
                    boolean hasEmojis = result.getBoolean("has_emojis");
                    String currentChannel = result.getString("current_channel");
                    String color = result.getString("chat_color");
                    long muteTime = result.getLong("mute_time");
                    String nickname = result.getString("nickname");
                    ChatChannel channel = this.channelManager.getChannel(currentChannel);

                    PlayerData playerData = new PlayerData(uuid);
                    playerData.setMessageSpy(messageSpy);
                    playerData.setChannelSpy(channelSpy);
                    playerData.setGroupSpy(groupSpy);
                    playerData.setCanBeMessaged(canBeMessaged);
                    playerData.setTagSounds(hasTagSounds);
                    playerData.setMessageSounds(hasMessageSounds);
                    playerData.setEmojis(hasEmojis);
                    playerData.setCurrentChannel(channel == null ? this.channelManager.getDefaultChannel() : channel);
                    playerData.setColor(color);
                    playerData.setNickname(nickname);
                    if (muteTime > 0) playerData.mute(muteTime);
                    value.set(playerData);
                } else {
                    value.set(new PlayerData(uuid));
                }
            }

            String ignoreQuery = "SELECT * FROM " + this.getTablePrefix() + "player_data_ignore WHERE ignoring_uuid = ?";
            try (PreparedStatement statement = connection.prepareStatement(ignoreQuery)) {
                statement.setString(1, uuid.toString());
                ResultSet result = statement.executeQuery();

                if (result.next()) {
                    UUID ignored = UUID.fromString(result.getString("ignored_uuid"));
                    value.get().ignore(ignored);
                }
            }
        });
        return value.get();
    }

    public void updatePlayerData(PlayerData playerData) {
        this.databaseConnector.connect(connection -> {

            String query = "REPLACE INTO " + this.getTablePrefix() + "player_data (uuid, has_message_spy, has_channel_spy, has_group_spy, " +
                    "can_be_messaged, has_tag_sounds, has_message_sounds, has_emojis, current_channel, chat_color, mute_time, nickname) " +
                    "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(query)) {
                statement.setString(1, playerData.getUUID().toString());
                statement.setBoolean(2, playerData.hasMessageSpy());
                statement.setBoolean(3, playerData.hasChannelSpy());
                statement.setBoolean(4, playerData.hasGroupSpy());
                statement.setBoolean(5, playerData.canBeMessaged());
                statement.setBoolean(6, playerData.hasTagSounds());
                statement.setBoolean(7, playerData.hasMessageSounds());
                statement.setBoolean(8, playerData.hasEmojis());
                statement.setString(9, playerData.getCurrentChannel().getId());
                statement.setString(10, playerData.getColor());
                statement.setLong(11, playerData.getMuteExpirationTime());
                statement.setString(12, playerData.getNickname());
                statement.executeUpdate();
            }
        });
    }

    public void addIgnore(UUID ignoring, UUID ignored) {
        this.databaseConnector.connect(connection -> {
            String insertQuery = "INSERT INTO " + this.getTablePrefix() + "player_data_ignore (ignoring_uuid, ignored_uuid) " +
                    "VALUES(?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
                statement.setString(1, ignoring.toString());
                statement.setString(2, ignored.toString());
                statement.executeUpdate();
            }
        });
    }

    public void removeIgnore(UUID ignoring, UUID ignored) {
        this.databaseConnector.connect(connection -> {
            String deleteQuery = "DELETE FROM " + this.getTablePrefix() + "player_data_ignore WHERE ignoring_uuid = ? AND ignored_uuid = ?";
            try (PreparedStatement statement = connection.prepareStatement(deleteQuery)) {
                statement.setString(1, ignoring.toString());
                statement.setString(2, ignored.toString());
                statement.executeUpdate();
            }
        });
    }

    public List<ChatChannel> getMutedChannels() {
        List<ChatChannel> mutedChannels = new ArrayList<>();
        this.databaseConnector.connect(connection -> {
            String dataQuery = "SELECT * FROM " + this.getTablePrefix() + "muted_channels";
            try (PreparedStatement statement = connection.prepareStatement(dataQuery)) {
                ResultSet result = statement.executeQuery();
                while (result.next()) {
                    ChatChannel channel = this.channelManager.getChannel(result.getString("id"));
                    channel.setMuted(true);
                    mutedChannels.add(channel);
                }
            }
        });
        return mutedChannels;
    }

    public void addMutedChannel(ChatChannel channel) {
        this.databaseConnector.connect(connection -> {
            String insertQuery = "INSERT INTO " + this.getTablePrefix() + "muted_channels (id) " +
                    "VALUES(?)";
            try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
                statement.setString(1, channel.getId());
                statement.executeUpdate();
            }
        });
    }

    public void removeMutedChannel(ChatChannel channel) {
        this.databaseConnector.connect(connection -> {
            String deleteQuery = "DELETE FROM " + this.getTablePrefix() + "muted_channels WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(deleteQuery)) {
                statement.setString(1, channel.getId());
                statement.executeUpdate();
            }
        });
    }

    public List<GroupChat> getMemberGroupChats(UUID member) {
        List<GroupChat> groupChats = new ArrayList<>();
        this.getDatabaseConnector().connect(connection -> {
            String groupQuery = "SELECT gc.id, gc.name, gc.owner, gcm.uuid AS member_uuid FROM " + this.getTablePrefix() + "group_chat_member gcm JOIN " +
                    this.getTablePrefix() + "group_chat gc ON gc.id = gcm.group_chat WHERE gc.id IN " +
                    "(SELECT group_chat FROM " + this.getTablePrefix() + "group_chat_member WHERE uuid = ?) ORDER BY id;";

            try (PreparedStatement statement = connection.prepareStatement(groupQuery)) {
                statement.setString(1, member.toString());
                ResultSet result = statement.executeQuery();
                GroupChat current = null;
                String previousId = "";

                while (result.next()) {
                    String id = result.getString(1);
                    if (current != null && !id.equals(previousId)) {
                        groupChats.add(current);
                        current = null;
                    }

                    if (current == null) {
                        current = new GroupChat(id);
                        current.setName(result.getString(2));
                        current.setOwner(UUID.fromString(result.getString(3)));
                    }

                    current.addMember(UUID.fromString(result.getString(4)));
                    previousId = id;
                }

                if (current != null)
                    groupChats.add(current);
            }
        });
        return groupChats;
    }

    public List<String> getGroupChatNames() {
        List<String> groupChatNames = new ArrayList<>();
        this.getDatabaseConnector().connect(connection -> {
            String getQuery = "SELECT id FROM " + this.getTablePrefix() + "group_chat";
            try (PreparedStatement statement = connection.prepareStatement(getQuery)) {
                ResultSet result = statement.executeQuery();
                if (result.next())
                    groupChatNames.add(result.getString("id"));
            }
        });
        return groupChatNames;
    }


    public void addGroupChatMember(GroupChat groupChat, UUID member) {
        this.getDatabaseConnector().connect(connection -> {
            String insertQuery = "INSERT INTO " + this.getTablePrefix() + "group_chat_member (group_chat, uuid) " +
                    "VALUES (?, ?)";
            try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
                statement.setString(1, groupChat.getId());
                statement.setString(2, member.toString());
                statement.executeUpdate();
            }
        });
    }

    public void removeGroupChatMember(GroupChat groupChat, UUID member) {
        this.getDatabaseConnector().connect(connection -> {
            String deleteQuery = "DELETE FROM " + this.getTablePrefix() + "group_chat_member WHERE group_chat = ? AND uuid = ?";
            try (PreparedStatement statement = connection.prepareStatement(deleteQuery)) {
                statement.setString(1, groupChat.getId());
                statement.setString(2, member.toString());
                statement.executeUpdate();
            }
        });
    }

    public List<UUID> getGroupChatMembers(String id) {
        List<UUID> groupChatMembers = new ArrayList<>();
        this.getDatabaseConnector().connect(connection -> {
            String membersQuery = "SELECT * FROM " + this.getTablePrefix() + "group_chat_member WHERE group_chat = ?";
            try (PreparedStatement statement = connection.prepareStatement(membersQuery)) {
                statement.setString(1, id);
                ResultSet result = statement.executeQuery();
                if (result.next())
                    groupChatMembers.add(UUID.fromString(result.getString("uuid")));
            }
        });
        return groupChatMembers;
    }

    public void createOrUpdateGroupChat(GroupChat groupChat) {
        this.getDatabaseConnector().connect(connection -> {
            boolean create;

            String checkQuery = "SELECT 1 FROM " + this.getTablePrefix() + "group_chat WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(checkQuery)) {
                statement.setString(1, groupChat.getId());
                ResultSet result = statement.executeQuery();
                create = !result.next();
            }

            if (create) {
                String insertQuery = "INSERT INTO " + this.getTablePrefix() + "group_chat (id, name, owner) " +
                        "VALUES (?, ?, ?)";
                try (PreparedStatement statement = connection.prepareStatement(insertQuery)) {
                    statement.setString(1, groupChat.getId());
                    statement.setString(2, groupChat.getName());
                    statement.setString(3, groupChat.getOwner().toString());
                    statement.executeUpdate();
                }
            } else {
                String updateQuery = "UPDATE " + this.getTablePrefix() + "group_chat SET " +
                        "name = ? WHERE owner = ?";
                try (PreparedStatement statement = connection.prepareStatement(updateQuery)) {
                    statement.setString(1, groupChat.getName());
                    statement.setString(2, groupChat.getOwner().toString());
                    statement.executeUpdate();
                }
            }
        });
    }

    public void deleteGroupChat(GroupChat groupChat) {
        this.getDatabaseConnector().connect(connection -> {
            String deleteQuery = "DELETE FROM " + this.getTablePrefix() + "group_chat WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(deleteQuery)) {
                statement.setString(1, groupChat.getId());
                statement.executeUpdate();
            }

            String deleteMembersQuery = "DELETE FROM " + this.getTablePrefix() + "group_chat_member WHERE group_chat = ?";
            try (PreparedStatement statement = connection.prepareStatement(deleteMembersQuery)) {
                statement.setString(1, groupChat.getId());
                statement.executeUpdate();
            }
        });
    }

    public GroupManager.GroupInfo getGroupInfo(String groupId) {
        AtomicReference<GroupManager.GroupInfo> groupInfo = new AtomicReference<>();
        this.getDatabaseConnector().connect(connection -> {
            String getQuery = "SELECT COUNT(gcm.group_chat) as members, gc.id, gc.name, gc.owner FROM " +
                    this.getTablePrefix() + "group_chat_member gcm JOIN " +
                    this.getTablePrefix() + "group_chat gc ON gc.id = gcm.group_chat WHERE id = ?";
            try (PreparedStatement statement = connection.prepareStatement(getQuery)) {
                statement.setString(1, groupId);
                ResultSet result = statement.executeQuery();

                if (result.next()) {
                    String id = result.getString("id");
                    String name = result.getString("name");
                    String owner = result.getString("owner");
                    int members = result.getInt("members");
                    groupInfo.set(new GroupManager.GroupInfo(id, name, owner, members));
                }
            }
        });
        return groupInfo.get();
    }
}
