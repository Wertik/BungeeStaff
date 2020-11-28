package bungeestaff.bungee.util;

import com.google.common.base.Strings;
import lombok.experimental.UtilityClass;
import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;

@UtilityClass
public class TextUtil {

    public void sendMessage(CommandSender sender, String message) {
        if (!Strings.isNullOrEmpty(message))
            sender.sendMessage(new TextComponent(color(message)));
    }

    public TextComponent format(String msg) {
        return new TextComponent(color(msg));
    }

    public String color(String msg) {
        return msg == null ? null : ChatColor.translateAlternateColorCodes('&', msg);
    }
}