package bungeestaff.bungee.listeners;

import bungeestaff.bungee.BungeeStaffPlugin;
import bungeestaff.bungee.rabbit.MessageType;
import bungeestaff.bungee.system.staff.StaffUser;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.event.EventHandler;

public class QuitListener extends EventListener {

    public QuitListener(BungeeStaffPlugin plugin) {
        super(plugin);
    }

    @EventHandler
    public void onQuit(PlayerDisconnectEvent event) {
        ProxiedPlayer player = event.getPlayer();

        StaffUser user = plugin.getStaffManager().getUser(player);

        if (user == null)
            return;

        plugin.getStaffManager().sendMessage(plugin.getMessage("Staff-Messages.Staff-Leave")
                        .replace("%server_from%", player.getServer().getInfo().getName())
                        .replace("%player%", player.getName())
                        .replace("%prefix%", plugin.getPrefix(player)),
                MessageType.STAFF_MESSAGE);
    }
}
