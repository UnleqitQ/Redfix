package de.redfox.redfix.modules;

import de.redfox.redfix.RedfixPlugin;
import de.redfox.redfix.economy.EconomyManager;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class RedfixPlaceholder extends PlaceholderExpansion {
	
	@Override
	public @NotNull String getIdentifier() {
		return "redfix";
	}
	
	@Override
	public @NotNull String getAuthor() {
		return "UnleqitQ";
	}
	
	@Override
	public @NotNull String getVersion() {
		return "1.0.0";
	}
	
	@Override
	public @Nullable String onPlaceholderRequest(Player player, @NotNull String params) {
		return onRequest(player, params);
	}
	
	@Override
	public @Nullable String onRequest(OfflinePlayer player, @NotNull String params) {
		if (RedfixPlugin.isEconomyEnabled()) {
			if (params.equalsIgnoreCase("balance")) {
				return Double.toString(EconomyManager.getMoney(player.getUniqueId()));
			}
			if (params.equalsIgnoreCase("balance_format")) {
				return RedfixPlugin.getInstance().vaultEconomy.format(EconomyManager.getMoney(player.getUniqueId()));
			}
		}
		return "";
	}
	
}
