package mc.dragons.core.gameobject.loader;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bson.Document;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.util.HiddenStringUtil;

public class ItemLoader extends GameObjectLoader<Item> {

	private static ItemLoader INSTANCE;
	private GameObjectRegistry masterRegistry;
	//private static Map<ItemStack, Item> itemStackToItem;
	private static Map<String, Item> uuidToItem;
	
	private ItemLoader(Dragons instance, StorageManager storageManager) {
		super(instance, storageManager);
		masterRegistry = instance.getGameObjectRegistry();
		//itemStackToItem = new HashMap<>();
		uuidToItem = new HashMap<>();
	}
	
	public synchronized static ItemLoader getInstance(Dragons instance, StorageManager storageManager) {
		if(INSTANCE == null) {
			INSTANCE = new ItemLoader(instance, storageManager);
		}
		return INSTANCE;
	}
	
	@Override
	public Item loadObject(StorageAccess storageAccess) {
		Material type = Material.valueOf((String) storageAccess.get("materialType"));
		ItemStack itemStack = new ItemStack(type);
		Item item = new Item(itemStack, storageManager, storageAccess);
		masterRegistry.getRegisteredObjects().add(item);
		//itemStackToItem.put(itemStack, item);
		uuidToItem.put(item.getUUID().toString(), item);
		return new Item(itemStack, storageManager, storageAccess);
	}
	
	public Item loadObject(UUID uuid) {
		return loadObject(storageManager.getStorageAccess(GameObjectType.ITEM, uuid));
	}
	
	public Item registerNew(ItemClass itemClass) {
		return registerNew(itemClass.getClassName(), itemClass.getName(), false, itemClass.getNameColor(), itemClass.getMaterial(), itemClass.getLevelMin(), itemClass.getCooldown(), itemClass.getSpeedBoost(),
				itemClass.isUnbreakable(), itemClass.getDamage(), itemClass.getArmor(), itemClass.getLore());
	}
	
	public Item registerNew(String className, String name, boolean custom, ChatColor nameColor, Material material, int levelMin, double cooldown, double speedBoost, boolean unbreakable, double damage, double armor, List<String> lore) {
		Document data = new Document("_id", UUID.randomUUID())
				.append("className", className)
				.append("name", name)
				.append("isCustom", custom)
				.append("nameColor", nameColor.name())
				.append("materialType", material.toString())
				.append("lvMin", levelMin)
				.append("cooldown", cooldown)
				.append("speedBoost", speedBoost)
				.append("unbreakable", unbreakable)
				.append("damage", damage)
				.append("armor", armor)
				.append("lore", lore);
		StorageAccess storageAccess = storageManager.getNewStorageAccess(GameObjectType.ITEM, data);
		ItemStack itemStack = new ItemStack(material);
		Item item = new Item(itemStack, storageManager, storageAccess);
		//itemStackToItem.put(itemStack, item);
		uuidToItem.put(item.getUUID().toString(), item);
		masterRegistry.getRegisteredObjects().add(item);
		return item;
	}
	
	public static Item fromBukkit(ItemStack itemStack) {
		if(itemStack == null) return null;
		if(itemStack.getItemMeta() == null) return null;
		if(itemStack.getItemMeta().getLore() == null) return null;
		if(itemStack.getItemMeta().getLore().size() < 2) return null;
		//return itemStackToItem.get(itemStack);
		return uuidToItem.get(HiddenStringUtil.extractHiddenString(itemStack.getItemMeta().getLore().get(1)));
	}

}
