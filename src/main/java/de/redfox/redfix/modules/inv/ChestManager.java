package de.redfox.redfix.modules.inv;

import de.redfox.redfix.RedfixPlugin;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChestManager {
	
	private HashMap<String, RegWatcher> WatcherList = new HashMap<>();
	
	public ChestManager() {
	}
	
	public boolean isWatching(Player paramPlayer) {
		return this.WatcherList.containsKey(paramPlayer.getName());
	}
	
	public RegWatcher getWatcher(Player paramPlayer) {
		return this.WatcherList.get(paramPlayer.getName());
	}
	
	public void ReopenNormalChest(final Player player, final Player Target) {
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask((Plugin) RedfixPlugin.getInstance(), new Runnable() {
			
			public void run() {
				ChestManager.this.openInventory(player, Target);
			}
		}, 1L);
	}
	
	public void removeWatcher(Player paramPlayer) {
		RegWatcher regWatcher = this.WatcherList.remove(paramPlayer.getName());
		if (regWatcher != null)
			Bukkit.getScheduler().cancelTask(regWatcher.getSchedId());
	}
	
	public boolean isThisSlave(Player paramPlayer) {
		if (this.WatcherList.isEmpty())
			return false;
		for (Map.Entry<String, RegWatcher> entry : this.WatcherList.entrySet()) {
			if (((RegWatcher) entry.getValue()).getTarget().getName().equalsIgnoreCase(paramPlayer.getName()))
				return true;
		}
		return false;
	}
	
	public RegWatcher getWatcherInfoBySlave(Player paramPlayer) {
		if (this.WatcherList.isEmpty())
			return null;
		for (Map.Entry<String, RegWatcher> entry : this.WatcherList.entrySet()) {
			if (((RegWatcher) entry.getValue()).getTarget().getName().equalsIgnoreCase(paramPlayer.getName()))
				return (RegWatcher) entry.getValue();
		}
		return null;
	}
	
	public boolean CheckInvClick(Player paramPlayer, int paramInt, String paramString) {
		if (this.WatcherList.isEmpty())
			return false;
		RegWatcher regWatcher = getWatcher(paramPlayer);
		if (regWatcher == null)
			return false;
		if (paramInt >= 3 && paramInt <= 5)
			return true;
		if (paramInt >= 12 && paramInt <= 14)
			return true;
		if (paramInt == 8 || paramInt == 17)
			return true;
		if (paramInt == 6 || paramInt == 7)
			return true;
		if (paramInt == 15 || paramInt == 16)
			return true;
		if (paramString.equalsIgnoreCase("COLLECT_TO_CURSOR") || paramString.equalsIgnoreCase(
				"MOVE_TO_OTHER_INVENTORY"))
			return true;
		return false;
	}
	
	public boolean openInventory(Player paramPlayer1, Player paramPlayer2) {
		if (paramPlayer2 == null)
			return false;
		Inventory inventory = CreateGui(paramPlayer1, paramPlayer2);
		paramPlayer1.openInventory(inventory);
		RegWatcher regWatcher = new RegWatcher(paramPlayer1, paramPlayer2);
		this.WatcherList.put(paramPlayer1.getName(), regWatcher);
		regWatcher.setContents((ItemStack[]) paramPlayer2.getInventory().getContents().clone());
		regWatcher.setSchedId(
				Bukkit.getServer().getScheduler().scheduleSyncRepeatingTask(RedfixPlugin.getInstance(), () -> {
					ItemStack[] arrayOfItemStack = paramPlayer2.getInventory().getContents();
					for (byte b = 0; b < arrayOfItemStack.length; b++) {
						if ((arrayOfItemStack[b] == null && regWatcher.getContents()[b] != null) || (arrayOfItemStack[b] != null && regWatcher.getContents()[b] == null) || (arrayOfItemStack[b] != null && regWatcher.getContents()[b] != null && (!arrayOfItemStack[b].equals(
								regWatcher.getContents()[b]) || arrayOfItemStack[b].getAmount() != regWatcher.getContents()[b].getAmount()))) {
							UpdateMasterContents(paramPlayer2, false);
							regWatcher.setContents((ItemStack[]) arrayOfItemStack.clone());
							break;
						}
					}
				}, 0L, 1L));
		return true;
	}
	
	public void UpdateSlaveContents(final Player player, final ItemStack[] MasterContents) {
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(RedfixPlugin.getInstance(), new Runnable() {
			
			public void run() {
				RegWatcher regWatcher = ChestManager.this.getWatcher(player);
				if (regWatcher == null)
					return;
				Player player = regWatcher.getTarget();
				PlayerInventory playerInventory = player.getInventory();
				ItemStack[] arrayOfItemStack1 = playerInventory.getContents();
				ItemStack[] arrayOfItemStack2 = MasterContents;
				if (arrayOfItemStack2 == null)
					arrayOfItemStack2 = player.getOpenInventory().getTopInventory().getContents();
				if (arrayOfItemStack2.length < 45)
					return;
				byte b1 = 0;
				byte b2;
				for (b2 = 45; b2 < 54; b2++) {
					if (arrayOfItemStack2.length >= b2)
						arrayOfItemStack1[b1] = arrayOfItemStack2[b2];
					b1++;
				}
				for (b2 = 18; b2 < 45; b2++) {
					if (arrayOfItemStack2.length >= b2)
						arrayOfItemStack1[b1] = arrayOfItemStack2[b2];
					b1++;
				}
				player.getInventory().setContents(arrayOfItemStack1);
				if (player.isOnline()) {
					player.updateInventory();
				}
				else {
					player.saveData();
				}
			}
		}, 0L);
	}
	
	public void UpdateMaster(Player paramPlayer) {
		UpdateMasterContents(paramPlayer);
		UpdateMasterCrafting(paramPlayer);
		UpdateMasterArmor(paramPlayer);
		UpdateMasterItemInHand(paramPlayer);
		UpdateMasterOffHand(paramPlayer);
	}
	
	public void UpdateMasterContents(Player paramPlayer) {
		UpdateMasterContents(paramPlayer, true);
	}
	
	public void UpdateMasterContents(final Player player, boolean paramBoolean) {
		if (paramBoolean) {
			Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(RedfixPlugin.getInstance(), new Runnable() {
				
				public void run() {
					ChestManager.this.updateMaster(player);
				}
			}, 0L);
		}
		else {
			updateMaster(player);
		}
	}
	
	private void updateMaster(Player paramPlayer) {
		RegWatcher regWatcher = getWatcherInfoBySlave(paramPlayer);
		if (regWatcher == null)
			return;
		Player player = regWatcher.getMaster();
		Inventory inventory = player.getOpenInventory().getTopInventory();
		if (inventory.getSize() != 54)
			return;
		ItemStack[] arrayOfItemStack1 = inventory.getContents();
		ItemStack[] arrayOfItemStack2 = paramPlayer.getInventory().getContents();
		byte b1 = 0;
		byte b2;
		for (b2 = 45; b2 < arrayOfItemStack1.length && b2 < 54; b2++) {
			if (arrayOfItemStack2.length >= b1)
				arrayOfItemStack1[b2] = arrayOfItemStack2[b1];
			b1++;
		}
		for (b2 = 18; b2 < arrayOfItemStack1.length && b2 < 45; b2++) {
			if (arrayOfItemStack2.length >= b1)
				arrayOfItemStack1[b2] = arrayOfItemStack2[b1];
			b1++;
		}
		player.getOpenInventory().getTopInventory().setContents(arrayOfItemStack1);
		player.updateInventory();
	}
	
	public void UpdateMasterCrafting(final Player player) {
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask(RedfixPlugin.getInstance(), new Runnable() {
			
			public void run() {
				RegWatcher regWatcher = ChestManager.this.getWatcherInfoBySlave(player);
				if (regWatcher == null)
					return;
				Player player = regWatcher.getMaster();
				Inventory inventory1 = player.getOpenInventory().getTopInventory();
				ItemStack[] arrayOfItemStack1 = inventory1.getContents();
				Inventory inventory2 = player.getOpenInventory().getTopInventory();
				ItemStack[] arrayOfItemStack2 = inventory2.getContents();
				if (arrayOfItemStack2.length != 5)
					return;
				arrayOfItemStack1[8] = arrayOfItemStack2[0];
				arrayOfItemStack1[6] = arrayOfItemStack2[1];
				arrayOfItemStack1[7] = arrayOfItemStack2[2];
				arrayOfItemStack1[15] = arrayOfItemStack2[3];
				arrayOfItemStack1[16] = arrayOfItemStack2[4];
				player.getOpenInventory().getTopInventory().setContents(arrayOfItemStack1);
				player.updateInventory();
			}
		}, 0L);
	}
	
	@Deprecated
	public void UpdateSlaveCrafting(Player paramPlayer) {}
	
	@Deprecated
	public void UpdateMasterCraftingResult(Player paramPlayer) {}
	
	public void UpdateMasterArmor(final Player player) {
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask((Plugin) RedfixPlugin.getInstance(), new Runnable() {
			
			public void run() {
				RegWatcher regWatcher = ChestManager.this.getWatcherInfoBySlave(player);
				if (regWatcher == null)
					return;
				Player player = regWatcher.getMaster();
				Inventory inventory = player.getOpenInventory().getTopInventory();
				ItemStack[] arrayOfItemStack1 = inventory.getContents();
				if (inventory.getSize() < 11)
					return;
				ItemStack[] arrayOfItemStack2 = player.getInventory().getArmorContents();
				arrayOfItemStack1[10] = arrayOfItemStack2[0];
				arrayOfItemStack1[1] = arrayOfItemStack2[1];
				arrayOfItemStack1[9] = arrayOfItemStack2[2];
				arrayOfItemStack1[0] = arrayOfItemStack2[3];
				player.getOpenInventory().getTopInventory().setContents(arrayOfItemStack1);
				player.updateInventory();
			}
		}, 0L);
	}
	
	public void UpdateMasterItemInHand(final Player player) {
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask((Plugin) RedfixPlugin.getInstance(), new Runnable() {
			
			public void run() {
				RegWatcher regWatcher = ChestManager.this.getWatcherInfoBySlave(player);
				if (regWatcher == null)
					return;
				Player player = regWatcher.getMaster();
				Inventory inventory = player.getOpenInventory().getTopInventory();
				ItemStack[] arrayOfItemStack = inventory.getContents();
				if (arrayOfItemStack.length <= 11)
					return;
				ItemStack itemStack = player.getItemOnCursor();
				arrayOfItemStack[11] = (itemStack == null) ? new ItemStack(Material.AIR) : itemStack;
				player.getOpenInventory().getTopInventory().setContents(arrayOfItemStack);
				player.updateInventory();
			}
		}, 0L);
	}
	
	public void UpdateSlaveItemInHand(final Player player, final ItemStack[] masterContents) {
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask((Plugin) RedfixPlugin.getInstance(), new Runnable() {
			
			public void run() {
				RegWatcher regWatcher = ChestManager.this.getWatcher(player);
				if (regWatcher == null)
					return;
				Player player = regWatcher.getTarget();
				if (player == null)
					return;
				ItemStack[] arrayOfItemStack = masterContents;
				if (arrayOfItemStack == null)
					arrayOfItemStack = player.getOpenInventory().getTopInventory().getContents();
				if (arrayOfItemStack.length <= 11)
					return;
				ItemStack itemStack = arrayOfItemStack[11];
				if (player.isOnline()) {
					player.setItemOnCursor(itemStack);
					player.updateInventory();
				}
			}
		}, 0L);
	}
	
	public void UpdateMasterOffHand(final Player player) {
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask((Plugin) RedfixPlugin.getInstance(), new Runnable() {
			
			public void run() {
				RegWatcher regWatcher = ChestManager.this.getWatcherInfoBySlave(player);
				if (regWatcher == null)
					return;
				Player player = regWatcher.getMaster();
				Inventory inventory = player.getOpenInventory().getTopInventory();
				ItemStack[] arrayOfItemStack = inventory.getContents();
				ItemStack itemStack = player.getInventory().getItemInOffHand();
				arrayOfItemStack[2] = (itemStack == null) ? new ItemStack(Material.AIR) : itemStack;
				player.getOpenInventory().getTopInventory().setContents(arrayOfItemStack);
				player.updateInventory();
			}
		}, 0L);
	}
	
	public void UpdateSlaveOffHand(final Player player, final ItemStack[] MasterContents) {
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask((Plugin) RedfixPlugin.getInstance(), new Runnable() {
			
			public void run() {
				RegWatcher regWatcher = ChestManager.this.getWatcher(player);
				if (regWatcher == null)
					return;
				Player player = regWatcher.getTarget();
				if (player == null)
					return;
				ItemStack[] arrayOfItemStack = MasterContents;
				if (arrayOfItemStack == null)
					arrayOfItemStack = player.getOpenInventory().getTopInventory().getContents();
				ItemStack itemStack = arrayOfItemStack[2];
				player.getInventory().setItemInOffHand(itemStack);
				if (player.isOnline()) {
					player.updateInventory();
				}
				else {
					player.saveData();
				}
			}
		}, 0L);
	}
	
	public void UpdateSlaveArmor(final Player player, final ItemStack[] MasterContents) {
		Bukkit.getServer().getScheduler().scheduleSyncDelayedTask((Plugin) RedfixPlugin.getInstance(), new Runnable() {
			
			public void run() {
				RegWatcher regWatcher = ChestManager.this.getWatcher(player);
				if (regWatcher == null)
					return;
				Player player = regWatcher.getTarget();
				if (player == null)
					return;
				ItemStack[] arrayOfItemStack = MasterContents;
				if (arrayOfItemStack == null)
					arrayOfItemStack = player.getOpenInventory().getTopInventory().getContents();
				if (arrayOfItemStack.length > 0)
					player.getInventory().setHelmet(
							(arrayOfItemStack[0] != null) ? arrayOfItemStack[0] : new ItemStack(Material.AIR));
				if (arrayOfItemStack.length > 9)
					player.getInventory().setChestplate(
							(arrayOfItemStack[9] != null) ? arrayOfItemStack[9] : new ItemStack(Material.AIR));
				if (arrayOfItemStack.length > 1)
					player.getInventory().setLeggings(
							(arrayOfItemStack[1] != null) ? arrayOfItemStack[1] : new ItemStack(Material.AIR));
				if (arrayOfItemStack.length > 10)
					player.getInventory().setBoots(
							(arrayOfItemStack[10] != null) ? arrayOfItemStack[10] : new ItemStack(Material.AIR));
				if (player.isOnline()) {
					player.updateInventory();
				}
				else {
					player.saveData();
				}
			}
		}, 0L);
	}
	
	public Inventory CreateGui(Player paramPlayer1, Player paramPlayer2) {
		Inventory inventory = Bukkit.createInventory(null, 54, "InvSee");
		ItemStack[] arrayOfItemStack1 = paramPlayer2.getInventory().getContents();
		ItemStack[] arrayOfItemStack2 = paramPlayer2.getInventory().getArmorContents();
		ArrayList<String> arrayList = new ArrayList();
		for (PotionEffect potionEffect : paramPlayer2.getActivePotionEffects()) {
			String str2 = potionEffect.getType().getName();
			str2 = str2.toLowerCase();
			int i = potionEffect.getAmplifier();
			String str3 = null;
			if (i == 0) {
				str3 = "";
			}
			else if (i == 1) {
				str3 = " II";
			}
			else {
				str3 = String.valueOf(i);
			}
			int j = potionEffect.getDuration();
			int k = j / 20;
			String str1 = str2 + " lvl: " + str3 + " dur: " + k;
			arrayList.add(str1);
		}
		ItemStack itemStack1 = new ItemStack(Material.POTION);
		ItemMeta itemMeta1 = itemStack1.getItemMeta();
		itemMeta1.setLore(arrayList);
		itemMeta1.setDisplayName("Effects");
		itemStack1.setItemMeta(itemMeta1);
		inventory.setItem(3, itemStack1);
		ItemStack itemStack2 = arrayOfItemStack2[0];
		ItemStack itemStack3 = arrayOfItemStack2[1];
		ItemStack itemStack4 = arrayOfItemStack2[2];
		ItemStack itemStack5 = arrayOfItemStack2[3];
		inventory.setItem(0, itemStack5);
		inventory.setItem(1, itemStack3);
		inventory.setItem(9, itemStack4);
		inventory.setItem(10, itemStack2);
		byte b1 = 0;
		byte b2;
		for (b2 = 45; b2 <= 53; b2++) {
			inventory.setItem(b2, arrayOfItemStack1[b1]);
			b1++;
		}
		for (b2 = 18; b2 <= 44; b2++) {
			inventory.setItem(b2, arrayOfItemStack1[b1]);
			b1++;
		}
		ArrayList<String> arrayList1 = new ArrayList();
		ItemStack itemStack = new ItemStack(Material.MAP);
		ItemMeta itemMeta = itemStack.getItemMeta();
		Location location = paramPlayer2.getLocation();
		arrayList1.add("world: " + location.getWorld().getName());
		arrayList1.add("x: " + location.getX());
		arrayList1.add("y: " + location.getY());
		arrayList1.add("z: " + location.getZ());
		arrayList1.add("pitch: " + location.getPitch());
		arrayList1.add("yaw: " + location.getYaw());
		itemMeta.setLore(arrayList1);
		itemMeta.setDisplayName("Location");
		itemStack.setItemMeta(itemMeta);
		inventory.setItem(4, itemStack);
		ItemStack itemStack7 = new ItemStack(Material.BOOK);
		ItemStack itemStack8 = new ItemStack(Material.ENDER_CHEST);
		ItemMeta itemMeta2 = itemStack8.getItemMeta();
		itemMeta2.setDisplayName("Enderchest");
		itemStack8.setItemMeta(itemMeta2);
		inventory.setItem(5, itemStack8);
		arrayList1 = new ArrayList();
		ItemMeta itemMeta3 = itemStack7.getItemMeta();
		arrayList1.add("Health: " + paramPlayer2.getHealth() + "/" + paramPlayer2.getMaxHealth());
		arrayList1.add("Hunger: " + paramPlayer2.getFoodLevel());
		arrayList1.add("Saturation: " + paramPlayer2.getSaturation());
		arrayList1.add("XP: " + paramPlayer2.getTotalExperience());
		arrayList1.add("Gamemode: " + paramPlayer2.getGameMode().name());
		arrayList1.add("Fly Allowed: " + paramPlayer2.getAllowFlight());
		itemMeta3.setLore(arrayList1);
		itemMeta3.setDisplayName("Infos");
		itemStack7.setItemMeta(itemMeta3);
		inventory.setItem(12, itemStack7);
		ArrayList<String> arrayList2 = new ArrayList();
		itemStack = new ItemStack(Material.BOOK);
		ItemMeta itemMeta4 = itemStack7.getItemMeta();
		arrayList2.add("§e****************** UUID ******************");
		arrayList2.add(paramPlayer2.getUniqueId().toString());
		return inventory;
	}
	
	public ItemStack SlaveCraftInv(Player paramPlayer, int paramInt) {
		ItemStack itemStack = null;
		if (paramPlayer.getOpenInventory().getTopInventory().getType() == InventoryType.CRAFTING && paramPlayer.getOpenInventory().getTopInventory().getItem(
				paramInt) != null)
			itemStack = paramPlayer.getOpenInventory().getTopInventory().getItem(paramInt);
		return itemStack;
	}
	
}
