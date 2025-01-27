package dev.rosewood.rosechat.message.wrapper.tokenizer;

import dev.rosewood.rosechat.RoseChat;
import dev.rosewood.rosechat.api.RoseChatAPI;
import dev.rosewood.rosechat.chat.ChatReplacement;
import dev.rosewood.rosechat.chat.PlayerData;
import dev.rosewood.rosechat.manager.DebugManager;
import dev.rosewood.rosechat.message.MessageLocation;
import dev.rosewood.rosechat.message.MessageUtils;
import dev.rosewood.rosechat.message.MessageWrapper;
import dev.rosewood.rosechat.message.RoseSender;
import dev.rosewood.rosechat.message.wrapper.ComponentSimplifier;
import dev.rosewood.rosegarden.hook.PlaceholderAPIHook;
import dev.rosewood.rosegarden.utils.NMSUtil;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class MessageTokenizer {

    private final DebugManager debugManager;
    private final List<Tokenizer<?>> tokenizers;
    private final MessageWrapper messageWrapper;
    private final RoseSender viewer;
    private final List<Token> tokens;
    private final boolean ignorePermissions;

    public MessageTokenizer(MessageWrapper messageWrapper, RoseSender viewer, String message) {
        this(messageWrapper, viewer, message, false, Tokenizers.DEFAULT_BUNDLE);
    }

    public MessageTokenizer(MessageWrapper messageWrapper, RoseSender viewer, String message, String... tokenizerBundles) {
        this(messageWrapper, viewer, message, false, tokenizerBundles);
    }

    public MessageTokenizer(MessageWrapper messageWrapper, RoseSender viewer, String message, boolean ignorePermissions, String... tokenizerBundles) {
        this.debugManager = RoseChat.getInstance().getManager(DebugManager.class);

        this.messageWrapper = messageWrapper;
        this.viewer = viewer;
        this.tokens = new ArrayList<>();
        this.ignorePermissions = ignorePermissions;
        this.tokenizers = Arrays.stream(tokenizerBundles).flatMap(x -> Tokenizers.getBundleValues(x).stream()).distinct().collect(Collectors.toList());
        if (this.debugManager.isEnabled())
            this.debugManager.addMessage("Tokenizing New Message: " + message);
        this.tokenize(parseReplacements(message));
    }

    private String parseReplacements(String message) {
        if (this.debugManager.isEnabled())
            this.debugManager.addMessage("Parsing Replacements...");
        for (ChatReplacement replacement : RoseChatAPI.getInstance().getReplacements()) {
            if (replacement.isRegex() || !message.contains(replacement.getText())) continue;
            if (!MessageUtils.isMessageEmpty(replacement.getReplacement())) continue;
            String groupPermission = this.messageWrapper.getGroup() == null ? "" : "." + this.messageWrapper.getGroup().getLocationPermission();
            if (this.messageWrapper.getLocation() != MessageLocation.NONE
                    && !this.messageWrapper.getSender().hasPermission("rosechat.replacements." + this.messageWrapper.getLocation().toString().toLowerCase() + groupPermission)
                    || !this.messageWrapper.getSender().hasPermission("rosechat.replacement." + replacement.getId())) continue;
            message = message.replace(replacement.getText(), replacement.getReplacement());
        }

        return message;
    }

    private void tokenize(String message) {
        this.tokens.clear();
        this.tokens.addAll(this.tokenizeContent(message, true, 0, null));
    }

    private List<Token> tokenizeContent(String content, boolean tokenizeHover, int depth, Token parent) {
        if (this.debugManager.isEnabled())
            this.debugManager.addMessage("Tokenizing Content... Parent: " + (parent == null ? "none" : parent.toString()));
        List<Token> added = new ArrayList<>();
        for (int i = 0; i < content.length(); i++) {
            String substring = content.substring(i);
            for (Tokenizer<?> tokenizer : this.tokenizers) {
                if (parent != null && parent.getIgnoredTokenizers().contains(tokenizer))
                    continue;
                if (this.debugManager.isEnabled())
                    this.debugManager.addMessage("Starting Tokenizing " + tokenizer.toString() + "...");
                Token token = tokenizer.tokenize(this.messageWrapper, this.viewer, substring, parent != null || this.ignorePermissions);
                if (token != null) {
                    if (this.debugManager.isEnabled())
                        this.debugManager.addMessage("Completed Tokenizing " + tokenizer.toString() + ", " + token.toString() + ", " + token.getOriginalContent() + " -> " + token.getContent());
                    i += token.getOriginalContent().length() - 1;
                    if (depth > 15) {
                        RoseChat.getInstance().getLogger().warning("Exceeded a depth of 15 when tokenizing message. This is probably due to infinite recursion somewhere: " + this.messageWrapper.getMessage());
                        continue;
                    }

                    if (token.requiresTokenizing()) {
                        // Inherit things from parent
                        if (parent != null)
                            parent.applyInheritance(token);

                        added.add(token);
                        List<Token> generatedContent = this.tokenizeContent(token.getContent(), true, depth + 1, token);
                        token.addChildren(generatedContent);
                    } else {
                        added.add(token);
                    }

                    if (tokenizeHover && token.getHover() != null && !token.getHover().isEmpty())
                        token.addHoverChildren(this.tokenizeContent(token.getHover(), false, depth + 1, token));

                    break;
                }
            }
        }
        return added;
    }

    public BaseComponent[] toComponents() {
        ComponentBuilder componentBuilder = new ComponentBuilder();
        this.toComponents(componentBuilder, new FormattedColorGenerator(null), this.tokens);

        // Appends an empty string to always have something in the component.
        if (componentBuilder.getParts().isEmpty()) componentBuilder.append("", ComponentBuilder.FormatRetention.FORMATTING);
        return ComponentSimplifier.simplify(componentBuilder.create());
    }

    public void toComponents(ComponentBuilder componentBuilder, FormattedColorGenerator colorGenerator, List<Token> tokens) {
        for (int i = 0; i < tokens.size(); i++) {
            Token token = tokens.get(i);
            if (!token.getChildren().isEmpty()) {
                this.toComponents(componentBuilder, colorGenerator, token.getChildren());
                continue;
            }

            if (token.hasColorGenerator() || token.hasFormatCodes()) {
                List<Token> futureTokens = tokens.subList(i, tokens.size());
                if (token.hasColorGenerator()) {
                    FormattedColorGenerator newColorGenerator = new FormattedColorGenerator(token.getColorGenerator(futureTokens));
                    colorGenerator.copyFormatsTo(newColorGenerator);
                    colorGenerator = newColorGenerator;
                }

                if (token.hasFormatCodes()) {
                    PlayerData senderData = this.messageWrapper.getSenderData();
                    String color;
                    if (senderData != null) {
                        color = senderData.getColor();
                        if (color == null || color.isEmpty())
                            color = "&f";
                    } else {
                        color = "&f";
                    }

                    FormattedColorGenerator overrideColorGenerator = token.applyFormatCodes(colorGenerator, color, futureTokens);
                    if (overrideColorGenerator != null)
                        colorGenerator = overrideColorGenerator;
                }
            }

            if (!colorGenerator.isApplicable()) {
                componentBuilder.append(token.getContent(), token.shouldRetainColour() ? ComponentBuilder.FormatRetention.FORMATTING : ComponentBuilder.FormatRetention.NONE);
                if (NMSUtil.getVersionNumber() >= 16) componentBuilder.font(token.getEffectiveFont());

                if (token.getHover() != null) {
                    if (token.getHoverChildren().isEmpty()) {
                        componentBuilder.event(new HoverEvent(token.getHoverAction(), TextComponent.fromLegacyText(token.getHover())));
                    } else {
                        ComponentBuilder hoverBuilder = new ComponentBuilder();
                        this.toComponents(hoverBuilder, new FormattedColorGenerator(null), token.getHoverChildren());
                        componentBuilder.event(new HoverEvent(token.getHoverAction(), hoverBuilder.create()));
                    }
                }

                if (token.getClick() != null)
                    componentBuilder.event(new ClickEvent(token.getClickAction(), PlaceholderAPIHook.applyPlaceholders(this.messageWrapper.getSender().asPlayer(), MessageUtils.getSenderViewerPlaceholders(this.messageWrapper.getSender(), this.viewer).build().apply(token.getClick()))));
            } else {
                // Make sure to apply the color even if there's no content
                if (token.getContent().isEmpty()) {
                    componentBuilder.append("", token.shouldRetainColour() ? ComponentBuilder.FormatRetention.ALL : ComponentBuilder.FormatRetention.FORMATTING);
                    colorGenerator.apply(componentBuilder, false);
                    continue;
                }

                for (char c : token.getContent().toCharArray()) {
                    componentBuilder.append(String.valueOf(c), token.shouldRetainColour() ? ComponentBuilder.FormatRetention.ALL : ComponentBuilder.FormatRetention.FORMATTING);
                    if (NMSUtil.getVersionNumber() >= 16) componentBuilder.font(token.getEffectiveFont());

                    colorGenerator.apply(componentBuilder, Character.isSpaceChar(c));

                    if (token.getHover() != null) {
                        if (token.getHoverChildren().isEmpty()) {
                            componentBuilder.event(new HoverEvent(token.getHoverAction(), TextComponent.fromLegacyText(token.getHover())));
                        } else {
                            ComponentBuilder hoverBuilder = new ComponentBuilder();
                            this.toComponents(hoverBuilder, new FormattedColorGenerator(null), token.getHoverChildren());
                            componentBuilder.event(new HoverEvent(token.getHoverAction(), hoverBuilder.create()));
                        }
                    }

                    if (token.getClick() != null)
                        componentBuilder.event(new ClickEvent(token.getClickAction(), PlaceholderAPIHook.applyPlaceholders(this.messageWrapper.getSender().asPlayer(), MessageUtils.getSenderViewerPlaceholders(this.messageWrapper.getSender(), this.viewer).build().apply(token.getClick()))));
                }
            }
        }
    }

}
