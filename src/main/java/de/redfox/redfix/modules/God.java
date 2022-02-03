package de.redfox.redfix.modules;

import de.redfox.redfix.RedfixPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class God implements Listener {
	
	public static Map<UUID, Boolean[]> players = new HashMap<>();
	
	public God() {
		Bukkit.getPluginManager().registerEvents(this, RedfixPlugin.getInstance());
	}
	
	@EventHandler
	public void onDamage(@NotNull EntityDamageEvent event) {
		if (event.getEntityType() == EntityType.PLAYER)
			if (players.containsKey(event.getEntity().getUniqueId()))
				if (players.get(event.getEntity().getUniqueId())[0])
					event.setDamage(0);
				else
					event.setCancelled(true);
	}
	
	@EventHandler
	public void onDamage(@NotNull EntityDamageByEntityEvent event) {
		if (event.getEntityType() == EntityType.PLAYER)
			if (players.containsKey(event.getEntity().getUniqueId()))
				if (players.get(event.getEntity().getUniqueId())[0])
					event.setDamage(0);
				else
					event.setCancelled(true);
		//event.setCancelled(true);
	}
	
	@EventHandler
	public void onDamage(@NotNull EntityDamageByBlockEvent event) {
		if (event.getEntityType() == EntityType.PLAYER)
			if (players.containsKey(event.getEntity().getUniqueId()))
				if (players.get(event.getEntity().getUniqueId())[0])
					event.setDamage(0);
				else
					event.setCancelled(true);
		//event.setCancelled(true);
	}
	
	@EventHandler
	public void onLooseAir(@NotNull EntityAirChangeEvent event) {
		if (event.getEntityType() == EntityType.PLAYER)
			if (players.containsKey(
					event.getEntity().getUniqueId()) && event.getAmount() < ((Player) event.getEntity()).getRemainingAir())
				event.setCancelled(true);
	}
	
	@EventHandler
	public void onExhaustion(@NotNull EntityExhaustionEvent event) {
		if (event.getEntityType() == EntityType.PLAYER)
			if (players.containsKey(event.getEntity().getUniqueId())) {
				event.setCancelled(true);
			}
	}
	
	@EventHandler
	public void onTarget(@NotNull EntityTargetLivingEntityEvent event) {
		if (event.getTarget().getType() == EntityType.PLAYER)
			if (event.getTarget() != null && players.containsKey(event.getTarget().getUniqueId()) && players.get(
					event.getTarget().getUniqueId())[1])
				event.setCancelled(true);
	}
	
	@EventHandler
	public void onTarget(@NotNull EntityTargetEvent event) {
		if (event.getTarget().getType() == EntityType.PLAYER)
			if (event.getTarget() != null && players.containsKey(event.getTarget().getUniqueId()) && players.get(
					event.getTarget().getUniqueId())[1])
				event.setCancelled(true);
	}
	
}
