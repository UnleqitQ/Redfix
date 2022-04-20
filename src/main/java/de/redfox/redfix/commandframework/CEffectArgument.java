package de.redfox.redfix.commandframework;

import me.unleqitq.commandframework.building.argument.FrameworkArgument;
import me.unleqitq.custompotioneffectapi.CPotionEffectType;
import org.bukkit.NamespacedKey;

import java.util.ArrayList;

public class CEffectArgument extends FrameworkArgument<CPotionEffectType> {
	
	public CEffectArgument(CEffectArgument.Builder builder) {
		super(builder);
	}
	
	public static CEffectArgument.Builder of(String name) {
		return new CEffectArgument.Builder(name);
	}
	
	public static CEffectArgument.Builder optional(String name, CPotionEffectType defaultValue) {
		return (CEffectArgument.Builder) (new CEffectArgument.Builder(name)).optional(defaultValue);
	}
	
	public static class Builder
			extends me.unleqitq.commandframework.building.argument.FrameworkArgument.Builder<CPotionEffectType> {
		
		public Builder(String name) {
			super(name, (c, a) -> a.contains(":") ? CPotionEffectType.getByKey(
					NamespacedKey.fromString(a.toLowerCase())) : CPotionEffectType.getByKey(
					NamespacedKey.minecraft(a.toLowerCase())), (c, a) -> new ArrayList(
					CPotionEffectType.values().stream().map(CPotionEffectType::getKey).filter(
							(k) -> k.getKey().toLowerCase().startsWith(
									a.toLowerCase()) || k.toString().toLowerCase().startsWith(a.toLowerCase())).map(
							NamespacedKey::asString).toList()));
		}
		
		public CEffectArgument.Builder setDescription(String description) {
			this.description = description;
			return this;
		}
		
		public CEffectArgument build() {
			return new CEffectArgument(this);
		}
		
		public CEffectArgument.Builder clone() {
			CEffectArgument.Builder builder = new CEffectArgument.Builder(this.name);
			builder.optional = this.optional;
			builder.parser = this.parser;
			builder.defaultValue = this.defaultValue;
			builder.tabCompleteProvider = this.tabCompleteProvider;
			builder.description = this.description;
			return builder;
		}
		
	}
	
}