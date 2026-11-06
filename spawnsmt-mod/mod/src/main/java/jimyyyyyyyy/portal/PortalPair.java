package jimyyyyyyyy.portal;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

/** The blue and orange portal of one player. A missing portal is {@code null}. */
final class PortalPair {
	private Portal blue;
	private Portal orange;

	Portal get(boolean isOrange) {
		return isOrange ? orange : blue;
	}

	void set(boolean isOrange, Portal portal) {
		if (isOrange) {
			orange = portal;
		} else {
			blue = portal;
		}
	}

	boolean isEmpty() {
		return blue == null && orange == null;
	}

	boolean occupies(ResourceKey<Level> dimension, BlockPos pos) {
		return (blue != null && blue.occupies(dimension, pos)) || (orange != null && orange.occupies(dimension, pos));
	}
}
