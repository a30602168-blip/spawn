package jimyyyyyyyy.registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import jimyyyyyyyy.SpawnSmt;
import jimyyyyyyyy.item.BlinkStaffItem;
import jimyyyyyyyy.item.DefibrillatorItem;
import jimyyyyyyyy.item.FireExtinguisherItem;
import jimyyyyyyyy.item.GrappleHookItem;
import jimyyyyyyyy.item.GravityWandItem;
import jimyyyyyyyy.item.PortalGunItem;
import jimyyyyyyyy.item.ThunderStaffItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.component.ItemLore;

public final class SmtItems {
	private static final Map<String, Item> ITEMS = new LinkedHashMap<>();

	public static final Item PORTAL_GUN = register("portalgun", PortalGunItem::new, Rarity.EPIC);
	public static final Item GRAPPLE_HOOK = register("grapplehook", GrappleHookItem::new, Rarity.RARE);
	public static final Item BLINK_STAFF = register("blinkstaff", BlinkStaffItem::new, Rarity.RARE);
	public static final Item THUNDER_STAFF = register("thunderstaff", ThunderStaffItem::new, Rarity.EPIC);
	public static final Item GRAVITY_WAND = register("gravitywand", GravityWandItem::new, Rarity.RARE);
	public static final Item FIRE_EXTINGUISHER = register("fireextinguisher", FireExtinguisherItem::new, Rarity.UNCOMMON);
	public static final Item DEFIBRILLATOR = register("defibrillator", DefibrillatorItem::new, Rarity.UNCOMMON);

	private SmtItems() {
	}

	private static Item register(String name, Function<Item.Properties, Item> factory, Rarity rarity) {
		ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SpawnSmt.id(name));
		Item.Properties properties = new Item.Properties()
				.setId(key)
				.stacksTo(1)
				.rarity(rarity)
				.component(DataComponents.LORE, new ItemLore(lore(name)));
		Item item = Registry.register(BuiltInRegistries.ITEM, key, factory.apply(properties));
		ITEMS.put(name, item);
		return item;
	}

	private static List<Component> lore(String name) {
		return List.of(loreLine("item.spawnsmt." + name + ".lore.1"), loreLine("item.spawnsmt." + name + ".lore.2"));
	}

	private static Component loreLine(String key) {
		return Component.translatable(key)
				.withStyle(ChatFormatting.GRAY)
				.withStyle(style -> style.withItalic(false));
	}

	/** Names usable as the argument of {@code /spawnsmt}. */
	public static Set<String> names() {
		return Collections.unmodifiableSet(ITEMS.keySet());
	}

	public static Optional<Item> get(String name) {
		return Optional.ofNullable(ITEMS.get(name));
	}

	/** Forces the class to load so the items get registered. */
	public static void init() {
	}
}
