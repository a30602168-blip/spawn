package jimyyyyyyyy.item;

import jimyyyyyyyy.portal.PortalManager;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Right click shoots a blue portal, sneak + right click shoots an orange one. */
public final class PortalGunItem extends Item {
	private static final int COOLDOWN_TICKS = 8;

	public PortalGunItem(Item.Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResult.SUCCESS;
		}
		PortalManager.shoot(serverPlayer, serverPlayer.isShiftKeyDown());
		player.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN_TICKS);
		return InteractionResult.SUCCESS;
	}
}
