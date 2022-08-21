package dev.rosewood.rosechat.message.wrapper.tokenizer.discord.spoiler;

import dev.rosewood.rosechat.manager.ConfigurationManager;
import dev.rosewood.rosechat.message.MessageWrapper;
import dev.rosewood.rosechat.message.RoseSender;
import dev.rosewood.rosechat.message.wrapper.tokenizer.Token;
import dev.rosewood.rosechat.message.wrapper.tokenizer.Tokenizer;

public class ToDiscordSpoilerTokenizer implements Tokenizer<Token> {

    @Override
    public Token tokenize(MessageWrapper messageWrapper, RoseSender viewer, String input, boolean ignorePermissions) {
        String spoiler = ConfigurationManager.Setting.DISCORD_FORMAT_SPOILER.getString();
        String prefix = spoiler.substring(0, spoiler.indexOf("%message%"));
        String suffix = spoiler.substring(spoiler.indexOf("%message%") + "%message%".length());
        if (!input.startsWith(prefix)) return null;

        String originalContent = input.substring(0, input.indexOf(suffix) + suffix.length());
        String content = input.substring(prefix.length(), input.indexOf(suffix));

        return new Token(new Token.TokenSettings(originalContent).content("||" + content + "||").ignoreTokenizer(this));
    }
}
