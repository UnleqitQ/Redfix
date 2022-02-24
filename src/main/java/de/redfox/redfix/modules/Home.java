package de.redfox.redfix.modules;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class Home {
	
	@NotNull
	public String name;
	@NotNull
	public Location pos;
	@NotNull
	public UUID player;
	
	public Home(@NotNull String name, @NotNull Location pos, @NotNull UUID player) {
		this.name = name;
		this.pos = pos;
		this.player = player;
	}
	
	public static Home load(JsonObject object) {
		int x = object.get("x").getAsInt();
		int y = object.get("y").getAsInt();
		int z = object.get("z").getAsInt();
		UUID world = UUID.fromString(object.get("world").getAsString());
		UUID player = UUID.fromString(object.get("player").getAsString());
		String name = object.get("name").getAsString();
		return new Home(name, new Location(Bukkit.getWorld(world), x, y, z), player);
	}
	
	public JsonObject save() {
		JsonObject object = new JsonObject();
		object.addProperty("x", pos.getBlockX());
		object.addProperty("y", pos.getBlockY());
		object.addProperty("z", pos.getBlockZ());
		object.addProperty("world", pos.getWorld().getUID().toString());
		object.addProperty("player", player.toString());
		return object;
	}
	
}
