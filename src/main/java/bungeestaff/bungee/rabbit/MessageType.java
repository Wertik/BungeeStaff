package bungeestaff.bungee.rabbit;

import bungeestaff.bungee.BungeeStaffPlugin;
import bungeestaff.bungee.rabbit.cache.CachedUser;
import bungeestaff.bungee.system.staff.StaffUser;
import bungeestaff.bungee.util.ParseUtil;
import com.google.common.base.Strings;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

public enum MessageType {

    STAFF((plugin, message, serverId) -> {
        plugin.getStaffManager().sendRawMessage(message, null);
    }),

    PUBLIC((plugin, message, serverId) -> {
        plugin.getBroadcastManager().broadcastRaw(message, false);
    }),

    UPDATE_USERS((plugin, message, serverId) -> {
        if (Strings.isNullOrEmpty(message))
            return;

        Set<CachedUser> users = ParseUtil.deserializeSet(message.trim(), CachedUser::deserialize);
        plugin.getMessagingService().getUserCache().updateUsers(serverId, users);
    }),

    UPDATE_STAFF(((plugin, message, serverId) -> {
        Set<StaffUser> users = ParseUtil.deserializeSet(message, str -> StaffUser.deserialize(plugin, str));
        plugin.getStaffManager().importUsers(users);
    })),

    STAFF_JOIN((plugin, message, serverId) -> {
        StaffUser user = plugin.getStaffManager().getUser(message.trim());
        if (user == null)
            return;
        user.setOnline(true);
    }),

    STAFF_LEAVE((plugin, message, serverId) -> {
        StaffUser user = plugin.getStaffManager().getUser(message.trim());
        if (user == null)
            return;
        user.setOnline(false);
    }),

    STAFF_ADD((plugin, message, serverId) -> {
        StaffUser user = StaffUser.deserialize(plugin, message);
        if (user == null)
            return;
        plugin.getStaffManager().addUser(user, false);
    }),

    STAFF_REMOVE((plugin, message, serverId) -> {
        StaffUser user = plugin.getStaffManager().getUser(message.trim());
        if (user == null)
            return;
        plugin.getStaffManager().removeUser(user, false);
    });

    private final MessageDispatcher dispatcher;

    MessageType(MessageDispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }

    public void dispatch(BungeeStaffPlugin plugin, String message, String serverId) {
        dispatcher.dispatch(plugin, message, serverId);
    }

    @Nullable
    public static MessageType fromString(String str) {
        try {
            return valueOf(str.trim());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public interface MessageDispatcher {
        void dispatch(BungeeStaffPlugin plugin, String message, String serverId);
    }
}
