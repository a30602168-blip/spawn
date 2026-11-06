package jimyyyyyyyy.registry;

import jimyyyyyyyy.SpawnSmt;
import jimyyyyyyyy.portal.PortalBlock;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;

public final class SmtBlocks {
	public static final PortalBlock PORTAL = registerPortal();

	private SmtBlocks() {
	}

	private static PortalBlock registerPortal() {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SpawnSmt.id("portal"));
		BlockBehaviour.Properties properties = BlockBehaviour.Properties.of()
				.setId(key)
				.noCollision()
				.noOcclusion()
				.strength(-1.0F, 3_600_000.0F)
				.lightLevel(state -> 11)
				.randomTicks();
		return Registry.register(BuiltInRegistries.BLOCK, key, new PortalBlock(properties));
	}

	/** Forces the class to load so the blocks get registered. */
	public static void init() {
	}
}
