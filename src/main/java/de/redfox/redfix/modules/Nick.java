package de.redfox.redfix.modules;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.*;
import com.comphenix.protocol.injector.GamePhase;
import com.comphenix.protocol.wrappers.WrappedGameProfile;
import de.redfox.redfix.RedfixPlugin;
import net.kyori.adventure.text.Component;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.plugin.Plugin;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class Nick implements Listener, PacketListener {
	
	public static ConcurrentMap<UUID, String> nicks = new ConcurrentHashMap<>();
	
	public static ListeningWhitelist sendingWhitelist;
	
	static {
		ListeningWhitelist.Builder builder = ListeningWhitelist.newBuilder();
		builder.gamePhase(GamePhase.LOGIN);
		Set<PacketType> types = new HashSet<>();
		types.add(PacketType.Play.Server.PLAYER_INFO);
		builder.types(types);
		builder.options(Collections.singleton(ListenerOptions.ASYNC));
		sendingWhitelist = builder.build();
	}
	
	@EventHandler
	public void onJoin(PlayerJoinEvent event) {
		update(event.getPlayer());
	}
	
	@EventHandler
	public void onQuit(PlayerQuitEvent event) {
		update(event.getPlayer());
	}
	
	public static void update(Player player) {
		if (nicks.containsKey(player.getUniqueId())) {
			String nick = nicks.get(player.getUniqueId());
			player.setDisplayName(nick);
			player.setCustomName(nick);
			player.setPlayerListName(nick);
			player.customName(Component.text(nick));
			player.playerListName(Component.text(nick));
			player.displayName(Component.text(nick));
			player.setCustomNameVisible(true);
		}
		else {
			String nick = player.getName();
			player.setDisplayName(nick);
			player.setCustomName(nick);
			player.setPlayerListName(nick);
			player.customName(Component.text(nick));
			player.playerListName(Component.text(nick));
			player.displayName(Component.text(nick));
			player.setCustomNameVisible(true);
		}
	}
	
	@Override
	public void onPacketSending(PacketEvent event) {
		try {
			if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO) {
				PacketContainer packet = event.getPacket();
				/*Object packetHandle = packet.getHandle();
				try {
					packetHandle.getClass().getDeclaredField("b").trySetAccessible();
					Object piList = packetHandle.getClass().getDeclaredField("b").get(packetHandle);
					int size = (int) piList.getClass().getDeclaredMethod("size").invoke(piList);
					for (int i = 0; i < size; i++) {
						Object playerInfo0 = piList.getClass().getDeclaredMethod("get", Integer.class).invoke(piList, i);
						playerInfo0.getClass().getDeclaredField("a").setAccessible(true);
						playerInfo0.getClass().getDeclaredField("b").setAccessible(true);
						playerInfo0.getClass().getDeclaredField("c").setAccessible(true);
						playerInfo0.getClass().getDeclaredField("d").setAccessible(true);
						Object gameProfile0 = playerInfo0.getClass().getDeclaredField("c").get(playerInfo0);
						gameProfile0.getClass().getDeclaredField("id").setAccessible(true);
						gameProfile0.getClass().getDeclaredField("name").setAccessible(true);
						gameProfile0.getClass().getDeclaredField("properties").setAccessible(true);
						gameProfile0.getClass().getDeclaredField("legacy").setAccessible(true);
						UUID uuid = (UUID) gameProfile0.getClass().getDeclaredField("id").get(gameProfile0);
						if (!nicks.containsKey(uuid))
							continue;
						String nick = nicks.get(uuid);
						Object gameProfile = gameProfile0.getClass().getDeclaredConstructor(UUID.class,
								String.class).newInstance(uuid, nick);
						Object playerInfo = playerInfo0.getClass().getDeclaredConstructor(gameProfile0.getClass(),
								Integer.class, playerInfo0.getClass().getDeclaredField("b").getType(),
								playerInfo0.getClass().getDeclaredField("d").getType()).newInstance(gameProfile,
								playerInfo0.getClass().getDeclaredField("a").getInt(playerInfo0),
								playerInfo0.getClass().getDeclaredField("b").get(playerInfo0),
								playerInfo0.getClass().getDeclaredField("d").get(playerInfo0));
						piList.getClass().getDeclaredMethod("set", Integer.class, playerInfo.getClass()).invoke(piList, i,
								playerInfo);
					}
				} catch (NoSuchFieldException | IllegalAccessException | NoSuchMethodException | InvocationTargetException | InstantiationException e) {
					event.getPlayer().sendMessage("§4" + e.getMessage());
					Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).forEach(
							s -> event.getPlayer().sendMessage("§4" + s));
				}*/
				List<WrappedGameProfile> profiles = packet.getGameProfiles().getValues();
				for (int i = 0; i < profiles.size(); i++) {
					WrappedGameProfile profile = profiles.get(i);
					String nick = nicks.get(profile.getUUID());
					profiles.set(i, profile.withName(nick));
				}
			}
		} catch (Exception e) {
			event.getPlayer().sendMessage("§4" + e.getMessage());
			Arrays.stream(e.getStackTrace()).map(StackTraceElement::toString).forEach(
					s -> event.getPlayer().sendMessage("§4" + s));
		}
	}
	
	@Override
	public void onPacketReceiving(PacketEvent event) {
	
	}
	
	@Override
	public ListeningWhitelist getSendingWhitelist() {
		return sendingWhitelist;
	}
	
	@Override
	public ListeningWhitelist getReceivingWhitelist() {
		return ListeningWhitelist.EMPTY_WHITELIST;
	}
	
	@Override
	public Plugin getPlugin() {
		return RedfixPlugin.getInstance();
	}
	
}
