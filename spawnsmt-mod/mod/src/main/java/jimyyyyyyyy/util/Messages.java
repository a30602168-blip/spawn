package jimyyyyyyyy.util;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class Messages {
	private Messages() {
	}

	public static void actionBar(ServerPlayer player, Component message) {
		player.displayClientMessage(message, true);
	}

	public static void actionBar(ServerPlayer player, String translationKey) {
		actionBar(player, Component.translatable(translationKey));
	}
}
