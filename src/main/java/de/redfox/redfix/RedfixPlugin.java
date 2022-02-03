package de.redfox.redfix;

import cloud.commandframework.Command;
import cloud.commandframework.CommandTree;
import cloud.commandframework.arguments.flags.CommandFlag;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import de.redfox.redfix.commands.CommandSpy;
import de.redfox.redfix.config.ConfigManager;
import de.redfox.redfix.modules.God;
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
		
		Command.Builder<CommandSender> builder = this.manager.commandBuilder("jail");
		builder = builder.senderType(Player.class).argument(PlayerArgument.of("player")).handler(commandContext -> {
			CommandSender sender = commandContext.getSender();
			Player target = commandContext.get("player");
			sender.sendMessage("You jailed " + target.getName());
			Player player = commandContext.get("player");
			player.sendMessage("Jailed XD");
		});
		
		this.manager.command(builder);
		
		builder = this.manager.commandBuilder("god");
		builder = builder.senderType(Player.class)
				.flag(CommandFlag.newBuilder("silent").withAliases("s").build())
				//.argument(PlayerArgument.of("player"))
				.handler(commandContext -> {
					Player player = (Player) commandContext.getSender();
					if (God.players.containsKey(player.getUniqueId())) {
						God.players.remove(player.getUniqueId());
						player.sendMessage("Disabled God");
					}
					else {
						God.players.put(player.getUniqueId(), commandContext.contains("silent"));
						player.sendMessage("Enabled God");
					}
				});
		
		this.manager.command(builder);
	}
	
	public void registerCommand(String cmd, CommandExecutor handler) {
		Objects.requireNonNull(getCommand(cmd)).setExecutor(handler);
	}
	
	public static RedfixPlugin getInstance() {
		return instance;
	}
	
}
