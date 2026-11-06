package jimyyyyyyyy.portal;

import java.util.UUID;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * One end of a portal pair. A portal is a 1x2 opening on a wall: {@code bottom} is the lower air
 * cell, {@code facing} points from the wall into the room.
 */
public record Portal(UUID owner, ResourceKey<Level> dimension, BlockPos bottom, Direction facing, boolean orange) {
	public static final int HEIGHT = 2;

	public BlockPos top() {
		return bottom.above();
	}

	public boolean occupies(ResourceKey<Level> otherDimension, BlockPos pos) {
		return dimension.equals(otherDimension) && (bottom.equals(pos) || top().equals(pos));
	}

	/** Centre of the wall surface the portal sits on, at the height of its lower edge. */
	public Vec3 wallBase() {
		return new Vec3(
				bottom.getX() + 0.5 - facing.getStepX() * 0.5,
				bottom.getY(),
				bottom.getZ() + 0.5 - facing.getStepZ() * 0.5);
	}

	/** Thin slab in front of the wall; entities touching it travel through the portal. */
	public AABB triggerBox(double depth) {
		Vec3 base = wallBase();
		double outerX = base.x + facing.getStepX() * depth;
		double outerZ = base.z + facing.getStepZ() * depth;
		double halfX = facing.getStepX() != 0 ? 0.0 : 0.5;
		double halfZ = facing.getStepZ() != 0 ? 0.0 : 0.5;
		return new AABB(
				Math.min(base.x, outerX) - halfX, base.y, Math.min(base.z, outerZ) - halfZ,
				Math.max(base.x, outerX) + halfX, base.y + HEIGHT, Math.max(base.z, outerZ) + halfZ);
	}
}
