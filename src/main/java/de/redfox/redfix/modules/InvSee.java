package de.redfox.redfix.modules;

import de.redfox.redfix.RedfixPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.scheduler.BukkitTask;

public class InvSee implements Listener {
	
	public Player opener;
	public Player owner;
	public Inventory inventory;
	public BukkitTask task;
	
	public InvSee(Player opener, Player owner) {
		this.opener = opener;
		this.owner = owner;
		this.inventory = Bukkit.createInventory(opener, 6 * 9);
		task = Bukkit.getScheduler().runTaskTimer(RedfixPlugin.getInstance(), this::update, 1, 4);
		Bukkit.getPluginManager().registerEvents(this, RedfixPlugin.getInstance());
		opener.openInventory(inventory);
	}
	
	@EventHandler
	public void onInventoryClose(InventoryCloseEvent event) {
		if (event.getPlayer() == opener)
			dispose();
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		if (event.getPlayer() == opener || event.getPlayer() == owner)
			dispose();
	}
	
	private void dispose() {
		task.cancel();
		HandlerList.unregisterAll(this);
		opener.closeInventory();
	}
	
	@EventHandler
	public void onInventoryChange(InventoryClickEvent event) {
		PlayerInventory inv = owner.getInventory();
		//opener.sendMessage("Clicked: " + event.getClickedInventory() + "\nInvolved: " + event.getInventory());
		if (event.getClickedInventory() == inventory) {
			if (event.getSlot() >= 2 * 9) {
				inv.setItem(event.getSlot() - 2 * 9, event.getCursor());
			}
			else if (event.getSlot() == 0 + 0 * 9) {
				inv.setHelmet(event.getCursor());
			}
			else if (event.getSlot() == 0 + 1 * 9) {
				inv.setChestplate(event.getCursor());
			}
			else if (event.getSlot() == 1 + 0 * 9) {
				inv.setLeggings(event.getCursor());
			}
			else if (event.getSlot() == 1 + 1 * 9) {
				inv.setBoots(event.getCursor());
			}
			else if (event.getSlot() == 3 + 0 * 9) {
				inv.setItemInOffHand(event.getCursor());
			}
			else {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler
	public void onInventoryChange(InventoryDragEvent event) {
	}
	
	public void update() {
		PlayerInventory inv = owner.getInventory();
		for (int i = 0; i < 4 * 9; i++) {
			inventory.setItem(i + 2 * 9, inv.getItem(i));
		}
		inventory.setItem(0 + 0 * 9, inv.getHelmet());
		inventory.setItem(0 + 1 * 9, inv.getChestplate());
		inventory.setItem(1 + 0 * 9, inv.getLeggings());
		inventory.setItem(1 + 1 * 9, inv.getBoots());
		inventory.setItem(3 + 0 * 9, inv.getItemInOffHand());
	}
	
}
