package de.redfox.redfix;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.CommandTree;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.EnumArgument;
import cloud.commandframework.arguments.standard.FloatArgument;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.parsers.EnchantmentArgument;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import de.redfox.redfix.commands.CommandSpy;
import de.redfox.redfix.config.ConfigManager;
import de.redfox.redfix.config.LanguageConfig;
import de.redfox.redfix.modules.God;
import de.redfox.redfix.modules.jail.Jail;
import de.redfox.redfix.modules.jail.JailHandler;
import de.redfox.redfix.modules.jail.JailedPlayer;
import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeInstance;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.Vector;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.function.Function;

public class RedfixPlugin extends JavaPlugin {
	
	private static RedfixPlugin instance;
	public CommandSpy commandSpy;
	public static final String pluginPath = "plugins/Redfix";
	
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
		
		initLanguage();
		
		new God();
		new JailHandler();
		
		commandSpy = new CommandSpy();
		commandSpy.load();
		registerCommand("commandspy", commandSpy);
		registerCommands();
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
					Player.class).argument(StringArgument.of("name"),
					ArgumentDescription.of("The name of the jail to create")).handler(commandContext -> {
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
			
			Command.Builder<CommandSender> removeBuilder = topBuilder.literal("remove").argument(jailArgument,
					ArgumentDescription.of("The name of the jail to remove")).handler(commandContext -> {
				CommandSender sender = (CommandSender) commandContext.getSender();
				if (!JailHandler.jails.containsKey(commandContext.get("name"))) {
					sendMessage(sender, "This jail does not exist");
					return;
				}
				JailHandler.jails.remove(commandContext.get("name"));
				sendMessage(sender, "Removed jail \"" + commandContext.get("name") + "\"");
			});
			
			Command.Builder<CommandSender> jailBuilder = topBuilder.literal("jail").argument(
					PlayerArgument.of("player"), ArgumentDescription.of("The player to jail")).argument(jailArgument,
					ArgumentDescription.of("The name of the jail to remove")).argument(
					IntegerArgument.newBuilder("duration").withMin(1).withMax(60 * 60 * 24).asOptional(),
					ArgumentDescription.of("Duration to jail the player in seconds")).handler(commandContext -> {
				CommandSender sender = (CommandSender) commandContext.getSender();
				Player player = (Player) commandContext.get("player");
				String name = (String) commandContext.get("name");
				int duration = (int) commandContext.getOptional("duration").orElseGet(() -> -1);
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
			
			Command.Builder<CommandSender> freeBuilder = topBuilder.literal("unjail").argument(
					PlayerArgument.of("player"), ArgumentDescription.of("The player to unjail")).handler(
					commandContext -> {
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
			builder = builder.senderType(Player.class).argument(PlayerArgument.optional("player"),
							ArgumentDescription.of("player")).flag(CommandFlag.newBuilder("silent").withDescription(
							ArgumentDescription.of("You get damage but the amount is set to zero")).withAliases(
							"s").build()).flag(CommandFlag.newBuilder("notarget").withDescription(
							ArgumentDescription.of("Mobs don't target you")).withAliases("t").build())
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
			builder = builder.senderType(Player.class).argument(PlayerArgument.optional("player"),
					ArgumentDescription.of("player")).flag(
					CommandFlag.newBuilder("particle").withAliases("p").withDescription(
							ArgumentDescription.of("Spawn a heart particle"))).handler(commandContext -> {
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
			builder = builder.senderType(Player.class).argument(PlayerArgument.optional("player"),
					ArgumentDescription.of("player")).handler(commandContext -> {
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
			
			builder = builder.senderType(Player.class).argument(gmArgument,
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
			builder = builder.senderType(Player.class).argument(IntegerArgument.optional("time"),
					ArgumentDescription.of("Time, if none given resets")).flag(
					CommandFlag.newBuilder("relative").withAliases("r").withDescription(
							ArgumentDescription.of("makes the player time relative"))).handler(commandContext -> {
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
			builder = builder.senderType(Player.class).argument(EnumArgument.optional(WeatherType.class, "weather"),
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
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("rftime", "time");
			builder = builder.senderType(Player.class).argument(IntegerArgument.of("time"),
					ArgumentDescription.of("Time")).handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				int time = commandContext.get("time");
				player.getWorld().setFullTime(time);
				sendMessage(player, "Set time");
			});
			this.manager.command(builder);
		}
		
		//Wspeed
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("wspeed");
			builder = builder.senderType(Player.class).argument(FloatArgument.of("speed"),
					ArgumentDescription.of("Walking speed")).handler(commandContext -> {
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
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("fspeed");
			FloatArgument.Builder speedArg = FloatArgument.newBuilder("speed").withMin(0).withMax(10);
			builder = builder.senderType(Player.class).argument(speedArg,
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
			builder = builder.senderType(Player.class).argument(speedArg, ArgumentDescription.of("Speed")).handler(
					commandContext -> {
						Player player = (Player) commandContext.getSender();
						float speed = (float) commandContext.get("speed");
						if (player.isFlying()) {
							player.setFlySpeed(speed / 10);
							sendMessage(player, "Set fly speed to " + speed);
						}
						else {
							AttributeInstance attributeInstance = player.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED);
							attributeInstance.getModifiers().stream().filter(
									am -> am.getName().contentEquals("redfix")).forEach(
									attributeInstance::removeModifier);
							attributeInstance.addModifier(new AttributeModifier("redfix", speed - 1,
									AttributeModifier.Operation.MULTIPLY_SCALAR_1));
							player.setWalkSpeed(0.2f);
							sendMessage(player, "Set walk speed to " + speed);
						}
					});
			this.manager.command(builder);
		}
		
		//Distance
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("distance");
			builder = builder.senderType(Player.class).argument(PlayerArgument.of("player"),
					ArgumentDescription.of("Player to measure distance to")).handler(commandContext -> {
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
			this.manager.command(builder);
		}
		
		//Enchant
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("rfenchant", "enchant");
			builder = builder.senderType(Player.class).argument(EnchantmentArgument.of("enchantment"),
					ArgumentDescription.of("The Enchantment to apply")).argument(IntegerArgument.of("level"),
					ArgumentDescription.of("The Level to apply")).handler(commandContext -> {
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
				} catch (Exception ignored) {
				}
			});
			this.manager.command(builder);
		}
		
		//TODO: weather, effect, enchant, i / give, clear, repair, unbreakable
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
				Map.entry("commandspy.command_disable", "§7CommandSpy wurde §eaktiviert"),
				Map.entry("commandspy.command_enable", "§7CommandSpy wurde §edeaktiviert"),
				
				Map.entry("prefix", "§4Red§eFix §a» §r"),
				Map.entry("suffix", "§7CommandSpy wurde §eaktiviert")
				));
	}
	//@formatter:on
	
}
