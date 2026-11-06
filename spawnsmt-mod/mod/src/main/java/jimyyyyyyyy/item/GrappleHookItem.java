package jimyyyyyyyy.item;

import jimyyyyyyyy.util.Fx;
import jimyyyyyyyy.util.Messages;
import jimyyyyyyyy.util.Motion;
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
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Right click hurls the player towards the block they are looking at. */
public final class GrappleHookItem extends Item {
	private static final double RANGE = 40.0;
	private static final double MIN_SPEED = 0.8;
	private static final double MAX_SPEED = 2.4;
	private static final int COOLDOWN_TICKS = 15;

	public GrappleHookItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		BlockHitResult hit = Rays.raycast(player, RANGE);
		if (hit.getType() == HitResult.Type.MISS) {
			Messages.actionBar(serverPlayer, "message.spawnsmt.grapple.no_target");
			return InteractionResult.FAIL;
		}

		Vec3 target = hit.getLocation();
		Vec3 origin = player.position().add(0.0, player.getBbHeight() * 0.5, 0.0);
		Vec3 toTarget = target.subtract(origin);
		double speed = Math.min(MAX_SPEED, MIN_SPEED + toTarget.length() * 0.07);
		Vec3 velocity = toTarget.normalize().scale(speed).add(0.0, 0.15, 0.0);

		Fx.line(serverLevel, player.getEyePosition().subtract(0.0, 0.3, 0.0), target, ParticleTypes.END_ROD, 0.6);
		Fx.burst(serverLevel, ParticleTypes.CRIT, target, 10, 0.2, 0.1);
		serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.8F, 0.8F);

		player.resetFallDistance();
		Motion.set(player, velocity);
		player.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN_TICKS);
		return InteractionResult.SUCCESS;
	}
}
