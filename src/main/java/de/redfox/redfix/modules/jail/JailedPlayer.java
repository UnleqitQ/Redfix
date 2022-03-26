package de.redfox.redfix.modules.jail;

import com.google.gson.JsonObject;
import org.bukkit.Bukkit;
import org.bukkit.Statistic;

import java.util.UUID;

public class JailedPlayer {
	
	public UUID player;
	public String jail;
	public int jailed;
	public int duration;
	
	private JailedPlayer(UUID player, String jail, int duration, int jailed) {
		this.jail = jail;
		this.player = player;
		this.duration = duration;
		this.jailed = jailed;
	}
	
	public JailedPlayer(UUID player, String jail, int duration) {
		this.jail = jail;
		this.player = player;
		this.duration = duration;
		jailed = Bukkit.getPlayer(player).getStatistic(Statistic.PLAY_ONE_MINUTE);
	}
	
	public boolean active() {
		return duration == -1 || (Bukkit.getPlayer(player).getStatistic(
				Statistic.PLAY_ONE_MINUTE) - jailed) < duration * 20;
	}
	
	public boolean valid() {
		return JailHandler.jails.containsKey(jail);
	}
	
	public Jail getJail() {
		return JailHandler.jails.get(jail);
	}
	
	public static JailedPlayer load(JsonObject object) {
		return new JailedPlayer(UUID.fromString(object.get("player").getAsString()), object.get("jail").getAsString(),
				object.get("duration").getAsInt(), object.get("jailed").getAsInt());
	}
	
	public JsonObject save() {
		JsonObject object = new JsonObject();
		object.addProperty("player", player.toString());
		object.addProperty("jail", jail);
		object.addProperty("jailed", jailed);
		object.addProperty("duration", duration);
		return object;
	}
	
}
