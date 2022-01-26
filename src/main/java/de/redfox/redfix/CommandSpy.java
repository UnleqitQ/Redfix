package de.redfox.redfix;

import org.bukkit.Bukkit;
import org.bukkit.event.Listener;

public class CommandSpy implements Listener {
	
	public CommandSpy() {
		Bukkit.getPluginManager().registerEvents(this, RedfixPlugin.getInstance());
	}
	
}
