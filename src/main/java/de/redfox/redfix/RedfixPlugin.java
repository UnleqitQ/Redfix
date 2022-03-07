package de.redfox.redfix;

import com.comphenix.protocol.ProtocolLibrary;
import com.google.common.base.Predicates;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.redfox.redfix.chat.ChatListener;
import de.redfox.redfix.commands.CommandSpy;
import de.redfox.redfix.config.ConfigManager;
import de.redfox.redfix.config.LanguageConfig;
import de.redfox.redfix.economy.EconomyManager;
import de.redfox.redfix.economy.VaultEconomy;
import de.redfox.redfix.modules.*;
import de.redfox.redfix.modules.jail.Jail;
import de.redfox.redfix.modules.jail.JailHandler;
import de.redfox.redfix.modules.jail.JailedPlayer;
import de.redfox.redfix.utils.PlayerWeatherType;
import de.redfox.redfix.utils.WeatherType;
import me.unleqitq.commandframework.CommandManager;
import me.unleqitq.commandframework.building.argument.*;
import me.unleqitq.commandframework.building.command.FrameworkCommand;
import me.unleqitq.commandframework.building.flag.FrameworkFlag;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.*;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.Damageable;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.util.RayTraceResult;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Stream;

public class RedfixPlugin extends JavaPlugin {
	
	private static final int METRICS_ID = 14559;
	
	private static RedfixPlugin instance;
	public CommandSpy commandSpy;
	public static final String pluginPath = "plugins/Redfix";
	public Chat vaultChat;
	public VaultEconomy vaultEconomy;
	public Afk afk;
	public static Map<UUID, Long> muted = new HashMap<>();
	public static CommandManager commandManager;
	
	public static Map<UUID, Location> playerDeathLocations = new HashMap<>();
	public static Map<UUID, Deque<Location>> playerLocationHistory = new HashMap<>();
	public static Map<UUID, Map<String, Home>> homes = new HashMap<>();
	public static Map<String, Warp> warps = new HashMap<>();
	
	public RedfixPlugin() {
		instance = this;
		new File(pluginPath).mkdirs();
	}
	
	public static Map<UUID, UUID> lastMessaged = new HashMap<>();
	
	
	@Override
	public void onEnable() {
		//WorthCalculator.reset();
		//WorthCalculator.calculate();
		
		ConfigManager.init();
		
		saveDefaultConfig();
		reloadConfig();
		
		commandManager = new CommandManager(this);
		
		EconomyManager.loadData(new File(pluginPath, "economy.json"));
		loadHomes(new File(pluginPath, "homes.json"));
		loadWarps(new File(pluginPath, "warps.json"));
		WorthCalculator.load(new File(pluginPath, "worth.yml"));
		
		initLanguage();
		
		afk = new Afk();
		Bukkit.getPluginManager().registerEvents(afk, this);
		new God();
		Bukkit.getPluginManager().registerEvents(new DeathListener(), this);
		ProtocolLibrary.getProtocolManager().addPacketListener(afk);
		new JailHandler();
		Afk.init();
		Bukkit.getScheduler().runTaskTimer(this, Afk::check, 20, 20);
		
		Bukkit.getScheduler().runTaskTimer(this, new ArmorStandArms()::updateArmorStands, 20, 20);
		
		commandSpy = new CommandSpy();
		commandSpy.load();
		registerCommands();
		RegisteredServiceProvider<Chat> rspC = RedfixPlugin.getInstance().getServer().getServicesManager().getRegistration(
				Chat.class);
		if (rspC != null) {
			vaultChat = rspC.getProvider();
		}
		new ChatListener();
		
		vaultEconomy = new VaultEconomy();
		getServer().getServicesManager().register(Economy.class, vaultEconomy, this, ServicePriority.Normal);
		
	}
	
	@Override
	public void onDisable() {
		saveEco();
		saveHomes(new File(pluginPath, "homes.json"));
		saveWarps(new File(pluginPath, "warps.json"));
		ProtocolLibrary.getProtocolManager().removePacketListeners(this);
	}
	
	public static void saveEco() {
		EconomyManager.saveData(new File(pluginPath, "economy.json"));
	}
	
	private void registerCommands() {
		
		//Jail
		{
			FrameworkCommand.Builder<CommandSender> topBuilder = FrameworkCommand.commandBuilder("jail");
			/*Command.Builder<CommandSender> createBuilder = topBuilder.literal("create").senderType(
					Player.class).argument(PlayerArgument.of("player")).handler(commandContext -> {
				CommandSender sender = commandContext.getSender();
				Player target = commandContext.get("player");
				sender.sendMessage("You jailed " + target.getName());
				Player player = commandContext.get("player");
				player.sendMessage("Jailed XD");
			});*/
			FrameworkCommand.Builder<Player> createBuilder = topBuilder.subPlayerCommand("create").permission(
					"redfix.command.jail.create").argument(StringArgument.of("name"),
					"The name of the jail to create").handler(commandContext -> {
				Player sender = (Player) commandContext.getSender();
				if (JailHandler.jails.containsKey(commandContext.getArgument("name"))) {
					sendMessage(sender, "A jail with this name already exists");
					return;
				}
				Jail jail = new Jail(commandContext.getArgument("name"), sender.getLocation().getBlock().getLocation());
				JailHandler.jails.put(jail.name, jail);
				sendMessage(sender, "Created jail \"" + jail.name + "\"");
			});
			
			StringArgument.Builder jailArgument = (StringArgument.Builder) StringArgument.of("name").tabComplete(
					(context, arg) -> JailHandler.jails.keySet().stream().filter(
							s -> s.toLowerCase().contains(arg.toLowerCase())).toList());
			
			FrameworkCommand.Builder<CommandSender> removeBuilder = topBuilder.subCommand("remove").permission(
					"redfix.command.jail.remove").argument(jailArgument, "The name of the jail to remove").handler(
					commandContext -> {
						CommandSender sender = (CommandSender) commandContext.getSender();
						if (!JailHandler.jails.containsKey(commandContext.getArgument("name"))) {
							sendMessage(sender, "This jail does not exist");
							return;
						}
						JailHandler.jails.remove(commandContext.getArgument("name"));
						sendMessage(sender, "Removed jail \"" + commandContext.getArgument("name") + "\"");
					});
			
			FrameworkCommand.Builder<CommandSender> jailBuilder = topBuilder.subCommand("jail").permission(
					"redfix.command.jail.jail").argument(PlayerArgument.of("player"), "The player to jail").argument(
					jailArgument, "The name of the jail to remove").argument(
					IntegerArgument.of("duration").optional(-1), "Duration to jail the player in seconds").handler(
					commandContext -> {
						CommandSender sender = commandContext.getSender();
						Player player = commandContext.getArgument("player");
						String name = commandContext.getArgument("name");
						int duration = commandContext.getArgument("duration");
						if (duration == -1 && !sender.hasPermission("redfix.jail.jail.permanent")) {
							sendMessage(sender, "I'm sorry, but you don't have the permission to jail permanently");
							return;
						}
						if (!JailHandler.jails.containsKey(name)) {
							sendMessage(sender, "This jail does not exist");
							return;
						}
						
						JailedPlayer jp = new JailedPlayer(player.getUniqueId(), name, duration);
						JailHandler.jailedPlayers.put(player.getUniqueId(), jp);
						Bukkit.getScheduler().runTask(RedfixPlugin.getInstance(),
								() -> player.teleport(jp.getJail().location));
						sendMessage(sender,
								"You jailed " + player.getName() + ((duration != -1) ? " for " + duration + " seconds" : ""));
						sendMessage(player,
								"You got jailed" + ((duration != -1) ? " for " + duration + " seconds" : ""));
					});
			
			FrameworkCommand.Builder<CommandSender> freeBuilder = topBuilder.subCommand("unjail").permission(
					"redfix.command.jail.unjail").argument(PlayerArgument.of("player"), "The player to unjail").handler(
					commandContext -> {
						CommandSender sender = commandContext.getSender();
						Player player = commandContext.getArgument("player");
						if (!JailHandler.jailedPlayers.containsKey(player.getUniqueId())) {
							sendMessage(sender, "This player is not jailed");
							return;
						}
						
						sendMessage(sender, "You freed " + player.getName());
						JailHandler.jailedPlayers.remove(player.getUniqueId());
						sendMessage(player, "You got freed");
					});
			
			commandManager.register(createBuilder);
			commandManager.register(removeBuilder);
			commandManager.register(jailBuilder);
			commandManager.register(freeBuilder);
		}
		
		//God
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("god");
			builder = builder.permission("redfix.command.god").flag(
							FrameworkFlag.of("silent").setDescription("You get damage but the amount is set to zero")).flag(
							FrameworkFlag.of("notarget").setDescription("Mobs don't target you")).argument(
							PlayerArgument.of("player").optional(), "player")
					//.argument(PlayerArgument.of("player"))
					.handler(commandContext -> {
						Player player = (Player) commandContext.getSender();
						Player target = commandContext.getOrDefault("player", player);
						if (God.players.containsKey(target.getUniqueId())) {
							God.players.remove(target.getUniqueId());
							sendMessage(player, "Disabled God");
						}
						else {
							God.players.put(target.getUniqueId(),
									new Boolean[]{commandContext.getFlag("silent"), commandContext.getFlag(
											"notarget")});
							sendMessage(player, "Enabled God");
						}
					});
			commandManager.register(builder);
		}
		
		//Heal
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("heal");
			builder = builder.permission("redfix.command.heal").flag(
					FrameworkFlag.of("particle").setDescription("Spawn a heart particle")).argument(
					PlayerArgument.of("player").optional(), "player").handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				Player target = (Player) commandContext.getOrDefault("player", player);
				target.setHealth(
						target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() + target.getAbsorptionAmount());
				target.setExhaustion(0);
				target.setSaturation(20);
				target.setFoodLevel(20);
				sendMessage(target, "You got healed");
				if (commandContext.getFlag("particle")) {
					target.getWorld().spawnParticle(Particle.HEART, target.getLocation().clone().add(0, 1.5, 0), 1);
				}
			});
			commandManager.register(builder);
		}
		
		//Saturation
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("saturation", "eat");
			builder = builder.permission("redfix.command.saturation").argument(PlayerArgument.of("player").optional(),
					"player").handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				Player target = commandContext.getOrDefault("player", player);
				target.setExhaustion(0);
				target.setSaturation(20);
				target.setFoodLevel(20);
				sendMessage(target, "Your stomach is full");
			});
			commandManager.register(builder);
		}
		
		//Fly
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("fly");
			builder = builder.permission("redfix.command.fly").argument(PlayerArgument.of("player").optional(),
					"player").handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				Player target = commandContext.getOrDefault("player", player);
				target.setAllowFlight(!target.getAllowFlight());
				sendMessage(player, target.getAllowFlight() ? "Enabled fly" : "Disabled fly");
			});
			commandManager.register(builder);
		}
		
		//Gm
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("gamemode", "gm");
			Map<String, GameMode> values = new HashMap<>();
			values.put("0", GameMode.SURVIVAL);
			values.put("survival", GameMode.SURVIVAL);
			values.put("su", GameMode.SURVIVAL);
			values.put("s", GameMode.SURVIVAL);
			
			values.put("2", GameMode.ADVENTURE);
			values.put("adventure", GameMode.ADVENTURE);
			values.put("a", GameMode.ADVENTURE);
			
			values.put("1", GameMode.CREATIVE);
			values.put("creative", GameMode.CREATIVE);
			values.put("c", GameMode.CREATIVE);
			
			values.put("3", GameMode.SPECTATOR);
			values.put("spectator", GameMode.SPECTATOR);
			values.put("sp", GameMode.SPECTATOR);
			
			StringArgument.Builder gmArgument = (StringArgument.Builder) StringArgument.of("gamemode").tabComplete(
					(context, arg) -> {
						List<String> l = new ArrayList<>();
						Arrays.stream(GameMode.values()).filter(
								v -> v.name().toLowerCase().contains(arg.toLowerCase())).forEach(
								v -> l.add(v.name().toLowerCase()));
						return l;
					});
			
			builder = builder.permission("redfix.command.gamemode").argument(gmArgument, "gamemode").argument(
					PlayerArgument.of("player").optional(null), "player").handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				Player target = (Player) commandContext.getOptional("player").orElseGet(() -> player);
				GameMode gameMode = values.get(commandContext.get("gamemode"));
				if (gameMode == null) {
					sendMessage(player, "Please use a valid gamemode");
					return;
				}
				Bukkit.getScheduler().runTask(RedfixPlugin.getInstance(), () -> target.setGameMode(gameMode));
				sendMessage(target,
						"Switched GameMode to " + gameMode.name().substring(0, 1) + gameMode.name().substring(
								1).toLowerCase());
			});
			commandManager.register(builder);
		}
		
		//PTime
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("ptime");
			builder = builder.permission("redfix.command.ptime").flag(
					FrameworkFlag.of("relative").setDescription("makes the player time relative")).argument(
					IntegerArgument.of("time").optional(), "Time, if none given resets").argument(
					PlayerArgument.of("player").optional()).handler(commandContext -> {
				Player sender = (Player) commandContext.getSender();
				Player player = commandContext.getOrDefault("player", sender);
				if (commandContext.contains("time")) {
					int time = commandContext.get("time");
					player.setPlayerTime(time, commandContext.getFlag("relative"));
					sendMessage(player, "Set player time");
				}
				else {
					player.resetPlayerTime();
					sendMessage(player, "Reset player time");
				}
			});
			commandManager.register(builder);
		}
		
		//Weather
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("weather");
			builder = builder.permission("redfix.command.pweather").argument(
					EnumArgument.of("weather", WeatherType.class).parser(
							(c, a) -> WeatherType.getByName(a)).tabComplete(
							(c, a) -> WeatherType.getAllNames().stream().filter(
									s -> s.startsWith(a.toLowerCase())).toList()), "Weather type").handler(
					commandContext -> {
						Player player = (Player) commandContext.getSender();
						WeatherType weatherType = commandContext.get("weather");
						player.getWorld().setStorm(weatherType != WeatherType.CLEAR);
						player.getWorld().setThundering(weatherType == WeatherType.THUNDER);
					});
			commandManager.register(builder);
		}
		
		//PWeather
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("pweather");
			builder = builder.permission("redfix.command.pweather").argument(
					EnumArgument.of("weather", PlayerWeatherType.class).parser(
							(c, a) -> PlayerWeatherType.getByName(a)).tabComplete(
							(c, a) -> PlayerWeatherType.getAllNames().stream().filter(
									s -> s.startsWith(a.toLowerCase())).toList()).optional(),
					"Weather type, if none given resets").argument(PlayerArgument.of("optional").optional()).handler(
					commandContext -> {
						Player sender = (Player) commandContext.getSender();
						Player player = commandContext.getOrDefault("player", sender);
						if (commandContext.contains("weather")) {
							PlayerWeatherType type = commandContext.getArgument("weather");
							player.setPlayerWeather(type.getBase());
							sendMessage(player, "Set player weather");
						}
						else {
							player.resetPlayerWeather();
							sendMessage(player, "Reset player weather");
						}
					});
			commandManager.register(builder);
		}
		
		//Time
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("time", "rftime");
			builder = builder.permission("redfix.command.time").argument(IntegerArgument.of("time"), "Time").handler(
					commandContext -> {
						Player player = (Player) commandContext.getSender();
						int time = commandContext.get("time");
						player.getWorld().setFullTime(time);
						sendMessage(player, "Set time");
					});
			commandManager.register(builder);
		}
		
		//SetHome
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("sethome");
			builder = builder.permission("redfix.command.sethome").argument(StringArgument.of("name"),
					"Home name").handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				String name = commandContext.getArgument("name");
				addHome(player, name);
				sendMessage(player, "Created Home");
			});
			commandManager.register(builder);
		}
		
		//Home
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("home");
			builder = builder.permission("redfix.command.home").argument(
					StringArgument.of("name").tabComplete((c, a) -> {
						if (!homes.containsKey(((Player) c.getSender()).getUniqueId())) {
							homes.put(((Player) c.getSender()).getUniqueId(), new HashMap<>());
						}
						return homes.get(((Player) c.getSender()).getUniqueId()).keySet().stream().filter(
								s -> s.toLowerCase().startsWith(a.toLowerCase())).toList();
					}), "Home name").handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				String name = commandContext.getArgument("name");
				Home home = homes.get(player.getUniqueId()).get(name);
				if (home == null) {
					sendMessage(player, "Did not find home");
					return;
				}
				addToHistory(player);
				player.teleport(home.pos);
			});
			commandManager.register(builder);
		}
		
		//Homes
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("homes");
			builder = builder.permission("redfix.command.homes").handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				if (!homes.containsKey(player.getUniqueId()) || homes.get(player.getUniqueId()).size() == 0)
					sendMessage(player, "You have no homes");
				else
					sendMessage(player, String.join(", ", homes.get(player.getUniqueId()).keySet()));
			});
			commandManager.register(builder);
		}
		
		//SetWarp
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("setwarp");
			builder = builder.permission("redfix.command.setwarp").argument(StringArgument.of("name"),
					"Warp name").handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				String name = commandContext.getArgument("name");
				addWarp(player, name);
				sendMessage(player, "Created Warp");
			});
			commandManager.register(builder);
		}
		
		//Warp
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("warp");
			builder = builder.permission("redfix.command.warp").argument(StringArgument.of("name").tabComplete(
					(c, a) -> warps.keySet().stream().filter(
							s -> s.toLowerCase().startsWith(a.toLowerCase())).toList()), "Warp name").handler(
					commandContext -> {
						Player player = (Player) commandContext.getSender();
						String name = commandContext.getArgument("name");
						Warp warp = warps.get(name);
						if (warp == null) {
							sendMessage(player, "Did not find warp");
							return;
						}
						addToHistory(player);
						player.teleport(warp.pos);
					});
			commandManager.register(builder);
		}
		
		//Warps
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("warps");
			builder = builder.permission("redfix.command.warps").handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				if (warps.size() == 0)
					sendMessage(player, "There are no warps");
				else
					sendMessage(player, String.join(", ", warps.keySet()));
			});
			commandManager.register(builder);
		}
		
		//Wspeed
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("walkspeed", "wspeed");
			builder = builder.permission("redfix.command.walkspeed").argument(FloatArgument.of("speed"),
					"Walking speed").argument(PlayerArgument.of("optional").optional()).handler(commandContext -> {
				Player sender = (Player) commandContext.getSender();
				Player player = commandContext.getOrDefault("player", sender);
				float speed = commandContext.get("speed");
				AttributeInstance attributeInstance = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
				attributeInstance.getModifiers().stream().filter(am -> am.getName().contentEquals("redfix")).forEach(
						attributeInstance::removeModifier);
				attributeInstance.addModifier(
						new AttributeModifier("redfix", speed - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
				player.setWalkSpeed(0.2f);
				sendMessage(player, "Set walk speed to " + speed);
			});
			commandManager.register(builder);
		}
		
		//Fspeed
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("flyspeed", "fspeed");
			FloatArgument.Builder speedArg = FloatArgument.of("speed").withMin(0).withMax(10);
			builder = builder.permission("redfix.command.flyspeed").argument(speedArg, "Flying speed").argument(
					PlayerArgument.of("optional").optional()).handler(commandContext -> {
				Player sender = (Player) commandContext.getSender();
				Player player = commandContext.getOrDefault("player", sender);
				float speed = commandContext.get("speed");
				player.setFlySpeed(speed / 10);
				sendMessage(player, "Set fly speed to " + speed);
			});
			commandManager.register(builder);
		}
		
		//Speed
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("speed");
			FloatArgument.Builder speedArg = FloatArgument.of("speed").withMin(0).withMax(10);
			builder = builder.permission("redfix.command.speed").argument(speedArg, "Speed").argument(
					PlayerArgument.of("optional").optional()).handler(commandContext -> {
				Player sender = (Player) commandContext.getSender();
				Player player = commandContext.getOrDefault("player", sender);
				float speed = commandContext.get("speed");
				if (player.isFlying()) {
					player.setFlySpeed(speed / 10);
					sendMessage(player, "Set fly speed to " + speed);
				}
				else {
					AttributeInstance attributeInstance = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
					attributeInstance.getModifiers().stream().filter(
							am -> am.getName().contentEquals("redfix")).forEach(attributeInstance::removeModifier);
					attributeInstance.addModifier(
							new AttributeModifier("redfix", speed - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
					player.setWalkSpeed(0.2f);
					sendMessage(player, "Set walk speed to " + speed);
				}
			});
			commandManager.register(builder);
		}
		
		//Distance
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("distance");
			builder = builder.permission("redfix.command.distance").argument(PlayerArgument.of("player"),
					"Player to measure distance to").handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				Player target = commandContext.get("player");
				if (!player.getWorld().getUID().equals(target.getWorld().getUID())) {
					sendMessage(player, "Target is in a different world");
					return;
				}
				Location l1 = player.getLocation();
				Location l2 = target.getLocation();
				Vector v1 = l1.toVector();
				Vector v2 = l2.toVector();
				Vector d = v2.subtract(v1);
				sendMessage(player, String.format("Measuring Distance to Player %s", target.getName()));
				sendMessage(player, String.format("Distance: %3.02f", d.length()));
				sendMessage(player, String.format("Difference: %3.02f %3.02f %3.02f", d.getX(), d.getY(), d.getZ()));
			});
			commandManager.register(builder);
		}
		
		//Enchant
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("enchant", "rfenchant");
			builder = builder.permission("redfix.command.enchant").argument(EnchantmentArgument.of("enchantment"),
					"The Enchantment to apply").argument(IntegerArgument.of("level"), "The Level to apply").handler(
					commandContext -> {
						Player player = (Player) commandContext.getSender();
						Enchantment enchantment = commandContext.get("enchantment");
						int level = commandContext.get("level");
						try {
							if (level < 0) {
								sendMessage(player, "Please use as level at least 0");
								return;
							}
							ItemStack item = player.getInventory().getItemInMainHand();
							if (item.getType() == Material.AIR)
								item = player.getInventory().getItemInOffHand();
							if (item.getType() == Material.AIR) {
								sendMessage(player, "You are not holding any item");
								return;
							}
							item.removeEnchantment(enchantment);
							if (level != 0)
								item.addUnsafeEnchantment(enchantment, level);
							if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
								player.getInventory().setItemInMainHand(item);
							}
							else {
								player.getInventory().setItemInOffHand(item);
							}
							if (level > 0)
								sendMessage(player,
										"Enchanted " + item.getType() + " with " + enchantment + " : " + level);
							else
								sendMessage(player, "Removed " + enchantment + " from " + item.getType());
						} catch (Exception ignored) {
						}
					});
			commandManager.register(builder);
		}
		
		//Give
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("i", "give", "item");
			builder = builder.permission("redfix.command.give").argument(MaterialArgument.of("material"),
					"The Item").argument(IntegerArgument.optional("count", 1), "The Count").handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				Material material = commandContext.get("material");
				int count = commandContext.get("count");
				try {
					for (int i = 0; i < count / material.getMaxStackSize(); i++) {
						player.getInventory().addItem(new ItemStack(material, material.getMaxStackSize()));
					}
					player.getInventory().addItem(new ItemStack(material, count % material.getMaxStackSize()));
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			commandManager.register(builder);
		}
		
		//Playtime
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder("playtime");
			builder = builder.permission("redfix.command.playtime").argument(
					OfflinePlayerArgument.of("player").optional()).handler(commandContext -> {
				CommandSender sender = commandContext.getSender();
				OfflinePlayer target;
				try {
					target = commandContext.getOrSupplyDefault("player", () -> (Player) sender);
				} catch (ClassCastException e) {
					sendMessage(sender, "You are not a player");
					return;
				}
				int playedTicks = target.getStatistic(Statistic.PLAY_ONE_MINUTE);
				int seconds = playedTicks / 20;
				int minutes = seconds / 60;
				int hours = minutes / 60;
				int days = hours / 24;
				sendMessage(sender,
						String.format("Play Time: %02d days %02d h %02d m %02d s", days, hours % 24, minutes % 60,
								seconds % 60));
			});
			commandManager.register(builder);
		}
		//PlaytimeTop
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder("playtimetop");
			builder = builder.permission("redfix.command.playtimetop").handler(commandContext -> {
				CommandSender sender = commandContext.getSender();
				List<OfflinePlayer> players = new ArrayList<>(Arrays.stream(Bukkit.getOfflinePlayers()).toList());
				players.sort((p2, p1) -> p1.getStatistic(Statistic.PLAY_ONE_MINUTE) - p2.getStatistic(
						Statistic.PLAY_ONE_MINUTE));
				sendMessage(sender, "Top PlayTime:");
				for (int i = 0; i < Math.min(10, players.size()); i++) {
					int playedTicks = players.get(i).getStatistic(Statistic.PLAY_ONE_MINUTE);
					int seconds = playedTicks / 20;
					int minutes = seconds / 60;
					int hours = minutes / 60;
					int days = hours / 24;
					sender.sendMessage("§6" + (i + 1) + ": §3" + players.get(i).getName() + " §a- §6" + String.format(
							"%02d days %02d h %02d m %02d s", days, hours % 24, minutes % 60, seconds % 60));
				}
			});
			commandManager.register(builder);
		}
		
		//Repair
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("repair");
			builder = builder.permission("redfix.command.repair").flag(
					FrameworkFlag.of("all").setDescription("Repairs all your items")).handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				if (commandContext.getFlag("all")) {
					try {
						for (int i = 0; i < player.getInventory().getSize(); i++) {
							ItemStack item = player.getInventory().getItem(i);
							if (item == null)
								continue;
							if (item.getType() == Material.AIR)
								continue;
							if (item.getItemMeta() instanceof Damageable meta) {
								meta.setDamage(0);
								item.setItemMeta(meta);
								player.getInventory().setItem(i, item);
							}
						}
					} catch (Exception ignored) {
					}
					sendMessage(player, "Repaired all");
				}
				else {
					try {
						ItemStack item = player.getInventory().getItemInMainHand();
						if (item.getType() == Material.AIR) {
							item = player.getInventory().getItemInOffHand();
							if (item.getType() == Material.AIR) {
								sendMessage(player, "You are not holding any item");
								return;
							}
							if (!(item.getItemMeta() instanceof Damageable)) {
								sendMessage(player, "You are not holding any damageable item");
								return;
							}
							Damageable meta = (Damageable) item.getItemMeta();
							meta.setDamage(0);
							item.setItemMeta(meta);
							player.getInventory().setItemInOffHand(item);
						}
						else {
							if (!(item.getItemMeta() instanceof Damageable)) {
								item = player.getInventory().getItemInOffHand();
								if (item.getType() == Material.AIR) {
									sendMessage(player, "You are not holding any damageable item");
									return;
								}
								if (!(item.getItemMeta() instanceof Damageable)) {
									sendMessage(player, "You are not holding any damageable item");
									return;
								}
								Damageable meta = (Damageable) item.getItemMeta();
								meta.setDamage(0);
								item.setItemMeta(meta);
								player.getInventory().setItemInOffHand(item);
							}
							else {
								Damageable meta = (Damageable) item.getItemMeta();
								meta.setDamage(0);
								item.setItemMeta(meta);
								player.getInventory().setItemInMainHand(item);
							}
						}
						sendMessage(player, "Repaired " + item.getType());
					} catch (Exception ignored) {
					}
				}
			});
			commandManager.register(builder);
		}
		
		//Unbreakable
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("unbreakable");
			builder = builder.permission("redfix.command.unbreakable").handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				try {
					ItemStack item = player.getInventory().getItemInMainHand();
					if (item.getType() == Material.AIR)
						item = player.getInventory().getItemInOffHand();
					if (item.getType() == Material.AIR) {
						sendMessage(player, "You are not holding any item");
						return;
					}
					ItemMeta meta = item.getItemMeta();
					meta.setUnbreakable(true);
					item.setItemMeta(meta);
					if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
						player.getInventory().setItemInMainHand(item);
					}
					else {
						player.getInventory().setItemInOffHand(item);
					}
					sendMessage(player, "Made " + item.getType() + " unbreakable");
				} catch (Exception ignored) {
				}
			});
			commandManager.register(builder);
		}
		
		//AddLore
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("addlore");
			builder = builder.permission("redfix.command.addlore").argument(StringArrayArgument.of("lore")).handler(
					commandContext -> {
						Player player = (Player) commandContext.getSender();
						try {
							ItemStack item = player.getInventory().getItemInMainHand();
							if (item.getType() == Material.AIR)
								item = player.getInventory().getItemInOffHand();
							if (item.getType() == Material.AIR) {
								sendMessage(player, "You are not holding any item");
								return;
							}
							ItemMeta meta = item.getItemMeta();
							List<String> lore = new ArrayList<>();
							String[] l0 = commandContext.get("lore");
							String l = Arrays.stream(l0).collect(StringBuilder::new, (sb, s) -> {
								sb.append(s);
								sb.append(" ");
							}, StringBuilder::append).toString().replaceAll("&&", "&§§").replaceAll("&([0-9a-fkomnrl])",
									"§$1").replaceAll("&§§", "&");
							lore.addAll(Objects.requireNonNullElse(meta.getLore(), new ArrayList<>()));
							lore.add(l);
							meta.setLore(lore);
							item.setItemMeta(meta);
							if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
								player.getInventory().setItemInMainHand(item);
							}
							else {
								player.getInventory().setItemInOffHand(item);
							}
							sendMessage(player, "Added lore to " + item.getType());
						} catch (Exception ignored) {
						}
					});
			commandManager.register(builder);
		}
		
		//SetItemName
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("setitemname");
			builder = builder.permission("redfix.command.setitemname").argument(
					StringArrayArgument.of("displayname")).handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				try {
					ItemStack item = player.getInventory().getItemInMainHand();
					if (item.getType() == Material.AIR)
						item = player.getInventory().getItemInOffHand();
					if (item.getType() == Material.AIR) {
						sendMessage(player, "You are not holding any item");
						return;
					}
					ItemMeta meta = item.getItemMeta();
					List<String> lore = new ArrayList<>();
					String[] n0 = commandContext.get("displayname");
					String n = Arrays.stream(n0).collect(StringBuilder::new, (sb, s) -> {
						sb.append(s);
						sb.append(" ");
					}, StringBuilder::append).toString().replaceAll("&&", "&§§").replaceAll("&([0-9a-fkomnrl])",
							"§$1").replaceAll("&§§", "&");
					meta.setDisplayName(n);
					item.setItemMeta(meta);
					if (player.getInventory().getItemInMainHand().getType() != Material.AIR) {
						player.getInventory().setItemInMainHand(item);
					}
					else {
						player.getInventory().setItemInOffHand(item);
					}
					sendMessage(player, "Added lore to " + item.getType());
				} catch (Exception ignored) {
				}
			});
			commandManager.register(builder);
		}
		
		//Craft
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("craft");
			builder = builder.permission("redfix.command.craft").handler(commandContext -> {
				Bukkit.getScheduler().runTask(this, () -> {
					Player player = (Player) commandContext.getSender();
					player.openWorkbench(null, true);
				});
			});
			commandManager.register(builder);
		}
		
		//Anvil
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("anvil");
			builder = builder.permission("redfix.command.anvil").handler(commandContext -> {
				Bukkit.getScheduler().runTask(this, () -> {
					Player player = (Player) commandContext.getSender();
					player.openInventory(Bukkit.createInventory(player, InventoryType.ANVIL));
				});
			});
			commandManager.register(builder);
		}
		
		//Grindstone
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("grindstone");
			builder = builder.permission("redfix.command.grindstone").handler(commandContext -> {
				Bukkit.getScheduler().runTask(this, () -> {
					Player player = (Player) commandContext.getSender();
					player.openInventory(Bukkit.createInventory(player, InventoryType.GRINDSTONE));
				});
			});
			commandManager.register(builder);
		}
		
		//Stonecutter
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("stonecutter");
			builder = builder.permission("redfix.command.stonecutter").handler(commandContext -> {
				Bukkit.getScheduler().runTask(this, () -> {
					Player player = (Player) commandContext.getSender();
					player.openInventory(Bukkit.createInventory(player, InventoryType.STONECUTTER));
				});
			});
			commandManager.register(builder);
		}
		
		//Loom
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("loom");
			builder = builder.permission("redfix.command.loom").handler(commandContext -> {
				Bukkit.getScheduler().runTask(this, () -> {
					Player player = (Player) commandContext.getSender();
					player.openInventory(Bukkit.createInventory(player, InventoryType.LOOM));
				});
			});
			commandManager.register(builder);
		}
		
		//Cartography
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("cartography");
			builder = builder.permission("redfix.command.cartography").handler(commandContext -> {
				Bukkit.getScheduler().runTask(this, () -> {
					Player player = (Player) commandContext.getSender();
					player.openInventory(Bukkit.createInventory(player, InventoryType.CARTOGRAPHY));
				});
			});
			commandManager.register(builder);
		}
		
		//Smithing
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("smithing");
			builder = builder.permission("redfix.command.smithing").handler(commandContext -> {
				Bukkit.getScheduler().runTask(this, () -> {
					Player player = (Player) commandContext.getSender();
					player.openInventory(Bukkit.createInventory(player, InventoryType.SMITHING));
				});
			});
			commandManager.register(builder);
		}
		
		//Ec
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("ec");
			builder = builder.permission("redfix.command.ec").argument(PlayerArgument.of("target").optional()).handler(
					commandContext -> {
						Bukkit.getScheduler().runTask(this, () -> {
							Player player = (Player) commandContext.getSender();
							Player target = commandContext.getOrDefault("target", player);
							player.openInventory(target.getEnderChest());
						});
					});
			commandManager.register(builder);
		}
		
		//Invsee
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("invsee");
			builder = builder.permission("redfix.command.invsee").argument(PlayerArgument.of("target")).handler(
					commandContext -> {
						Bukkit.getScheduler().runTask(this, () -> {
							Player player = (Player) commandContext.getSender();
							Player target = commandContext.get("target");
							player.openInventory(target.getInventory());
						});
					});
			commandManager.register(builder);
		}
		
		//BetaInvsee
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("betainvsee");
			builder = builder.permission("redfix.command.invsee").argument(PlayerArgument.of("target")).handler(
					commandContext -> {
						Bukkit.getScheduler().runTask(this, () -> {
							Player player = (Player) commandContext.getSender();
							Player target = commandContext.get("target");
							new InvSee(player, target);
						});
					});
			commandManager.register(builder);
		}
		
		//Afk
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("afk");
			builder = builder.permission("redfix.command.afk").argument(PlayerArgument.of("target").optional()).handler(
					commandContext -> {
						Player player = (Player) commandContext.getSender();
						Player target = commandContext.getOrDefault("target", player);
						if (Afk.isAfk(target.getUniqueId())) {
							Afk.afkTimes.put(target.getUniqueId(), System.currentTimeMillis());
						}
						else {
							Afk.afkTimes.put(target.getUniqueId(), System.currentTimeMillis() - 1000 * 60 * 5);
						}
					});
			commandManager.register(builder);
		}
		
		//List
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder("list", "ls");
			builder = builder.permission("redfix.command.list").handler(commandContext -> {
				CommandSender sender = commandContext.getSender();
				List<? extends Player> players = Bukkit.getOnlinePlayers().stream().toList();
				for (String groupName : vaultChat.getGroups()) {
					List<String> pl = players.stream().filter(
							p -> vaultChat.getPrimaryGroup(p).contentEquals(groupName)).map(
							p -> (Afk.isAfk(p.getUniqueId()) ? "§7[AFK] §f" : "§f") + vaultChat.getPlayerPrefix(
									p) + p.getDisplayName() + vaultChat.getPlayerSuffix(p) + "§f").map(
							s -> s.replaceAll("&&", "&§§").replaceAll("&([0-9a-fkomnrl])", "§$1").replaceAll("&§§",
									"&")).toList();
					if (pl.size() > 0) {
						sender.sendMessage("§6" + groupName + ": " + String.join(", ", pl));
					}
				}
			});
			commandManager.register(builder);
		}
		
		//SpawnMob
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("spawnmob");
			builder = builder.permission("redfix.command.spawnmob").argument(EntityTypeArgument.of("entity"),
					"The Entity to spawn").argument(IntegerArgument.optional("count", 1)).handler(commandContext -> {
				try {
					Player player = (Player) commandContext.getSender();
					EntityType type = commandContext.get("entity");
					int count = commandContext.get("count");
					Bukkit.getScheduler().runTask(RedfixPlugin.getInstance(), () -> {
								/*RayTraceResult result = player.getWorld().rayTrace(player.getEyeLocation(),
										player.getEyeLocation().getDirection(), 50, FluidCollisionMode.SOURCE_ONLY,
										true, 0, Predicates.alwaysFalse());*/
						RayTraceResult result = player.rayTraceBlocks(50);
						if (result == null) {
							for (int i = 0; i < count; i++) {
								player.getWorld().spawnEntity(player.getLocation(), type);
							}
						}
						else {
							Location pos = new Location(player.getWorld(), result.getHitPosition().getX(),
									result.getHitPosition().getY(), result.getHitPosition().getZ());
							for (int i = 0; i < count; i++) {
								player.getWorld().spawnEntity(pos, type);
							}
						}
					});
				} catch (Exception ignored) {
				}
			});
			commandManager.register(builder);
		}
		
		//Smite
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("smite");
			builder = builder.permission("redfix.command.smite").flag(
					FrameworkFlag.of("effect").setDescription("Only the animation without damage")).handler(
					commandContext -> {
						try {
							Player player = (Player) commandContext.getSender();
							boolean effect = commandContext.getFlag("effect");
							RayTraceResult result = player.getWorld().rayTrace(player.getEyeLocation(),
									player.getEyeLocation().getDirection(), 50, FluidCollisionMode.SOURCE_ONLY, true, 0,
									e -> e != player);
							Location loc = null;
							if (result != null) {
								loc = result.getHitPosition().toLocation(player.getWorld());
								if (effect)
									player.getWorld().strikeLightningEffect(loc);
								else
									player.getWorld().strikeLightning(loc);
							}
						} catch (Exception ignored) {
						}
					});
			commandManager.register(builder);
		}
		
		//EditSign
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("editsign");
			builder = builder.permission("redfix.command.editsign").argument(IntegerArgument.of("line")).argument(
					StringArrayArgument.of("content").optional(new String[0])).handler(commandContext -> {
				try {
					Player player = (Player) commandContext.getSender();
					int line = commandContext.get("line");
					String[] v0 = commandContext.get("content");
					String v = Arrays.stream(v0).collect(StringBuilder::new, (sb, s) -> {
						sb.append(s);
						sb.append(" ");
					}, StringBuilder::append).toString().replaceAll("&&", "&§§").replaceAll("&([0-9a-fkomnrl])",
							"§$1").replaceAll("&§§", "&");
					RayTraceResult result = player.getWorld().rayTrace(player.getEyeLocation(),
							player.getEyeLocation().getDirection(), 50, FluidCollisionMode.NEVER, true, 0,
							Predicates.alwaysFalse());
					if (result != null && result.getHitBlock() != null) {
						Block block = result.getHitBlock();
						if (block instanceof Sign) {
							Sign sign = (Sign) block;
							sign.setLine(line - 1, v);
						}
						else
							sendMessage(player, "You are not looking at a sign");
					}
					else
						sendMessage(player, "You are not looking at any block");
				} catch (Exception ignored) {
				}
			});
			commandManager.register(builder);
		}
		
		//ToggleSignGlowing
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("togglesignglowing");
			builder = builder.permission("redfix.command.editsign").handler(commandContext -> {
				try {
					Player player = (Player) commandContext.getSender();
					RayTraceResult result = player.getWorld().rayTrace(player.getEyeLocation(),
							player.getEyeLocation().getDirection(), 50, FluidCollisionMode.NEVER, true, 0,
							Predicates.alwaysFalse());
					if (result != null && result.getHitBlock() != null) {
						Block block = result.getHitBlock();
						if (block instanceof Sign) {
							Sign sign = (Sign) block;
							sign.setGlowingText(!sign.isGlowingText());
						}
						else
							sendMessage(player, "You are not looking at a sign");
					}
					else
						sendMessage(player, "You are not looking at any block");
				} catch (Exception ignored) {
				}
			});
			commandManager.register(builder);
		}
		
		//CommandSpy
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("commandspy");
			builder = builder.permission("redfix.command.commandspy").handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				if (commandSpy.players.contains(player.getUniqueId())) {
					commandSpy.players.remove(player.getUniqueId());
					player.sendMessage(
							CommandSpy.Messages.get(CommandSpy.Messages.PREFIX, CommandSpy.Messages.COMMAND_DISABLE));
				}
				else {
					if (!player.hasPermission("redfix.command.commandspy")) {
						player.sendMessage(CommandSpy.Messages.get(
								CommandSpy.Messages.PREFIX) + "I'm sorry, but you don't have the permission");
						return;
					}
					commandSpy.players.add(player.getUniqueId());
					player.sendMessage(
							CommandSpy.Messages.get(CommandSpy.Messages.PREFIX, CommandSpy.Messages.COMMAND_ENABLE));
				}
				commandSpy.save();
			});
			commandManager.register(builder);
		}
		
		//Effect
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder("effect");
			builder = builder.permission("redfix.command.effect").argument(PlayerArgument.of("player")).argument(
					EffectArgument.of("effect")).argument(IntegerArgument.optional("duration", 30)).argument(
					IntegerArgument.optional("level", 0)).handler(commandContext -> {
				try {
					Player player = commandContext.get("player");
					PotionEffectType effectType = commandContext.get("effect");
					int duration = commandContext.get("duration");
					int level = commandContext.get("level");
					Bukkit.getScheduler().runTask(RedfixPlugin.getInstance(),
							() -> player.addPotionEffect(new PotionEffect(effectType, duration * 20, level)));
				} catch (Exception e) {
					e.printStackTrace();
				}
			});
			commandManager.register(builder);
		}
		
		//Item Attribute
		{
			FrameworkCommand.Builder<CommandSender> topBuilder = FrameworkCommand.commandBuilder(
					"itemattribute").permission("redfix.command.itemattribute");
			StringArgument.Builder attributeArgument = (StringArgument.Builder) StringArgument.of(
					"attribute").tabComplete((context, arg) -> {
				List<String> l = new ArrayList<>();
				Arrays.stream(Attribute.values()).filter(et -> et.name().replaceAll("\\W", "").toLowerCase().contains(
						arg.replaceAll("\\W", "").toLowerCase())).forEach(et -> l.add(et.name()));
				return l;
			});
			StringArgument.Builder operationArgument = (StringArgument.Builder) StringArgument.of(
					"operation").tabComplete((context, arg) -> {
				List<String> l = new ArrayList<>();
				Arrays.stream(AttributeModifier.Operation.values()).filter(
						et -> et.name().replaceAll("\\W", "").toLowerCase().contains(
								arg.replaceAll("\\W", "").toLowerCase())).forEach(et -> l.add(et.name()));
				return l;
			});
			StringArgument.Builder slotArgument = (StringArgument.Builder) StringArgument.of("slot").tabComplete(
					(context, arg) -> {
						List<String> l = new ArrayList<>();
						Arrays.stream(EquipmentSlot.values()).filter(
								et -> et.name().replaceAll("\\W", "").toLowerCase().contains(
										arg.replaceAll("\\W", "").toLowerCase())).forEach(et -> l.add(et.name()));
						return l;
					}).optional("");
			StringArgument.Builder uuidArgument = (StringArgument.Builder) StringArgument.of("uuid").tabComplete(
					(context, arg) -> {
						List<String> l = new ArrayList<>();
						Player player = (Player) context.getSender();
						ItemStack item;
						if (player.getInventory().getItemInMainHand().getType() == Material.AIR) {
							if (player.getInventory().getItemInOffHand().getType() == Material.AIR) {
								return List.of("You are not holding any item");
							}
							else
								item = player.getInventory().getItemInOffHand();
						}
						else
							item = player.getInventory().getItemInMainHand();
						ItemMeta meta = item.getItemMeta();
						if (!meta.hasAttributeModifiers())
							return List.of();
						for (AttributeModifier modifier : meta.getAttributeModifiers().values()) {
							l.add(modifier.getUniqueId().toString());
						}
						return l;
					});
			FrameworkCommand.Builder<Player> addBuilder = topBuilder.subPlayerCommand("add").argument(
					attributeArgument).argument(operationArgument).argument(FloatArgument.of("amount")).argument(
					slotArgument).handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				try {
					Attribute attribute = Attribute.valueOf((String) commandContext.get("attribute"));
					AttributeModifier.Operation operation = AttributeModifier.Operation.valueOf(
							(String) commandContext.get("operation"));
					String slotString = (String) commandContext.getOrDefault("slot", "");
					float amount = (float) commandContext.get("amount");
					
					AttributeModifier modifier;
					try {
						EquipmentSlot slot = EquipmentSlot.valueOf(slotString);
						modifier = new AttributeModifier(UUID.randomUUID(), "", amount, operation, slot);
					} catch (IllegalArgumentException ignore) {
						modifier = new AttributeModifier("", amount, operation);
					}
					
					ItemStack item = player.getInventory().getItemInMainHand();
					if (item.getType() == Material.AIR) {
						item = player.getInventory().getItemInOffHand();
						if (item.getType() == Material.AIR) {
							sendMessage(player, "You are not holding any item");
							return;
						}
						ItemMeta meta = item.getItemMeta();
						meta.addAttributeModifier(attribute, modifier);
						item.setItemMeta(meta);
						player.getInventory().setItemInOffHand(item);
					}
					else {
						Damageable meta = (Damageable) item.getItemMeta();
						meta.addAttributeModifier(attribute, modifier);
						item.setItemMeta(meta);
						player.getInventory().setItemInMainHand(item);
					}
					sendMessage(player, "Added attribute modifier " + modifier + " to " + item.getType());
				} catch (Exception ignore) {
				}
			});
			FrameworkCommand.Builder<Player> removeBuilder = topBuilder.subPlayerCommand("remove").argument(
					uuidArgument).handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				try {
					UUID uuid = UUID.fromString((String) commandContext.get("uuid"));
					
					Map.Entry<Attribute, AttributeModifier> entry;
					
					ItemStack item = player.getInventory().getItemInMainHand();
					if (item.getType() == Material.AIR) {
						item = player.getInventory().getItemInOffHand();
						if (item.getType() == Material.AIR) {
							sendMessage(player, "You are not holding any item");
							return;
						}
						ItemMeta meta = item.getItemMeta();
						entry = meta.getAttributeModifiers().entries().stream().filter(
								e -> e.getValue().getUniqueId().equals(uuid)).findFirst().get();
						meta.removeAttributeModifier(entry.getKey(), entry.getValue());
						item.setItemMeta(meta);
						player.getInventory().setItemInOffHand(item);
					}
					else {
						Damageable meta = (Damageable) item.getItemMeta();
						entry = meta.getAttributeModifiers().entries().stream().filter(
								e -> e.getValue().getUniqueId().equals(uuid)).findFirst().get();
						meta.removeAttributeModifier(entry.getKey(), entry.getValue());
						item.setItemMeta(meta);
						player.getInventory().setItemInMainHand(item);
					}
					sendMessage(player,
							"Removed attribute modifier " + entry.getValue() + " (" + entry.getKey() + ") from " + item.getType());
				} catch (Exception ignore) {
				}
			});
			
			commandManager.register(addBuilder);
			commandManager.register(removeBuilder);
		}
		
		//Bal
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("balance", "bal", "money");
			builder = builder.permission("redfix.command.balance").argument(
					OfflinePlayerArgument.of("player").optional(), "player").handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				OfflinePlayer target = commandContext.getOrDefault("player", player);
				sendMessage(player, "§aBalance of " + target.getName() + ": " + EconomyManager.getMoney(
						target.getUniqueId()) + getConfig().getString("economy.symbol", "$"));
			});
			commandManager.register(builder);
		}
		
		//Baltop
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder("balancetop", "baltop");
			builder = builder.permission("redfix.command.balancetop").handler(commandContext -> {
				CommandSender sender = commandContext.getSender();
				List<UUID> players = new ArrayList<>(
						Arrays.stream(Bukkit.getOfflinePlayers()).map(OfflinePlayer::getUniqueId).toList());
				players.sort((p2, p1) -> Double.compare(EconomyManager.getMoney(p1), EconomyManager.getMoney(p2)));
				sendMessage(sender, "Top Balance:");
				for (int i = 0; i < Math.min(10, players.size()); i++) {
					sender.sendMessage("§6" + (i + 1) + ": §3" + Bukkit.getOfflinePlayer(
							players.get(i)).getName() + " §a- §6" + vaultEconomy.format(
							EconomyManager.getMoney(players.get(i))));
				}
			});
			commandManager.register(builder);
		}
		
		//Economy
		{
			FrameworkCommand.Builder<CommandSender> topBuilder = FrameworkCommand.commandBuilder("economy", "eco");
			
			FrameworkCommand.Builder<CommandSender> setBuilder = topBuilder.subCommand("set").permission(
					"redfix.command.economy.set").argument(OfflinePlayerArgument.of("player")).argument(
					DoubleArgument.of("amount")).handler(commandContext -> {
				Player sender = (Player) commandContext.getSender();
				OfflinePlayer player = commandContext.get("player");
				double amount = commandContext.get("amount");
				EconomyManager.setMoney(player.getUniqueId(), amount);
				sendMessage(sender, "Set money of player " + player.getName() + " to " + amount + getConfig().getString(
						"economy.symbol", "$"));
				saveEco();
			});
			FrameworkCommand.Builder<CommandSender> giveBuilder = topBuilder.subCommand("give").permission(
					"redfix.command.economy.give").argument(OfflinePlayerArgument.of("player")).argument(
					DoubleArgument.of("amount")).handler(commandContext -> {
				Player sender = (Player) commandContext.getSender();
				OfflinePlayer player = commandContext.get("player");
				double amount = commandContext.get("amount");
				EconomyManager.addMoney(player.getUniqueId(), amount);
				sendMessage(sender,
						"Gave player " + player.getName() + " " + amount + getConfig().getString("economy.symbol",
								"$"));
				saveEco();
			});
			FrameworkCommand.Builder<CommandSender> takeBuilder = topBuilder.subCommand("take").permission(
					"redfix.command.economy.take").argument(OfflinePlayerArgument.of("player")).argument(
					DoubleArgument.of("amount")).handler(commandContext -> {
				Player sender = (Player) commandContext.getSender();
				OfflinePlayer player = commandContext.get("player");
				double amount = commandContext.get("amount");
				EconomyManager.addMoney(player.getUniqueId(), -amount);
				sendMessage(sender, "Took " + +amount + getConfig().getString("economy.symbol",
						"$") + " from player " + player.getName());
				saveEco();
			});
			FrameworkCommand.Builder<CommandSender> resetBuilder = topBuilder.subCommand("reset").permission(
					"redfix.command.economy.reset").argument(OfflinePlayerArgument.of("player")).handler(
					commandContext -> {
						Player sender = (Player) commandContext.getSender();
						OfflinePlayer player = commandContext.get("player");
						EconomyManager.setMoney(player.getUniqueId(), getConfig().getDouble("economy.startMoney", 100));
						sendMessage(sender, "Reset player's " + player.getName() + " money");
						saveEco();
					});
			
			commandManager.register(setBuilder);
			commandManager.register(giveBuilder);
			commandManager.register(takeBuilder);
			commandManager.register(resetBuilder);
		}
		
		//Pay
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("pay").argument(
					OfflinePlayerArgument.of("player")).argument(DoubleArgument.of("amount")).handler(
					commandContext -> {
						Player sender = (Player) commandContext.getSender();
						OfflinePlayer player = commandContext.get("player");
						double amount = commandContext.get("amount");
						if (!sender.hasPermission("redfix.command.pay.offline") && !player.isOnline()) {
							sendMessage(sender, "§4You can't pay offline Players");
						}
						if (!sender.hasPermission("redfix.command.pay.offline"))
							if (!sender.hasPermission(
									"redfix.command.pay.ignoredistance") && player.getPlayer().getLocation().distance(
									sender.getLocation()) > this.getConfig().getDouble("pay.distance", 50)) {
								sendMessage(sender, "§4You can't pay Players out of your range");
							}
						if (EconomyManager.getMoney(sender.getUniqueId()) < amount) {
							sendMessage(sender, "§4You have not enough money");
							return;
						}
						EconomyManager.addMoney(player.getUniqueId(), amount);
						EconomyManager.addMoney(sender.getUniqueId(), -amount);
						sendMessage(sender, "§bPayed §a" + amount + getConfig().getString("economy.symbol",
								"$") + "§b to §6" + player.getName());
						sendMessage(sender, "§bYou got §a" + amount + getConfig().getString("economy.symbol",
								"$") + "§b from §6" + sender.getDisplayName());
						saveEco();
					});
			
			commandManager.register(builder);
		}
		
		//Broadcast
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder("broadcast",
					"bc").permission("redfix.command.broadcast").argument(StringArrayArgument.of("message")).handler(
					commandContext -> {
						String[] msg = commandContext.get("message");
						String message = Arrays.stream(msg).collect(StringBuilder::new, (sb, s) -> {
							sb.append(s);
							sb.append(" ");
						}, StringBuilder::append).toString().replaceAll("&&", "&§§").replaceAll("&([0-9a-fkomnrl])",
								"§$1").replaceAll("&§§", "&");
						Bukkit.broadcastMessage("§6[§4Broadcast§6] §a" + message);
					});
			
			commandManager.register(builder);
		}
		
		//Tp
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("tp").permission(
					"redfix.command.tp.toplayer").argument(PlayerArgument.of("player")).argument(
					PlayerArgument.of("target").optional()).handler(commandContext -> {
				Player sender = (Player) commandContext.getSender();
				Player player = commandContext.get("player");
				Player target = commandContext.getOrDefault("target", sender);
				addToHistory(target);
				Bukkit.getScheduler().runTask(this, () -> target.teleport(player));
			});
			commandManager.register(builder);
		}
		
		//TpHere
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("tphere").permission(
					"redfix.command.tp.here").argument(PlayerArgument.of("target")).handler(commandContext -> {
				Player sender = (Player) commandContext.getSender();
				Player target = commandContext.get("target");
				addToHistory(target);
				Bukkit.getScheduler().runTask(this, () -> target.teleport(sender));
			});
			commandManager.register(builder);
		}
		
		//TpAll
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("tpall").permission(
					"redfix.command.tp.all").argument(PlayerArgument.of("player").optional()).handler(
					commandContext -> {
						Player sender = (Player) commandContext.getSender();
						Player player = commandContext.getOrDefault("player", sender);
						Bukkit.getScheduler().runTask(this, () -> Bukkit.getOnlinePlayers().forEach(p -> {
							p.teleport(player);
							addToHistory(p);
						}));
					});
			commandManager.register(builder);
		}
		
		//TpPos
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("tppos").permission(
					"redfix.command.tp.pos").argument(DoubleArgument.of("x").tabComplete(
					(c, a) -> List.of(Integer.toString(((Player) c.getSender()).getLocation().getBlockX())))).argument(
					DoubleArgument.of("y").tabComplete((c, a) -> List.of(
							Integer.toString(((Player) c.getSender()).getLocation().getBlockY())))).argument(
					DoubleArgument.of("z").tabComplete((c, a) -> List.of(
							Integer.toString(((Player) c.getSender()).getLocation().getBlockZ())))).argument(
					WorldArgument.of("world").optional()).argument(PlayerArgument.of("target").optional()).handler(
					commandContext -> {
						Player sender = (Player) commandContext.getSender();
						Player target = commandContext.getOrDefault("target", sender);
						World world = commandContext.getOrDefault("world", sender.getWorld());
						addToHistory(target);
						double x = commandContext.get("x");
						double y = commandContext.get("y");
						double z = commandContext.get("z");
						Bukkit.getScheduler().runTask(this, () -> target.teleport(new Location(world, x, y, z)));
					});
			commandManager.register(builder);
		}
		
		//Back
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("back").permission(
					"redfix.command.tp.back").argument(PlayerArgument.of("target").optional()).handler(
					commandContext -> {
						Player sender = (Player) commandContext.getSender();
						Player target = commandContext.getOrDefault("target", sender);
						Bukkit.getScheduler().runTask(this, () -> pollHistory(target, sender));
					});
			commandManager.register(builder);
		}
		
		//DBack
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("dback").permission(
					"redfix.command.tp.dback").argument(PlayerArgument.of("target").optional()).handler(
					commandContext -> {
						Player sender = (Player) commandContext.getSender();
						Player target = commandContext.getOrDefault("target", sender);
						Bukkit.getScheduler().runTask(this, () -> pollDeath(target, sender));
					});
			commandManager.register(builder);
		}
		
		//Kick
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder("kick").permission(
					"redfix.command.kick").argument(PlayerArgument.of("player")).argument(
					StringArrayArgument.of("message").optional()).handler(commandContext -> {
				Player player = commandContext.get("player");
				String[] smsg = commandContext.getOrDefault("message", new String[0]);
				String message0 = String.join(" ", smsg);
				String message = message0.replaceAll("&", "§");
				Bukkit.getScheduler().runTask(this, () -> player.kickPlayer(message));
			});
			commandManager.register(builder);
		}
		
		//Ban
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder("ban").permission(
					"redfix.command.ban").argument(OfflinePlayerArgument.of("player")).argument(
					StringArrayArgument.of("message").optional()).handler(commandContext -> {
				OfflinePlayer player = commandContext.get("player");
				String[] smsg = commandContext.getOrDefault("message", new String[0]);
				String message0 = String.join(" ", smsg);
				String message = message0.replaceAll("&", "§");
				Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), message, null, null).save();
				if (player.isOnline())
					Bukkit.getScheduler().runTask(this, () -> player.getPlayer().kickPlayer(message));
			});
			commandManager.register(builder);
		}
		
		//TempBan
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder("tempban").permission(
					"redfix.command.tempban").argument(OfflinePlayerArgument.of("player")).argument(
					IntegerArgument.of("duration")).argument(StringArrayArgument.of("message").optional()).handler(
					commandContext -> {
						OfflinePlayer player = commandContext.get("player");
						int minutes = commandContext.get("duration");
						String[] smsg = commandContext.getOrDefault("message", new String[0]);
						String message0 = String.join(" ", smsg);
						String message = message0.replaceAll("&", "§");
						Bukkit.getBanList(BanList.Type.NAME).addBan(player.getName(), message,
								Date.from(Instant.now().plus(minutes, ChronoUnit.MINUTES)), null).save();
						if (player.isOnline())
							Bukkit.getScheduler().runTask(this, () -> player.getPlayer().kickPlayer(message));
					});
			commandManager.register(builder);
		}
		
		//Mute
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder("mute").permission(
					"redfix.command.mute").argument(PlayerArgument.of("player")).handler(commandContext -> {
				Player player = commandContext.get("player");
				muted.put(player.getUniqueId(), Long.MAX_VALUE);
			});
			commandManager.register(builder);
		}
		
		//TempMute
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder("tempmute").permission(
					"redfix.command.mute").argument(PlayerArgument.of("player")).argument(
					IntegerArgument.of("duration")).handler(commandContext -> {
				Player player = commandContext.get("player");
				int minutes = commandContext.get("duration");
				muted.put(player.getUniqueId(), System.currentTimeMillis() + minutes * 60 * 1000);
			});
			commandManager.register(builder);
		}
		
		//UnMute
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder("unmute").permission(
					"redfix.command.unmute").argument(PlayerArgument.of("player")).handler(commandContext -> {
				Player player = commandContext.get("player");
				muted.remove(player.getUniqueId());
			});
			commandManager.register(builder);
		}
		
		//Roll
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder("roll").permission(
					"redfix.command.roll").argument(IntegerArgument.optional("maxValue", 100)).handler(
					commandContext -> {
						int maxValue = commandContext.get("maxValue");
						sendMessage(commandContext.getSender(), "Roll: " + (new Random().nextInt(maxValue) + 1));
					});
			commandManager.register(builder);
		}
		
		//Msg
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("msg").permission(
					"redfix.command.msg").argument(PlayerArgument.of("player")).argument(
					StringArrayArgument.of("message")).handler(commandContext -> {
				String[] msg = commandContext.get("message");
				Player player = commandContext.get("player");
				Player sender = (Player) commandContext.getSender();
				String message = Arrays.stream(msg).collect(StringBuilder::new, (sb, s) -> {
					sb.append(s);
					sb.append(" ");
				}, StringBuilder::append).toString();
				lastMessaged.put(player.getUniqueId(), sender.getUniqueId());
				lastMessaged.put(sender.getUniqueId(), player.getUniqueId());
				if (vaultChat != null) {
					player.sendMessage(("§7[§5" + vaultChat.getPlayerPrefix(sender) + Objects.requireNonNullElse(
							sender.getDisplayName(), sender.getName()) + vaultChat.getPlayerSuffix(
							sender) + " §6-> §4me§7] §f" + message).replaceAll("&&", "&§§").replaceAll(
							"&([0-9a-fkomnrl])", "§$1").replaceAll("&§§", "&"));
					player.sendMessage(
							("§7[§4I §6-> §5" + vaultChat.getPlayerPrefix(player) + Objects.requireNonNullElse(
									player.getDisplayName(), player.getName()) + vaultChat.getPlayerSuffix(
									player) + "§7] §f" + message).replaceAll("&&", "&§§").replaceAll(
									"&([0-9a-fkomnrl])", "§$1").replaceAll("&§§", "&"));
				}
				else {
					player.sendMessage(("§7[§5" + Objects.requireNonNullElse(sender.getDisplayName(),
							sender.getName()) + " §6-> §4me§7] §f" + message).replaceAll("&&", "&§§").replaceAll(
							"&([0-9a-fkomnrl])", "§$1").replaceAll("&§§", "&"));
					player.sendMessage(("§7[§4I §6-> §5" + Objects.requireNonNullElse(player.getDisplayName(),
							player.getName()) + "§7] §f" + message).replaceAll("&&", "&§§").replaceAll(
							"&([0-9a-fkomnrl])", "§$1").replaceAll("&§§", "&"));
				}
			});
			commandManager.register(builder);
		}
		
		//Re
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("r").permission(
					"redfix.command.msg").argument(StringArrayArgument.of("message")).handler(commandContext -> {
				String[] msg = commandContext.get("message");
				Player sender = (Player) commandContext.getSender();
				if (!lastMessaged.containsKey(sender.getUniqueId())) {
					sendMessage(sender, "Du hast keinen Dialog mit einem Spieler.");
					return;
				}
				UUID target = lastMessaged.get(sender.getUniqueId());
				if (!Bukkit.getOfflinePlayer(target).isOnline()) {
					sendMessage(sender, "Der Spieler ist nicht online.");
					return;
				}
				Player player = Bukkit.getPlayer(target);
				
				String message = Arrays.stream(msg).collect(StringBuilder::new, (sb, s) -> {
					sb.append(s);
					sb.append(" ");
				}, StringBuilder::append).toString();
				lastMessaged.put(player.getUniqueId(), sender.getUniqueId());
				lastMessaged.put(sender.getUniqueId(), player.getUniqueId());
				if (vaultChat != null) {
					player.sendMessage(("§7[§5" + vaultChat.getPlayerPrefix(sender) + Objects.requireNonNullElse(
							sender.getDisplayName(), sender.getName()) + vaultChat.getPlayerSuffix(
							sender) + " §6-> §4me§7] §f" + message).replaceAll("&&", "&§§").replaceAll(
							"&([0-9a-fkomnrl])", "§$1").replaceAll("&§§", "&"));
					player.sendMessage(
							("§7[§4I §6-> §5" + vaultChat.getPlayerPrefix(player) + Objects.requireNonNullElse(
									player.getDisplayName(), player.getName()) + vaultChat.getPlayerSuffix(
									player) + "§7] §f" + message).replaceAll("&&", "&§§").replaceAll(
									"&([0-9a-fkomnrl])", "§$1").replaceAll("&§§", "&"));
				}
				else {
					player.sendMessage(("§7[§5" + Objects.requireNonNullElse(sender.getDisplayName(),
							sender.getName()) + " §6-> §4me§7] §f" + message).replaceAll("&&", "&§§").replaceAll(
							"&([0-9a-fkomnrl])", "§$1").replaceAll("&§§", "&"));
					player.sendMessage(("§7[§4I §6-> §5" + Objects.requireNonNullElse(player.getDisplayName(),
							player.getName()) + "§7] §f" + message).replaceAll("&&", "&§§").replaceAll(
							"&([0-9a-fkomnrl])", "§$1").replaceAll("&§§", "&"));
				}
			});
			commandManager.register(builder);
		}
		
		//Me
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("me", "action").permission(
					"redfix.command.me").argument(StringArrayArgument.of("message")).handler(commandContext -> {
				String[] msg = commandContext.get("message");
				Player sender = (Player) commandContext.getSender();
				String name = Objects.requireNonNullElse(sender.getDisplayName(), sender.getName());
				String message = Arrays.stream(msg).collect(StringBuilder::new, (sb, s) -> {
					sb.append(s);
					sb.append(" ");
				}, StringBuilder::append).toString();
				if (vaultChat != null) {
					Bukkit.broadcastMessage(
							(vaultChat.getPlayerPrefix(sender) + "§7" + name + vaultChat.getPlayerSuffix(
									sender) + "§7 " + message).replaceAll("&&", "&§§").replaceAll("&([0-9a-fkomnrl])",
									"§$1").replaceAll("&§§", "&"));
				}
				else {
					Bukkit.broadcastMessage(
							("§7" + name + "§7 " + message).replaceAll("&&", "&§§").replaceAll("&([0-9a-fkomnrl])",
									"§$1").replaceAll("&§§", "&"));
				}
			});
			
			commandManager.register(builder);
		}
		
		//Worth
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("worth").permission(
					"redfix.command.worth.get").argument(MaterialArgument.of("material")).handler(commandContext -> {
				Material material = commandContext.get("material");
				double value = WorthCalculator.getWorth(material, new HashSet<>());
				if (Double.isNaN(value))
					sendMessage(commandContext.getSender(), "No Worth");
				else
					sendMessage(commandContext.getSender(), "Worth of " + material + " is §6" + vaultEconomy.format(
							value) + "§f,\nbut you will only receive §6" + vaultEconomy.format(
							value * (1 - getConfig().getDouble("economy.sellFee", 0.2))) + "§f for selling");
			});
			
			commandManager.register(builder);
		}
		
		//DefaultWorth
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder(
					"defaultworth").permission("redfix.command.worth.default").handler(commandContext -> {
				WorthCalculator.setToDefault();
				sendMessage(commandContext.getSender(), "Reset worth");
			});
			
			commandManager.register(builder);
		}
		
		//ClearWorth
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder("clearworth").permission(
					"redfix.command.worth.clear").handler(commandContext -> {
				WorthCalculator.clear();
				sendMessage(commandContext.getSender(), "Cleared worth");
			});
			
			commandManager.register(builder);
		}
		
		//SetWorth
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder("setworth").permission(
					"redfix.command.worth.set").argument(MaterialArgument.of("material")).argument(
					DoubleArgument.of("value")).handler(commandContext -> {
				Material material = commandContext.get("material");
				double value = commandContext.get("value");
				if (value < 0)
					value = Double.NaN;
				WorthCalculator.worthMap.put(material, value);
			});
			
			commandManager.register(builder);
		}
		
		//LoadWorth
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder("loadworth").permission(
					"redfix.command.worth.load").handler(commandContext -> {
				WorthCalculator.load(new File(pluginPath, "worth.yml"));
				sendMessage(commandContext.getSender(), "Loaded Worth");
			});
			
			commandManager.register(builder);
		}
		
		//SaveWorth
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder("saveworth").permission(
					"redfix.command.worth.save").handler(commandContext -> {
				WorthCalculator.save(new File(pluginPath, "worth.yml"));
				sendMessage(commandContext.getSender(), "Saved Worth");
			});
			
			commandManager.register(builder);
		}
		
		//CalculateWorth
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder(
					"calculateworth").permission("redfix.command.worth.calculate").argument(IntegerArgument.of("loops"),
					"Loops of calculating (-1 for also saving not calculatable)").argument(
					BooleanArgument.of("replace"), "Replace preexisting values").handler(commandContext -> {
				int loops = commandContext.get("loops");
				boolean replace = commandContext.get("replace");
				if (loops == -1) {
					WorthCalculator.calculate(true, replace);
					sendMessage(commandContext.getSender(), "Caluclated Worth");
				}
				else
					Bukkit.getScheduler().runTaskAsynchronously(this, () -> {
						for (int i = 0; i < loops; i++) {
							WorthCalculator.calculate(false, replace);
							sendMessage(commandContext.getSender(), "Finished Loop " + (i + 1) + "/" + loops);
						}
						sendMessage(commandContext.getSender(), "Caluclated Worth");
					});
			});
			
			commandManager.register(builder);
		}
		
		//Sell
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("sell").permission(
					"redfix.command.sell").handler(commandContext -> {
				Player sender = (Player) commandContext.getSender();
				ItemStack item = sender.getInventory().getItemInMainHand();
				if (item != null) {
					if (item.getItemMeta() instanceof Damageable damageable) {
						if (damageable.hasDamage()) {
							sendMessage(sender, "Cannot sell damaged items");
							return;
						}
					}
					double val = WorthCalculator.getWorth(item.getType(), new HashSet<>());
					if (Double.isNaN(val)) {
						sendMessage(sender, "Item has no Worth");
						return;
					}
					double cnt = sender.getInventory().getItemInMainHand().getAmount();
					sender.getInventory().setItemInMainHand(new ItemStack(Material.AIR));
					double pay = val * cnt * (1 - getConfig().getDouble("economy.sellFee", 0.2));
					EconomyManager.addMoney(sender.getUniqueId(), pay);
					sendMessage(sender, "You received for selling " + vaultEconomy.format(pay));
				}
			});
			
			commandManager.register(builder);
		}
		
		//Colors
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder("colors").permission(
					"redfix.command.colors").handler(commandContext -> {
				CommandSender sender = commandContext.getSender();
				sender.sendMessage("§r0 - §0Color");
				sender.sendMessage("§r1 - §1Color");
				sender.sendMessage("§r2 - §2Color");
				sender.sendMessage("§r3 - §3Color");
				sender.sendMessage("§r4 - §4Color");
				sender.sendMessage("§r5 - §5Color");
				sender.sendMessage("§r6 - §6Color");
				sender.sendMessage("§r7 - §7Color");
				sender.sendMessage("§r8 - §8Color");
				sender.sendMessage("§r9 - §9Color");
				sender.sendMessage("§ra - §aColor");
				sender.sendMessage("§rb - §bColor");
				sender.sendMessage("§rc - §cColor");
				sender.sendMessage("§rd - §dColor");
				sender.sendMessage("§re - §eColor");
				sender.sendMessage("§rf - §fColor");
				sender.sendMessage("§rl - §lBold");
				sender.sendMessage("§ro - §oCursive");
				sender.sendMessage("§rn - §nUnderline");
				sender.sendMessage("§rm - §mStrikethrough");
				sender.sendMessage("§rk - §kMagic");
				sender.sendMessage("§rr - §rReset");
			});
			
			commandManager.register(builder);
		}
		
		//KillAll
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("killall").permission(
					"redfix.command.killall").argument(EntityTypeArgument.of("type").optional()).handler(
					commandContext -> {
						Bukkit.getScheduler().runTask(this, () -> {
							Player sender = (Player) commandContext.getSender();
							if (commandContext.contains("type")) {
								EntityType type = commandContext.get("type");
								Stream<Entity> entityStream = sender.getWorld().getEntities().stream().filter(
										e -> e.getType() == type).filter(e -> e.getType() != EntityType.PLAYER);
								List<Entity> entities = entityStream.toList();
								entities.forEach(Entity::remove);
								sendMessage(sender, "Removed " + entities.size() + " Entities");
							}
							else {
								Stream<LivingEntity> entityStream = sender.getWorld().getLivingEntities().stream().filter(
										e -> e instanceof Mob);
								List<LivingEntity> entities = entityStream.toList();
								entities.forEach(LivingEntity::remove);
								sendMessage(sender, "Removed " + entities.size() + " Entities");
							}
						});
					});
			commandManager.register(builder);
		}
		
		//InvisibleItemFrame
		{
			FrameworkCommand.Builder<Player> builder = FrameworkCommand.playerCommandBuilder("invitemframe",
					"iif").permission("redfix.command.invitemframe").handler(commandContext -> {
				Bukkit.getScheduler().runTask(this, () -> {
					Player sender = (Player) commandContext.getSender();
					RayTraceResult result = sender.getWorld().rayTrace(sender.getEyeLocation(),
							sender.getEyeLocation().getDirection(), 5, FluidCollisionMode.NEVER, true, 0,
							e -> e.getType() == EntityType.ITEM_FRAME);
					if (result != null) {
						Entity entity = result.getHitEntity();
						if (entity instanceof ItemFrame itemFrame) {
							itemFrame.setVisible(!itemFrame.isVisible());
						}
						else {
							sendMessage(sender, "Du schaust nicht auf einen Itemframe");
						}
					}
					else {
						sendMessage(sender, "Du schaust nicht auf einen Itemframe");
					}
				});
			});
			commandManager.register(builder);
		}
		
		//Sudo
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder("sudo").permission(
					"redfix.command.sudo").argument(PlayerArgument.of("player")).argument(
					StringArrayArgument.of("command")).handler(commandContext -> {
				String[] scmd = commandContext.get("command");
				String cmd = String.join(" ", scmd);
				Player player = commandContext.get("player");
				Bukkit.dispatchCommand(player, cmd);
			});
			
			commandManager.register(builder);
		}
		
		//PSudo
		{
			FrameworkCommand.Builder<CommandSender> builder = FrameworkCommand.commandBuilder("psudo").permission(
					"redfix.command.psudo").argument(PlayerArgument.of("player")).argument(
					StringArrayArgument.of("command")).handler(commandContext -> {
				String[] scmd = commandContext.get("command");
				String cmd = String.join(" ", scmd);
				Player player = commandContext.get("player");
				Command command = Bukkit.getCommandMap().getCommand(scmd[0]);
				if (command != null)
					command.execute(player, scmd[0], Arrays.copyOfRange(scmd, 1, scmd.length));
			});
			
			commandManager.register(builder);
		}
		
		//TODO: weather, clear
		//TODO: killall, suicide, sudo
		//TODO: msg, mail
		//TODO: tp, tphere, tppos, tpall
		//TODO: tpa, tpahere, tpaall, tpaaccept, tpareject / tpadeny
		//TODO: home, warp, invsee (see, see+edit)
		//To Improve:
		//TODO: ptime, pweather, time
	}
	
	public static void sendMessage(@NotNull CommandSender receiver, String message) {
		receiver.sendMessage(ConfigManager.language.getMessage("prefix") + message);
	}
	
	public void registerCommand(String cmd, CommandExecutor handler) {
		Objects.requireNonNull(getCommand(cmd)).setExecutor(handler);
	}
	
	public static RedfixPlugin getInstance() {
		return instance;
	}
	
	public static void addToHistory(Player player) {
		if (!playerLocationHistory.containsKey(player.getUniqueId())) {
			playerLocationHistory.put(player.getUniqueId(), new ConcurrentLinkedDeque<>());
		}
		playerLocationHistory.get(player.getUniqueId()).add(player.getLocation());
	}
	
	public static void addHome(Player player, String name) {
		if (!homes.containsKey(player.getUniqueId())) {
			homes.put(player.getUniqueId(), new HashMap<>());
		}
		homes.get(player.getUniqueId()).put(name, new Home(name, player.getLocation(), player.getUniqueId()));
		saveHomes(new File(pluginPath, "homes.json"));
	}
	
	public static void addHome(Home home) {
		if (!homes.containsKey(home.player)) {
			homes.put(home.player, new HashMap<>());
		}
		homes.get(home.player).put(home.name, home);
	}
	
	public static void addWarp(Player player, String name) {
		warps.put(name, new Warp(name, player.getLocation()));
		saveWarps(new File(pluginPath, "warps.json"));
	}
	
	public static void addWarp(Warp warp) {
		warps.put(warp.name, warp);
	}
	
	public static void pollHistory(Player player, Player sender) {
		if (!playerLocationHistory.containsKey(player.getUniqueId())) {
			playerLocationHistory.put(player.getUniqueId(), new ConcurrentLinkedDeque<>());
		}
		Deque<Location> locationHistory = playerLocationHistory.get(player.getUniqueId());
		if (!locationHistory.isEmpty()) {
			player.teleport(locationHistory.pollLast());
			sendMessage(sender, "§6Spieler wurde zurück teleportiert");
		}
		else {
			sendMessage(sender, "§4History ist leer");
		}
	}
	
	public static void pollDeath(Player player, Player sender) {
		if (!playerLocationHistory.containsKey(player.getUniqueId()))
			return;
		Location location = playerDeathLocations.get(player.getUniqueId());
		if (location != null) {
			player.teleport(location);
			sendMessage(sender, "§6Spieler wurde zum Todespunkt teleportiert");
		}
		else {
			sendMessage(sender, "§4Kein Todespunkt gespeichert");
		}
	}
	
	//@formatter:off
	public void initLanguage() {
		LanguageConfig language = ConfigManager.language;
		language.registerMessages(LanguageConfig.Locale.DE, Map.ofEntries(
				Map.entry("commandspy.prefix", "§cCommandSpy » "),
				Map.entry("commandspy.command_enable", "§7CommandSpy wurde §eaktiviert"),
				Map.entry("commandspy.command_disable", "§7CommandSpy wurde §edeaktiviert"),

				Map.entry("prefix", "§4Red§eFix §a» §r"),
				Map.entry("chat.shout.prefix", "§a[Shout] "),
				Map.entry("chat.ask.prefix", "§9[Question] ")
				));
	}
	//@formatter:on
	
	
	public static void loadHomes(File file) {
		file.getParentFile().mkdirs();
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		JsonArray array;
		try {
			FileInputStream fis = new FileInputStream(file);
			array = JsonParser.parseString(new String(fis.readAllBytes())).getAsJsonArray();
			fis.close();
		} catch (IOException | IllegalStateException e) {
			e.printStackTrace();
			return;
		}
		try {
			for (JsonElement element : array) {
				Home home = Home.load(element.getAsJsonObject());
				addHome(home);
			}
		} catch (Exception ignored) {
		}
	}
	
	public static void saveHomes(File file) {
		file.getParentFile().mkdirs();
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		JsonArray array = new JsonArray();
		homes.values().forEach(m0 -> m0.values().forEach(h -> array.add(h.save())));
		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(array.toString().getBytes());
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void loadWarps(File file) {
		file.getParentFile().mkdirs();
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		JsonArray array;
		try {
			FileInputStream fis = new FileInputStream(file);
			array = JsonParser.parseString(new String(fis.readAllBytes())).getAsJsonArray();
			fis.close();
		} catch (IOException | IllegalStateException e) {
			e.printStackTrace();
			return;
		}
		try {
			for (JsonElement element : array) {
				Warp warp = Warp.load(element.getAsJsonObject());
				addWarp(warp);
			}
		} catch (Exception ignored) {
		}
	}
	
	public static void saveWarps(File file) {
		file.getParentFile().mkdirs();
		try {
			file.createNewFile();
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		JsonArray array = new JsonArray(warps.size());
		warps.values().forEach(w -> array.add(w.save()));
		try {
			FileOutputStream fos = new FileOutputStream(file);
			fos.write(array.toString().getBytes());
			fos.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
}
