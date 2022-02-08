package de.redfox.redfix.config;

public class ConfigManager {
	
	public static LanguageConfig language;
	public static ConfigObject command_spy;
	public static ConfigObject economyData;
	
	private static String pluginPath = "plugins/Redfix";
	
	public static void init() {
		language = new LanguageConfig(pluginPath, "lang.json");
		command_spy = new ConfigObject(pluginPath, "command_spy.json");
		economyData = new ConfigObject(pluginPath, "economy.json");
	}
	
	public static void finish() {
		language.save();
	}
	
}
