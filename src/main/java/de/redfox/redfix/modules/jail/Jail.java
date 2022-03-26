package de.redfox.redfix.modules.jail;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Location;

import java.util.UUID;

public class Jail {
	
	public Location location;
	public String name;
	
	public Jail(String name, Location location) {
		this.location = location;
		this.name = name;
	}
	
	public static Jail load(JsonObject object) {
		return new Jail(object.get("name").getAsString(),
				new Location(Bukkit.getWorld(UUID.fromString(object.get("world").getAsString())),
						object.get("x").getAsDouble(), object.get("y").getAsDouble(), object.get("z").getAsDouble()));
	}
	
	public JsonObject save() {
		JsonObject object = new JsonObject();
		object.addProperty("name", name);
		object.addProperty("world", location.getWorld().getUID().toString());
		object.addProperty("x", location.getX());
		object.addProperty("y", location.getY());
		object.addProperty("z", location.getZ());
		return object;
	}
	
}
