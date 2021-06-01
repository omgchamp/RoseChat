package dev.rosewood.rosechat.command.group;

import dev.rosewood.rosechat.chat.GroupChat;
import dev.rosewood.rosechat.chat.PlayerData;
import dev.rosewood.rosechat.command.api.AbstractCommand;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class AcceptGroupCommand extends AbstractCommand {

    public AcceptGroupCommand() {
        super(true, "accept", "join");
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
            this.accept(playerData, player, invite);
        } else {
            String who = args[0];

            for (GroupChat invite : playerData.getGroupInvites()) {
                if (Bukkit.getOfflinePlayer(invite.getOwner()).getName().equalsIgnoreCase(who)) {
                    this.accept(playerData, player, invite);
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
                tab.add(Bukkit.getOfflinePlayer(groupChat.getOwner()).getName());
            }

            return tab;
        }

        return tab;
    }

    @Override
    public String getPermission() {
        return "rosechat.group.accept";
    }

    @Override
    public String getSyntax() {
        return this.getAPI().getLocaleManager().getLocaleMessage("command-group-accept-usage");
    }

    private void accept(PlayerData data, Player player, GroupChat groupChat) {
        this.getAPI().getLocaleManager().sendMessage(player, "command-gc-accept-success",
                StringPlaceholders.builder().addPlaceholder("name", groupChat.getName()).addPlaceholder("player", player.getDisplayName()).build());

        for (UUID uuid : groupChat.getMembers()) {
            Player member = Bukkit.getPlayer(uuid);
            if (member != null) this.getAPI().getLocaleManager()
                    .sendMessage(member, "command-gc-accept-accepted",
                            StringPlaceholders.builder().addPlaceholder("name", groupChat.getName()).addPlaceholder("player", player.getDisplayName()).build());
        }

        groupChat.addMember(player);
        data.getGroupInvites().remove(groupChat);
    }
}
