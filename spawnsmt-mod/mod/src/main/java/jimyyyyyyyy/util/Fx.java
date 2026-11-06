package jimyyyyyyyy.util;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;

public final class Fx {
	private Fx() {
	}

	/** Draws a line of particles between two points. */
	public static void line(ServerLevel level, Vec3 from, Vec3 to, ParticleOptions particle, double spacing) {
		Vec3 delta = to.subtract(from);
		double length = delta.length();
		if (length < 1.0E-4) {
			return;
		}
		Vec3 step = delta.scale(1.0 / length);
		for (double travelled = 0.0; travelled <= length; travelled += spacing) {
			Vec3 point = from.add(step.scale(travelled));
			level.sendParticles(particle, point.x, point.y, point.z, 1, 0.0, 0.0, 0.0, 0.0);
		}
	}

	/** Spawns a small cloud of particles around a point. */
	public static void burst(ServerLevel level, ParticleOptions particle, Vec3 center, int count, double spread, double speed) {
		level.sendParticles(particle, center.x, center.y, center.z, count, spread, spread, spread, speed);
	}
}
