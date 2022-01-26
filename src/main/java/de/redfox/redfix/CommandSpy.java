package de.redfox.redfix;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CommandSpy implements Listener, CommandExecutor {
	
	public Set<UUID> players = new HashSet<>();
	
	public CommandSpy() {
		Bukkit.getPluginManager().registerEvents(this, RedfixPlugin.getInstance());
	}
	
	@Override
	public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
		if (commandSender instanceof Player) {
			Player player = (Player) commandSender;
			if (players.contains(player.getUniqueId())) {
				players.remove(player.getUniqueId());
				player.sendMessage("Disabled CommandSpy");
			}
			else {
				players.add(player.getUniqueId());
				player.sendMessage("Enabled CommandSpy");
			}
		}
		return true;
	}
	
	@EventHandler
	public void onCommandSent(@NotNull PlayerCommandPreprocessEvent event) {
		String msg = ChatColor.GRAY + "[CommandSpy] " + ChatColor.GREEN + event.getPlayer().getName() + ChatColor.WHITE + ": " + event.getMessage();
		for (UUID uuid : players) {
			if (event.getPlayer().getUniqueId().equals(uuid))
				continue;
			Player player = Bukkit.getPlayer(uuid);
			if (player != null) {
				player.sendMessage(msg);
			}
		}
	}
	
	public void save() {
		try {
			File f = new File(RedfixPlugin.pluginPath, "commandspy.json");
			if (!f.exists())
				f.createNewFile();
			JsonArray array = new JsonArray();
			for (UUID uuid : players) {
				array.add(uuid.toString());
			}
			JsonWriter writer = new JsonWriter(new FileWriter(f));
			new Gson().toJson(array, writer);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public void load() {
		try {
			File f = new File(RedfixPlugin.pluginPath, "commandspy.json");
			if (!f.exists())
				return;
			JsonReader reader = new JsonReader(new FileReader(f));
			JsonArray array = JsonParser.parseReader(reader).getAsJsonArray();
			for (JsonElement element : array) {
				players.add(UUID.fromString(element.getAsString()));
			}
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
