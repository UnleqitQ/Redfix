package de.redfox.redfix.modules;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.PrepareAnvilEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;

public class ColorListener implements Listener {
	
	
	@EventHandler (priority = EventPriority.LOW, ignoreCancelled = true)
	public void onAnvil(PrepareAnvilEvent event) {
		if (event.getInventory().getRenameText() != null && !event.getInventory().getRenameText().isBlank()) {
			ItemStack r = event.getResult();
			if (r == null)
				return;
			ItemMeta meta = r.getItemMeta();
			meta.setDisplayName(ChatColor.translateAlternateColorCodes('&', event.getInventory().getRenameText()));
			r.setItemMeta(meta);
			event.setResult(r);
		}
	}
	
	@EventHandler (priority = EventPriority.LOW, ignoreCancelled = true)
	public void onSign(SignChangeEvent event) {
		for (int i = 0; i < event.getLines().length; i++) {
			String l = event.getLines()[i];
			l = ChatColor.translateAlternateColorCodes('&', l);
			event.setLine(i, l);
		}
	}
	
	@EventHandler (priority = EventPriority.LOW, ignoreCancelled = true)
	public void onBook(PlayerEditBookEvent event) {
		if (event.isSigning()) {
			BookMeta meta = event.getNewBookMeta();
			for (int i = 0; i < meta.getPageCount(); i++) {
				try {
					String p = meta.getPage(i + 1);
					p = ChatColor.translateAlternateColorCodes('&', p);
					meta.setPage(i + 1, p);
				} catch (Exception e) {
					Bukkit.broadcastMessage(e.getMessage());
					e.printStackTrace();
				}
			}
			event.setNewBookMeta(meta);
		}
	}
	
	
}
