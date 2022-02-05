package de.redfox.redfix.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import de.redfox.redfix.RedfixPlugin;
import de.redfox.redfix.config.ConfigManager;
import de.redfox.redfix.config.LanguageConfig;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class CommandSpy implements Listener {
	
	public enum Messages {
		PREFIX, COMMAND_DISABLE, COMMAND_ENABLE;
		
		String val;
		
		public static String get(Messages... messages) {
			StringBuilder ret = new StringBuilder();
			for (Messages message : messages) {
				ret.append(message.val);
			}
			
			return ret.toString();
		}
	}
	
	
	public Set<UUID> players = new HashSet<>();
	
	public CommandSpy() {
		/*LanguageConfig language = ConfigManager.language;
		language.registerMessages(LanguageConfig.Locale.DE, Map.ofEntries(
				Map.entry("prefix", "§cCommandSpy » "),
				Map.entry("command_disable", "§7CommandSpy wurde §eaktiviert"),
				Map.entry("command_enable", "§7CommandSpy wurde §edeaktiviert")
		));

		Messages.PREFIX.val = language.getMessage("prefix");
		Messages.COMMAND_DISABLE.val = language.getMessage("command_disable");
		Messages.COMMAND_ENABLE.val = language.getMessage("command_enable");*/
		
		LanguageConfig language = ConfigManager.language;
		
		CommandSpy.Messages.PREFIX.val = language.getMessage("commandspy.prefix");
		CommandSpy.Messages.COMMAND_DISABLE.val = language.getMessage("commandspy.command_disable");
		CommandSpy.Messages.COMMAND_ENABLE.val = language.getMessage("commandspy.command_enable");
		
		Bukkit.getPluginManager().registerEvents(this, RedfixPlugin.getInstance());
	}
	
	@EventHandler
	public void onCommandSent(@NotNull PlayerCommandPreprocessEvent event) {
		String msg = Messages.get(Messages.PREFIX) + "§e" + event.getPlayer().getName() + "§7: §f" + event.getMessage();
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
		JsonArray array = new JsonArray();
		for (UUID uuid : players)
			array.add(uuid.toString());
		
		ConfigManager.command_spy.set("players", array);
	}
	
	public void load() {
		JsonElement jsonElement = ConfigManager.command_spy.get("players");
		if (jsonElement == null)
			return;
		
		for (JsonElement element : jsonElement.getAsJsonArray()) {
			UUID uuid = UUID.fromString(element.getAsString());
			if (Bukkit.getOnlinePlayers().stream().map(Player::getUniqueId).anyMatch(k -> k.equals(uuid))) {
				players.add(uuid);
			}
		}
	}
	
}
