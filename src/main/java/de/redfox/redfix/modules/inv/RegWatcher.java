package de.redfox.redfix.modules.inv;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

public class RegWatcher {
	Player master = null;
	
	Player target = null;
	
	private int schedId = 0;
	
	private ItemStack[] contents = new ItemStack[64];
	
	private int actionSchedId = 0;
	
	public RegWatcher(Player paramPlayer1, Player paramPlayer2) {
		this.master = paramPlayer1;
		this.target = paramPlayer2;
	}
	
	public Player getMaster() {
		return this.master;
	}
	
	public void setMaster(Player paramPlayer) {
		this.master = paramPlayer;
	}
	
	public Player getTarget() {
		if (Bukkit.getPlayer(this.target.getUniqueId()) != null)
			return Bukkit.getPlayer(this.target.getUniqueId());
		return this.target;
	}
	
	public void setTarget(Player paramPlayer) {
		this.target = paramPlayer;
	}
	
	public int getSchedId() {
		return this.schedId;
	}
	
	public void setSchedId(int paramInt) {
		this.schedId = paramInt;
	}
	
	public ItemStack[] getContents() {
		return this.contents;
	}
	
	public void setContents(ItemStack[] paramArrayOfItemStack) {
		for (byte b = 0; b < paramArrayOfItemStack.length; b++) {
			if (paramArrayOfItemStack[b] == null) {
				this.contents[b] = null;
			} else {
				this.contents[b] = paramArrayOfItemStack[b].clone();
			}
		}
	}
	
	public int getActionSchedId() {
		return this.actionSchedId;
	}
	
	public void setActionSchedId(int paramInt) {
		this.actionSchedId = paramInt;
	}
}
