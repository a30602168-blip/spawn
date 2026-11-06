package jimyyyyyyyy.util;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class Rays {
	private Rays() {
	}

	/** Casts a ray from the player's eyes along their view direction; non-solid blocks are ignored. */
	public static BlockHitResult raycast(Player player, double range) {
		Vec3 start = player.getEyePosition();
		Vec3 end = start.add(player.getLookAngle().scale(range));
		return player.level().clip(new ClipContext(start, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
	}
}
