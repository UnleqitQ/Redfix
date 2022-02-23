package de.redfox.redfix.modules.jail;

import de.redfox.redfix.RedfixPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JailHandler implements Listener {
	
	// wir brauchen die concurrent wegen der modcount!!!!
	public static Map<String, Jail> jails = new ConcurrentHashMap<>();
	public static Map<UUID, JailedPlayer> jailedPlayers = new ConcurrentHashMap<>();
	
	public JailHandler() {
		Bukkit.getPluginManager().registerEvents(this, RedfixPlugin.getInstance());
		Bukkit.getScheduler().runTaskTimer(RedfixPlugin.getInstance(), this::update, 20, 20);
	}
	
	@EventHandler
	public void onMove(@NotNull PlayerMoveEvent event) {
		if (jailedPlayers.containsKey(event.getPlayer().getUniqueId())) {
			JailedPlayer jp = jailedPlayers.get(event.getPlayer().getUniqueId());
			if (!jp.active() || !jp.valid()) {
				jailedPlayers.remove(jp.player);
				RedfixPlugin.sendMessage(Bukkit.getPlayer(jp.player),
						jp.valid() ? "Your jail was removed" : "Your time in jail ended");
				return;
			}
			Location to = event.getTo();
			if (to == null)
				to = event.getFrom();
			event.setTo(jp.getJail().location.clone().setDirection(to.getDirection()));
		}
	}
	
	public void update() {
		for (JailedPlayer jp : jailedPlayers.values()) {
			if (!jp.active() || !jp.valid()) {
				jailedPlayers.remove(jp.player);
				RedfixPlugin.sendMessage(Bukkit.getPlayer(jp.player),
						jp.valid() ? "Your jail was removed" : "Your time in jail ended");
			}
		}
	}
	
}
