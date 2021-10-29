package dev.rosewood.rosechat.command.group;

import dev.rosewood.rosechat.chat.GroupChat;
import dev.rosewood.rosechat.chat.PlayerData;
import dev.rosewood.rosechat.command.api.AbstractCommand;
import dev.rosewood.rosechat.message.RoseSender;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

public class DenyGroupCommand extends AbstractCommand {

    public DenyGroupCommand() {
        super(true, "deny");
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        Player player = (Player) sender;
        PlayerData playerData = this.getAPI().getPlayerData(player.getUniqueId());

        if (playerData.getGroupInvites().isEmpty()) {
            this.getAPI().getLocaleManager().sendMessage(player, "command-gc-accept-no-invites");
            return;
        }

        if (args.length == 0) {
            GroupChat invite = playerData.getGroupInvites().get(playerData.getGroupInvites().size() - 1);
            this.deny(playerData, player, invite);
        } else {
            String id = args[0];

            for (GroupChat invite : playerData.getGroupInvites()) {
                if (invite.getId().equalsIgnoreCase(id)) {
                    this.deny(playerData, player, invite);
                    return;
                }
            }

            this.getAPI().getLocaleManager().sendMessage(player, "command-gc-accept-not-invited");
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> tab = new ArrayList<>();
        if (args.length == 1) {
            for (GroupChat groupChat : this.getAPI().getPlayerData(((Player) sender).getUniqueId()).getGroupInvites()) {
                tab.add(groupChat.getId());
            }

            return tab;
        }

        return tab;
    }

    @Override
    public String getPermission() {
        return "rosechat.group.deny";
    }

    @Override
    public String getSyntax() {
        return this.getAPI().getLocaleManager().getLocaleMessage("command-group-deny-usage");
    }

    private void deny(PlayerData data, Player player, GroupChat groupChat) {
        OfflinePlayer owner = Bukkit.getOfflinePlayer(groupChat.getOwner());
        data.getGroupInvites().remove(groupChat);

        RoseSender roseSender = new RoseSender(player);
        BaseComponent[] groupName = this.getAPI().parse(roseSender, roseSender, groupChat.getName());
        String formattedGroupName = ComponentSerializer.toString(groupName);

        BaseComponent[] name = this.getAPI().parse(roseSender, roseSender, data.getNickname() == null ? player.getDisplayName() : data.getNickname());
        String formattedName = ComponentSerializer.toString(name);

        this.getAPI().getLocaleManager().sendMessage(player, "command-gc-deny-success",
                StringPlaceholders.builder().addPlaceholder("name", formattedGroupName).build());
        if (owner != null && owner.isOnline()) {
            this.getAPI().getLocaleManager().sendMessage(owner.getPlayer(), "command-gc-deny-denied", StringPlaceholders.single("player", formattedName));
        }
    }
}
