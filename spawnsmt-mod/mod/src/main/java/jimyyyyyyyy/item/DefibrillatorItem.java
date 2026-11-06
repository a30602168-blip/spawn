package jimyyyyyyyy.item;

import java.util.List;

import jimyyyyyyyy.util.Fx;
import jimyyyyyyyy.util.Messages;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Right click shocks the creature in front of the player (or the player themself) and restores health. */
public final class DefibrillatorItem extends Item {
	private static final double RANGE = 3.5;
	private static final double MIN_ALIGNMENT = 0.8;
	private static final float HEAL_AMOUNT = 12.0F;
	private static final int COOLDOWN_TICKS = 100;

	public DefibrillatorItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		LivingEntity target = findTarget(serverLevel, player);
		if (target.getHealth() >= target.getMaxHealth()) {
			Messages.actionBar(serverPlayer, "message.spawnsmt.defibrillator.full_health");
			return InteractionResult.FAIL;
		}

		target.heal(HEAL_AMOUNT);
		Vec3 chest = target.position().add(0.0, target.getBbHeight() * 0.6, 0.0);
		Fx.burst(serverLevel, ParticleTypes.ELECTRIC_SPARK, chest, 25, 0.4, 0.3);
		Fx.burst(serverLevel, ParticleTypes.HEART, chest.add(0.0, 0.4, 0.0), 5, 0.3, 0.0);
		serverLevel.playSound(null, target.getX(), target.getY(), target.getZ(),
				SoundEvents.FIREWORK_ROCKET_BLAST, SoundSource.PLAYERS, 1.0F, 0.6F);

		player.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN_TICKS);
		return InteractionResult.SUCCESS;
	}

	/** The closest creature the player is looking at, or the player if there is none. */
	private static LivingEntity findTarget(ServerLevel level, Player player) {
		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		AABB area = new AABB(eye, eye.add(look.scale(RANGE))).inflate(1.0);
		List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, area,
				entity -> entity != player && entity.isAlive() && !entity.isSpectator());

		LivingEntity best = player;
		double bestDistance = Double.MAX_VALUE;
		for (LivingEntity candidate : candidates) {
			Vec3 offset = candidate.position().add(0.0, candidate.getBbHeight() * 0.5, 0.0).subtract(eye);
			double distance = offset.length();
			if (distance <= RANGE && distance < bestDistance && offset.normalize().dot(look) >= MIN_ALIGNMENT) {
				best = candidate;
				bestDistance = distance;
			}
		}
		return best;
	}
}
