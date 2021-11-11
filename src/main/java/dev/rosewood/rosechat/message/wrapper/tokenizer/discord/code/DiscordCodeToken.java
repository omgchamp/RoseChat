package dev.rosewood.rosechat.message.wrapper.tokenizer.discord.code;

import dev.rosewood.rosechat.manager.ConfigurationManager.Setting;
import dev.rosewood.rosechat.message.RoseSender;
import dev.rosewood.rosechat.message.wrapper.tokenizer.Token;

public class DiscordCodeToken extends Token {

    public DiscordCodeToken(RoseSender sender, RoseSender viewer, String originalContent) {
        super(sender, viewer, originalContent);
    }

    @Override
    public String asString() {
        String content = this.getOriginalContent().substring(1, this.getOriginalContent().length() - 1);
        return Setting.DISCORD_FORMAT_CODE_BLOCK_ONE.getString() + content + "&r";
    }
}
