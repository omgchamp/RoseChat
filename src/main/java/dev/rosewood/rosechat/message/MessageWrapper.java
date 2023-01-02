package dev.rosewood.rosechat.message;

import dev.rosewood.rosechat.api.RoseChatAPI;
import dev.rosewood.rosechat.api.event.PostParseMessageEvent;
import dev.rosewood.rosechat.api.event.PreParseMessageEvent;
import dev.rosewood.rosechat.chat.FilterType;
import dev.rosewood.rosechat.chat.Group;
import dev.rosewood.rosechat.chat.PlayerData;
import dev.rosewood.rosechat.manager.ConfigurationManager.Setting;
import dev.rosewood.rosechat.message.wrapper.tokenizer.MessageTokenizer;
import dev.rosewood.rosechat.message.wrapper.tokenizer.Tokenizers;
import dev.rosewood.rosechat.placeholders.RoseChatPlaceholder;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.chat.ComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.regex.Matcher;

/**
 * Wrapper to turn regular messages into BaseComponent[]'s with colour, emoji, tags, etc.
 */
public class MessageWrapper {

    private final RoseSender sender;
    private final Group group;
    private final MessageLocation location;
    private final String locationPermission;
    private String message;
    private PlayerData senderData;
    private StringPlaceholders placeholders;
    private boolean logMessages;
    private boolean privateMessage;
    private PrivateMessageInfo privateMessageInfo;

    private final List<UUID> taggedPlayers;
    private Sound tagSound;
    private boolean canBeSent;
    private FilterType filterType;
    private BaseComponent[] tokenized;
    private UUID id;
    private DeletableMessage deletableMessage;

    public MessageWrapper(RoseSender sender, MessageLocation messageLocation, Group group, String message) {
        this.sender = sender;
        this.group = group;
        this.location = messageLocation;
        this.locationPermission = this.location.toString().toLowerCase() + (group == null ? "" : "." + group.getLocationPermission());
        this.message = message;
        this.canBeSent = true;
        if (sender.isPlayer()) this.senderData = RoseChatAPI.getInstance().getPlayerData(sender.getUUID());
        this.taggedPlayers = new ArrayList<>();
        this.id = UUID.randomUUID();
        this.logMessages = true;
        this.placeholders = StringPlaceholders.empty();
    }

    public MessageWrapper(RoseSender sender, MessageLocation location, Group group, String message, StringPlaceholders placeholders) {
        this(sender, location, group, message);
        this.placeholders = placeholders;
    }

    public MessageWrapper(Player sender, MessageLocation messageLocation, Group group, String message) {
        this(new RoseSender(sender), messageLocation, group, message);
    }

    private boolean isCaps() {
        if (this.sender.hasPermission("rosechat.caps." + this.locationPermission)) return false;
        if (!Setting.CAPS_CHECKING_ENABLED.getBoolean()) return false;
        int caps = 0;
        for (char c : this.message.toCharArray()) {
            if (Character.isAlphabetic(c) && c == Character.toUpperCase(c)) caps++;
        }

        return caps > Setting.MAXIMUM_CAPS_ALLOWED.getInt();
    }

    /**
     * Decides to filter based on permissions, filters based on configuration settings.
     * @return The MessageWrapper.
     */
    public MessageWrapper filterCaps() {
        if (this.sender.hasPermission("rosechat.caps." + this.locationPermission)) return this;
        if (!Setting.CAPS_CHECKING_ENABLED.getBoolean()) return this;
        if (!this.isCaps()) return this;

        if (Setting.WARN_ON_CAPS_SENT.getBoolean()) this.filterType = FilterType.CAPS;
        if (Setting.LOWERCASE_CAPS_ENABLED.getBoolean()) {
            this.message = this.message.toLowerCase();
            return this;
        }

        this.canBeSent = false;
        return this;
    }

    /**
     * Decides to filter based on permissions, filters based on configuration settings.
     * @return The MessageWrapper.
     */
    public MessageWrapper filterSpam() {
        if (this.sender.hasPermission("rosechat.spam." + this.locationPermission) || this.senderData == null) return this;
        if (!Setting.SPAM_CHECKING_ENABLED.getBoolean()) return this;
        if (this.senderData != null && !this.senderData.getMessageLog().addMessageWithSpamCheck(this.message)) return this;
        if (Setting.WARN_ON_SPAM_SENT.getBoolean()) this.filterType = FilterType.SPAM;
        this.canBeSent = false;
        return this;
    }

    /**
     * Decides to filter based on permissions, filters based on configuration settings.
     * @return The MessageWrapper.
     */
    public MessageWrapper filterURLs() {
        if (this.sender.hasPermission("rosechat.links." + this.locationPermission)) return this;
        if (!Setting.URL_CHECKING_ENABLED.getBoolean()) return this;

        boolean hasURL = false;
        Matcher matcher = MessageUtils.URL_PATTERN.matcher(this.message);
        while (matcher.find()) {
            String url = this.message.substring(matcher.start(), matcher.end());
            this.message = this.message.replace(url, "&m" + url.replace(".", " ") + "&r");
            hasURL = true;
        }

        if (!hasURL) return this;
        if (Setting.WARN_ON_URL_SENT.getBoolean()) this.filterType = FilterType.URL;

        this.canBeSent = Setting.URL_CENSORING_ENABLED.getBoolean();
        return this;
    }

    /**
     * Decides to filter based on permissions, filters based on configuration settings.
     * @return The MessageWrapper.
     */
    public MessageWrapper filterLanguage() {
        if (this.sender.hasPermission("rosechat.language." + this.locationPermission)) return this;
        if (!Setting.SWEAR_CHECKING_ENABLED.getBoolean()) return this;
        String rawMessage = MessageUtils.stripAccents(this.message.toLowerCase());

        // Split by space to avoid things like 'grass' being caught.
        // This is an impossible problem to fix.
        for (String swear : Setting.BLOCKED_SWEARS.getStringList()) {
            for (String word : rawMessage.split(" ")) {
                double similarity = MessageUtils.getLevenshteinDistancePercent(swear, word);
                if (similarity >= (Setting.SWEAR_FILTER_SENSITIVITY.getDouble() / 100)) {
                    if (Setting.WARN_ON_BLOCKED_SWEAR_SENT.getBoolean()) this.filterType = FilterType.SWEAR;
                    this.canBeSent = false;
                    return this;
                }
            }
        }

        for (String replacements : Setting.SWEAR_REPLACEMENTS.getStringList()) {
            String[] swearReplacement = replacements.split(":");
            String swear = swearReplacement[0];
            String replacement = swearReplacement[1];
            for (String word : this.message.split(" ")) {
                double similarity = MessageUtils.getLevenshteinDistancePercent(swear, word);
                if (similarity >= (Setting.SWEAR_FILTER_SENSITIVITY.getDouble() / 100)) {
                    this.message = this.message.replace(word, replacement);
                }
            }
        }

        return this;
    }

    /**
     * Filter the messaged based on permissions and configuration settings.
     * @return The MessageWrapper.
     */
    public MessageWrapper filter() {
        return this.filterCaps().filterSpam().filterLanguage().filterURLs();
    }

    /**
     * Applys the sender's default chat colour.
     * This should be used after validating and filtering as it may cause issues.
     * @return The MessageWrapper.
     */
    public MessageWrapper applyDefaultColor() {
        if (this.senderData != null)
            this.message = this.senderData.getColor() + this.message;

        return this;
    }

    /**
     * Allows ignoring message logging.
     * @return The MessageWrapper.
     */
    public MessageWrapper ignoreMessageLogging() {
        this.logMessages = false;
        return this;
    }

    /**
     * Sets this message as a private message. This means that the sender is used to see if the message can be deleted.
     * @return The MessageWrapper.
     */
    public MessageWrapper setPrivateMessage() {
        this.privateMessage = true;
        return this;
    }

    /**
     * @param info The information about the private message that this wrapper contains.
     * @return The MessageWrapper.
     */
    public MessageWrapper setPrivateMessageInfo(PrivateMessageInfo info) {
        this.privateMessageInfo = info;
        return this;
    }

    public String parseToString() {
        return this.message;
    }

    private void logMessage(MessageLog log, String discordId) {
        if (!this.logMessages) return;
        this.deletableMessage = new DeletableMessage(this.id, ComponentSerializer.toString(this.tokenized), false, discordId);
        this.deletableMessage.setPrivateMessageInfo(this.getPrivateMessageInfo());
        log.addDeletableMessage(this.deletableMessage);
    }

    private String getChatColorFromFormat(String format, RoseSender viewer) {
        String lastColor = "";
        String lastFormat = "";

        String[] placeholders = format.split("\\{");
        String colorPlaceholder = placeholders.length > 2 ? placeholders[placeholders.length - 2] : placeholders[0];
        if (!colorPlaceholder.endsWith("}")) colorPlaceholder = colorPlaceholder.substring(0, colorPlaceholder.lastIndexOf("}") + 1);
        RoseChatPlaceholder placeholder = RoseChatAPI.getInstance().getPlaceholderManager().getPlaceholder(colorPlaceholder.substring(0, colorPlaceholder.length() - 1));
        String value = placeholder.getText().parseToString(this.sender, viewer, this.placeholders);

        Matcher colorMatcher = MessageUtils.STOP.matcher(value);
        while (colorMatcher.find())
            lastColor = colorMatcher.group();

        Matcher formatMatcher = MessageUtils.VALID_LEGACY_REGEX_FORMATTING.matcher(value);
        while (formatMatcher.find())
            lastFormat = formatMatcher.group();

        // Check the format string for colours after, e.g. {player}:&c{message}
        colorMatcher = MessageUtils.STOP.matcher(format);
        while (colorMatcher.find()) {
            if (format.indexOf(colorMatcher.group()) > format.indexOf(colorPlaceholder)) lastColor = colorMatcher.group();
        }

        formatMatcher = MessageUtils.VALID_LEGACY_REGEX_FORMATTING.matcher(format);
        while (formatMatcher.find()) {
            if (format.indexOf(formatMatcher.group()) > format.indexOf(colorPlaceholder)) lastFormat = formatMatcher.group();
        }

        return lastFormat + lastColor;
    }

    private final Map<UUID, List<QueuedMessage>> queue = new HashMap<>();

    public BaseComponent[] parse(String format, RoseSender viewer) {
        PreParseMessageEvent preParseMessageEvent = new PreParseMessageEvent(this, viewer);
        Bukkit.getPluginManager().callEvent(preParseMessageEvent);

        if (!preParseMessageEvent.isCancelled()) {
            RoseSender receivingViewer = this.isPrivateMessage() ? this.getPrivateMessageInfo().getReceiver() : viewer;

            ComponentBuilder componentBuilder = new ComponentBuilder();

            String[] formatSplit = format.split("\\{message\\}");
            String before = formatSplit[0];
            String after = formatSplit.length > 1 ? formatSplit[1] : null;

            if (before != null && !before.isEmpty()) {
                new MessageTokenizer(this, receivingViewer, this.message,
                        Tokenizers.DISCORD_EMOJI_BUNDLE, Tokenizers.MARKDOWN_BUNDLE, Tokenizers.DISCORD_FORMATTING_BUNDLE, Tokenizers.DEFAULT_BUNDLE)
                        .queue(this.message, (message) -> {
                            //this.queue.put(0, message);
                        });
            }

            if (format.contains("{message}")) {
                new MessageTokenizer(this, receivingViewer, this.message,
                        Tokenizers.DISCORD_EMOJI_BUNDLE, Tokenizers.MARKDOWN_BUNDLE, Tokenizers.DISCORD_FORMATTING_BUNDLE, Tokenizers.DEFAULT_BUNDLE)
                        .queue(this.message, (message) -> {
                           // this.queue.put(1, message);
                        });
            }

            if (after != null && !after.isEmpty()) {
                new MessageTokenizer(this, receivingViewer, after,
                        Tokenizers.DISCORD_EMOJI_BUNDLE, Tokenizers.MARKDOWN_BUNDLE, Tokenizers.DISCORD_FORMATTING_BUNDLE, Tokenizers.DEFAULT_BUNDLE)
                        .queue(after, (message) -> {
                           // this.queue.put(2, message);
                        });
            }

            this.tokenized = componentBuilder.create();

            PostParseMessageEvent postParseMessageEvent = new PostParseMessageEvent(this, viewer);
            Bukkit.getPluginManager().callEvent(postParseMessageEvent);

            if (viewer != null) {
                PlayerData viewerData = RoseChatAPI.getInstance().getPlayerData(viewer.getUUID());
                if (viewerData != null) this.logMessage(viewerData.getMessageLog(), null);
            }

            return this.tokenized;
        }

        return this.toComponents();
    }

    public void parseAsynchronously(String format, RoseSender viewer, Consumer<BaseComponent[]> callback) {
        PreParseMessageEvent preParseMessageEvent = new PreParseMessageEvent(this, viewer);
        Bukkit.getPluginManager().callEvent(preParseMessageEvent);

        if (!preParseMessageEvent.isCancelled()) {
            RoseSender receivingViewer = this.isPrivateMessage() ? this.getPrivateMessageInfo().getReceiver() : viewer;

            String[] formatSplit = format.split("\\{message\\}");
            String before = formatSplit[0];
            String after = formatSplit.length > 1 ? formatSplit[1] : null;

            if (before != null && !before.isEmpty()) {
                new MessageTokenizer(this, receivingViewer, before,
                        Tokenizers.DISCORD_EMOJI_BUNDLE, Tokenizers.MARKDOWN_BUNDLE, Tokenizers.DISCORD_FORMATTING_BUNDLE, Tokenizers.DEFAULT_BUNDLE)
                        .queue(before, (message) -> {
                            List<QueuedMessage> messages = this.queue.getOrDefault(viewer.getUUID(), new ArrayList<>());
                            messages.add(new QueuedMessage(message, 0));
                            this.queue.put(viewer.getUUID(), messages);
                            BaseComponent[] components = waitForComponents(formatSplit.length, viewer);
                            if (components != null) callback.accept(this.tokenized);
                        });
            }

            if (format.contains("{message}")) {
                new MessageTokenizer(this, receivingViewer, this.message,
                        Tokenizers.DISCORD_EMOJI_BUNDLE, Tokenizers.MARKDOWN_BUNDLE, Tokenizers.DISCORD_FORMATTING_BUNDLE, Tokenizers.DEFAULT_BUNDLE)
                        .queue(this.message, (message) -> {
                            List<QueuedMessage> messages = this.queue.getOrDefault(viewer.getUUID(), new ArrayList<>());
                            messages.add(new QueuedMessage(message, 1));
                            this.queue.put(viewer.getUUID(), messages);
                            BaseComponent[] components = waitForComponents(formatSplit.length, viewer);
                            if (components != null) callback.accept(this.tokenized);
                        });
            }

            if (after != null && !after.isEmpty()) {
                new MessageTokenizer(this, receivingViewer, after,
                        Tokenizers.DISCORD_EMOJI_BUNDLE, Tokenizers.MARKDOWN_BUNDLE, Tokenizers.DISCORD_FORMATTING_BUNDLE, Tokenizers.DEFAULT_BUNDLE)
                        .queue(after, (message) -> {
                            List<QueuedMessage> messages = this.queue.getOrDefault(viewer.getUUID(), new ArrayList<>());
                            messages.add(new QueuedMessage(message, 2));
                            this.queue.put(viewer.getUUID(), messages);
                            BaseComponent[] components = waitForComponents(formatSplit.length, viewer);
                            if (components != null) callback.accept(this.tokenized);
                        });
            }
        }
    }

    private BaseComponent[] waitForComponents(int size, RoseSender viewer) {
        System.out.println("Waiting for comps, size: " + (size + 1) + " Obtained: " + this.queue.size());
        if (this.queue.get(viewer.getUUID()).size() == size + 1) {
            System.out.println("Found all components needed");
            ComponentBuilder componentBuilder = new ComponentBuilder();
            this.queue.get(viewer.getUUID()).stream().sorted(Comparator.comparing(QueuedMessage::getId)).forEach(entry -> {
                Bukkit.getConsoleSender().sendMessage("Message");
                Bukkit.getConsoleSender().spigot().sendMessage(entry.getMessage());
                componentBuilder.append(entry.getMessage(), ComponentBuilder.FormatRetention.FORMATTING);
            });

            this.tokenized = componentBuilder.create();

            PostParseMessageEvent postParseMessageEvent = new PostParseMessageEvent(this, viewer);
            Bukkit.getPluginManager().callEvent(postParseMessageEvent);

            if (viewer != null) {
                PlayerData viewerData = RoseChatAPI.getInstance().getPlayerData(viewer.getUUID());
                if (viewerData != null) this.logMessage(viewerData.getMessageLog(), null);
            }

            this.queue.remove(viewer.getUUID());
            return tokenized;
        }

        return null;
    }

    public BaseComponent[] parseFromDiscord(String id, String format, RoseSender viewer) {
        return null;
    }

    public BaseComponent[] parseToDiscord(String format, RoseSender viewer) {
        return null;
    }

    public BaseComponent[] toComponents() {
        return this.tokenized;
    }

    public void setComponents(BaseComponent[] components) {
        this.tokenized = components;
    }

    public List<UUID> getTaggedPlayers() {
        return this.taggedPlayers;
    }

    public void setTagSound(Sound tagSound) {
        this.tagSound = tagSound;
    }

    public Sound getTagSound() {
        return this.tagSound;
    }

    public boolean canBeSent() {
        return this.canBeSent;
    }

    public FilterType getFilterType() {
        return this.filterType;
    }

    public String getMessage() {
        return this.message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public RoseSender getSender() {
        return this.sender;
    }

    public PlayerData getSenderData() {
        return this.senderData;
    }

    public MessageLocation getLocation() {
        return this.location;
    }

    public StringPlaceholders getPlaceholders() {
        return this.placeholders;
    }

    public Group getGroup() {
        return this.group;
    }

    public UUID getId() {
        return this.id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public DeletableMessage getDeletableMessage() {
        return this.deletableMessage;
    }

    public boolean shouldLogMessages() {
        return this.logMessages;
    }

    public void setShouldLogMessages(boolean logMessages) {
        this.logMessages = logMessages;
    }

    public boolean isPrivateMessage() {
        return this.privateMessage;
    }

    public PrivateMessageInfo getPrivateMessageInfo() {
        return this.privateMessageInfo;
    }

}
