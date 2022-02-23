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
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.EntityType;
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
public class EntityTypeArgument<C> extends CommandArgument<C, EntityType> {
	
	protected EntityTypeArgument(final boolean required, final @NonNull String name, final @NonNull String defaultValue, final @Nullable BiFunction<@NonNull CommandContext<C>, @NonNull String, @NonNull List<@NonNull String>> suggestionsProvider, final @NonNull ArgumentDescription defaultDescription) {
		super(required, name, new EntityTypeParser<>(), defaultValue, EntityType.class, suggestionsProvider,
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
	public static <C> @NonNull CommandArgument<C, EntityType> of(final @NonNull String name) {
		return EntityTypeArgument.<C>newBuilder(name).asRequired().build();
	}
	
	/**
	 * Create a new optional argument
	 *
	 * @param name Argument name
	 * @param <C>  Command sender type
	 * @return Created argument
	 */
	public static <C> @NonNull CommandArgument<C, EntityType> optional(final @NonNull String name) {
		return EntityTypeArgument.<C>newBuilder(name).asOptional().build();
	}
	
	/**
	 * Create a new optional argument with a default value
	 *
	 * @param name       Argument name
	 * @param entityType Default value
	 * @param <C>        Command sender type
	 * @return Created argument
	 */
	public static <C> @NonNull CommandArgument<C, EntityType> optional(final @NonNull String name, final @NonNull EntityType entityType) {
		return EntityTypeArgument.<C>newBuilder(name).asOptionalWithDefault(entityType.getKey().toString()).build();
	}
	
	public static final class Builder<C> extends CommandArgument.Builder<C, EntityType> {
		
		private Builder(final @NonNull String name) {
			super(EntityType.class, name);
		}
		
		@Override
		public @NonNull CommandArgument<C, EntityType> build() {
			return new EntityTypeArgument<>(this.isRequired(), this.getName(), this.getDefaultValue(),
					this.getSuggestionsProvider(), this.getDefaultDescription());
		}
		
	}
	
	public static final class EntityTypeParser<C> implements ArgumentParser<C, EntityType> {
		
		@Override
		@SuppressWarnings ("deprecation")
		public @NonNull ArgumentParseResult<EntityType> parse(final @NonNull CommandContext<C> commandContext, final @NonNull Queue<@NonNull String> inputQueue) {
			final String input = inputQueue.peek();
			if (input == null) {
				return ArgumentParseResult.failure(
						new NoInputProvidedException(EntityTypeParser.class, commandContext));
			}
			
			
			final EntityType enchantment = EntityType.fromName(input);
			if (enchantment == null) {
				return ArgumentParseResult.failure(new EntityTypeParseException(input, commandContext));
			}
			inputQueue.remove();
			return ArgumentParseResult.success(enchantment);
		}
		
		@Override
		public @NonNull List<@NonNull String> suggestions(final @NonNull CommandContext<C> commandContext, final @NonNull String input) {
			final List<String> completions = new ArrayList<>();
			for (EntityType value : EntityType.values()) {
				if (value.getName() != null)
					completions.add(value.getName());
			}
			return completions;
		}
		
	}
	
	
	public static final class EntityTypeParseException extends ParserException {
		
		private static final long serialVersionUID = 1415174766296065151L;
		private final String input;
		
		/**
		 * Construct a new EnchantmentParseException
		 *
		 * @param input   Input
		 * @param context Command context
		 */
		public EntityTypeParseException(final @NonNull String input, final @NonNull CommandContext<?> context) {
			super(EntityTypeParser.class, context, BukkitCaptionKeys.ARGUMENT_PARSE_FAILURE_ENCHANTMENT,
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
