package de.redfox.redfix.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public enum WeatherType {
	
	THUNDER("thunder", "storm"), RAIN("rain", "downfall"), CLEAR("clear", "sun");
	
	private static final Map<String, WeatherType> byNameMap = new HashMap<>();
	
	private final String[] names;
	
	WeatherType(String... names) {
		this.names = names;
	}
	
	static {
		for (WeatherType v : WeatherType.values()) {
			for (String name : v.names) {
				byNameMap.put(name, v);
			}
		}
	}
	
	public String[] getNames() {
		return names;
	}
	
	public static Set<String> getAllNames() {
		return byNameMap.keySet();
	}
	
	public static WeatherType getByName(String name) {
		return byNameMap.get(name.toLowerCase());
	}
	
}
