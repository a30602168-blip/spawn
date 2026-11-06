package jimyyyyyyyy.item;

import jimyyyyyyyy.util.Fx;
import jimyyyyyyyy.util.Messages;
import jimyyyyyyyy.util.Rays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Right click sprays foam that puts out fires and burning creatures where the player is aiming. */
public final class FireExtinguisherItem extends Item {
	private static final double RANGE = 8.0;
	private static final int RADIUS = 2;
	private static final int COOLDOWN_TICKS = 10;

	public FireExtinguisherItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		BlockHitResult hit = Rays.raycast(player, RANGE);
		Vec3 end = hit.getLocation();
		BlockPos center = hit.getType() == HitResult.Type.MISS
				? BlockPos.containing(end)
				: hit.getBlockPos().relative(hit.getDirection());

		Vec3 muzzle = player.getEyePosition().add(player.getLookAngle().scale(0.8)).subtract(0.0, 0.3, 0.0);
		Fx.line(serverLevel, muzzle, end, ParticleTypes.CLOUD, 0.4);
		Fx.burst(serverLevel, ParticleTypes.POOF, end, 8, 0.4, 0.02);

		int extinguished = 0;
		for (BlockPos pos : BlockPos.betweenClosed(center.offset(-RADIUS, -RADIUS, -RADIUS), center.offset(RADIUS, RADIUS, RADIUS))) {
			if (serverLevel.getBlockState(pos).is(BlockTags.FIRE)) {
				serverLevel.removeBlock(pos, false);
				extinguished++;
			}
		}
		AABB area = new AABB(center).inflate(RADIUS + 0.5);
		for (LivingEntity entity : serverLevel.getEntitiesOfClass(LivingEntity.class, area)) {
			if (entity.isOnFire()) {
				entity.setRemainingFireTicks(0);
				extinguished++;
			}
		}

		serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
				extinguished > 0 ? SoundEvents.FIRE_EXTINGUISH : SoundEvents.FIREWORK_ROCKET_LAUNCH, SoundSource.PLAYERS, 0.8F, 1.0F);
		if (extinguished > 0) {
			Messages.actionBar(serverPlayer, "message.spawnsmt.extinguisher.done");
		}
		player.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN_TICKS);
		return InteractionResult.SUCCESS;
	}
}
