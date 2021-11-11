package dev.rosewood.rosechat.hook.discord;

import dev.rosewood.rosechat.api.RoseChatAPI;
import dev.rosewood.rosechat.chat.Group;
import dev.rosewood.rosechat.message.MessageUtils;
import dev.rosewood.rosechat.message.MessageWrapper;
import dev.rosewood.rosechat.placeholders.CustomPlaceholder;
import dev.rosewood.rosechat.placeholders.DiscordPlaceholder;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.EmbedType;
import github.scarsz.discordsrv.dependencies.jda.api.entities.MessageEmbed;
import github.scarsz.discordsrv.dependencies.jda.api.entities.TextChannel;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.TextComponent;
import java.time.OffsetDateTime;

public class DiscordSRVProvider implements DiscordChatProvider {

    @Override
    public void sendMessage(MessageWrapper messageWrapper, Group group, String channel) {
        TextChannel textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channel);

        BaseComponent[] message;
        boolean hasMessagePlaceholder = false;
        StringPlaceholders placeholders = MessageUtils.getSenderViewerPlaceholders(messageWrapper.getSender(), messageWrapper.getSender(), group).build();
        DiscordPlaceholder placeholder = RoseChatAPI.getInstance().getPlaceholderSettingManager().getDiscordPlaceholder();

        CustomPlaceholder textPlaceholder = placeholder.getPlaceholder("text");
        String text = textPlaceholder != null ? textPlaceholder.getText().parse(messageWrapper.getSender(), messageWrapper.getSender(), placeholders) : null;
        if (text != null && text.contains("{message}")) {
            message = messageWrapper.parse(text, messageWrapper.getSender());
            text = TextComponent.toPlainText(message);
        }

        CustomPlaceholder urlPlaceholder = placeholder.getPlaceholder("url");
        String url = urlPlaceholder != null ? urlPlaceholder.getText().parse(messageWrapper.getSender(), messageWrapper.getSender(), placeholders) : null;

        CustomPlaceholder titlePlaceholder = placeholder.getPlaceholder("title");
        String title = titlePlaceholder != null ? titlePlaceholder.getText().parse(messageWrapper.getSender(), messageWrapper.getSender(), placeholders) : null;
        if (title != null && title.contains("{message}")) {
            message = messageWrapper.parse(title, messageWrapper.getSender());
            title = TextComponent.toPlainText(message);
            hasMessagePlaceholder = true;
        }

        CustomPlaceholder descriptionPlaceholder = placeholder.getPlaceholder("description");
        String description = descriptionPlaceholder != null ? descriptionPlaceholder.getText().parse(messageWrapper.getSender(), messageWrapper.getSender(), placeholders) : null;
        if (description != null && description.contains("{message}")) {
            message = messageWrapper.parse(description, messageWrapper.getSender());
            description = TextComponent.toPlainText(message);
            hasMessagePlaceholder = true;
        }

        CustomPlaceholder timestampPlaceholder = placeholder.getPlaceholder("timestamp");
        boolean timestamp = timestampPlaceholder != null && Boolean.parseBoolean(timestampPlaceholder.getText().parse(messageWrapper.getSender(), messageWrapper.getSender(), placeholders));

        CustomPlaceholder colorPlaceholder = placeholder.getPlaceholder("color");
        int color = colorPlaceholder != null ? Integer.parseInt(colorPlaceholder.getText().parse(messageWrapper.getSender(), messageWrapper.getSender(), placeholders)) : 0;

        CustomPlaceholder thumbnailPlaceholder = placeholder.getPlaceholder("thumbnail");
        String thumbnail = thumbnailPlaceholder != null? placeholders.apply(thumbnailPlaceholder.getText().parse(messageWrapper.getSender(), messageWrapper.getSender(), placeholders)) : null;

        CustomPlaceholder thumbnailWidthPlaceholder = placeholder.getPlaceholder("thumbnail-width");
        int thumbnailWidth = thumbnailWidthPlaceholder != null? Integer.parseInt(thumbnailWidthPlaceholder.getText().parse(messageWrapper.getSender(), messageWrapper.getSender(), placeholders)) : 128;

        CustomPlaceholder thumbnailHeightPlaceholder = placeholder.getPlaceholder("thumbnail-height");
        int thumbnailHeight = thumbnailHeightPlaceholder != null? Integer.parseInt(thumbnailHeightPlaceholder.getText().parse(messageWrapper.getSender(), messageWrapper.getSender(), placeholders)) : 128;

        if (hasMessagePlaceholder) {
            MessageEmbed messageEmbed = new MessageEmbed(url,
                    title,
                    description,
                    EmbedType.RICH,
                    timestamp ? OffsetDateTime.now(): null,
                    color,
                    new MessageEmbed.Thumbnail(thumbnail, thumbnail, thumbnailWidth, thumbnailHeight),
                    null,
                    null,
                    null,
                    null,
                    null,
                    null);
            textChannel.sendMessageEmbeds(messageEmbed).queue();
        } else {
            if (text != null) textChannel.sendMessage(text).queue();
        }
    }
}