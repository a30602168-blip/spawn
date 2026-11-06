package jimyyyyyyyy.item;

import jimyyyyyyyy.util.Fx;
import jimyyyyyyyy.util.Messages;
import jimyyyyyyyy.util.Rays;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/** Right click calls down a lightning bolt on the block the player is looking at. */
public final class ThunderStaffItem extends Item {
	private static final double RANGE = 50.0;
	private static final double MIN_DISTANCE = 4.0;
	private static final int COOLDOWN_TICKS = 60;

	public ThunderStaffItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (!(level instanceof ServerLevel serverLevel) || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}

		BlockHitResult hit = Rays.raycast(player, RANGE);
		if (hit.getType() == HitResult.Type.MISS) {
			Messages.actionBar(serverPlayer, "message.spawnsmt.thunder.no_target");
			return InteractionResult.FAIL;
		}
		if (hit.getLocation().distanceTo(player.getEyePosition()) < MIN_DISTANCE) {
			Messages.actionBar(serverPlayer, "message.spawnsmt.thunder.too_close");
			return InteractionResult.FAIL;
		}

		BlockPos strikePos = hit.getBlockPos().relative(hit.getDirection());
		LightningBolt bolt = EntityType.LIGHTNING_BOLT.spawn(serverLevel, strikePos, EntitySpawnReason.TRIGGERED);
		if (bolt == null) {
			return InteractionResult.FAIL;
		}
		bolt.setCause(serverPlayer);

		Fx.line(serverLevel, player.getEyePosition().subtract(0.0, 0.3, 0.0), hit.getLocation(), ParticleTypes.ELECTRIC_SPARK, 0.5);
		player.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN_TICKS);
		return InteractionResult.SUCCESS;
	}
}
