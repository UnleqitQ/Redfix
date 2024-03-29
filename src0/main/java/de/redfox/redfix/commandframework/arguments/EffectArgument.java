//
// MIT License
//
// Copyright (c) 2021 Alexander Söderberg & Contributors
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
//
// The above copyright notice and this permission notice shall be included in all
// copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
//
package de.redfox.redfix.commandframework.arguments;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.bukkit.BukkitCaptionKeys;
import cloud.commandframework.captions.CaptionVariable;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import cloud.commandframework.exceptions.parsing.ParserException;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.potion.PotionEffectType;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.checkerframework.checker.nullness.qual.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.function.BiFunction;

/**
 * cloud argument type that parses Bukkit {@link Enchantment enchantments}
 *
 * @param <C> Command sender type
 */
public class EffectArgument<C> extends CommandArgument<C, PotionEffectType> {
	
	protected EffectArgument(final boolean required, final @NonNull String name, final @NonNull String defaultValue, final @Nullable BiFunction<@NonNull CommandContext<C>, @NonNull String, @NonNull List<@NonNull String>> suggestionsProvider, final @NonNull ArgumentDescription defaultDescription) {
		super(required, name, new EffectParser<>(), defaultValue, PotionEffectType.class, suggestionsProvider,
				defaultDescription);
	}
	
	/**
	 * Create a new builder
	 *
	 * @param name Name of the argument
	 * @param <C>  Command sender type
	 * @return Created builder
	 */
	public static @NonNull <C> Builder<C> newBuilder(final @NonNull String name) {
		return new Builder<>(name);
	}
	
	/**
	 * Create a new required argument
	 *
	 * @param name Argument name
	 * @param <C>  Command sender type
	 * @return Created argument
	 */
	public static <C> @NonNull CommandArgument<C, PotionEffectType> of(final @NonNull String name) {
		return EffectArgument.<C>newBuilder(name).asRequired().build();
	}
	
	/**
	 * Create a new optional argument
	 *
	 * @param name Argument name
	 * @param <C>  Command sender type
	 * @return Created argument
	 */
	public static <C> @NonNull CommandArgument<C, PotionEffectType> optional(final @NonNull String name) {
		return EffectArgument.<C>newBuilder(name).asOptional().build();
	}
	
	/**
	 * Create a new optional argument with a default value
	 *
	 * @param name        Argument name
	 * @param enchantment Default value
	 * @param <C>         Command sender type
	 * @return Created argument
	 */
	public static <C> @NonNull CommandArgument<C, PotionEffectType> optional(final @NonNull String name, final @NonNull PotionEffectType enchantment) {
		return EffectArgument.<C>newBuilder(name).asOptionalWithDefault(enchantment.getKey().toString()).build();
	}
	
	public static final class Builder<C> extends CommandArgument.Builder<C, PotionEffectType> {
		
		private Builder(final @NonNull String name) {
			super(PotionEffectType.class, name);
		}
		
		@Override
		public @NonNull CommandArgument<C, PotionEffectType> build() {
			return new EffectArgument<>(this.isRequired(), this.getName(), this.getDefaultValue(),
					this.getSuggestionsProvider(), this.getDefaultDescription());
		}
		
	}
	
	public static final class EffectParser<C> implements ArgumentParser<C, PotionEffectType> {
		
		@Override
		@SuppressWarnings ("deprecation")
		public @NonNull ArgumentParseResult<PotionEffectType> parse(final @NonNull CommandContext<C> commandContext, final @NonNull Queue<@NonNull String> inputQueue) {
			final String input = inputQueue.peek();
			if (input == null) {
				return ArgumentParseResult.failure(new NoInputProvidedException(EffectParser.class, commandContext));
			}
			
			final NamespacedKey key;
			try {
				if (input.contains(":")) {
					final String[] splitInput = input.split(":");
					key = new NamespacedKey(splitInput[0], splitInput[1]);
				}
				else {
					key = NamespacedKey.minecraft(input);
				}
			} catch (final Exception ex) {
				return ArgumentParseResult.failure(new EffectParseException(input, commandContext));
			}
			
			final PotionEffectType enchantment = PotionEffectType.getByKey(key);
			if (enchantment == null) {
				return ArgumentParseResult.failure(new EffectParseException(input, commandContext));
			}
			inputQueue.remove();
			return ArgumentParseResult.success(enchantment);
		}
		
		@Override
		public @NonNull List<@NonNull String> suggestions(final @NonNull CommandContext<C> commandContext, final @NonNull String input) {
			final List<String> completions = new ArrayList<>();
			for (PotionEffectType value : PotionEffectType.values()) {
				if (value.getKey().getNamespace().equals(NamespacedKey.MINECRAFT)) {
					completions.add(value.getKey().getKey());
				}
				else {
					completions.add(value.getKey().toString());
				}
			}
			return completions;
		}
		
	}
	
	
	public static final class EffectParseException extends ParserException {
		
		private static final long serialVersionUID = 1415174766296065151L;
		private final String input;
		
		/**
		 * Construct a new EnchantmentParseException
		 *
		 * @param input   Input
		 * @param context Command context
		 */
		public EffectParseException(final @NonNull String input, final @NonNull CommandContext<?> context) {
			super(EffectParser.class, context, BukkitCaptionKeys.ARGUMENT_PARSE_FAILURE_ENCHANTMENT,
					CaptionVariable.of("input", input));
			this.input = input;
		}
		
		/**
		 * Get the input
		 *
		 * @return Input
		 */
		public @NonNull String getInput() {
			return this.input;
		}
		
	}
	
}
