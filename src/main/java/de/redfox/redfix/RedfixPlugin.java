package de.redfox.redfix;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.Command;
import cloud.commandframework.CommandTree;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.arguments.standard.IntegerArgument;
import cloud.commandframework.arguments.standard.StringArgument;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import de.redfox.redfix.commands.CommandSpy;
import de.redfox.redfix.config.ConfigManager;
import de.redfox.redfix.modules.God;
import de.redfox.redfix.modules.jail.Jail;
import de.redfox.redfix.modules.jail.JailHandler;
import de.redfox.redfix.modules.jail.JailedPlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.Objects;
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
					sender.sendMessage("A jail with this name already exists");
					return;
				}
				Jail jail = new Jail(commandContext.get("name"), sender.getLocation().getBlock().getLocation());
				JailHandler.jails.put(jail.name, jail);
			});
			StringArgument.Builder jailArgument = StringArgument.newBuilder("name").withSuggestionsProvider(
					(context, arg) -> JailHandler.jails.keySet().stream().filter(
							s -> s.toLowerCase().contains(arg.toLowerCase())).toList());
			Command.Builder<CommandSender> removeBuilder = topBuilder.literal("remove").argument(jailArgument,
					ArgumentDescription.of("The name of the jail to remove")).handler(commandContext -> {
				CommandSender sender = (CommandSender) commandContext.getSender();
				if (!JailHandler.jails.containsKey(commandContext.get("name"))) {
					sender.sendMessage("This jail does not exist");
					return;
				}
				JailHandler.jails.remove(commandContext.get("name"));
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
					sender.sendMessage("This jail does not exist");
					return;
				}
				
				JailedPlayer jp = new JailedPlayer(player.getUniqueId(), name, duration);
				JailHandler.jailedPlayers.put(player.getUniqueId(), jp);
				player.teleport(jp.getJail().location);
				sender.sendMessage(
						"You jailed " + player.getName() + ((duration != -1) ? " for " + duration + " seconds" : ""));
				player.sendMessage("You got jailed" + ((duration != -1) ? " for " + duration + " seconds" : ""));
			});
			
			Command.Builder<CommandSender> freeBuilder = topBuilder.literal("unjail").argument(
					PlayerArgument.of("player"), ArgumentDescription.of("The player to unjail")).handler(
					commandContext -> {
						CommandSender sender = (CommandSender) commandContext.getSender();
						Player player = (Player) commandContext.get("player");
						if (!JailHandler.jailedPlayers.containsKey(player.getUniqueId())) {
							sender.sendMessage("This player is not jailed");
							return;
						}
						
						sender.sendMessage("You freed " + player.getName());
						JailHandler.jailedPlayers.remove(player.getUniqueId());
						player.sendMessage("You got freed");
					});
			this.manager.command(createBuilder);
			this.manager.command(removeBuilder);
			this.manager.command(jailBuilder);
			this.manager.command(freeBuilder);
		}
		
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("god");
			builder = builder.senderType(Player.class).flag(CommandFlag.newBuilder("silent").withDescription(
							ArgumentDescription.of("You get damage but the amount is set to zero")).withAliases(
							"s").build()).flag(CommandFlag.newBuilder("notarget").withDescription(
							ArgumentDescription.of("Mobs don't target you")).withAliases("t").build())
					//.argument(PlayerArgument.of("player"))
					.handler(commandContext -> {
						Player player = (Player) commandContext.getSender();
						if (God.players.containsKey(player.getUniqueId())) {
							God.players.remove(player.getUniqueId());
							player.sendMessage("Disabled God");
						}
						else {
							God.players.put(player.getUniqueId(), new Boolean[]{commandContext.flags().contains(
									"silent"), commandContext.flags().contains("notarget")});
							player.sendMessage("Enabled God");
						}
					});
			this.manager.command(builder);
		}
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("heal");
			builder = builder.senderType(Player.class).handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				player.setHealth(
						player.getAttribute(Attribute.GENERIC_MAX_HEALTH).getValue() + player.getAbsorptionAmount());
				player.setExhaustion(0);
				player.setSaturation(20);
				player.setFoodLevel(20);
				player.sendMessage("You got healed");
			});
			this.manager.command(builder);
		}
		
		{
			Command.Builder<CommandSender> builder = this.manager.commandBuilder("fly");
			builder = builder.senderType(Player.class).handler(commandContext -> {
				Player player = (Player) commandContext.getSender();
				player.setAllowFlight(!player.getAllowFlight());
				player.sendMessage(player.getAllowFlight() ? "Enabled fly" : "Disabled fly");
			});
			this.manager.command(builder);
		}
		
		//TODO: ptime, pweather, walkspeed, flyspeed, speed, distance, jail, weather, time
	}
	
	public void registerCommand(String cmd, CommandExecutor handler) {
		Objects.requireNonNull(getCommand(cmd)).setExecutor(handler);
	}
	
	public static RedfixPlugin getInstance() {
		return instance;
	}
	
}
