package de.redfox.redfix.modules;

import net.kyori.adventure.text.Component;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinQuitListener implements Listener {
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		event.joinMessage(Component.text("§e" + event.getPlayer() + " joined the game"));
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		event.quitMessage(Component.text("§e" + event.getPlayer() + " left the game"));
	}
	
}
