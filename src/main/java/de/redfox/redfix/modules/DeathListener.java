package de.redfox.redfix.modules;

import de.redfox.redfix.RedfixPlugin;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

public class DeathListener implements Listener {
	
	@EventHandler
	public void onDeath(PlayerDeathEvent event) {
		RedfixPlugin.playerDeathLocations.put(event.getEntity().getUniqueId(), event.getEntity().getLocation());
	}
	
}

