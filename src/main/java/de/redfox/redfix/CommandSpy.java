package de.redfox.redfix;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CommandSpy implements Listener {
	
	public Set<UUID> players = new HashSet<>();
	
	public CommandSpy() {
		Bukkit.getPluginManager().registerEvents(this, RedfixPlugin.getInstance());
	}
	
	@EventHandler
	public void onCommand(@NotNull PlayerCommandSendEvent event) {
		String[] msgs = new String[event.getCommands().size()];
		int i = 0;
		for (String cmd : event.getCommands()) {
			msgs[i++] = ChatColor.GRAY + "[CommandSpy] " + ChatColor.GREEN + event.getPlayer().getName() + ChatColor.WHITE + ": " + cmd;
		}
		for (UUID uuid : players) {
			if (event.getPlayer().getUniqueId().equals(uuid))
				continue;
			Player player = Bukkit.getPlayer(uuid);
			if (player != null) {
				for (String msg : msgs) {
					player.sendMessage(msg);
				}
			}
		}
	}
	
}
