package dev.rosewood.rosechat.message.wrapper.tokenizer.replacement;

import dev.rosewood.rosechat.api.RoseChatAPI;
import dev.rosewood.rosechat.chat.ChatReplacement;
import dev.rosewood.rosechat.message.MessageWrapper;
import dev.rosewood.rosechat.message.RoseSender;
import dev.rosewood.rosechat.message.wrapper.tokenizer.Token;
import dev.rosewood.rosechat.message.wrapper.tokenizer.Tokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RegexReplacementTokenizer implements Tokenizer<Token> {

    @Override
    public Token tokenize(MessageWrapper messageWrapper, RoseSender viewer, String input, boolean ignorePermissions) {
        for (ChatReplacement replacement : RoseChatAPI.getInstance().getReplacements()) {
            if (!replacement.isRegex()) continue;
            if (!this.hasExtendedPermission(messageWrapper, ignorePermissions, "rosechat.replacements", "rosechat.replacement." + replacement.getId())) return null;

            Matcher matcher = Pattern.compile(replacement.getText()).matcher(input);
            if (matcher.find()) {
                String originalContent = input.substring(matcher.start(), matcher.end());
                if (!input.startsWith(originalContent)) return null;

                String content = replacement.getReplacement();

                return new Token(new Token.TokenSettings(originalContent).content(content).placeholder("message", originalContent)
                        .placeholder("extra", originalContent).ignoreTokenizer(this));
            }
        }

        return null;
    }

}
