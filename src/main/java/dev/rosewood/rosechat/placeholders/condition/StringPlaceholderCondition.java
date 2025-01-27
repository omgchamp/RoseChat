package dev.rosewood.rosechat.placeholders.condition;

import dev.rosewood.rosechat.message.RoseSender;
import dev.rosewood.rosegarden.utils.StringPlaceholders;
import net.md_5.bungee.api.chat.ClickEvent;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;

public class StringPlaceholderCondition extends PlaceholderCondition  {

    public StringPlaceholderCondition(ConfigurationSection section, String condition) {
        super(section, condition);
    }

    @Override
    protected String parse(RoseSender sender, RoseSender viewer, StringPlaceholders placeholders) {
        StringBuilder resultBuilder = new StringBuilder();
        for (String placeholder : this.condition.split(",")) {
            String parsed = "";

            if (placeholder.contains("||")) {
                parsed = String.valueOf(new CompoundPlaceholderCondition(this.section, placeholder).parseValues().parseToBoolean(sender, viewer, placeholders));
            } else {
                Player player = this.condition.startsWith("%other_") ? viewer.asPlayer() : sender.asPlayer();
                String condition = placeholder.replace("other_", "");
                parsed = this.parsePlaceholders(player, condition, placeholders);

                // Convert PAPI 'yes' and 'no' to 'true' and 'false'
                parsed = parsed.equalsIgnoreCase("yes") ? "true" : parsed;
                parsed = parsed.equalsIgnoreCase("no") ? "false" : parsed;
            }

            if (resultBuilder.length() != 0) resultBuilder.append(",");

            boolean hasCondition = false;
            for (String conditionValue : this.conditionValues.keySet()) {
                hasCondition = conditionValue.toLowerCase().contains(resultBuilder.toString().toLowerCase() + parsed.toLowerCase());
                if (hasCondition) break;
            }

            resultBuilder.append(hasCondition ? parsed.toLowerCase() : "default");
        }

        return resultBuilder.toString();
    }


    @Override
    public String parseToString(RoseSender sender, RoseSender viewer, StringPlaceholders placeholders) {
        String parsed = this.parse(sender, viewer, placeholders);
        String result = this.combineConditionValues(parsed);
        return result == null || result.isEmpty() ? this.combineConditionValues("default") : result;
    }

    @Override
    public ClickEvent.Action parseToAction(RoseSender sender, RoseSender viewer, StringPlaceholders placeholders) {
        String parsed = this.parse(sender, viewer, placeholders);
        return this.getClickAction(parsed);
    }

}
