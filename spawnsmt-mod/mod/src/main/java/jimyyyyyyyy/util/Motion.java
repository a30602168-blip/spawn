package jimyyyyyyyy.util;

import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

public final class Motion {
	private Motion() {
	}

	/** Sets an entity's velocity on the server and tells the client when the entity is a player. */
	public static void set(Entity entity, Vec3 velocity) {
		entity.setDeltaMovement(velocity);
		if (entity instanceof ServerPlayer player) {
			player.connection.send(new ClientboundSetEntityMotionPacket(player));
		}
	}
}
