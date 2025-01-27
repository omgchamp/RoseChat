package dev.rosewood.rosechat.command;

import dev.rosewood.rosechat.chat.PlayerData;
import dev.rosewood.rosechat.command.api.AbstractCommand;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

public class IgnoreCommand extends AbstractCommand {

    public IgnoreCommand() {
        super(true, "ignore");
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            this.getAPI().getLocaleManager().sendComponentMessage(sender, "invalid-arguments", StringPlaceholders.single("syntax", this.getSyntax()));
            return;
        }

        Player player = (Player) sender;
        OfflinePlayer target = Bukkit.getOfflinePlayer(args[0]);
        if (target == null) {
            this.getAPI().getLocaleManager().sendComponentMessage(sender, "player-not-found");
            return;
        }

        PlayerData playerData = this.getAPI().getPlayerData(player.getUniqueId());
        PlayerData targetData = this.getAPI().getPlayerData(target.getUniqueId());
        if (targetData == null) {
            this.getAPI().getLocaleManager().sendComponentMessage(sender, "player-not-found");
            return;
        }

        String name = targetData.getNickname() == null ? target.getName() : targetData.getNickname();

        if (playerData.getIgnoringPlayers().contains(target.getUniqueId())) {
            playerData.unignore(target.getUniqueId());
            this.getAPI().getLocaleManager().sendComponentMessage(sender, "command-ignore-unignored", StringPlaceholders.single("player", name));
        } else {
            playerData.ignore(target.getUniqueId());
            this.getAPI().getLocaleManager().sendComponentMessage(sender, "command-ignore-ignored", StringPlaceholders.single("player", name));
        }
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> tab = new ArrayList<>();

        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player != sender) tab.add(player.getName());
            }
        }

        return tab;
    }

    @Override
    public String getPermission() {
        return "rosechat.ignore";
    }

    @Override
    public String getSyntax() {
        return this.getAPI().getLocaleManager().getLocaleMessage("command-ignore-usage");
    }

}
