package de.redfox.redfix.modules.jail;

import org.bukkit.Location;

public class Jail {
	
	public Location location;
	public String name;
	
	public Jail(String name, Location location) {
		this.location = location;
		this.name = name;
	}
	
}
