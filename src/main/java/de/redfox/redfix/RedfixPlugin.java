package de.redfox.redfix;

import cloud.commandframework.Command;
import cloud.commandframework.CommandTree;
import cloud.commandframework.bukkit.parsers.PlayerArgument;
import cloud.commandframework.execution.AsynchronousCommandExecutionCoordinator;
import cloud.commandframework.execution.CommandExecutionCoordinator;
import cloud.commandframework.paper.PaperCommandManager;
import de.redfox.redfix.commands.CommandSpy;
import de.redfox.redfix.config.ConfigManager;
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
	Function<CommandTree<CommandSender>, CommandExecutionCoordinator<CommandSender>> executionCoordinatorFunction =
					AsynchronousCommandExecutionCoordinator.<CommandSender>newBuilder().build();
	Function<CommandSender, CommandSender> mapperFunction = Function.identity();

	@Override
	public void onEnable() {
		ConfigManager.init();
		commandSpy = new CommandSpy();
		commandSpy.load();
		registerCommand("commandspy", commandSpy);
		registerCommands();
	}

	private void registerCommands() {
		try {
			manager = new PaperCommandManager<>(this, executionCoordinatorFunction, mapperFunction, mapperFunction);
			this.getLogger().severe("Failed to initialize the command this.manager");
			this.getServer().getPluginManager().disablePlugin(this);
		} catch (Exception e) {
			e.printStackTrace();
		}

		Command.Builder<CommandSender> builder = this.manager.commandBuilder("builder");
		this.manager.command(builder.literal("jail")
				.senderType(Player.class)
				.argument(PlayerArgument.of("player"))
				.handler(commandContext -> {
					CommandSender sender = commandContext.getSender();
					sender.sendMessage("You jailed " + commandContext.getRawInput().get(0));
					Player player = commandContext.get("player");
					player.sendMessage("Jailed XD");
				})
		);
	}
	
	public void registerCommand(String cmd, CommandExecutor handler) {
		Objects.requireNonNull(getCommand(cmd)).setExecutor(handler);
	}
	
	public static RedfixPlugin getInstance() {
		return instance;
	}
	
}
