package de.redfox.redfix.economy;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import de.redfox.redfix.RedfixPlugin;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class EconomyManager {
	
	private static Map<UUID, Double> money = new HashMap<>();
	
	public static void setMoney(UUID player, double value) {
		getMoney(player);
		if (RedfixPlugin.sql != null && RedfixPlugin.getInstance().getConfig().getBoolean("mysql.modules.economy",
				false)) {
			try {
				RedfixPlugin.sql.update("economy", Map.of("money", "" + value), Map.of("UUID", player.toString()));
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		money.put(player, value);
	}
	
	public static double getMoney(UUID player) {
		if (RedfixPlugin.sql != null && RedfixPlugin.getInstance().getConfig().getBoolean("mysql.modules.economy",
				false)) {
			try {
				double v = RedfixPlugin.getInstance().getConfig().getDouble("economy.startMoney", 100);
				ResultSet result = RedfixPlugin.sql.select("economy", Map.of("UUID", player.toString()));
				if (!result.next()) {
					RedfixPlugin.sql.insertInto("economy", Map.of("UUID", player.toString(), "money", "" + v));
					return v;
				}
				else {
					v = result.getDouble("money");
					money.put(player, v);
					return v;
				}
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		if (!money.containsKey(player))
			money.put(player, RedfixPlugin.getInstance().getConfig().getDouble("economy.startMoney", 100));
		return money.get(player);
	}
	
	public static double addMoney(UUID player, double value) {
		double val = value + getMoney(player);
		if (RedfixPlugin.sql != null && RedfixPlugin.getInstance().getConfig().getBoolean("mysql.modules.economy",
				false)) {
			try {
				RedfixPlugin.sql.update("economy", Map.of("money", "" + val), Map.of("UUID", player.toString()));
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		money.put(player, val);
		return val;
	}
	
	public static Map<UUID, Double> getAll() {
		return Collections.unmodifiableMap(money);
	}
	
	public static void loadData(File file) {
		file.getParentFile().mkdirs();
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		JsonArray array;
		try {
			FileInputStream fis = new FileInputStream(file);
			array = JsonParser.parseString(new String(fis.readAllBytes())).getAsJsonArray();
			fis.close();
		} catch (IOException | IllegalStateException e) {
			e.printStackTrace();
			return;
		}
		try {
			array.forEach(e -> {
				JsonObject object = e.getAsJsonObject();
				UUID player = UUID.fromString(object.get("player").getAsString());
				double amount = object.get("money").getAsDouble();
				setMoney(player, amount);
			});
		} catch (Exception ignored) {
		}
	}
	
	public static void saveData(File file) {
		file.getParentFile().mkdirs();
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		JsonArray array = new JsonArray(money.size());
		money.forEach((player, amount) -> {
			JsonObject object = new JsonObject();
			object.add("player", new JsonPrimitive(player.toString()));
			object.add("money", new JsonPrimitive(amount));
			array.add(object);
		});
		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(array.toString().getBytes());
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
