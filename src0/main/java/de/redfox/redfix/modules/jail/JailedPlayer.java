package de.redfox.redfix.modules.jail;

import org.bukkit.Bukkit;
import org.bukkit.Statistic;

import java.util.UUID;

public class JailedPlayer {
	
	public UUID player;
	public String jail;
	public int jailed;
	public int duration;
	
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
	
}
