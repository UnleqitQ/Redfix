package de.redfox.redfix.modules;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListenerOptions;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import com.comphenix.protocol.injector.GamePhase;
import de.redfox.redfix.RedfixPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetLivingEntityEvent;
import org.bukkit.event.player.*;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.util.*;

public class Afk implements Listener, PacketListener {
	
	public static Set<UUID> registered = new HashSet<>();
	public static Map<UUID, Long> afkTimes = new HashMap<>();
	
	public static ListeningWhitelist receivingWhitelist;
	
	static {
		ListeningWhitelist.Builder builder = ListeningWhitelist.newBuilder();
		builder.gamePhase(GamePhase.LOGIN);
		Set<PacketType> types = new HashSet<>();
		types.add(PacketType.Play.Client.WINDOW_CLICK);
		types.add(PacketType.Play.Client.ENCHANT_ITEM);
		types.add(PacketType.Play.Client.CLOSE_WINDOW);
		types.add(PacketType.Play.Client.B_EDIT);
		types.add(PacketType.Play.Client.USE_ENTITY);
		types.add(PacketType.Play.Client.POSITION);
		types.add(PacketType.Play.Client.BOAT_MOVE);
		types.add(PacketType.Play.Client.PICK_ITEM);
		types.add(PacketType.Play.Client.BLOCK_DIG);
		types.add(PacketType.Play.Client.ENTITY_ACTION);
		types.add(PacketType.Play.Client.STEER_VEHICLE);
		types.add(PacketType.Play.Client.ITEM_NAME);
		types.add(PacketType.Play.Client.TR_SEL);
		types.add(PacketType.Play.Client.BEACON);
		types.add(PacketType.Play.Client.SET_COMMAND_BLOCK);
		types.add(PacketType.Play.Client.SET_COMMAND_MINECART);
		types.add(PacketType.Play.Client.SET_CREATIVE_SLOT);
		types.add(PacketType.Play.Client.UPDATE_SIGN);
		types.add(PacketType.Play.Client.BLOCK_PLACE);
		types.add(PacketType.Play.Client.USE_ITEM);
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
		if (!afkTimes.containsKey(player))
			return false;
		return System.currentTimeMillis() - afkTimes.get(player) > 5 * 60 * 1000;
	}
	
	public static void check() {
		for (Map.Entry<UUID, Long> entry : afkTimes.entrySet()) {
			if (isAfk(entry.getKey()) && !registered.contains(entry.getKey())) {
				Player player = Bukkit.getPlayer(entry.getKey());
				Bukkit.getOnlinePlayers().stream().filter(p -> RedfixPlugin.canSee(p, player)).forEach(
						p -> p.sendMessage("§7" + player.getName() + " is now AFK"));
				registered.add(entry.getKey());
				Bukkit.getPlayer(entry.getKey()).setSleepingIgnored(true);
				
			}
			else if (!isAfk(entry.getKey()) && registered.contains(entry.getKey())) {
				Player player = Bukkit.getPlayer(entry.getKey());
				Bukkit.getOnlinePlayers().stream().filter(p -> RedfixPlugin.canSee(p, player)).forEach(
						p -> p.sendMessage("§7" + Bukkit.getPlayer(entry.getKey()).getName() + " is now back"));
				registered.remove(entry.getKey());
				Bukkit.getPlayer(entry.getKey()).setSleepingIgnored(false);
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
		if (isAfk(event.getPlayer().getUniqueId())) {
			Location to = event.getFrom();
			to.setDirection(event.getTo().getDirection());
			event.setTo(to);
		}
	}
	
	@EventHandler
	public void onDamage(EntityDamageEvent event) {
		if (event.getEntityType() == EntityType.PLAYER && isAfk(event.getEntity().getUniqueId())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onPickup(PlayerAttemptPickupItemEvent event) {
		if (isAfk(event.getPlayer().getUniqueId())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onPickup(PlayerPickupArrowEvent event) {
		if (isAfk(event.getPlayer().getUniqueId())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler
	public void onVelocity(PlayerVelocityEvent event) {
		if (isAfk(event.getPlayer().getUniqueId())) {
			event.setVelocity(new Vector());
		}
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
		if (event.getPacketType() == PacketType.Play.Client.POSITION) {
			if (isAfk(event.getPlayer().getUniqueId())) {
				double x = event.getPacket().getDoubles().read(0);
				double y = event.getPacket().getDoubles().read(1);
				double z = event.getPacket().getDoubles().read(2);
				if (Math.abs(event.getPlayer().getLocation().getX() - x) > 0.1 || Math.abs(
						event.getPlayer().getLocation().getY() - y) > 0.1 || Math.abs(
						event.getPlayer().getLocation().getZ() - z) > 0.1) {
					afkTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
					event.getPlayer().setSleepingIgnored(false);
				}
			}
		}
		else {
			/*if (isAfk(event.getPlayer().getUniqueId()))
				event.getPlayer().sendMessage("Packet Type: " + event.getPacketType() + "\n" + event.getPacket());*/
			afkTimes.put(event.getPlayer().getUniqueId(), System.currentTimeMillis());
			event.getPlayer().setSleepingIgnored(false);
		}
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
	
	@EventHandler
	public void onTarget(@NotNull EntityTargetLivingEntityEvent event) {
		if (event.getTarget() != null && event.getTarget().getType() == EntityType.PLAYER)
			if (isAfk(event.getTarget().getUniqueId()))
				event.setCancelled(true);
	}
	
	@EventHandler
	public void onTarget(@NotNull EntityTargetEvent event) {
		if (event.getTarget() != null && event.getTarget().getType() == EntityType.PLAYER)
			if (isAfk(event.getTarget().getUniqueId()))
				event.setCancelled(true);
	}
	
}
