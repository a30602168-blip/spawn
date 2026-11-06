package jimyyyyyyyy.portal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import jimyyyyyyyy.registry.SmtBlocks;
import jimyyyyyyyy.util.Fx;
import jimyyyyyyyy.util.Messages;
import jimyyyyyyyy.util.Rays;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/**
 * Server-side portal logic: placing portals, keeping them valid and moving entities between the
 * two portals of a pair. Every player owns one blue and one orange portal; anyone can use them.
 * Portals are not persisted - they are removed when their owner leaves or the server stops.
 */
public final class PortalManager {
	private static final double REACH = 64.0;
	private static final double TRIGGER_DEPTH = 0.4;
	private static final int TRAVEL_COOLDOWN_TICKS = 10;
	private static final int VALIDATE_INTERVAL_TICKS = 20;
	private static final int COOLDOWN_CLEANUP_INTERVAL_TICKS = 200;

	private static final Map<UUID, PortalPair> PAIRS = new HashMap<>();
	private static final Map<UUID, Long> LAST_TRAVEL = new HashMap<>();

	private PortalManager() {
	}

	// ---------------------------------------------------------------- placing

	/** Fires a portal from the player's gun; {@code orange} selects the colour. */
	public static void shoot(ServerPlayer player, boolean orange) {
		if (!(player.level() instanceof ServerLevel level)) {
			return;
		}

		BlockHitResult hit = Rays.raycast(player, REACH);
		Vec3 muzzle = player.getEyePosition().add(player.getLookAngle().scale(0.8)).subtract(0.0, 0.25, 0.0);
		Fx.line(level, muzzle, hit.getLocation(), trailParticle(orange), 0.5);
		level.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.6F, orange ? 1.4F : 1.1F);

		if (hit.getType() == HitResult.Type.MISS) {
			Messages.actionBar(player, "message.spawnsmt.portal.no_surface");
			return;
		}
		Direction face = hit.getDirection();
		if (face.getAxis().isVertical()) {
			Messages.actionBar(player, "message.spawnsmt.portal.walls_only");
			return;
		}

		PortalPair pair = PAIRS.computeIfAbsent(player.getUUID(), id -> new PortalPair());
		Portal previous = pair.get(orange);
		Optional<BlockPos> spot = findSpot(level, hit, face, previous);
		if (spot.isEmpty()) {
			Messages.actionBar(player, "message.spawnsmt.portal.invalid_surface");
			return;
		}

		if (previous != null) {
			removeBlocks(level.getServer(), previous);
		}
		Portal portal = new Portal(player.getUUID(), level.dimension(), spot.get(), face, orange);
		placeBlocks(level, portal);
		pair.set(orange, portal);

		Vec3 center = portal.wallBase().add(face.getStepX() * 0.5, Portal.HEIGHT * 0.5, face.getStepZ() * 0.5);
		Fx.burst(level, trailParticle(orange), center, 24, 0.4, 0.03);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 0.8F, orange ? 1.3F : 0.8F);
		Messages.actionBar(player, Component.translatable(orange ? "message.spawnsmt.portal.orange" : "message.spawnsmt.portal.blue")
				.withStyle(orange ? ChatFormatting.GOLD : ChatFormatting.AQUA));
	}

	private static ParticleOptions trailParticle(boolean orange) {
		return orange ? ParticleTypes.FLAME : ParticleTypes.SOUL_FIRE_FLAME;
	}

	private static Optional<BlockPos> findSpot(ServerLevel level, BlockHitResult hit, Direction face, Portal previous) {
		BlockPos front = hit.getBlockPos().relative(face);
		boolean upperHalf = hit.getLocation().y - front.getY() >= 0.5;
		BlockPos preferred = upperHalf ? front.below() : front;
		BlockPos alternative = upperHalf ? front : front.below();
		for (BlockPos candidate : List.of(preferred, alternative)) {
			if (canHost(level, candidate, face, previous)) {
				return Optional.of(candidate);
			}
		}
		return Optional.empty();
	}

	private static boolean canHost(ServerLevel level, BlockPos bottom, Direction face, Portal previous) {
		for (BlockPos cell : List.of(bottom, bottom.above())) {
			if (!level.isInWorldBounds(cell) || !isFree(level, cell, previous)) {
				return false;
			}
			if (!isSolidWall(level, cell.relative(face.getOpposite()), face)) {
				return false;
			}
		}
		return true;
	}

	private static boolean isFree(ServerLevel level, BlockPos pos, Portal previous) {
		if (previous != null && previous.occupies(level.dimension(), pos)) {
			return true;
		}
		BlockState state = level.getBlockState(pos);
		return state.isAir() || (state.canBeReplaced() && state.getFluidState().isEmpty());
	}

	private static boolean isSolidWall(ServerLevel level, BlockPos wall, Direction facing) {
		return level.getBlockState(wall).isFaceSturdy(level, wall, facing);
	}

	private static void placeBlocks(ServerLevel level, Portal portal) {
		BlockState lower = SmtBlocks.PORTAL.defaultBlockState()
				.setValue(PortalBlock.FACING, portal.facing())
				.setValue(PortalBlock.ORANGE, portal.orange())
				.setValue(PortalBlock.UPPER, false);
		level.setBlock(portal.bottom(), lower, Block.UPDATE_ALL);
		level.setBlock(portal.top(), lower.setValue(PortalBlock.UPPER, true), Block.UPDATE_ALL);
	}

	private static void removeBlocks(MinecraftServer server, Portal portal) {
		ServerLevel level = server.getLevel(portal.dimension());
		if (level == null || !level.hasChunkAt(portal.bottom())) {
			return; // leftover blocks in unloaded chunks clean themselves up via random ticks
		}
		for (BlockPos pos : List.of(portal.bottom(), portal.top())) {
			if (level.getBlockState(pos).is(SmtBlocks.PORTAL)) {
				level.removeBlock(pos, false);
			}
		}
	}

	private static boolean isIntact(ServerLevel level, Portal portal) {
		if (!matches(level.getBlockState(portal.bottom()), portal, false)
				|| !matches(level.getBlockState(portal.top()), portal, true)) {
			return false;
		}
		Direction wallSide = portal.facing().getOpposite();
		return isSolidWall(level, portal.bottom().relative(wallSide), portal.facing())
				&& isSolidWall(level, portal.top().relative(wallSide), portal.facing());
	}

	private static boolean matches(BlockState state, Portal portal, boolean upper) {
		return state.is(SmtBlocks.PORTAL)
				&& state.getValue(PortalBlock.FACING) == portal.facing()
				&& state.getValue(PortalBlock.ORANGE) == portal.orange()
				&& state.getValue(PortalBlock.UPPER) == upper;
	}

	// ---------------------------------------------------------------- lifecycle

	/** Whether the given block position belongs to a registered portal. */
	public static boolean isPortalCell(ResourceKey<Level> dimension, BlockPos pos) {
		for (PortalPair pair : PAIRS.values()) {
			if (pair.occupies(dimension, pos)) {
				return true;
			}
		}
		return false;
	}

	/** Removes both portals of a player. */
	public static void removeAll(MinecraftServer server, UUID owner) {
		PortalPair pair = PAIRS.remove(owner);
		if (pair != null) {
			removeBoth(server, pair);
		}
	}

	/** Removes every portal in the world. */
	public static void clearAll(MinecraftServer server) {
		for (PortalPair pair : PAIRS.values()) {
			removeBoth(server, pair);
		}
		PAIRS.clear();
		LAST_TRAVEL.clear();
	}

	private static void removeBoth(MinecraftServer server, PortalPair pair) {
		for (boolean orange : new boolean[] {false, true}) {
			Portal portal = pair.get(orange);
			if (portal != null) {
				removeBlocks(server, portal);
			}
		}
	}

	// ---------------------------------------------------------------- ticking

	public static void tick(MinecraftServer server) {
		if (PAIRS.isEmpty()) {
			return;
		}
		long time = server.overworld().getGameTime();
		boolean validate = time % VALIDATE_INTERVAL_TICKS == 0;

		for (PortalPair pair : List.copyOf(PAIRS.values())) {
			for (boolean orange : new boolean[] {false, true}) {
				Portal portal = pair.get(orange);
				if (portal == null) {
					continue;
				}
				ServerLevel level = server.getLevel(portal.dimension());
				if (level == null) {
					discard(server, pair, portal);
					continue;
				}
				if (validate && level.hasChunkAt(portal.bottom()) && !isIntact(level, portal)) {
					discard(server, pair, portal);
					continue;
				}
				Portal target = pair.get(!orange);
				if (target != null) {
					moveEntities(server, level, portal, target, time);
				}
			}
		}

		PAIRS.values().removeIf(PortalPair::isEmpty);
		if (time % COOLDOWN_CLEANUP_INTERVAL_TICKS == 0) {
			LAST_TRAVEL.values().removeIf(last -> time - last > 100);
		}
	}

	private static void discard(MinecraftServer server, PortalPair pair, Portal portal) {
		pair.set(portal.orange(), null);
		removeBlocks(server, portal);
	}

	private static void moveEntities(MinecraftServer server, ServerLevel level, Portal from, Portal to, long time) {
		List<Entity> candidates = level.getEntitiesOfClass(Entity.class, from.triggerBox(TRIGGER_DEPTH), PortalManager::canTravel);
		if (candidates.isEmpty()) {
			return;
		}
		ServerLevel destination = server.getLevel(to.dimension());
		if (destination == null) {
			return;
		}
		destination.getChunkAt(to.bottom()); // make sure the exit is loaded
		if (!isIntact(destination, to)) {
			return; // the periodic validation removes it
		}

		for (Entity entity : candidates) {
			Long last = LAST_TRAVEL.get(entity.getUUID());
			if (last == null || time - last >= TRAVEL_COOLDOWN_TICKS) {
				travel(entity, level, from, destination, to, time);
			}
		}
	}

	private static boolean canTravel(Entity entity) {
		return entity.isAlive()
				&& !entity.isSpectator()
				&& !entity.isPassenger()
				&& !entity.isVehicle()
				&& (entity instanceof LivingEntity || entity instanceof ItemEntity);
	}

	private static void travel(Entity entity, ServerLevel fromLevel, Portal from, ServerLevel toLevel, Portal to, long time) {
		// Walking into the wall (-from.facing) must turn into walking out of the exit wall (+to.facing).
		float turnDegrees = to.facing().toYRot() - from.facing().getOpposite().toYRot();
		double turn = Math.toRadians(turnDegrees);
		double cos = Math.cos(turn);
		double sin = Math.sin(turn);

		Vec3 entry = from.wallBase();
		Vec3 exit = to.wallBase();
		double relX = entity.getX() - entry.x;
		double relZ = entity.getZ() - entry.z;
		double depth = relX * from.facing().getStepX() + relZ * from.facing().getStepZ();
		double sideX = relX - depth * from.facing().getStepX();
		double sideZ = relZ - depth * from.facing().getStepZ();
		double rotatedSideX = sideX * cos - sideZ * sin;
		double rotatedSideZ = sideZ * cos + sideX * sin;

		double maxSide = Math.max(0.0, 0.5 - entity.getBbWidth() / 2.0);
		double sideLength = Math.hypot(rotatedSideX, rotatedSideZ);
		if (sideLength > maxSide && sideLength > 0.0) {
			double scale = maxSide / sideLength;
			rotatedSideX *= scale;
			rotatedSideZ *= scale;
		}

		double clearance = TRIGGER_DEPTH + entity.getBbWidth() / 2.0 + 0.1;
		double x = exit.x + rotatedSideX + to.facing().getStepX() * clearance;
		double z = exit.z + rotatedSideZ + to.facing().getStepZ() * clearance;
		double heightInPortal = Mth.clamp(entity.getY() - from.bottom().getY(), 0.0, Math.max(0.0, Portal.HEIGHT - entity.getBbHeight()));
		double y = to.bottom().getY() + heightInPortal;

		Vec3 velocity = entity.getDeltaMovement();
		Vec3 rotatedVelocity = new Vec3(velocity.x * cos - velocity.z * sin, velocity.y, velocity.z * cos + velocity.x * sin);
		float yaw = Mth.wrapDegrees(entity.getYRot() + turnDegrees);

		playTravelEffects(fromLevel, entity.position(), from.orange());
		Entity moved = entity.teleport(new TeleportTransition(
				toLevel, new Vec3(x, y, z), rotatedVelocity, yaw, entity.getXRot(), TeleportTransition.DO_NOTHING));
		Entity result = moved != null ? moved : entity;
		LAST_TRAVEL.put(result.getUUID(), time);
		playTravelEffects(toLevel, new Vec3(x, y, z), to.orange());
	}

	private static void playTravelEffects(ServerLevel level, Vec3 position, boolean orange) {
		Fx.burst(level, trailParticle(orange), position.add(0.0, 1.0, 0.0), 12, 0.3, 0.02);
		level.playSound(null, position.x, position.y, position.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, orange ? 1.2F : 0.9F);
	}
}
