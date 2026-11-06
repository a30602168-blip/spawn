package jimyyyyyyyy.item;

import java.util.List;

import jimyyyyyyyy.util.Fx;
import jimyyyyyyyy.util.Messages;
import jimyyyyyyyy.util.Motion;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/** Right click pulls creatures and items in front of the player towards them; sneak + right click shoves them away. */
public final class GravityWandItem extends Item {
	private static final double RANGE = 12.0;
	private static final double MIN_ALIGNMENT = 0.75;
	private static final double PULL_STRENGTH = 1.4;
	private static final double PUSH_STRENGTH = 1.8;
	private static final int COOLDOWN_TICKS = 20;

	public GravityWandItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		Vec3 eye = player.getEyePosition();
		Vec3 look = player.getLookAngle();
		boolean push = player.isShiftKeyDown();

		AABB area = new AABB(eye, eye.add(look.scale(RANGE))).inflate(4.0);
		List<Entity> targets = serverLevel.getEntitiesOfClass(Entity.class, area, entity -> isAffected(entity, player));

		int affected = 0;
		for (Entity target : targets) {
			Vec3 offset = target.position().add(0.0, target.getBbHeight() * 0.5, 0.0).subtract(eye);
			double distance = offset.length();
			if (distance < 0.5 || distance > RANGE || offset.normalize().dot(look) < MIN_ALIGNMENT) {
				continue;
			}

			Vec3 away = offset.normalize();
			Vec3 impulse;
			if (push) {
				impulse = away.scale(PUSH_STRENGTH).add(0.0, 0.3, 0.0);
			} else {
				double falloff = 1.0 - distance / RANGE * 0.5;
				impulse = away.scale(-PULL_STRENGTH * falloff).add(0.0, 0.1, 0.0);
				target.resetFallDistance();
			}
			Motion.set(target, target.getDeltaMovement().add(impulse));
			affected++;
		}

		if (affected == 0) {
			Messages.actionBar(serverPlayer, "message.spawnsmt.gravity.nothing");
			return InteractionResult.FAIL;
		}

		Vec3 muzzle = eye.add(look.scale(0.8)).subtract(0.0, 0.25, 0.0);
		Fx.line(serverLevel, muzzle, eye.add(look.scale(RANGE)), push ? ParticleTypes.CLOUD : ParticleTypes.END_ROD, 0.8);
		serverLevel.playSound(null, player.getX(), player.getY(), player.getZ(),
				SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.7F, push ? 0.6F : 1.6F);
		player.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN_TICKS);
		return InteractionResult.SUCCESS;
	}

	private static boolean isAffected(Entity entity, Player user) {
		return entity != user
				&& entity.isAlive()
				&& !entity.isSpectator()
				&& (entity instanceof LivingEntity || entity instanceof ItemEntity);
	}
}
