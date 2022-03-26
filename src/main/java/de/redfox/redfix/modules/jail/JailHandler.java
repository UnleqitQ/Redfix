package de.redfox.redfix.modules.jail;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.redfox.redfix.RedfixPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class JailHandler implements Listener {
	
	// wir brauchen die concurrent wegen der modcount!!!!
	public static Map<String, Jail> jails = new ConcurrentHashMap<>();
	public static Map<UUID, JailedPlayer> jailedPlayers = new ConcurrentHashMap<>();
	
	public JailHandler() {
		Bukkit.getPluginManager().registerEvents(this, RedfixPlugin.getInstance());
		Bukkit.getScheduler().runTaskTimer(RedfixPlugin.getInstance(), this::update, 20, 20);
	}
	
	@EventHandler
	public void onMove(@NotNull PlayerMoveEvent event) {
		if (jailedPlayers.containsKey(event.getPlayer().getUniqueId())) {
			JailedPlayer jp = jailedPlayers.get(event.getPlayer().getUniqueId());
			if (!jp.active() || !jp.valid()) {
				jailedPlayers.remove(jp.player);
				RedfixPlugin.sendMessage(Bukkit.getPlayer(jp.player),
						jp.valid() ? "Your jail was removed" : "Your time in jail ended");
				return;
			}
			Location to = event.getTo();
			if (to == null)
				to = event.getFrom();
			event.setTo(jp.getJail().location.clone().setDirection(to.getDirection()));
		}
	}
	
	public void update() {
		for (JailedPlayer jp : jailedPlayers.values()) {
			if (!jp.active() || !jp.valid()) {
				jailedPlayers.remove(jp.player);
				RedfixPlugin.sendMessage(Bukkit.getPlayer(jp.player),
						jp.valid() ? "Your jail was removed" : "Your time in jail ended");
			}
		}
	}
	
	public static void saveJails(File file) {
		file.getParentFile().mkdirs();
		if (!file.exists())
			try {
				file.createNewFile();
				return;
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		JsonArray array = new JsonArray(jails.size());
		jails.values().forEach(w -> array.add(w.save()));
		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(RedfixPlugin.gson.toJson(array).getBytes());
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void loadJails(File file) {
		file.getParentFile().mkdirs();
		if (!file.exists())
			try {
				file.createNewFile();
				return;
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
			for (JsonElement element : array) {
				Jail jail = Jail.load(element.getAsJsonObject());
				jails.put(jail.name, jail);
			}
		} catch (Exception ignored) {
		}
	}
	
	public static void saveJailedPlayers(File file) {
		file.getParentFile().mkdirs();
		if (!file.exists())
			try {
				file.createNewFile();
				return;
			} catch (IOException e) {
				e.printStackTrace();
				return;
			}
		JsonArray array = new JsonArray(jails.size());
		jailedPlayers.values().forEach(w -> array.add(w.save()));
		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(RedfixPlugin.gson.toJson(array).getBytes());
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void loadJailedPlayers(File file) {
		file.getParentFile().mkdirs();
		if (!file.exists())
			try {
				file.createNewFile();
				return;
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
			for (JsonElement element : array) {
				JailedPlayer jailedPlayer = JailedPlayer.load(element.getAsJsonObject());
				jailedPlayers.put(jailedPlayer.player, jailedPlayer);
			}
		} catch (Exception ignored) {
		}
	}
	
}
