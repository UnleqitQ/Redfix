package de.redfox.redfix.modules;

import de.redfox.redfix.RedfixPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

public class RecipeUnlocker implements Listener {
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		if (RedfixPlugin.getInstance().getConfig().getBoolean("auto-unlock-recipes", false)) {
			Player p = event.getPlayer();
			Bukkit.recipeIterator().forEachRemaining(r -> {
				if (r instanceof Keyed k)
					if (!p.hasDiscoveredRecipe(k.getKey()))
						p.discoverRecipe(k.getKey());
			});
		}
	}
	
}
