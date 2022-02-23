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
		if (RedfixPlugin.muted.containsKey(player.getUniqueId())) {
			long until = RedfixPlugin.muted.get(player.getUniqueId());
			if (until < System.currentTimeMillis())
				RedfixPlugin.muted.remove(player.getUniqueId());
			else {
				player.sendMessage("§4You are Muted!");
				return;
			}
		}
		message = message.replaceAll("&&", "&§§").replaceAll("&([0-9a-fkomnrl])", "§$1").replaceAll("&§§", "&");
		String namePrefix = "";
		String nameSuffix = "";
		if (RedfixPlugin.getInstance().vaultChat != null) {
			namePrefix = RedfixPlugin.getInstance().vaultChat.getPlayerPrefix(player);
			nameSuffix = RedfixPlugin.getInstance().vaultChat.getPlayerSuffix(player);
		}
		String name = (namePrefix + player.getCustomName() + nameSuffix).replaceAll("&&", "&§§").replaceAll(
				"&([0-9a-fkomnrl])", "§$1").replaceAll("&§§", "&");
		if (event.getMessage().startsWith("!")) {
			String msg = name + "§7 >> §r" + message.substring(1);
			Bukkit.broadcastMessage(
					RedfixPlugin.getInstance().getConfig().getString("chat.shout.prefix", "§a[Shout] ") + msg);
			//event.getRecipients().forEach(p -> p.sendMessage(msg));
		}
		else if (event.getMessage().startsWith("?")) {
			String msg = name + "§7 >> §r" + message.substring(1);
			Bukkit.broadcastMessage(
					RedfixPlugin.getInstance().getConfig().getString("chat.ask.prefix", "§9[Question] ") + msg);
			//event.getRecipients().forEach(p -> p.sendMessage(msg));
		}
		else {
			String msg = name + "§7 >> §r" + message;
			double dist = RedfixPlugin.getInstance().getConfig().getDouble("chat.distance", 50);
			Bukkit.getOnlinePlayers().stream().filter(
					p -> (p.getWorld().equals(player.getWorld()) && p.getLocation().distance(
							player.getLocation()) < dist)).forEach(p -> p.sendMessage(msg));
		}
	}
	
}
