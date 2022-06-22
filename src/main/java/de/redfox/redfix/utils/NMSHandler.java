package de.redfox.redfix.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.bukkit.Bukkit;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class NMSHandler {
	
	public static Version version;
	
	public static String mappingsContent;
	public static Map<String, ClassData> classesByRaw = new HashMap<>();
	public static Map<String, ClassData> classesByObfuscated = new HashMap<>();
	public static Set<ClassData> classes = new HashSet<>();
	
	public static void init() throws IOException {
		getMappings();
		parse();
	}
	
	private static void getMappings() throws IOException {
		String versionString = Bukkit.getBukkitVersion().split("-")[0];
		Version version = Version.getByVersion(versionString, Version.Type.SERVER);
		URL mappingsUrl = new URL(version.getMappings());
		URLConnection connection = mappingsUrl.openConnection();
		InputStream is = connection.getInputStream();
		ByteBuf buffer = Unpooled.buffer();
		int bytesRead;
		byte[] bufferBytes = new byte[1024];
		while ((bytesRead = is.read(bufferBytes)) != -1) {
			buffer.writeBytes(bufferBytes, 0, bytesRead);
		}
		is.close();
		mappingsContent = new String(buffer.array());
	}
	
	private static void parse() {
		String[] lines = mappingsContent.split("\n");
		Set<FieldData> fields = new HashSet<>();
		Set<MethodData> methods = new HashSet<>();
		String rawClassName = null;
		String obfuscatedClassName = null;
		for (int i = 1; i < lines.length; i++) {
			String line = lines[i];
			if (!line.contains("->"))
				continue;
			if (line.endsWith(":")) {
				if (rawClassName != null) {
					ClassData classData = new ClassData(rawClassName, obfuscatedClassName, fields, methods);
					classes.add(classData);
					classesByRaw.put(rawClassName, classData);
					classesByObfuscated.put(obfuscatedClassName, classData);
				}
				
				String l0 = line.substring(0, line.length() - 1);
				String[] parts = l0.split("->");
				rawClassName = parts[0].strip();
				obfuscatedClassName = parts[1].strip();
				fields = new HashSet<>();
				methods = new HashSet<>();
				continue;
			}
			if (line.contains("(")) {
				String[] parts0 = line.strip().split(" ");
				String[] parts1 = parts0[0].strip().split(":");
				String obfuscated = parts0[3].strip();
				String returnClass = parts1[parts1.length - 1];
				String[] parts2 = parts0[1].split("\\(");
				String paraString = parts2[1].replaceAll("\\)", "");
				String[] paras = paraString.split(",");
				if (paras.length == 1 && paras[0].isBlank())
					paras = new String[0];
				methods.add(new MethodData(parts2[0], obfuscated, returnClass, paras));
				continue;
			}
			String[] parts = line.strip().split(" ");
			fields.add(new FieldData(parts[1], parts[3], parts[0]));
		}
		if (rawClassName != null) {
			ClassData classData = new ClassData(rawClassName, obfuscatedClassName, fields, methods);
			classes.add(classData);
			classesByRaw.put(rawClassName, classData);
			classesByObfuscated.put(obfuscatedClassName, classData);
		}
	}
	
	public static ClassData getClassData(String className) {
		return classesByRaw.get(className);
	}
	
	public record ClassData(String rawName, String obfuscatedName, Set<FieldData> fields, Set<MethodData> methods) {
		
		
		public MethodData getMethodData(String methodName, String... parameters) {
			for (MethodData method : methods) {
				if (method.rawName.contentEquals("methodName")) {
					if (stringArrayEquals(method.parameters, parameters))
						return method;
				}
			}
			return null;
		}
		
		public MethodData getMethodData(String methodName) {
			for (MethodData method : methods) {
				if (method.rawName.contentEquals("methodName")) {
					if (method.parameters.length == 0)
						return method;
				}
			}
			return null;
		}
		
		
		public MethodData getMethodData(String methodName, Class<?>... parameters) {
			String[] paras = new String[parameters.length];
			for (int i = 0; i < paras.length; i++) {
				paras[i] = parameters[i].getName();
			}
			for (MethodData method : methods) {
				if (method.rawName.contentEquals("methodName")) {
					if (stringArrayEquals(method.parameters, paras))
						return method;
				}
			}
			return null;
		}
		
		public FieldData getFieldData(String fieldName) {
			for (FieldData field : fields) {
				if (field.rawName.contentEquals(fieldName))
					return field;
			}
			return null;
		}
		
		private static boolean stringArrayEquals(String[] sa1, String[] sa2) {
			if (sa1.length != sa2.length)
				return false;
			for (int i = 0; i < sa1.length; i++) {
				if (!sa1[i].contentEquals(sa2[i]))
					return false;
			}
			return true;
		}
		
	}
	
	public record FieldData(String rawName, String obfuscatedName, String type) {
		
		
		public Field getField(Class<?> clazz) throws NoSuchFieldException {
			return clazz.getDeclaredField(obfuscatedName);
		}
		
		@Override
		public String toString() {
			return rawName + " -> " + obfuscatedName + ": " + type;
		}
		
	}
	
	public record MethodData(String rawName, String obfuscatedName, String returnClass, String[] parameters) {
		
		
		public Method getMethod(Class<?> clazz) throws ClassNotFoundException, NoSuchMethodException {
			Class<?>[] parameterClasses = new Class[parameters.length];
			for (int i = 0; i < parameters.length; i++) {
				if (parameters[i].isBlank())
					continue;
				parameterClasses[i] = Class.forName(parameters[i]);
			}
			return clazz.getDeclaredMethod(obfuscatedName, parameterClasses);
		}
		
		@Override
		public String toString() {
			return rawName + " -> " + obfuscatedName + "(" + String.join(",", parameters) + "):" + returnClass;
		}
		
	}
	
	
}
