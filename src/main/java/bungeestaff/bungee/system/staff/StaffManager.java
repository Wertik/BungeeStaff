package bungeestaff.bungee.system.staff;

import bungeestaff.bungee.BungeeStaffPlugin;
import bungeestaff.bungee.rabbit.MessageType;
import bungeestaff.bungee.rabbit.cache.CachedUser;
import bungeestaff.bungee.system.rank.Rank;
import bungeestaff.bungee.system.storage.IStaffStorage;
import bungeestaff.bungee.util.TextUtil;
import lombok.Getter;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.scheduler.ScheduledTask;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class StaffManager {

    private final BungeeStaffPlugin plugin;

    private final Map<UUID, StaffUser> users = new HashMap<>();

    @Getter
    private final IStaffStorage storage;

    private ScheduledTask autoSave;

    public StaffManager(BungeeStaffPlugin plugin, IStaffStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
    }

    public void stopAutoSave() {
        if (autoSave == null)
            return;

        autoSave.cancel();
        this.autoSave = null;
    }

    public void startAutoSave() {
        if (autoSave != null)
            stopAutoSave();

        int interval = plugin.getConfig().getInt("auto-save.interval", 60);

        this.autoSave = ProxyServer.getInstance().getScheduler().schedule(plugin, this::save, interval, interval, TimeUnit.SECONDS);
        plugin.getLogger().info(String.format("Started auto save with an interval of %d seconds.", interval));
    }

    public void reloadAutoSave() {
        stopAutoSave();

        if (plugin.getConfig().getBoolean("auto-save.enabled", false))
            startAutoSave();
    }

    public void load() {
        if (!storage.initialize()) {
            plugin.getLogger().warning("Could not initialize staff user storage.");
            return;
        }

        storage.loadAll().thenAcceptAsync(set -> {
            for (StaffUser user : set) {
                users.put(user.getUniqueID(), user);
            }
            plugin.getLogger().info(String.format("Loaded %d user(s)...", users.size()));
        });
    }

    public void save() {
        storage.saveAll(this.users.values()).thenRunAsync(() -> plugin.getLogger().info(String.format("Saved %d users.", users.size())));
    }

    @Nullable
    public StaffUser getUser(ProxiedPlayer player) {
        return player == null ? null : getUser(player.getUniqueId());
    }

    @Nullable
    public StaffUser getUser(UUID uniqueID) {
        return this.users.get(uniqueID);
    }

    @Nullable
    public StaffUser getUser(String name) {
        return users.values().stream()
                .filter(u -> u.getName().equals(name))
                .findAny().orElse(null);
    }

    public void createStaffUser(StaffUser user, boolean sync) {
        users.put(user.getUniqueID(), user);
        storage.save(user);

        if (sync)
            plugin.getMessagingService().sendStaffAdd(user);
    }

    // Add user to staff
    public void createStaffUser(CachedUser cachedUser, Rank rank, boolean sync) {
        StaffUser user = new StaffUser(cachedUser.getUniqueID(), rank);

        user.setName(cachedUser.getName());
        user.setStaffMessages(plugin.getConfig().getBoolean("Defaults.Staff-Messages", false));

        createStaffUser(user, sync);
    }

    public void removeUser(StaffUser user, boolean sync) {
        users.remove(user.getUniqueID());
        storage.delete(user.getUniqueID());

        if (sync)
            plugin.getMessagingService().sendStaffRemove(user.getName());
    }

    public Set<StaffUser> getUsers() {
        return new HashSet<>(users.values());
    }

    public Set<StaffUser> getUsers(Predicate<StaffUser> condition) {
        return users.values().stream()
                .filter(condition)
                .collect(Collectors.toSet());
    }

    /**
     * Send message to online staff/players and sync over rabbit.
     */
    public void sendMessage(String message, @NotNull MessageType type) {

        // Send one to console
        TextUtil.sendMessage(plugin.getProxy().getConsole(), message);

        if (type == MessageType.STAFF_MESSAGE)
            // To staff online
            getUsers().forEach(u -> u.sendStaffMessage(message));
        else if (type == MessageType.PUBLIC_MESSAGE)
            // To all players online
            plugin.getProxy().getPlayers().forEach(p -> TextUtil.sendMessage(p, message));

        plugin.getMessagingService().sendMessage(type, message);
    }

    /**
     * Send message to online staff on local proxy.
     */
    public void sendStaffMessageRaw(String message) {
        // Send one to console
        TextUtil.sendMessage(plugin.getProxy().getConsole(), message);

        getUsers().forEach(u -> u.sendStaffMessage(message));
    }

    /**
     * Format and send a message to staff chat on all proxies.
     */
    public void sendStaffMessage(StaffUser author, String message) {
        String wholeMessage = plugin.getMessages().getString("StaffChat-Module.StaffChat-Message")
                .replace("%server%", TextUtil.getOr(author::getServer, "none"))
                .replace("%player%", author.getName())
                .replace("%message%", message)
                .replace("%prefix%", plugin.getPrefix(author));

        sendMessage(wholeMessage, MessageType.STAFF_MESSAGE);
    }
}
