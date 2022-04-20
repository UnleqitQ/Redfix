package de.redfox.redfix.modules;

import de.redfox.redfix.RedfixPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class Freeze implements Listener {
	
	public static Set<UUID> players = new HashSet<>();
	
	public Freeze() {
		Bukkit.getPluginManager().registerEvents(this, RedfixPlugin.getInstance());
	}
	
	@EventHandler
	public void onMove(PlayerMoveEvent event) {
		if (players.contains(event.getPlayer().getUniqueId())) {
			Location to = event.getFrom();
			to.setDirection(event.getTo().getDirection());
			event.setTo(to);
		}
	}
	
	/*public static Freeze load(JsonObject object) {
		int x = object.get("x").getAsInt();
		int y = object.get("y").getAsInt();
		int z = object.get("z").getAsInt();
		UUID world = UUID.fromString(object.get("world").getAsString());
		UUID player = UUID.fromString(object.get("player").getAsString());
		String name = object.get("name").getAsString();
		return new Freeze(name, new Location(Bukkit.getWorld(world), x, y, z), player);
	}
	
	public JsonObject save() {
		JsonObject object = new JsonObject();
		object.addProperty("x", pos.getBlockX());
		object.addProperty("y", pos.getBlockY());
		object.addProperty("z", pos.getBlockZ());
		object.addProperty("world", pos.getWorld().getUID().toString());
		object.addProperty("player", player.toString());
		object.addProperty("name", name);
		return object;
	}*/
	
}
