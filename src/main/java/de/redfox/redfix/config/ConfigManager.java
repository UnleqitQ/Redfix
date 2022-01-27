package de.redfox.redfix.config;

public class ConfigManager {

    public static LanguageConfig language;
    public static ConfigObject data;

    private static String pluginPath = "plugins/Redfix";

    public static void init() {
        language = new LanguageConfig(pluginPath, "lang.json");
        data = new ConfigObject(pluginPath, "data.json");
    }
}
