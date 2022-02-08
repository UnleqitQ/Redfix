package de.redfox.redfix;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.CommandTree;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.*;
import cloud.commandframework.bukkit.parsers.EnchantmentArgument;
import cloud.commandframework.bukkit.parsers.MaterialArgument;
import cloud.commandframework.bukkit.parsers.OfflinePlayerArgument;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import cloud.commandframework.permission.Permission;
import de.redfox.redfix.chat.ChatListener;
import de.redfox.redfix.commands.CommandSpy;
import de.redfox.redfix.config.ConfigManager;
import de.redfox.redfix.config.LanguageConfig;
import de.redfox.redfix.economy.EconomyManager;
import de.redfox.redfix.economy.VaultEconomy;
import de.redfox.redfix.modules.God;
import de.redfox.redfix.modules.jail.Jail;
import de.redfox.redfix.modules.jail.JailHandler;
import de.redfox.redfix.modules.jail.JailedPlayer;
import net.milkbowl.vault.chat.Chat;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
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
import java.util.*;
import java.util.function.Function;

public class RedfixPlugin extends JavaPlugin {
	
	private static RedfixPlugin instance;
	public CommandSpy commandSpy;
	public static final String pluginPath = "plugins/Redfix";
	public Chat vaultChat;
	public VaultEconomy vaultEconomy;
	
	public RedfixPlugin() {
		instance = this;
		new File(pluginPath).mkdirs();
	}
	
	
	private PaperCommandManager<CommandSender> manager;
	Function<CommandTree<CommandSender>, CommandExecutionCoordinator<CommandSender>> executionCoordinatorFunction = AsynchronousCommandExecutionCoordinator.<CommandSender>newBuilder().build();
	Function<CommandSender, CommandSender> mapperFunction = Function.identity();
	
	@Override
	public void onEnable() {
		ConfigManager.init();
		
		saveDefaultConfig();
		reloadConfig();
		
		EconomyManager.loadData(new File(pluginPath, "economy.json"));
		
		initLanguage();
		
		new God();
		new JailHandler();
		
		commandSpy = new CommandSpy();
		commandSpy.load();
		registerCommands();
		RegisteredServiceProvider<Chat> rspC = RedfixPlugin.getInstance().getServer().getServicesManager().getRegistration(
				Chat.class);
		vaultChat = rspC.getProvider();
		
		new ChatListener();
		
		vaultEconomy = new VaultEconomy();
		RedfixPlugin.getInstance().getServer().getServicesManager().register(Economy.class, vaultEconomy, this,
				ServicePriority.Normal);
		
	}
	
	@Override
	public void onDisable() {
		saveEco();
	}
	
	public static void saveEco() {
		EconomyManager.saveData(new File(pluginPath, "economy.json"));
	}
	
	private void registerCommands() {
		try {
			manager = new PaperCommandManager<>(this, executionCoordinatorFunction, mapperFunction, mapperFunction);
		} catch (Exception e) {
			this.getLogger().severe("Failed to initialize the command this.manager");
			this.getServer().getPluginManager().disablePlugin(this);
			e.printStackTrace();
		}
		
		//Jail
		{
			Command.Builder<CommandSender> topBuilder = this.manager.commandBuilder("jail");
			/*Command.Builder<CommandSender> createBuilder = topBuilder.literal("create").senderType(
					Player.class).argument(PlayerArgument.of("player")).handler(commandContext -> {
				CommandSender sender = commandContext.getSender();
				Player target = commandContext.get("player");
				sender.sendMessage("You jailed " + target.getName());
				Player player = commandContext.get("player");
				player.sendMessage("Jailed XD");
			});*/
			Command.Builder<CommandSender> createBuilder = topBuilder.literal("create").senderType(
					Player.class).permission(Permission.of("redfix.command.jail.create")).argument(
					StringArgument.of("name"), ArgumentDescription.of("The name of the jail to create")).handler(
					commandContext -> {
						Player sender = (Player) commandContext.getSender();
						if (JailHandler.jails.containsKey(commandContext.get("name"))) {
							sendMessage(sender, "A jail with this name already exists");
							return;
						}
						Jail jail = new Jail(commandContext.get("name"), sender.getLocation().getBlock().getLocation());
						JailHandler.jails.put(jail.name, jail);
						sendMessage(sender, "Created jail \"" + jail.name + "\"");
					});
			
			StringArgument.Builder jailArgument = StringArgument.newBuilder("name").withSuggestionsProvider(
					(context, arg) -> JailHandler.jails.keySet().stream().filter(
							s -> s.toLowerCase().contains(arg.toLowerCase())).toList());
			
			Command.Builder<CommandSender> removeBuilder = topBuilder.literal("remove").permission(
					"redfix.command.jail.remove").argument(jailArgument,
					ArgumentDescription.of("The name of the jail to remove")).handler(commandContext -> {
				CommandSender sender = (CommandSender) commandContext.getSender();
				if (!JailHandler.jails.containsKey(commandContext.get("name"))) {
					sendMessage(sender, "This jail does not exist");
					return;
				}
				JailHandler.jails.remove(commandContext.get("name"));
				sendMessage(sender, "Removed jail \"" + commandContext.get("name") + "\"");
			});
			
			Command.Builder<CommandSender> jailBuilder = topBuilder.literal("jail").permission(
					"redfix.command.jail.jail").argument(PlayerArgument.of("player"),
					ArgumentDescription.of("The player to jail")).argument(jailArgument,
					ArgumentDescription.of("The name of the jail to remove")).argument(
					IntegerArgument.newBuilder("duration").withMin(1).withMax(60 * 60 * 24).asOptional(),
					ArgumentDescription.of("Duration to jail the player in seconds")).handler(commandContext -> {
				CommandSender sender = (CommandSender) commandContext.getSender();
				Player player = (Player) commandContext.get("player");
				String name = (String) commandContext.get("name");
				int duration = (int) commandContext.getOptional("duration").orElseGet(() -> -1);
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
				Bukkit.getScheduler().runTask(RedfixPlugin.getInstance(), () -> player.teleport(jp.getJail().location));
				sendMessage(sender,
						"You jailed " + player.getName() + ((duration != -1) ? " for " + duration + " seconds" : ""));
				sendMessage(player, "You got jailed" + ((duration != -1) ? " for " + duration + " seconds" : ""));
			});
			
			Command.Builder<CommandSender> freeBuilder = topBuilder.literal("unjail").permission(
					"redfix.command.jail.unjail").argument(PlayerArgument.of("player"),
					ArgumentDescription.of("The player to unjail")).handler(commandContext -> {
				CommandSender sender = commandContext.getSender();
				Player player = commandContext.get("player");
				if (!JailHandler.jailedPlayers.containsKey(player.getUniqueId())) {
					sendMessage(sender, "This player is not jailed");
					return;
				}
				
				sendMessage(sender, "You freed " + player.getName());
				JailHandler.jailedPlayers.remove(player.getUniqueId());
				sendMessage(player, "You got freed");
			});
			
			this.manager.command(createBuilder);
			this.manager.command(removeBuilder);
			this.manager.command(jailBuilder);
			this.manager.command(freeBuilder);
		}
		
		//God
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("god");
			builder = builder.senderType(Player.class).permission("redfix.command.god").flag(
							CommandFlag.newBuilder("silent").withDescription(
									ArgumentDescription.of("You get damage but the amount is set to zero")).withAliases(
									"s").build()).flag(CommandFlag.newBuilder("notarget").withDescription(
							ArgumentDescription.of("Mobs don't target you")).withAliases("t").build()).argument(
							PlayerArgument.optional("player"), ArgumentDescription.of("player"))
					//.argument(PlayerArgument.of("player"))
					.handler(commandContext -> {
						Player player = (Player) commandContext.getSender();
						Player target = (Player) commandContext.getOptional("player").orElseGet(() -> player);
						if (God.players.containsKey(target.getUniqueId())) {
							God.players.remove(target.getUniqueId());
							sendMessage(player, "Disabled God");
						}
						else {
							God.players.put(target.getUniqueId(), new Boolean[]{commandContext.flags().contains(
									"silent"), commandContext.flags().contains("notarget")});
							sendMessage(player, "Enabled God");
						}
					});
			this.manager.command(builder);
		}
		
		//Heal
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("heal");
			builder = builder.senderType(Player.class).permission("redfix.command.heal").flag(
					CommandFlag.newBuilder("particle").withAliases("p").withDescription(
							ArgumentDescription.of("Spawn a heart particle"))).argument(
					PlayerArgument.optional("player"), ArgumentDescription.of("player")).handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				Player target = (Player) commandContext.getOptional("player").orElseGet(() -> player);
				target.setHealth(
						target.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() + target.getAbsorptionAmount());
				target.setExhaustion(0);
				target.setSaturation(20);
				target.setFoodLevel(20);
				sendMessage(target, "You got healed");
				if (commandContext.flags().contains("particle")) {
					target.getWorld().spawnParticle(Particle.HEART, target.getLocation().clone().add(0, 1.5, 0), 1);
				}
			});
			this.manager.command(builder);
		}
		
		//Fly
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("fly");
			builder = builder.senderType(Player.class).permission("redfix.command.fly").argument(
					PlayerArgument.optional("player"), ArgumentDescription.of("player")).handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				Player target = (Player) commandContext.getOptional("player").orElse(player);
				target.setAllowFlight(!target.getAllowFlight());
				sendMessage(player, target.getAllowFlight() ? "Enabled fly" : "Disabled fly");
			});
			this.manager.command(builder);
		}
		
		//Gm
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("gamemode", "gm");
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
			
			StringArgument.Builder gmArgument = StringArgument.newBuilder("gamemode").withSuggestionsProvider(
					(context, arg) -> {
						List<String> l = new ArrayList<>();
						Arrays.stream(GameMode.values()).filter(
								v -> v.name().toLowerCase().contains(arg.toLowerCase())).forEach(
								v -> l.add(v.name().toLowerCase()));
						return l;
					});
			
			builder = builder.senderType(Player.class).permission("redfix.command.gamemode").argument(gmArgument,
					ArgumentDescription.of("gamemode")).argument(PlayerArgument.optional("player"),
					ArgumentDescription.of("player")).handler(commandContext -> {
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
			this.manager.command(builder);
		}
		
		//PTime
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("ptime");
			builder = builder.senderType(Player.class).permission("redfix.command.ptime").flag(
					CommandFlag.newBuilder("relative").withAliases("r").withDescription(
							ArgumentDescription.of("makes the player time relative"))).argument(
					IntegerArgument.optional("time"), ArgumentDescription.of("Time, if none given resets")).handler(
					commandContext -> {
						Player player = (Player) commandContext.getSender();
						if (commandContext.contains("time")) {
							int time = (int) commandContext.getOptional("time").orElse(0);
							player.setPlayerTime(time, commandContext.flags().contains("relative"));
							sendMessage(player, "Set player time");
						}
						else {
							player.resetPlayerTime();
							sendMessage(player, "Reset player time");
						}
					});
			this.manager.command(builder);
		}
		
		//PWeather
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("pweather");
			builder = builder.senderType(Player.class).permission("redfix.command.pweather").argument(
					EnumArgument.optional(WeatherType.class, "weather"),
					ArgumentDescription.of("Weather type, if none given resets")).handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				if (commandContext.contains("weather")) {
					WeatherType type = (WeatherType) commandContext.getOptional("weather").orElse(WeatherType.CLEAR);
					player.setPlayerWeather(type);
					sendMessage(player, "Set player weather");
				}
				else {
					player.resetPlayerWeather();
					sendMessage(player, "Reset player weather");
				}
			});
			this.manager.command(builder);
		}
		
		//Time
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("time", "rftime");
			builder = builder.senderType(Player.class).permission("redfix.command.time").argument(
					IntegerArgument.of("time"), ArgumentDescription.of("Time")).handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				int time = commandContext.get("time");
				player.getWorld().setFullTime(time);
				sendMessage(player, "Set time");
			});
			this.manager.command(builder);
		}
		
		//Wspeed
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("walkspeed", "wspeed");
			builder = builder.senderType(Player.class).permission("redfix.command.walkspeed").argument(
					FloatArgument.of("speed"), ArgumentDescription.of("Walking speed")).handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				float speed = commandContext.get("speed");
				AttributeInstance attributeInstance = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
				attributeInstance.getModifiers().stream().filter(am -> am.getName().contentEquals("redfix")).forEach(
						attributeInstance::removeModifier);
				attributeInstance.addModifier(
						new AttributeModifier("redfix", speed - 1, AttributeModifier.Operation.MULTIPLY_SCALAR_1));
				player.setWalkSpeed(0.2f);
				sendMessage(player, "Set walk speed to " + speed);
			});
			this.manager.command(builder);
		}
		
		//Fspeed
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("flyspeed", "fspeed");
			FloatArgument.Builder speedArg = FloatArgument.newBuilder("speed").withMin(0).withMax(10);
			builder = builder.senderType(Player.class).permission("redfix.command.flyspeed").argument(speedArg,
					ArgumentDescription.of("Flying speed")).handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				float speed = (float) commandContext.get("speed");
				player.setFlySpeed(speed / 10);
				sendMessage(player, "Set fly speed to " + speed);
			});
			this.manager.command(builder);
		}
		
		//Speed
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("speed");
			FloatArgument.Builder speedArg = FloatArgument.newBuilder("speed").withMin(0).withMax(10);
			builder = builder.senderType(Player.class).permission("redfix.command.speed").argument(speedArg,
					ArgumentDescription.of("Speed")).handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				float speed = (float) commandContext.get("speed");
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
			this.manager.command(builder);
		}
		
		//Distance
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("distance");
			builder = builder.senderType(Player.class).permission("redfix.command.distance").argument(
					PlayerArgument.of("player"), ArgumentDescription.of("Player to measure distance to")).handler(
					commandContext -> {
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
						sendMessage(player,
								String.format("Difference: %3.02f %3.02f %3.02f", d.getX(), d.getY(), d.getZ()));
					});
			this.manager.command(builder);
		}
		
		//Enchant
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("enchant", "rfenchant");
			builder = builder.senderType(Player.class).permission("redfix.command.enchant").argument(
					EnchantmentArgument.of("enchantment"), ArgumentDescription.of("The Enchantment to apply")).argument(
					IntegerArgument.of("level"), ArgumentDescription.of("The Level to apply")).handler(
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
			this.manager.command(builder);
		}
		
		//Give
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("i", "give", "item");
			builder = builder.senderType(Player.class).permission("redfix.command.give").argument(
					MaterialArgument.of("material"), ArgumentDescription.of("The Item")).argument(
					IntegerArgument.optional("count", 1), ArgumentDescription.of("The Count")).handler(
					commandContext -> {
						Player player = (Player) commandContext.getSender();
						Material material = commandContext.get("material");
						int count = commandContext.get("count");
						try {
							for (int i = 0; i < count / material.getMaxStackSize(); i++) {
								player.getInventory().addItem(new ItemStack(material, material.getMaxStackSize()));
							}
							player.getInventory().addItem(new ItemStack(material, count % material.getMaxStackSize()));
						} catch (Exception ignored) {
						}
					});
			this.manager.command(builder);
		}
		
		//Playtime
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("playtime");
			builder = builder.permission("redfix.command.playtime").argument(
					OfflinePlayerArgument.optional("player")).handler(commandContext -> {
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
			this.manager.command(builder);
		}
		//PlaytimeTop
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("playtimetop");
			builder = builder.permission("redfix.command.playtimetop").handler(commandContext -> {
				CommandSender sender = commandContext.getSender();
				UUID uuid = null;
				int time = Integer.MIN_VALUE;
				for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
					int t0 = offlinePlayer.getStatistic(Statistic.PLAY_ONE_MINUTE);
					if (t0 > time) {
						time = t0;
						uuid = offlinePlayer.getUniqueId();
					}
				}
				int playedTicks = time;
				int seconds = playedTicks / 20;
				int minutes = seconds / 60;
				int hours = minutes / 60;
				int days = hours / 24;
				sendMessage(sender, String.format("Player: %s", Bukkit.getOfflinePlayer(uuid).getName()));
				sendMessage(sender,
						String.format("Play Time: %02d days %02d h %02d m %02d s", days, hours % 24, minutes % 60,
								seconds % 60));
			});
			this.manager.command(builder);
		}
		
		//Repair
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("repair");
			builder = builder.senderType(Player.class).permission("redfix.command.repair").flag(
					CommandFlag.newBuilder("all").withAliases("a").withDescription(
							ArgumentDescription.of("Repairs all your items"))).handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				if (commandContext.flags().contains("all")) {
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
			this.manager.command(builder);
		}
		
		//Unbreakable
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("unbreakable");
			builder = builder.senderType(Player.class).permission("redfix.command.unbreakable").handler(
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
			this.manager.command(builder);
		}
		
		//AddLore
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("addlore");
			builder = builder.senderType(Player.class).permission("redfix.command.addlore").argument(
					StringArgument.of("lore")).handler(commandContext -> {
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
					lore.addAll(Objects.requireNonNullElse(meta.getLore(), new ArrayList<>()));
					lore.add(commandContext.get("lore"));
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
			this.manager.command(builder);
		}
		
		//SpawnMob
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("spawnmob");
			builder = builder.senderType(Player.class).permission("redfix.command.spawnmob").argument(
					EnumArgument.of(EntityType.class, "entity")).argument(IntegerArgument.optional("count", 1)).handler(
					commandContext -> {
						try {
							Player player = (Player) commandContext.getSender();
							EntityType type = commandContext.get("entity");
							int count = commandContext.get("count");
							Bukkit.getScheduler().runTask(RedfixPlugin.getInstance(), () -> {
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
										player.getWorld().spawnEntity(player.getLocation(), type);
									}
								}
							});
						} catch (Exception ignored) {
						}
					});
			this.manager.command(builder);
		}
		
		//CommandSpy
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("commandspy");
			builder = builder.senderType(Player.class).permission("redfix.command.commandspy").handler(
					commandContext -> {
						Player player = (Player) commandContext.getSender();
						if (commandSpy.players.contains(player.getUniqueId())) {
							commandSpy.players.remove(player.getUniqueId());
							player.sendMessage(CommandSpy.Messages.get(CommandSpy.Messages.PREFIX,
									CommandSpy.Messages.COMMAND_DISABLE));
						}
						else {
							if (!player.hasPermission("redfix.command.commandspy")) {
								player.sendMessage(CommandSpy.Messages.get(
										CommandSpy.Messages.PREFIX) + "I'm sorry, but you don't have the permission");
								return;
							}
							commandSpy.players.add(player.getUniqueId());
							player.sendMessage(CommandSpy.Messages.get(CommandSpy.Messages.PREFIX,
									CommandSpy.Messages.COMMAND_ENABLE));
						}
						commandSpy.save();
					});
			this.manager.command(builder);
		}
		
		//Effect
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("effect");
			StringArgument.Builder effectArgument = StringArgument.newBuilder("effect").withSuggestionsProvider(
					(context, arg) -> {
						List<String> l = new ArrayList<>();
						Arrays.stream(PotionEffectType.values()).filter(
								et -> et.getName().replaceAll("\\W", "").toLowerCase().contains(
										arg.replaceAll("\\W", "").toLowerCase())).forEach(et -> l.add(et.getName()));
						return l;
					});
			builder = builder.senderType(Player.class).permission("redfix.command.effect").argument(
					effectArgument).argument(IntegerArgument.optional("duration", 30)).argument(
					IntegerArgument.optional("level", 0)).handler(commandContext -> {
				try {
					Player player = (Player) commandContext.getSender();
					String effectName = ((String) commandContext.get("effect")).toLowerCase();
					int duration = (int) commandContext.get("duration");
					int level = (int) commandContext.get("level");
					Bukkit.getScheduler().runTask(RedfixPlugin.getInstance(), () -> player.addPotionEffect(
							new PotionEffect(PotionEffectType.getByName(effectName), duration * 20, level)));
				} catch (Exception ignored) {
				}
			});
			this.manager.command(builder);
		}
		
		//Item Attribute Modifier
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("itemattributemodifier");
			StringArgument.Builder attributeArgument = StringArgument.newBuilder("attribute").withSuggestionsProvider(
					(context, arg) -> {
						List<String> l = new ArrayList<>();
						Arrays.stream(Attribute.values()).filter(
								et -> et.name().replaceAll("\\W", "").toLowerCase().contains(
										arg.replaceAll("\\W", "").toLowerCase())).forEach(et -> l.add(et.name()));
						return l;
					});
			StringArgument.Builder operationArgument = StringArgument.newBuilder("operation").withSuggestionsProvider(
					(context, arg) -> {
						List<String> l = new ArrayList<>();
						Arrays.stream(AttributeModifier.Operation.values()).filter(
								et -> et.name().replaceAll("\\W", "").toLowerCase().contains(
										arg.replaceAll("\\W", "").toLowerCase())).forEach(et -> l.add(et.name()));
						return l;
					});
			StringArgument.Builder slotArgument = (StringArgument.Builder) StringArgument.newBuilder(
					"slot").withSuggestionsProvider((context, arg) -> {
				List<String> l = new ArrayList<>();
				Arrays.stream(EquipmentSlot.values()).filter(
						et -> et.name().replaceAll("\\W", "").toLowerCase().contains(
								arg.replaceAll("\\W", "").toLowerCase())).forEach(et -> l.add(et.name()));
				return l;
			}).asOptionalWithDefault("");
			builder = builder.senderType(Player.class).permission("redfix.command.itemattributemodifier").argument(
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
			this.manager.command(builder);
		}
		
		//Bal
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("balance", "bal", "money");
			builder = builder.senderType(Player.class).permission("redfix.command.balance").argument(
					OfflinePlayerArgument.optional("player"), ArgumentDescription.of("player")).handler(
					commandContext -> {
						Player player = (Player) commandContext.getSender();
						OfflinePlayer target = (Player) commandContext.getOptional("player").orElse(player);
						sendMessage(player,
								"§aBalance: " + EconomyManager.getMoney(target.getUniqueId()) + getConfig().getString(
										"economy.symbol", "$"));
					});
			this.manager.command(builder);
		}
		
		//Baltop
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("balancetop", "baltop");
			builder = builder.senderType(Player.class).permission("redfix.command.balance").handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				double value = Double.MIN_VALUE;
				UUID uuid = null;
				for (Map.Entry<UUID, Double> e : EconomyManager.getAll().entrySet()) {
					if (e.getValue() > value) {
						value = e.getValue();
						uuid = e.getKey();
					}
				}
				if (uuid == null) {
					sendMessage(player, "No Money registered so far");
					return;
				}
				sendMessage(player, "§6Player: " + Bukkit.getOfflinePlayer(uuid).getName());
				sendMessage(player, "§aBalance: " + value + getConfig().getString("economy.symbol", "$"));
			});
			this.manager.command(builder);
		}
		
		//Economy
		{
			Command.Builder<CommandSender> topBuilder = this.manager.commandBuilder("economy", "eco");
			
			Command.Builder<CommandSender> setBuilder = topBuilder.literal("set").permission(
					Permission.of("redfix.command.economy.set")).argument(OfflinePlayerArgument.of("player")).argument(
					DoubleArgument.of("amount")).handler(commandContext -> {
				Player sender = (Player) commandContext.getSender();
				OfflinePlayer player = commandContext.get("player");
				double amount = commandContext.get("amount");
				EconomyManager.setMoney(player.getUniqueId(), amount);
				sendMessage(sender, "Set money of player " + player.getName() + " to " + amount + getConfig().getString(
						"economy.symbol", "$"));
				saveEco();
			});
			Command.Builder<CommandSender> giveBuilder = topBuilder.literal("give").permission(
					Permission.of("redfix.command.economy.give")).argument(OfflinePlayerArgument.of("player")).argument(
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
			Command.Builder<CommandSender> takeBuilder = topBuilder.literal("take").permission(
					Permission.of("redfix.command.economy.take")).argument(OfflinePlayerArgument.of("player")).argument(
					DoubleArgument.of("amount")).handler(commandContext -> {
				Player sender = (Player) commandContext.getSender();
				OfflinePlayer player = commandContext.get("player");
				double amount = commandContext.get("amount");
				EconomyManager.addMoney(player.getUniqueId(), -amount);
				sendMessage(sender, "Took " + +amount + getConfig().getString("economy.symbol",
						"$") + " from player " + player.getName());
				saveEco();
			});
			Command.Builder<CommandSender> resetBuilder = topBuilder.literal("reset").permission(
					Permission.of("redfix.command.economy.reset")).argument(OfflinePlayerArgument.of("player")).handler(
					commandContext -> {
						Player sender = (Player) commandContext.getSender();
						OfflinePlayer player = commandContext.get("player");
						EconomyManager.setMoney(player.getUniqueId(), getConfig().getDouble("economy.startMoney", 100));
						sendMessage(sender, "Reset player's " + player.getName() + " money");
						saveEco();
					});
			
			this.manager.command(setBuilder);
			this.manager.command(giveBuilder);
			this.manager.command(takeBuilder);
			this.manager.command(resetBuilder);
		}
		
		//Pay
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("pay").argument(
					OfflinePlayerArgument.of("player")).argument(DoubleArgument.of("amount")).handler(
					commandContext -> {
						Player sender = (Player) commandContext.getSender();
						OfflinePlayer player = commandContext.get("player");
						double amount = commandContext.get("amount");
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
			
			this.manager.command(builder);
		}
		
		//TODO: weather, clear
		//TODO: killall, suicide, sudo
		//TODO: balance
		//TODO: tp, tphere, tppos, tpall
		//TODO: tpa, tpahere, tpaall, tpaaccept, tpareject / tpadeny
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
	
}
