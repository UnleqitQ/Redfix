package de.redfox.redfix.modules;

import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

import java.util.*;

public class Afk implements Listener {
	
	public static Set<UUID> registered = new HashSet<>();
	public static Map<UUID, Long> afkTimes = new HashMap<>();
	
	public static void init() {
		Bukkit.getOnlinePlayers().forEach(p -> afkTimes.put(p.getUniqueId(), System.currentTimeMillis()));
	}
	
	public static boolean isAfk(UUID player) {
		return System.currentTimeMillis() - afkTimes.get(player) > 5 * 60 * 1000;
	}
	
	public static void check() {
		for (Map.Entry<UUID, Long> entry : afkTimes.entrySet()) {
			if (isAfk(entry.getKey()) && !registered.contains(entry.getKey())) {
				Bukkit.broadcastMessage("§7" + Bukkit.getPlayer(entry.getKey()).getName() + " is now AFK");
				registered.add(entry.getKey());
			}
			else if (!isAfk(entry.getKey()) && registered.contains(entry.getKey())) {
				Bukkit.broadcastMessage("§7" + Bukkit.getPlayer(entry.getKey()).getName() + " is now back");
				registered.remove(entry.getKey());
			}
		}
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		afkTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		afkTimes.remove(event.getPlayer().getUniqueId());
	}
	
	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		if (event.getFrom().distance(event.getPlayer().getLocation()) > 0.01)
			afkTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
	}
	
	@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		afkTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
	}
	
	@EventHandler
	public void onChat(AsyncPlayerChatEvent event) {
		afkTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
	}
	
}
