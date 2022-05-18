package de.redfox.redfix.chat;

import de.redfox.redfix.RedfixPlugin;
import io.papermc.paper.event.player.AsyncChatEvent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.jetbrains.annotations.NotNull;

public class ChatListener implements Listener {
	
	public ChatListener() {
		Bukkit.getPluginManager().registerEvents(this, RedfixPlugin.getInstance());
	}
	
	/*@EventHandler (priority = EventPriority.LOW, ignoreCancelled = true)
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
		String nm = player.getCustomName();
		if (nm == null)
			nm = player.getDisplayName();
		String name = (namePrefix + nm + nameSuffix).replaceAll("&&", "&§§").replaceAll("&([0-9a-fkomnrl])",
				"§$1").replaceAll("&§§", "&");
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
			double dist = RedfixPlugin.getInstance().getConfig().getDouble("chat.distance", -1);
			if (dist < 0) {
				Bukkit.broadcastMessage(msg);
			}
			else {
				Bukkit.getOnlinePlayers().stream().filter(
						p -> (p.getWorld().equals(player.getWorld()) && p.getLocation().distance(
								player.getLocation()) < dist)).forEach(p -> p.sendMessage(msg));
			}
		}
	}*/
	
	/*@EventHandler (priority = EventPriority.LOW, ignoreCancelled = true)
	public void onChat(@NotNull AsyncChatEvent event) {
		event.setCancelled(true);
		Component msg = event.message();
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
		Component namePrefix = Component.text("");
		Component nameSuffix = Component.text("");
		if (RedfixPlugin.getInstance().vaultChat != null) {
			namePrefix = Component.text(RedfixPlugin.getInstance().vaultChat.getPlayerPrefix(player));
			nameSuffix = Component.text(RedfixPlugin.getInstance().vaultChat.getPlayerSuffix(player));
		}
		Component nm = player.customName();
		if (nm == null)
			nm = player.displayName();
		if (((TextComponent) event.message()).content().startsWith("!")) {
			msg = msg.replaceText(TextReplacementConfig.builder().match("!").replacement("").once().build());
			Component component = Component.text("");
			TextComponent shout = Component.text(
					RedfixPlugin.getInstance().getConfig().getString("chat.shout.prefix", "§a[Shout] "));
			shout = shout.hoverEvent(HoverEvent.showText(
					Component.text(RedfixPlugin.getInstance().getConfig().getString("chat.shout.name", "Shout"))));
			component = component.append(shout).append(namePrefix);
			component = component.append(
					nm.hoverEvent(HoverEvent.showText(Component.text(player.getUniqueId().toString()))).clickEvent(
							ClickEvent.suggestCommand("/msg " + player.getName() + " ")));
			component = component.append(nameSuffix).append(Component.text("§7 >> §r")).append(msg);
			Bukkit.broadcast(RedfixPlugin.applyColor(component));
			//event.getRecipients().forEach(p -> p.sendMessage(msg));
		}
		else if (((TextComponent) event.message()).content().startsWith("?")) {
			msg = msg.replaceText(TextReplacementConfig.builder().match("\\?").replacement("").once().build());
			Component component = Component.text("");
			TextComponent shout = Component.text(
					RedfixPlugin.getInstance().getConfig().getString("chat.ask.prefix", "§9[Question] "));
			shout = shout.hoverEvent(HoverEvent.showText(
					Component.text(RedfixPlugin.getInstance().getConfig().getString("chat.ask.name", "Question"))));
			component = component.append(shout).append(namePrefix);
			component = component.append(
					nm.hoverEvent(HoverEvent.showText(Component.text(player.getUniqueId().toString()))).clickEvent(
							ClickEvent.suggestCommand("/msg " + player.getName() + " ")));
			component = component.append(nameSuffix).append(Component.text("§7 >> §r")).append(msg);
			Bukkit.broadcast(RedfixPlugin.applyColor(component));
		}
		else {
			Component component = Component.text("");
			component = component.append(namePrefix);
			component = component.append(
					nm.hoverEvent(HoverEvent.showText(Component.text(player.getUniqueId().toString()))).clickEvent(
							ClickEvent.suggestCommand("/msg " + player.getName() + " ")));
			component = component.append(nameSuffix).append(Component.text("§7 >> §r")).append(msg);
			double dist = RedfixPlugin.getInstance().getConfig().getDouble("chat.distance", -1);
			Component cmp = RedfixPlugin.applyColor(component);
			if (dist < 0) {
				Bukkit.broadcast(cmp);
			}
			else {
				Bukkit.getOnlinePlayers().stream().filter(
						p -> (p.getWorld().equals(player.getWorld()) && p.getLocation().distance(
								player.getLocation()) < dist)).forEach(p -> p.sendMessage(cmp));
			}
		}
	}*/
	
	@EventHandler (priority = EventPriority.LOW, ignoreCancelled = true)
	public void onChat(@NotNull AsyncChatEvent event) {
		Component msg = RedfixPlugin.applyColor(event.message());
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
		Component namePrefix = Component.text("");
		Component nameSuffix = Component.text("");
		if (RedfixPlugin.getInstance().vaultChat != null) {
			namePrefix = Component.text(ChatColor.translateAlternateColorCodes('&',
					RedfixPlugin.getInstance().vaultChat.getPlayerPrefix(player)));
			nameSuffix = Component.text(ChatColor.translateAlternateColorCodes('&',
					RedfixPlugin.getInstance().vaultChat.getPlayerSuffix(player)));
		}
		String nm0 = player.getCustomName();
		if (nm0 == null)
			nm0 = player.getDisplayName();
		Component nm = Component.text(ChatColor.translateAlternateColorCodes('&', nm0));
		if (RedfixPlugin.getInstance().getConfig().getBoolean("chat.shout.enabled",
				false) && ((TextComponent) event.message()).content().startsWith("!")) {
			msg = msg.replaceText(TextReplacementConfig.builder().match("!").replacement("").once().build());
			Component component = Component.text("");
			TextComponent shout = Component.text(
					RedfixPlugin.getInstance().getConfig().getString("chat.shout.prefix", "§a[Shout] "));
			shout = shout.hoverEvent(HoverEvent.showText(
					Component.text(RedfixPlugin.getInstance().getConfig().getString("chat.shout.name", "Shout"))));
			component = component.append(shout).append(namePrefix);
			component = component.append(
					nm.hoverEvent(HoverEvent.showText(Component.text(player.getUniqueId().toString()))).clickEvent(
							ClickEvent.suggestCommand("/msg " + player.getName() + " ")));
			component = component.append(nameSuffix).append(Component.text("§7 >> §r")).append(msg);
			Bukkit.broadcast(RedfixPlugin.applyColor(component));
			//event.getRecipients().forEach(p -> p.sendMessage(msg));
		}
		else if (RedfixPlugin.getInstance().getConfig().getBoolean("chat.ask.enabled",
				false) && ((TextComponent) event.message()).content().startsWith("?")) {
			msg = msg.replaceText(TextReplacementConfig.builder().match("\\?").replacement("").once().build());
			Component component = Component.text("");
			TextComponent shout = Component.text(
					RedfixPlugin.getInstance().getConfig().getString("chat.ask.prefix", "§9[Question] "));
			shout = shout.hoverEvent(HoverEvent.showText(
					Component.text(RedfixPlugin.getInstance().getConfig().getString("chat.ask.name", "Question"))));
			component = component.append(shout).append(namePrefix);
			component = component.append(
					nm.hoverEvent(HoverEvent.showText(Component.text(player.getUniqueId().toString()))).clickEvent(
							ClickEvent.suggestCommand("/msg " + player.getName() + " ")));
			component = component.append(nameSuffix).append(Component.text("§7 >> §r")).append(msg);
			Bukkit.broadcast(RedfixPlugin.applyColor(component));
		}
		else {
			Component component = Component.text("");
			component = component.append(namePrefix);
			component = component.append(
					nm.hoverEvent(HoverEvent.showText(Component.text(player.getUniqueId().toString()))).clickEvent(
							ClickEvent.suggestCommand("/msg " + player.getName() + " ")));
			component = component.append(nameSuffix).append(Component.text("§7 >> §r")).append(msg);
			double dist = RedfixPlugin.getInstance().getConfig().getDouble("chat.distance", -1);
			Component cmp = RedfixPlugin.applyColor(component);
			if (dist < 0) {
				Bukkit.broadcast(cmp);
			}
			else {
				Bukkit.getOnlinePlayers().stream().filter(
						p -> (p.getWorld().equals(player.getWorld()) && p.getLocation().distance(
								player.getLocation()) < dist)).forEach(p -> p.sendMessage(cmp));
			}
		}
		event.setCancelled(true);
	}
	
}
