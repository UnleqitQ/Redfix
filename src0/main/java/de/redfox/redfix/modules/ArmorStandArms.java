package de.redfox.redfix.modules;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;

import java.util.HashSet;
import java.util.Set;

public class ArmorStandArms {
	
	public void updateArmorStands() {
		Set<Entity> entities = new HashSet<>();
		Bukkit.getOnlinePlayers().stream().map(p -> p.getNearbyEntities(4, 4, 4)).forEach(entities::addAll);
		entities.stream().filter(e -> e.getType() == EntityType.ARMOR_STAND).forEach(e -> {
			ArmorStand armorStand = (ArmorStand) e;
			if (!armorStand.hasArms()) {
				ItemStack i1 = null;
				for (Entity entity : armorStand.getNearbyEntities(0.5, 0.5, 0.5)) {
					if (entity instanceof Item item) {
						if (item.getItemStack().getType() == Material.STICK) {
							if (item.getItemStack().getAmount() >= 2) {
								item.getItemStack().setAmount(item.getItemStack().getAmount() - 2);
								armorStand.setArms(true);
								break;
							}
							else if (item.getItemStack().getAmount() == 1) {
								if (i1 == null) {
									i1 = item.getItemStack();
								}
								else {
									item.getItemStack().setAmount(item.getItemStack().getAmount() - 1);
									i1.setAmount(i1.getAmount() - 1);
									armorStand.setArms(true);
									break;
								}
							}
						}
					}
				}
			}
		});
	}
	
}

