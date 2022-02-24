package de.redfox.redfix.economy;

import de.redfox.redfix.RedfixPlugin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;

import java.util.Collections;
import java.util.List;

public class VaultEconomy implements Economy {
	
	@Override
	public boolean isEnabled() {
		return true;
	}
	
	@Override
	public String getName() {
		return "RedFix Economy";
	}
	
	@Override
	public boolean hasBankSupport() {
		return false;
	}
	
	@Override
	public int fractionalDigits() {
		return -1;
	}
	
	@Override
	public String format(double amount) {
		return String.format("%.02f%s", amount,
				RedfixPlugin.getInstance().getConfig().getString("economy.symbol", "$"));
	}
	
	@Override
	public String currencyNamePlural() {
		return RedfixPlugin.getInstance().getConfig().getString("economy.name.plural", "Dollars");
	}
	
	@Override
	public String currencyNameSingular() {
		return RedfixPlugin.getInstance().getConfig().getString("economy.name.singular", "Dollar");
	}
	
	@Override
	public boolean hasAccount(String playerName) {
		return true;
	}
	
	@Override
	public boolean hasAccount(OfflinePlayer player) {
		return true;
	}
	
	@Override
	public boolean hasAccount(String playerName, String worldName) {
		return true;
	}
	
	@Override
	public boolean hasAccount(OfflinePlayer player, String worldName) {
		return true;
	}
	
	@Override
	public double getBalance(String playerName) {
		return EconomyManager.getMoney(Bukkit.getOfflinePlayer(playerName).getUniqueId());
	}
	
	@Override
	public double getBalance(OfflinePlayer player) {
		return EconomyManager.getMoney(player.getUniqueId());
	}
	
	@Override
	public double getBalance(String playerName, String world) {
		return EconomyManager.getMoney(Bukkit.getOfflinePlayer(playerName).getUniqueId());
	}
	
	@Override
	public double getBalance(OfflinePlayer player, String world) {
		return EconomyManager.getMoney(player.getUniqueId());
	}
	
	@Override
	public boolean has(String playerName, double amount) {
		return getBalance(playerName) >= amount;
	}
	
	@Override
	public boolean has(OfflinePlayer player, double amount) {
		return getBalance(player) >= amount;
	}
	
	@Override
	public boolean has(String playerName, String worldName, double amount) {
		return getBalance(playerName) >= amount;
	}
	
	@Override
	public boolean has(OfflinePlayer player, String worldName, double amount) {
		return getBalance(player) >= amount;
	}
	
	@Override
	public EconomyResponse withdrawPlayer(String playerName, double amount) {
		if (playerName == null) {
			return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Player name cannot be null!");
		}
		if (amount < 0) {
			return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Cannot withdraw negative funds!");
		}
		OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
		
		
		EconomyManager.addMoney(player.getUniqueId(), -amount);
		return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
	}
	
	@Override
	public EconomyResponse withdrawPlayer(OfflinePlayer player, double amount) {
		if (amount < 0) {
			return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Cannot withdraw negative funds!");
		}
		
		
		EconomyManager.addMoney(player.getUniqueId(), -amount);
		return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
	}
	
	@Override
	public EconomyResponse withdrawPlayer(String playerName, String worldName, double amount) {
		return withdrawPlayer(playerName, amount);
	}
	
	@Override
	public EconomyResponse withdrawPlayer(OfflinePlayer player, String worldName, double amount) {
		return withdrawPlayer(player, amount);
	}
	
	@Override
	public EconomyResponse depositPlayer(String playerName, double amount) {
		if (playerName == null) {
			return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Player name cannot be null!");
		}
		if (amount < 0) {
			return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Cannot deposit negative funds!");
		}
		OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
		
		
		EconomyManager.addMoney(player.getUniqueId(), amount);
		return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
	}
	
	@Override
	public EconomyResponse depositPlayer(OfflinePlayer player, double amount) {
		if (amount < 0) {
			return new EconomyResponse(0, 0, EconomyResponse.ResponseType.FAILURE, "Cannot deposit negative funds!");
		}
		
		
		EconomyManager.addMoney(player.getUniqueId(), amount);
		return new EconomyResponse(amount, getBalance(player), EconomyResponse.ResponseType.SUCCESS, null);
	}
	
	@Override
	public EconomyResponse depositPlayer(String playerName, String worldName, double amount) {
		return depositPlayer(playerName, amount);
	}
	
	@Override
	public EconomyResponse depositPlayer(OfflinePlayer player, String worldName, double amount) {
		return depositPlayer(player, amount);
	}
	
	
	@Override
	public boolean createPlayerAccount(String playerName) {
		return false;
	}
	
	@Override
	public boolean createPlayerAccount(OfflinePlayer player) {
		return false;
	}
	
	@Override
	public boolean createPlayerAccount(String playerName, String worldName) {
		return false;
	}
	
	@Override
	public boolean createPlayerAccount(OfflinePlayer player, String worldName) {
		return false;
	}
	
	
	@Override
	public EconomyResponse createBank(String name, String player) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
				"RedFix does not support bank accounts!");
	}
	
	@Override
	public EconomyResponse createBank(String name, OfflinePlayer player) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
				"RedFix does not support bank accounts!");
	}
	
	@Override
	public EconomyResponse deleteBank(String name) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
				"RedFix does not support bank accounts!");
	}
	
	@Override
	public EconomyResponse bankBalance(String name) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
				"RedFix does not support bank accounts!");
	}
	
	@Override
	public EconomyResponse bankHas(String name, double amount) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
				"RedFix does not support bank accounts!");
	}
	
	@Override
	public EconomyResponse bankWithdraw(String name, double amount) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
				"RedFix does not support bank accounts!");
	}
	
	@Override
	public EconomyResponse bankDeposit(String name, double amount) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
				"RedFix does not support bank accounts!");
	}
	
	@Override
	public EconomyResponse isBankOwner(String name, String playerName) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
				"RedFix does not support bank accounts!");
	}
	
	@Override
	public EconomyResponse isBankOwner(String name, OfflinePlayer player) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
				"RedFix does not support bank accounts!");
	}
	
	@Override
	public EconomyResponse isBankMember(String name, String playerName) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
				"RedFix does not support bank accounts!");
	}
	
	@Override
	public EconomyResponse isBankMember(String name, OfflinePlayer player) {
		return new EconomyResponse(0, 0, EconomyResponse.ResponseType.NOT_IMPLEMENTED,
				"RedFix does not support bank accounts!");
	}
	
	@Override
	public List<String> getBanks() {
		return Collections.emptyList();
	}
	
	/*@Override
	public boolean isEnabled() {
		return true;
	}
	
	@Override
	public String getName() {
		return "RedFix";
	}
	
	@Override
	public boolean hasBankSupport() {
		return false;
	}
	
	@Override
	public int fractionalDigits() {
		return 4;
	}
	
	@Override
	public String format(double v) {
		return String.format("%.02f%s", v, RedfixPlugin.getInstance().getConfig().getString("economy.symbol", "$"));
	}
	
	@Override
	public String currencyNamePlural() {
		return RedfixPlugin.getInstance().getConfig().getString("economy.name.plural", "Dollars");
	}
	
	@Override
	public String currencyNameSingular() {
		return RedfixPlugin.getInstance().getConfig().getString("economy.name.singular", "Dollar");
	}
	
	@Override
	public boolean hasAccount(String s) {
		return true;
	}
	
	@Override
	public boolean hasAccount(String s, String s1) {
		return true;
	}
	
	@Override
	public double getBalance(String s) {
		return EconomyManager.getMoney(Bukkit.getOfflinePlayer(s).getUniqueId());
	}
	
	@Override
	public double getBalance(String s, String s1) {
		return getBalance(s);
	}
	
	@Override
	public boolean has(String s, double v) {
		return getBalance(s) >= v;
	}
	
	@Override
	public boolean has(String s, String s1, double v) {
		return has(s, v);
	}
	
	@Override
	public EconomyResponse withdrawPlayer(String s, double v) {
		if (v < 0)
			return new EconomyResponse(0, getBalance(s), EconomyResponse.ResponseType.FAILURE,
					"You cannot withdraw a negative amount");
		if (has(s, v)) {
			EconomyManager.addMoney(Bukkit.getOfflinePlayer(s).getUniqueId(), -v);
			return new EconomyResponse(v, getBalance(s), EconomyResponse.ResponseType.SUCCESS,
					"You have not enough money");
		}
		else {
			return new EconomyResponse(0, getBalance(s), EconomyResponse.ResponseType.FAILURE,
					"You have not enough money");
		}
	}
	
	@Override
	public EconomyResponse withdrawPlayer(String s, String s1, double v) {
		return withdrawPlayer(s, v);
	}
	
	@Override
	public EconomyResponse depositPlayer(String s, double v) {
		if (v < 0)
			return new EconomyResponse(0, getBalance(s), EconomyResponse.ResponseType.FAILURE,
					"You cannot deposit a negative amount");
		EconomyManager.addMoney(Bukkit.getOfflinePlayer(s).getUniqueId(), v);
		return new EconomyResponse(v, getBalance(s), EconomyResponse.ResponseType.SUCCESS, "You have not enough money");
	}
	
	@Override
	public EconomyResponse depositPlayer(String s, String s1, double v) {
		return depositPlayer(s, v);
	}
	
	@Override
	public EconomyResponse createBank(String s, String s1) {
		return null;
	}
	
	@Override
	public EconomyResponse deleteBank(String s) {
		return null;
	}
	
	@Override
	public EconomyResponse bankBalance(String s) {
		return null;
	}
	
	@Override
	public EconomyResponse bankHas(String s, double v) {
		return null;
	}
	
	@Override
	public EconomyResponse bankWithdraw(String s, double v) {
		return null;
	}
	
	@Override
	public EconomyResponse bankDeposit(String s, double v) {
		return null;
	}
	
	@Override
	public EconomyResponse isBankOwner(String s, String s1) {
		return null;
	}
	
	@Override
	public EconomyResponse isBankMember(String s, String s1) {
		return null;
	}
	
	@Override
	public List<String> getBanks() {
		return null;
	}
	
	@Override
	public boolean createPlayerAccount(String s) {
		EconomyManager.addMoney(Bukkit.getOfflinePlayer(s).getUniqueId(), RedfixPlugin.getInstance().getConfig().getDouble("economy.startMoney", 100));
		return true;
	}
	
	@Override
	public boolean createPlayerAccount(String s, String s1) {
		return createPlayerAccount(s);
	}*/
	
}
