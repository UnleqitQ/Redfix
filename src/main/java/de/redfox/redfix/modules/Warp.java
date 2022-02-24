package de.redfox.redfix.modules;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class Warp {
	
	@NotNull
	public String name;
	@NotNull
	public Location pos;
	
	public Warp(@NotNull String name, @NotNull Location pos) {
		this.name = name;
		this.pos = pos;
	}
	
	public static Warp load(JsonObject object) {
		int x = object.get("x").getAsInt();
		int y = object.get("y").getAsInt();
		int z = object.get("z").getAsInt();
		UUID world = UUID.fromString(object.get("world").getAsString());
		String name = object.get("name").getAsString();
		return new Warp(name, new Location(Bukkit.getWorld(world), x, y, z));
	}
	
	public JsonObject save() {
		JsonObject object = new JsonObject();
		object.addProperty("x", pos.getBlockX());
		object.addProperty("y", pos.getBlockY());
		object.addProperty("z", pos.getBlockZ());
		object.addProperty("world", pos.getWorld().getUID().toString());
		return object;
	}
	
}
