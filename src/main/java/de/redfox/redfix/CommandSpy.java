package de.redfox.redfix;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandSendEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CommandSpy implements Listener, CommandExecutor {
	
	public Set<UUID> players = new HashSet<>();
	
	public CommandSpy() {
		Bukkit.getPluginManager().registerEvents(this, RedfixPlugin.getInstance());
	}
	
	@Override
	public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
		if (commandSender instanceof Player) {
			Player player = (Player) commandSender;
			if (players.contains(player.getUniqueId())) {
				players.remove(player.getUniqueId());
				player.sendMessage("Disabled CommandSpy");
			}
			else {
				players.add(player.getUniqueId());
				player.sendMessage("Enabled CommandSpy");
			}
		}
		return true;
	}
	
	@EventHandler
	public void onCommandSent(@NotNull PlayerCommandSendEvent event) {
		String[] msgs = new String[event.getCommands().size()];
		int i = 0;
		for (String cmd : event.getCommands()) {
			msgs[i++] = ChatColor.GRAY + "[CommandSpy] " + ChatColor.GREEN + event.getPlayer().getName() + ChatColor.WHITE + ": " + cmd;
		}
		for (UUID uuid : players) {
			//if (event.getPlayer().getUniqueId().equals(uuid))
			//	continue;
			Player player = Bukkit.getPlayer(uuid);
			if (player != null) {
				for (String msg : msgs) {
					player.sendMessage(msg);
				}
			}
		}
	}
	
}
