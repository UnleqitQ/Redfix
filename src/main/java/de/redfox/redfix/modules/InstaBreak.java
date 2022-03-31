package de.redfox.redfix.modules;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Item;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class InstaBreak implements Listener {
	
	public static ConcurrentMap<UUID, Byte> players = new ConcurrentHashMap<>();
	
	@EventHandler (priority = EventPriority.LOW, ignoreCancelled = true)
	public void onInteract(PlayerInteractEvent event) {
		if (event.getAction() == Action.LEFT_CLICK_BLOCK && players.containsKey(event.getPlayer().getUniqueId())) {
			Block block = event.getClickedBlock();
			byte flags = players.get(event.getPlayer().getUniqueId());
			if (block.getState() instanceof Container)
				return;
			if (block.getType().getHardness() < 0 && (flags & 1 << 0) == 0)
				return;
			if ((flags & 1 << 1) == 0) {
				if (event.getItem() != null)
					block.breakNaturally(event.getItem(), true);
				else
					block.breakNaturally(true);
				block.setType(Material.AIR);
			}
			else {
				Item item = block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(block.getType()));
				block.setType(Material.AIR);
			}
		}
	}
	
}
