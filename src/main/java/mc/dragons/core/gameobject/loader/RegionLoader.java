package mc.dragons.core.gameobject.loader;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.Document;
import org.bukkit.Location;
import org.bukkit.World;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.region.Region;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.storage.StorageUtil;

public class RegionLoader extends GameObjectLoader<Region> {

	private static RegionLoader INSTANCE;
	private boolean allLoaded = false;
	private GameObjectRegistry masterRegistry;
	private Map<String, Set<Region>> worldToRegions;
	
	private RegionLoader(Dragons instance, StorageManager storageManager) {
		super(instance, storageManager);
		//loadAll(); // Apparently when we do this it thinks that GameObjectType.REGION is null until construction is completed. So we need to move this out of constructor. Grr lazy loading
		masterRegistry = instance.getGameObjectRegistry();
		worldToRegions = new HashMap<>();
	}
	
	public synchronized static RegionLoader getInstance(Dragons instance, StorageManager storageManager) {
		if(INSTANCE == null) {
			INSTANCE = new RegionLoader(instance, storageManager);
		}
		return INSTANCE;
	}
	
	@Override
	public Region loadObject(StorageAccess storageAccess) {
		lazyLoadAll();
		return new Region(storageManager, storageAccess);
	}
	
	public Region getRegionByName(String name) {
		lazyLoadAll();
		for(GameObject gameObject : masterRegistry.getRegisteredObjects(GameObjectType.REGION)) {
			Region region = (Region) gameObject;
			if(region.getName().equalsIgnoreCase(name)) {
				return region;
			}
		}
		return null;
	}
	
	public Set<Region> getRegionsByWorld(World world) {
		return getRegionsByWorld(world.getName());
	}
	
	public Set<Region> getRegionsByWorld(String worldName) {
		return worldToRegions.getOrDefault(worldName, new HashSet<>());
	}
	
	@Deprecated
	public Set<Region> getRegionsByLocation(Location loc) {
		lazyLoadAll();
		Set<Region> result = new HashSet<>();
		for(Region region : getRegionsByWorld(loc.getWorld())) {
			if(region.contains(loc)) {
				result.add(region);
			}
		}
		return result;
	}
	
	public Set<Region> getRegionsByLocationXZ(Location loc) {
		lazyLoadAll();
		Set<Region> result = new HashSet<>();
		for(Region region : getRegionsByWorld(loc.getWorld())) {
			if(region.containsXZ(loc)) {
				result.add(region);
			}
		}
		return result;
	}
	
	public Region registerNew(String name, Location corner1, Location corner2) {
		lazyLoadAll();
		if(corner1.getWorld() != corner2.getWorld()) {
			throw new IllegalArgumentException("Corners must be in the same world");
		}
		StorageAccess storageAccess = storageManager.getNewStorageAccess(GameObjectType.REGION);
		storageAccess.set("name", name);
		storageAccess.set("world", corner1.getWorld().getName());
		storageAccess.set("corner1", StorageUtil.vecToDoc(corner1.toVector()));
		storageAccess.set("corner2", StorageUtil.vecToDoc(corner2.toVector()));
		storageAccess.set("flags", new Document("fullname", "New Region").append("desc", "").append("lvmin", "0").append("lvrec", "0").append("showtitle", "false")
				.append("allowhostile", "true").append("pvp", "true").append("pve", "true"));
		storageAccess.set("spawnRates", new Document());
		Region region = new Region(storageManager, storageAccess);
		masterRegistry.getRegisteredObjects().add(region);
		Set<Region> regions = worldToRegions.getOrDefault(region.getWorld().getName(), new HashSet<>());
		regions.add(region);
		worldToRegions.put(region.getWorld().getName(), regions); // in case there wasn't an existing mapping
		return region;
	}
	
	public void loadAll(boolean force) {
		if(allLoaded && !force) return;
		allLoaded = true; // must be here to prevent infinite recursion -> stack overflow -> death
		masterRegistry.removeFromRegistry(GameObjectType.REGION);
		storageManager.getAllStorageAccess(GameObjectType.REGION).stream().forEach((storageAccess) -> {
			Region region = loadObject(storageAccess);
			masterRegistry.getRegisteredObjects().add(region);
			Set<Region> regions = worldToRegions.getOrDefault(region.getWorld().getName(), new HashSet<>());
			regions.add(region);
			worldToRegions.put(region.getWorld().getName(), regions); // in case there wasn't an existing mapping
		});
	}
	
	public void lazyLoadAll() {
		loadAll(false);
	}

}
