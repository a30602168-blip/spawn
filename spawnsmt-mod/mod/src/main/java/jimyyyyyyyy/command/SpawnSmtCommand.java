package jimyyyyyyyy.command;

import java.util.Locale;
import java.util.Optional;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import jimyyyyyyyy.registry.SmtItems;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** {@code /spawnsmt <item>} - gives the executing OP one of the mod's items. */
public final class SpawnSmtCommand {
	private SpawnSmtCommand() {
	}

	public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal("spawnsmt")
				.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
				.then(Commands.argument("item", StringArgumentType.word())
						.suggests((context, builder) -> SharedSuggestionProvider.suggest(SmtItems.names(), builder))
						.executes(context -> give(context.getSource(), StringArgumentType.getString(context, "item")))));
	}

	private static int give(CommandSourceStack source, String name) throws CommandSyntaxException {
		ServerPlayer player = source.getPlayerOrException();
		Optional<Item> item = SmtItems.get(name.toLowerCase(Locale.ROOT));
		if (item.isEmpty()) {
			source.sendFailure(Component.translatable("commands.spawnsmt.unknown", name, String.join(", ", SmtItems.names())));
			return 0;
		}

		ItemStack stack = new ItemStack(item.get());
		Component itemName = stack.getHoverName();
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
		source.sendSuccess(() -> Component.translatable("commands.spawnsmt.give", itemName), true);
		return 1;
	}
}
