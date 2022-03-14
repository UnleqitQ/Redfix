package de.redfox.redfix.modules.inv;

import de.redfox.redfix.RedfixPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class ChestListener implements Listener {
	
	ConcurrentMap<UUID, Long> lastClick;
	
	public ChestListener() {
		lastClick = new ConcurrentHashMap<>();
	}
	
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onNormalInventoryClose(InventoryCloseEvent paramInventoryCloseEvent) {
		Player player = (Player) paramInventoryCloseEvent.getPlayer();
		RedfixPlugin.getInstance().getRegChestManager().removeWatcher(player);
	}
	
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onInventoryClickSlave(InventoryClickEvent paramInventoryClickEvent) {
		Player player = (Player) paramInventoryClickEvent.getWhoClicked();
		if (!RedfixPlugin.getInstance().getRegChestManager().isThisSlave(player))
			return;
		Inventory inventory = paramInventoryClickEvent.getClickedInventory();
		if (inventory == null)
			return;
		RedfixPlugin.getInstance().getRegChestManager().UpdateMaster(player);
	}
	
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onInventoryMoveSlave(InventoryDragEvent paramInventoryDragEvent) {
		Player player = (Player) paramInventoryDragEvent.getWhoClicked();
		if (!RedfixPlugin.getInstance().getRegChestManager().isThisSlave(player))
			return;
		RedfixPlugin.getInstance().getRegChestManager().UpdateMaster(player);
	}
	
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onInventoryMoveSlave(PlayerGameModeChangeEvent paramPlayerGameModeChangeEvent) {
		final Player player = paramPlayerGameModeChangeEvent.getPlayer();
		if (!RedfixPlugin.getInstance().getRegChestManager().isThisSlave(player))
			return;
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask((Plugin) RedfixPlugin.getInstance(), new Runnable() {
			
			public void run() {
				RedfixPlugin.getInstance().getRegChestManager().UpdateMaster(player);
			}
		}, 10L);
	}
	
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onitemPickup(PlayerPickupItemEvent paramPlayerPickupItemEvent) {
		Player player = paramPlayerPickupItemEvent.getPlayer();
		if (!RedfixPlugin.getInstance().getRegChestManager().isThisSlave(player))
			return;
		RedfixPlugin.getInstance().getRegChestManager().UpdateMaster(player);
	}
	
	@EventHandler (priority = EventPriority.MONITOR, ignoreCancelled = true)
	public void onItemDrop(PlayerDropItemEvent paramPlayerDropItemEvent) {
		Player player = paramPlayerDropItemEvent.getPlayer();
		if (!RedfixPlugin.getInstance().getRegChestManager().isThisSlave(player))
			return;
		RedfixPlugin.getInstance().getRegChestManager().UpdateMaster(player);
	}
	
	@EventHandler (priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onInventoryClickMaster(InventoryClickEvent paramInventoryClickEvent) {
		Player player = (Player) paramInventoryClickEvent.getWhoClicked();
		if (!RedfixPlugin.getInstance().getRegChestManager().isWatching(player))
			return;
		RegWatcher regWatcher = RedfixPlugin.getInstance().getRegChestManager().getWatcher(player);
		if (paramInventoryClickEvent.getRawSlot() == 4) {
			paramInventoryClickEvent.setCancelled(true);
			return;
		}
		if (paramInventoryClickEvent.getRawSlot() == 5) {
			paramInventoryClickEvent.setCancelled(true);
			return;
		}
		if (RedfixPlugin.getInstance().getRegChestManager().CheckInvClick(player, paramInventoryClickEvent.getRawSlot(),
				paramInventoryClickEvent.getAction().name())) {
			paramInventoryClickEvent.setCancelled(true);
			if (paramInventoryClickEvent.getClick().isShiftClick()) {
				RegWatcher regWatcher1 = RedfixPlugin.getInstance().getRegChestManager().getWatcher(player);
				if (regWatcher1 != null && paramInventoryClickEvent.getClick() != ClickType.DOUBLE_CLICK) {
					Player player1 = regWatcher1.getTarget();
					int i = paramInventoryClickEvent.getRawSlot();
					if (i > 53 && player1.getInventory().firstEmpty() != -1) {
						if (i > 80) {
							i -= 81;
						}
						else if (i > 53) {
							i -= 45;
						}
						player1.getInventory().addItem(new ItemStack[]{paramInventoryClickEvent.getCurrentItem()});
						player.getInventory().setItem(i, null);
						if (player1.isOnline()) {
							player1.updateInventory();
						}
						else {
							player1.saveData();
						}
						RedfixPlugin.getInstance().getRegChestManager().UpdateMasterContents(player1);
					}
					else if (i > 17 && i <= 53 && player.getInventory().firstEmpty() != -1) {
						if (i > 44) {
							i -= 45;
						}
						else if (i > 17) {
							i -= 9;
						}
						player1.getInventory().setItem(i, null);
						player.getInventory().addItem(new ItemStack[]{paramInventoryClickEvent.getCurrentItem()});
						if (player1.isOnline()) {
							player1.updateInventory();
						}
						else {
							player1.saveData();
						}
						RedfixPlugin.getInstance().getRegChestManager().UpdateMasterContents(player1);
					}
				}
			}
			return;
		}
		Inventory inventory = paramInventoryClickEvent.getClickedInventory();
		if (inventory == null)
			return;
		String str = inventory.getType().toString();
		if (!str.equalsIgnoreCase("Chest"))
			return;
		RedfixPlugin.getInstance().getRegChestManager().UpdateSlaveContents(player, null);
		RedfixPlugin.getInstance().getRegChestManager().UpdateSlaveArmor(player, null);
		RedfixPlugin.getInstance().getRegChestManager().UpdateSlaveItemInHand(player, null);
		RedfixPlugin.getInstance().getRegChestManager().UpdateSlaveOffHand(player, null);
	}
	
	@EventHandler (priority = EventPriority.LOWEST, ignoreCancelled = true)
	public void onInventoryMoveMaster(InventoryDragEvent paramInventoryDragEvent) {
		Player player = (Player) paramInventoryDragEvent.getWhoClicked();
		if (!RedfixPlugin.getInstance().getRegChestManager().isWatching(player))
			return;
		RegWatcher regWatcher = RedfixPlugin.getInstance().getRegChestManager().getWatcher(player);
		for (Integer integer : paramInventoryDragEvent.getRawSlots()) {
			if (RedfixPlugin.getInstance().getRegChestManager().CheckInvClick(player, integer.intValue(), "")) {
				paramInventoryDragEvent.setCancelled(true);
				return;
			}
		}
		RedfixPlugin.getInstance().getRegChestManager().UpdateSlaveContents(player, null);
		RedfixPlugin.getInstance().getRegChestManager().UpdateSlaveArmor(player, null);
		RedfixPlugin.getInstance().getRegChestManager().UpdateSlaveItemInHand(player, null);
		RedfixPlugin.getInstance().getRegChestManager().UpdateSlaveOffHand(player, null);
	}
	
}
