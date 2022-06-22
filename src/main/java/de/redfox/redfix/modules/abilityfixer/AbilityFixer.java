package de.redfox.redfix.modules.abilityfixer;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.events.ListeningWhitelist;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.events.PacketListener;
import de.redfox.redfix.RedfixPlugin;
import de.redfox.redfix.utils.NMSHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.plugin.Plugin;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AbilityFixer implements PacketListener {
	
	ListeningWhitelist sendingWhitelist =
			ListeningWhitelist.newBuilder().types(PacketType.Play.Server.ABILITIES).build();
	ListeningWhitelist receivingWhitelist =
			ListeningWhitelist.newBuilder().types(PacketType.Play.Client.ABILITIES).build();
	
	@Override
	public void onPacketSending(PacketEvent event) {
		if (event.getPacketType() == PacketType.Play.Server.ABILITIES) {
			PacketContainer packet = event.getPacket();
			try {
				readAbilities(packet.getHandle());
			}
			catch (ClassNotFoundException | IllegalAccessException | InstantiationException |
			       InvocationTargetException | NoSuchMethodException e) {
				throw new RuntimeException(e);
			}
		}
	}
	
	protected void readAbilities(Object packet) throws ClassNotFoundException, NoSuchMethodException,
			InvocationTargetException, InstantiationException, IllegalAccessException {
		Class<?> friendlyByteBufClass = Class.forName("net.minecraft.network.PacketDataSerializer");
		NMSHandler.ClassData playerAbilitiesPacketMappingClass =
				NMSHandler.getClassData("net.minecraft.network.protocol.game.ClientboundPlayerAbilitiesPacket");
		Class<?> playerAbilitiesPacketClass =
				Class.forName("net.minecraft.network.protocol.game.PacketPlayOutAbilities");
		ByteBuf buf = Unpooled.buffer();
		Constructor<?> friendlyByteBufConstructor = friendlyByteBufClass.getDeclaredConstructor(ByteBuf.class);
		Object fbb = friendlyByteBufConstructor.newInstance(buf);
		Method packetWriteMethod = playerAbilitiesPacketClass.getDeclaredMethod(
				playerAbilitiesPacketMappingClass.methods().stream().filter(m -> m.rawName().contentEquals("write"))
						.findFirst().orElseThrow().obfuscatedName(), friendlyByteBufClass);
		packetWriteMethod.invoke(packet, fbb);
		byte flags = buf.readByte();
		float flySpeed = buf.readFloat();
		float fovMod = buf.readFloat();
		{
			String s = Integer.toBinaryString(flags & 0b1111);
			s = "0".repeat(4 - s.length()) + s;
			System.out.printf("%s %.02f %.02f\n", s, flySpeed, fovMod);
		}
	}
	
	protected Object writeAbilities(boolean invulnerable, boolean isFlying, boolean canFly, boolean instabuild,
			float flyingSpeed, float walkingSpeed) throws ClassNotFoundException, NoSuchMethodException,
			InvocationTargetException, InstantiationException, IllegalAccessException {
		Class<?> friendlyByteBufClass = Class.forName("net.minecraft.network.PacketDataSerializer");
		ByteBuf buf = Unpooled.buffer();
		int flags = 0;
		if (invulnerable)
			flags |= 0b0001;
		if (isFlying)
			flags |= 0b0010;
		if (canFly)
			flags |= 0b0100;
		if (instabuild)
			flags |= 0b1000;
		buf.writeByte(flags);
		buf.writeFloat(flyingSpeed);
		buf.writeFloat(walkingSpeed);
		Constructor<?> friendlyByteBufConstructor = friendlyByteBufClass.getDeclaredConstructor(ByteBuf.class);
		Object fbb = friendlyByteBufConstructor.newInstance(buf);
		Class<?> playerAbilitiesPacketClass =
				Class.forName("net.minecraft.network.protocol.game.PacketPlayOutAbilities");
		Constructor<?> playerAbilitiesPacketConstructor =
				playerAbilitiesPacketClass.getDeclaredConstructor(friendlyByteBufClass);
		return playerAbilitiesPacketConstructor.newInstance(fbb);
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
		return receivingWhitelist;
	}
	
	@Override
	public Plugin getPlugin() {
		return RedfixPlugin.getInstance();
	}
	
}
