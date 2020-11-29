package bungeestaff.bungee.commands;

import bungeestaff.bungee.BungeeStaffPlugin;
import bungeestaff.bungee.commands.framework.CommandBase;
import bungeestaff.bungee.rabbit.CachedUser;
import bungeestaff.bungee.rabbit.MessageType;
import bungeestaff.bungee.system.cooldown.CooldownType;
import bungeestaff.bungee.util.TextUtil;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;

public class ReportCommand extends CommandBase {

    public ReportCommand(BungeeStaffPlugin plugin) {
        super(plugin, "report");
        setPlayerOnly(true);
        setRange(2, -1);
    }

    @Override
    public void onCommand(CommandSender sender, String[] args) {

        ProxiedPlayer player = (ProxiedPlayer) sender;

        StringBuilder reason = new StringBuilder();
        Arrays.stream(args).skip(1).forEach(str -> reason.append(" ").append(str));

        ProxiedPlayer target = ProxyServer.getInstance().getPlayer(args[0]);
        CachedUser user;

        if (target == null) {
            // Try to fetch cached user from Rabbit
            user = plugin.getMessagingManager().getUser(args[0]);

            if (user == null) {
                plugin.sendLineMessage("Report-Module.Player-Not-Found", sender);
                return;
            }
        } else
            user = new CachedUser(target.getName(), target.getServer().getInfo().getName());

        if (player.equals(target)) {
            plugin.sendLineMessage("Report-Module.Player-Sender", sender);
            return;
        }

        if (!plugin.getCooldownManager().trigger(CooldownType.REPORT, player.getUniqueId())) {
            plugin.sendMessage(plugin.getLineMessage("Report-Module.Report-Cooldown-Message")
                    .replace("%amount%", String.valueOf(plugin.getCooldownManager().getRemaining(CooldownType.REPORT, player.getUniqueId(), TimeUnit.SECONDS))), player);
            return;
        }

        plugin.sendLineMessage("Report-Module.Report-Sent", sender);

        String format = plugin.getListMessage("Report-Module.Report-Broadcast");

        TextComponent message = TextUtil.format(format
                .replace("%reporter_server%", player.getServer().getInfo().getName())
                .replace("%reporter%", player.getName())
                .replace("%reported%", user.getName())
                .replace("%reported_server%", user.getServer())
                .replace("%reason%", reason));

        message.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(plugin.getLineMessage("Report-Module.Hover-Message")
                .replace("%reported%", user.getName())
                .replace("%reported_server%", user.getServer()))
                .create()));

        message.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, plugin.getLineMessage("Report-Module.JSONClick-Command")
                .replace("%reported%", user.getName())));

        if (plugin.getMessages().getBoolean("Report-Module.Report-Clickable", false)) {
            sendJson(message);

            // Rabbit message
            plugin.getMessagingManager().sendMessage(MessageType.STAFF, format
                    .replace("%reporter_server%", player.getServer().getInfo().getName())
                    .replace("%reporter%", player.getName())
                    .replace("%reported%", user.getName())
                    .replace("%reported_server%", user.getServer())
                    .replace("%reason%", reason));
        } else
            plugin.getStaffManager().sendRawMessage(format
                    .replace("%reporter_server%", player.getServer().getInfo().getName())
                    .replace("%reporter%", player.getName())
                    .replace("%reported%", user.getName())
                    .replace("%reported_server%", user.getServer())
                    .replace("%reason%", reason), MessageType.STAFF);
    }

    private void sendJson(TextComponent component) {
        plugin.getStaffManager().getUsers(u -> u.isOnline() && u.isStaffMessages())
                .forEach(u -> u.asPlayer().sendMessage(component));
    }
}
