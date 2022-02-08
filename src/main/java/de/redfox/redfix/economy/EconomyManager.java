package de.redfox.redfix.economy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {
	
	private static Map<UUID, Double> money = new HashMap<>();
	
	public static void setMoney(UUID player, double value) {
		money.put(player, value);
	}
	
	public static double getMoney(UUID player) {
		if (!money.containsKey(player))
			money.put(player, 0.);
		return money.get(player);
	}
	
	public static double addMoney(UUID player, double value) {
		double val = value + getMoney(player);
		setMoney(player, val);
		return val;
	}
	
	public static Map<UUID, Double> getAll() {
		return Collections.unmodifiableMap(money);
	}
	
}
