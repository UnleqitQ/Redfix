package de.redfox.redfix.config;

public class ConfigManager {
	
	public static ConfigObject command_spy;
	
	private static String pluginPath = "plugins/Redfix";
	
	public static void init() {
		command_spy = new ConfigObject(pluginPath, "command_spy.json");
	}
	
	public static void finish() {
	}
	
}
