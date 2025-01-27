package dev.rosewood.rosechat.message.wrapper.tokenizer.markdown;

import dev.rosewood.rosechat.manager.ConfigurationManager;
import dev.rosewood.rosechat.message.MessageUtils;
import dev.rosewood.rosechat.message.MessageWrapper;
import dev.rosewood.rosechat.message.RoseSender;
import dev.rosewood.rosechat.message.wrapper.tokenizer.Token;
import dev.rosewood.rosechat.message.wrapper.tokenizer.Tokenizer;
import java.util.regex.Matcher;

public class MarkdownStrikethroughTokenizer implements Tokenizer<Token> {

    @Override
    public Token tokenize(MessageWrapper messageWrapper, RoseSender viewer, String input, boolean ignorePermissions) {
        if (!this.hasPermission(messageWrapper, ignorePermissions, "rosechat.strikethrough")) return null;
        if (!input.startsWith("~~")) return null;

        Matcher matcher = MessageUtils.STRIKETHROUGH_MARKDOWN_PATTERN.matcher(input);
        if (matcher.find()) {
            if (matcher.start() != 0) return null;
            String originalContent = input.substring(0, matcher.end());
            String content = originalContent.substring(2, originalContent.length() - 2);

            String format = ConfigurationManager.Setting.MARKDOWN_FORMAT_STRIKETHROUGH.getString();
            content = format.contains("%message%") ? format.replace("%message%", content) : format + content;
            return new Token(new Token.TokenSettings(originalContent).content(content).ignoreTokenizer(this));
        }

        return null;
    }

}
