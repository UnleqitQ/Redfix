package de.redfox.redfix.modules;

import de.redfox.redfix.RedfixPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.jetbrains.annotations.NotNull;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class God implements Listener {
	
	public static Set<UUID> players = new HashSet<>();
	
	public God() {
		Bukkit.getPluginManager().registerEvents(this, RedfixPlugin.getInstance());
	}
	
	@EventHandler
	public void onDamage(@NotNull EntityDamageEvent event) {
		if (event.getEntityType() == EntityType.PLAYER)
			if (players.contains(event.getEntity().getUniqueId()))
				event.setDamage(0);
		//event.setCancelled(true);
	}
	
	@EventHandler
	public void onDamage(@NotNull EntityDamageByEntityEvent event) {
		if (event.getEntityType() == EntityType.PLAYER)
			if (players.contains(event.getEntity().getUniqueId()))
				event.setDamage(0);
		//event.setCancelled(true);
	}
	
	@EventHandler
	public void onDamage(@NotNull EntityDamageByBlockEvent event) {
		if (event.getEntityType() == EntityType.PLAYER)
			if (players.contains(event.getEntity().getUniqueId()))
				event.setDamage(0);
		//event.setCancelled(true);
	}
	
	@EventHandler
	public void onLooseAir(@NotNull EntityAirChangeEvent event) {
		System.out.println(event.getEntityType());
		System.out.println(players);
		if (event.getEntityType() == EntityType.PLAYER)
			if (players.contains(event.getEntity().getUniqueId())) {
				System.out.println("cancelled");
				event.setCancelled(true);
				System.out.println(event.getAmount());
				//event.setAmount(20);
			}
	}
	
	@EventHandler
	public void onExhaustion(@NotNull EntityExhaustionEvent event) {
		if (event.getEntityType() == EntityType.PLAYER)
			if (players.contains(event.getEntity().getUniqueId())) {
				event.setCancelled(true);
			}
	}
	
}
