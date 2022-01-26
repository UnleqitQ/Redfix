package de.redfox.redfix;

import org.bukkit.plugin.java.JavaPlugin;

public class RedfixPlugin extends JavaPlugin {

	private static RedfixPlugin instance;
	
	public RedfixPlugin() {
		instance = this;
	}
	
	public static RedfixPlugin getInstance() {
		return instance;
	}
	
}
