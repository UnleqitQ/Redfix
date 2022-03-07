package de.redfox.redfix.modules;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;
import de.redfox.redfix.RedfixPlugin;
import org.bukkit.Bukkit;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class Afk implements Listener, PacketListener {
	
	public static Set<UUID> registered = new HashSet<>();
	public static Map<UUID, Long> afkTimes = new HashMap<>();
	
	public static ListeningWhitelist receivingWhitelist;
	
	static {
		ListeningWhitelist.Builder builder = ListeningWhitelist.newBuilder();
		builder.gamePhase(GamePhase.LOGIN);
		Set<PacketType> types = new HashSet<>();
		for (PacketType type : PacketType.Play.Client.getInstance()) {
			if (type != PacketType.Play.Client.CLIENT_COMMAND) {
				types.add(type);
			}
		}
		builder.types(types);
		builder.options(Collections.singleton(ListenerOptions.ASYNC));
		receivingWhitelist = builder.build();
	}
	
	public Afk() {
	}
	
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
		/*if (event.getFrom().distance(event.getPlayer().getLocation()) > 0.01)
			afkTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());*/
		if (isAfk(event.getPlayer().getUniqueId()))
			event.setCancelled(true);
	}
	
	/*@EventHandler
	public void onInteract(PlayerInteractEvent event) {
		afkTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
	}
	
	@EventHandler
	public void onChat(AsyncPlayerChatEvent event) {
		afkTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
	}*/
	
	@Override
	public void onPacketSending(PacketEvent event) {}
	
	@Override
	public void onPacketReceiving(PacketEvent event) {
		afkTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
	}
	
	@Override
	public ListeningWhitelist getSendingWhitelist() {
		return ListeningWhitelist.EMPTY_WHITELIST;
	}
	
	@Override
	public ListeningWhitelist getReceivingWhitelist() {
		return receivingWhitelist;
	}
	
	@Override
	public Plugin getPlugin() {
		return RedfixPlugin.getInstance();
	}
	
}
