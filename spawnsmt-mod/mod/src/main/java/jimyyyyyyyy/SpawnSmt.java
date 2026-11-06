package jimyyyyyyyy;

import jimyyyyyyyy.command.SpawnSmtCommand;
import jimyyyyyyyy.portal.PortalManager;
import jimyyyyyyyy.registry.SmtBlocks;
import jimyyyyyyyy.registry.SmtItems;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SpawnSmt implements ModInitializer {
	public static final String MOD_ID = "spawnsmt";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		SmtBlocks.init();
		SmtItems.init();

		CommandRegistrationCallback.EVENT.register(
				(dispatcher, registryAccess, environment) -> SpawnSmtCommand.register(dispatcher));

		ServerTickEvents.END_SERVER_TICK.register(PortalManager::tick);
		ServerPlayConnectionEvents.DISCONNECT.register(
				(handler, server) -> PortalManager.removeAll(server, handler.getPlayer().getUUID()));
		ServerLifecycleEvents.SERVER_STOPPING.register(PortalManager::clearAll);

		LOGGER.info("SpawnSMT loaded with {} items", SmtItems.names().size());
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
