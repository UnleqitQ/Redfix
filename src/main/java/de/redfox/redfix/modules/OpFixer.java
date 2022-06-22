package de.redfox.redfix.modules;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import de.redfox.redfix.RedfixPlugin;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class OpFixer implements Listener {
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		Bukkit.getScheduler().runTaskLater(RedfixPlugin.getInstance(), () -> sendOpLevel(event.getPlayer(), 4), 20L);
	}
	
	public static void sendOpLevel(Player player, int level) {
		try {
			Object nmsPacket = writeOpLevel(player, level);
			PacketContainer packet = new PacketContainer(PacketType.Play.Server.ENTITY_STATUS, nmsPacket);
			ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
		}
		catch (ClassNotFoundException | IllegalAccessException | InstantiationException | InvocationTargetException |
		       NoSuchMethodException e) {
			throw new RuntimeException(e);
		}
	}
	
	private static Object writeOpLevel(Player player, int level) throws ClassNotFoundException, NoSuchMethodException,
			InvocationTargetException, InstantiationException, IllegalAccessException {
		Class<?> entityStatusPacketClass =
				Class.forName("net.minecraft.network.protocol.game.PacketPlayOutEntityStatus");
		Constructor<?> entityStatusPacketConstructor =
				entityStatusPacketClass.getConstructor(Class.forName("net.minecraft.world.entity.Entity"), byte.class);
		Method getHandleMethod = player.getClass().getDeclaredMethod("getHandle");
		Object nmsPlayer = getHandleMethod.invoke(player);
		
		byte b0;
		if (level <= 0) {
			b0 = 24;
		}
		else if (level >= 4) {
			b0 = 28;
		}
		else {
			b0 = (byte) (24 + level);
		}
		
		return entityStatusPacketConstructor.newInstance(nmsPlayer, b0);
	}
	
}
