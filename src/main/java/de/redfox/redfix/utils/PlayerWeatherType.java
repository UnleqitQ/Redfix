package de.redfox.redfix.utils;

import org.bukkit.WeatherType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public enum PlayerWeatherType {
	
	RAIN(WeatherType.DOWNFALL, "rain", "downfall"), CLEAR(WeatherType.CLEAR, "clear", "sun");
	
	private static final Map<String, PlayerWeatherType> byNameMap = new HashMap<>();
	
	private final String[] names;
	private final WeatherType base;
	
	PlayerWeatherType(WeatherType base, String... names) {
		this.names = names;
		this.base = base;
	}
	
	static {
		for (PlayerWeatherType v : PlayerWeatherType.values()) {
			for (String name : v.names) {
				byNameMap.put(name, v);
			}
		}
	}
	
	public String[] getNames() {
		return names;
	}
	
	public WeatherType getBase() {
		return base;
	}
	
	public static Set<String> getAllNames() {
		return byNameMap.keySet();
	}
	
	public static PlayerWeatherType getByName(String name) {
		return byNameMap.get(name.toLowerCase());
	}
	
}
