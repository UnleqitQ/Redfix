package de.redfox.redfix.modules.abilityfixer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AbilityFixer_1_18 extends AbilityFixer {
	
	@Override
	protected void readAbilities(Object packet) throws ClassNotFoundException, NoSuchMethodException,
			InvocationTargetException, InstantiationException, IllegalAccessException {
		Class<?> friendlyByteBufClass = Class.forName("net.minecraft.network.PacketDataSerializer");
		Class<?> playerAbilitiesPacketClass =
				Class.forName("net.minecraft.network.protocol.game.PacketPlayOutAbilities");
		ByteBuf buf = Unpooled.buffer();
		Constructor<?> friendlyByteBufConstructor = friendlyByteBufClass.getDeclaredConstructor(ByteBuf.class);
		Object fbb = friendlyByteBufConstructor.newInstance(buf);
		Method packetWriteMethod = playerAbilitiesPacketClass.getDeclaredMethod("a", friendlyByteBufClass);
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
	
	@Override
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
	
}
