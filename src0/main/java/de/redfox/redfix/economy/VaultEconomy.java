package de.redfox.redfix.economy;

import de.redfox.redfix.RedfixPlugin;
import net.milkbowl.vault.economy.AbstractEconomy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;

import java.util.List;

public class VaultEconomy extends AbstractEconomy {
	
	@Override
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
	}
	
}
