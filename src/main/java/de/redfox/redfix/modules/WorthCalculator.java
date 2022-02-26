package de.redfox.redfix.modules;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.*;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class WorthCalculator {
	
	public static Map<Material, Double> worthMap = new HashMap<>();
	
	public static void clear() {
		worthMap.clear();
	}
	
	public static void setToDefault() {
		worthMap.put(Material.DARK_OAK_LOG, 2.0);
		worthMap.put(Material.ACACIA_LOG, 2.0);
		worthMap.put(Material.BIRCH_LOG, 2.0);
		worthMap.put(Material.JUNGLE_LOG, 2.0);
		worthMap.put(Material.OAK_LOG, 2.0);
		worthMap.put(Material.SPRUCE_LOG, 2.0);
		worthMap.put(Material.WHITE_WOOL, 20.0);
		worthMap.put(Material.STONE, 6.0);
		worthMap.put(Material.COBBLESTONE, 1.0);
		worthMap.put(Material.GRAVEL, 1.0);
		worthMap.put(Material.CLAY_BALL, 3.0);
		worthMap.put(Material.SUGAR_CANE, 10.0);
		worthMap.put(Material.APPLE, 10.0);
		worthMap.put(Material.GRASS, 3.0);
		worthMap.put(Material.SLIME_BALL, 20.0);
		worthMap.put(Material.SAND, 1.0);
		worthMap.put(Material.DIAMOND_ORE, 200.0);
		worthMap.put(Material.DIAMOND, 200.0);
		worthMap.put(Material.IRON_ORE, 18.0);
		worthMap.put(Material.RAW_IRON, 18.0);
		worthMap.put(Material.IRON_INGOT, 22.0);
		worthMap.put(Material.GOLD_ORE, 45.0);
		worthMap.put(Material.RAW_GOLD, 45.0);
		worthMap.put(Material.REDSTONE_ORE, 30.0);
		worthMap.put(Material.REDSTONE, 32.0);
		worthMap.put(Material.GOLD_INGOT, 105.0);
		worthMap.put(Material.COAL, 15.0);
		worthMap.put(Material.COAL_ORE, 15.0);
		worthMap.put(Material.FEATHER, 3.0);
		worthMap.put(Material.WHEAT, 9.0);
		worthMap.put(Material.LAVA_BUCKET, 40.0);
		worthMap.put(Material.LAVA, 25.0);
		worthMap.put(Material.LAPIS_ORE, 100.0);
		worthMap.put(Material.LAPIS_LAZULI, 50.0);
		worthMap.put(Material.STRING, 5.0);
		worthMap.put(Material.BONE, 2.0);
	}
	
	public static void calculate(boolean setNan, boolean replace) {
		if (setNan) {
			if (replace) {
				for (Material m : Material.values()) {
					if (!m.isLegacy()) {
						double value = getWorth(m, new HashSet<>(), true);
						if (!worthMap.containsKey(m)) {
							worthMap.put(m, value);
							continue;
						}
						double old = worthMap.get(m);
						if (Double.isNaN(old)) {
							worthMap.put(m, value);
						}
						else {
							if (!Double.isNaN(value)) {
								if (value < old) {
									worthMap.put(m, value);
								}
							}
						}
					}
				}
			}
			else {
				for (Material m : Material.values()) {
					if (!m.isLegacy()) {
						getWorth(m, new HashSet<>(), true);
					}
				}
			}
		}
		else {
			if (replace) {
				for (Material m : Material.values()) {
					if (!m.isLegacy()) {
						double value = getWorth(m, new HashSet<>(), true);
						if (!Double.isNaN(value) && (!worthMap.containsKey(m) || worthMap.get(m) > value))
							worthMap.put(m, value);
					}
				}
			}
			else {
				for (Material m : Material.values()) {
					if (!m.isLegacy()) {
						getWorth(m, new HashSet<>(), true);
					}
				}
			}
		}
	}
	
	public static double getWorth(Material material, Set<Material> denied) {
		return getWorth(material, denied, false);
	}
	
	public static double getWorth(Material material, Set<Material> denied, boolean forceCalc) {
		if (forceCalc || !worthMap.containsKey(material)) {
			if (denied.contains(material))
				return Double.NaN;
			List<Recipe> recipes = Bukkit.getRecipesFor(new ItemStack(material));
			double min = Double.NaN;
			for (Recipe recipe : recipes) {
				if (recipe instanceof ShapedRecipe r) {
					double v = 0;
					for (char c : String.join("", r.getShape()).toCharArray()) {
						if (r.getIngredientMap().containsKey(c) && r.getIngredientMap().get(c) != null) {
							Set<Material> denied0 = new HashSet<>(denied);
							denied0.add(r.getIngredientMap().get(c).getType());
							double v0 = getWorth(r.getIngredientMap().get(c).getType(), denied0);
							if (Double.isNaN(v0)) {
								v = Double.NaN;
								break;
							}
							v += v0;
						}
					}
					if (!Double.isNaN(v)) {
						v /= r.getResult().getAmount();
						if (Double.isNaN(min))
							min = v;
						else if (min > v)
							min = v;
					}
				}
				if (recipe instanceof ShapelessRecipe r) {
					double v = 0;
					for (ItemStack i : r.getIngredientList()) {
						Set<Material> denied0 = new HashSet<>(denied);
						denied0.add(i.getType());
						double v0 = getWorth(i.getType(), denied0);
						if (Double.isNaN(v0)) {
							v = Double.NaN;
							break;
						}
						v += v0;
					}
					if (!Double.isNaN(v)) {
						v /= r.getResult().getAmount();
						if (Double.isNaN(min))
							min = v;
						else if (min > v)
							min = v;
					}
				}
				if (recipe instanceof CookingRecipe r) {
					Set<Material> denied0 = new HashSet<>(denied);
					denied0.add(r.getInput().getType());
					double v = getWorth(r.getInput().getType(), denied0);
					if (!Double.isNaN(v)) {
						v += r.getCookingTime() * 0.05 * 0.2;
						v /= r.getResult().getAmount();
						if (Double.isNaN(min))
							min = v;
						else if (min > v)
							min = v;
					}
				}
			}
			if (!Double.isNaN(min)) {
				worthMap.put(material, min);
			}
			return min;
		}
		return worthMap.get(material);
	}
	
	public static void load(File file) {
		file.getParentFile().mkdirs();
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		try {
			YamlConfiguration config = YamlConfiguration.loadConfiguration(file);
			Set<String> keys = config.getKeys(false);
			for (String key : keys) {
				try {
					Material material = Material.valueOf(key);
					double value = config.getDouble(key);
					if (value < 0)
						value = Double.NaN;
					worthMap.put(material, value);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void save(File file) {
		file.getParentFile().mkdirs();
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		try {
			YamlConfiguration config = new YamlConfiguration();
			for (Map.Entry<Material, Double> entry : worthMap.entrySet()) {
				if (Double.isNaN(entry.getValue()) || entry.getValue() < 0) {
					config.set(entry.getKey().name(), -1);
					continue;
				}
				config.set(entry.getKey().name(), entry.getValue());
			}
			config.save(file);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
}
