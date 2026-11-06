package jimyyyyyyyy.item;

import jimyyyyyyyy.util.Fx;
import jimyyyyyyyy.util.Messages;
import jimyyyyyyyy.util.Rays;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Right click teleports the player forward along their view direction until something blocks the way. */
public final class BlinkStaffItem extends Item {
	private static final double MAX_DISTANCE = 20.0;
	private static final double MIN_DISTANCE = 1.5;
	private static final double STEP = 0.5;
	private static final int COOLDOWN_TICKS = 30;

	public BlinkStaffItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		Vec3 look = player.getLookAngle();
		BlockHitResult hit = Rays.raycast(player, MAX_DISTANCE);
		double reach = hit.getType() == HitResult.Type.MISS
				? MAX_DISTANCE
				: Math.min(MAX_DISTANCE, hit.getLocation().distanceTo(player.getEyePosition()));

		Vec3 destination = null;
		for (double distance = reach; distance >= MIN_DISTANCE; distance -= STEP) {
			AABB box = player.getBoundingBox().move(look.scale(distance));
			if (serverLevel.noCollision(player, box) && !serverLevel.containsAnyLiquid(box)) {
				destination = player.position().add(look.scale(distance));
				break;
			}
		}
		if (destination == null) {
			Messages.actionBar(serverPlayer, "message.spawnsmt.blink.blocked");
			return InteractionResult.FAIL;
		}

		Vec3 origin = player.position();
		Fx.burst(serverLevel, ParticleTypes.PORTAL, origin.add(0.0, 1.0, 0.0), 30, 0.4, 0.5);
		serverLevel.playSound(null, origin.x, origin.y, origin.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);

		serverPlayer.teleport(new TeleportTransition(
				serverLevel, destination, Vec3.ZERO, serverPlayer.getYRot(), serverPlayer.getXRot(), TeleportTransition.DO_NOTHING));
		serverPlayer.resetFallDistance();

		Fx.burst(serverLevel, ParticleTypes.REVERSE_PORTAL, destination.add(0.0, 1.0, 0.0), 30, 0.4, 0.2);
		serverLevel.playSound(null, destination.x, destination.y, destination.z, SoundEvents.CHORUS_FRUIT_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.2F);
		player.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN_TICKS);
		return InteractionResult.SUCCESS;
	}
}
