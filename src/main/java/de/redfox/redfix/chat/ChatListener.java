package de.redfox.redfix.chat;

import de.redfox.redfix.RedfixPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.jetbrains.annotations.NotNull;

public class ChatListener implements Listener {
	
	public ChatListener() {
		Bukkit.getPluginManager().registerEvents(this, RedfixPlugin.getInstance());
	}
	
	@EventHandler (priority = EventPriority.LOW)
	public void onChat(@NotNull AsyncPlayerChatEvent event) {
		event.setCancelled(true);
		String message = event.getMessage();
		Player player = event.getPlayer();
		message = message.replaceAll("&&", "&§§").replaceAll("&([0-9a-fkomnrl])", "§$1").replaceAll("&§§", "&");
		String msg = "[" + player.getCustomName() + "] " + message;
		if (event.getMessage().startsWith("!")) {
			Bukkit.broadcastMessage(
					RedfixPlugin.getInstance().getConfig().getString("chat.shout.prefix", "§a[Shout] ") + msg);
			//event.getRecipients().forEach(p -> p.sendMessage(msg));
		}
		else if (event.getMessage().startsWith("?")) {
			Bukkit.broadcastMessage(
					RedfixPlugin.getInstance().getConfig().getString("chat.ask.prefix", "§9[Question] ") + msg);
			//event.getRecipients().forEach(p -> p.sendMessage(msg));
		}
		else {
			double dist = RedfixPlugin.getInstance().getConfig().getDouble("chat.distance", 50);
			player.getNearbyEntities(dist, dist, dist).stream().filter(
					e -> e.getLocation().distance(player.getLocation()) <= dist).filter(
					e -> e instanceof Player).forEach(e -> ((Player) e).sendMessage(msg));
		}
	}
	
}
