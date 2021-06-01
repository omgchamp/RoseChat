package dev.rosewood.rosechat.command.group;

import dev.rosewood.rosechat.chat.GroupChat;
import dev.rosewood.rosechat.chat.PlayerData;
import dev.rosewood.rosechat.command.api.AbstractCommand;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.List;

public class InviteGroupCommand extends AbstractCommand {

    public InviteGroupCommand() {
        super(true, "invite");
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {
        if (args.length == 0) {
            this.getAPI().getLocaleManager().sendMessage(sender, "invalid-arguments", StringPlaceholders.single("syntax", getSyntax()));
            return;
        }

        Player player = (Player) sender;
        GroupChat groupChat = this.getAPI().getGroupChat(player.getUniqueId());
        if (groupChat == null) {
            this.getAPI().getLocaleManager().sendMessage(sender, "no-gc");
            return;
        }

        // Hard limit of 128, no one should reach this... right?
        if (groupChat.getMembers().size() == 128) {
            this.getAPI().getLocaleManager().sendMessage(sender, "command-gc-invite-full");
            return;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            this.getAPI().getLocaleManager().sendMessage(sender, "player-not-found");
            return;
        }

        PlayerData data = this.getAPI().getPlayerData(target.getUniqueId());

        this.getAPI().getLocaleManager().sendMessage(target, "command-gc-invite-invited",
                StringPlaceholders.builder().addPlaceholder("player", player.getDisplayName()).addPlaceholder("name", groupChat.getName()).build());
        this.getAPI().getLocaleManager().sendMessage(player, "command-gc-invite-success",
                StringPlaceholders.builder().addPlaceholder("player", target.getDisplayName()).addPlaceholder("name", groupChat.getName()).build());
        sendAcceptMessage(target, player);

        data.inviteToGroup(groupChat);
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, String[] args) {
        List<String> tab = new ArrayList<>();
        if (args.length == 1) {
            for (Player player : Bukkit.getOnlinePlayers()) {
                tab.add(player.getName());
            }

            return tab;
        }

        return tab;
    }

    @Override
    public String getPermission() {
        return "rosechat.group.invite";
    }

    @Override
    public String getSyntax() {
        return this.getAPI().getLocaleManager().getLocaleMessage("command-gc-invite-usage");
    }

    private void sendAcceptMessage(Player player, Player owner) {
        ComponentBuilder componentBuilder = new ComponentBuilder();
        componentBuilder.append("          ");
        componentBuilder.append(this.getAPI().getLocaleManager().getLocaleMessage("command-gc-accept-accept"))
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gc accept " + owner.getName()))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(this.getAPI().getLocaleManager().getLocaleMessage("command-gc-accept-hover"))));
        componentBuilder.append("          ").retain(ComponentBuilder.FormatRetention.NONE);
        componentBuilder.append(this.getAPI().getLocaleManager().getLocaleMessage("command-gc-deny-deny"))
                .event(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/gc deny " + owner.getName()))
                .event(new HoverEvent(HoverEvent.Action.SHOW_TEXT, TextComponent.fromLegacyText(this.getAPI().getLocaleManager().getLocaleMessage("command-gc-deny-hover"))));
        player.spigot().sendMessage(componentBuilder.create());
    }
}
