package mc.dragons.core.gameobject.region;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.Vector;

import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.loader.FloorLoader;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;
import mc.dragons.core.storage.StorageUtil;

public class Region extends GameObject {
	
	private static FloorLoader floorLoader;
	
	// Used to cache data about a region when we know it hasn't been modified to speed up real-time operations
	private class CachedRegionData {
		private Location min;
		private Location max;
		
		public CachedRegionData(StorageAccess storageAccess) {
			World world = Bukkit.getWorld((String)storageAccess.get("world"));
			Document doc1 = (Document)storageAccess.get("corner1");
			Document doc2 = (Document)storageAccess.get("corner2");
			Vector vec1 = StorageUtil.docToVec(doc1);
			Vector vec2 = StorageUtil.docToVec(doc2);
			this.min = Vector.getMinimum(vec1, vec2).toLocation(world);
			this.max = Vector.getMaximum(vec1, vec2).toLocation(world);
		}
		
		public Location getMin() {
			return min;
		}
		
		public Location getMax() {
			return max;
		}
	}
	
	private CachedRegionData regionData;
	private Floor floor;
	
	public Region(StorageManager storageManager, StorageAccess storageAccess) {
		super(GameObjectType.REGION, storageAccess.getIdentifier().getUUID(), storageManager);
		regionData = new CachedRegionData(storageAccess);
		if(floorLoader == null) {
			floorLoader = (FloorLoader) GameObjectType.FLOOR.<Floor>getLoader();
		}
		floor = floorLoader.fromWorld(getWorld());
	}
	
	public Location getMin() {
		return regionData.getMin();
	}
	
	public Location getMax() {
		return regionData.getMax();
	}
	
	public String getName() {
		return (String) getData("name");
	}
	
	public Document getFlags() {
		return (Document) getData("flags");
	}
	
	public void setFlag(String flag, Object value) {
		Document update = storageAccess.getDocument();
		Document flags = (Document) update.get("flags");
		flags.append(flag, value);
		storageAccess.update(update);
	}
	
	public Map<String, Double> getSpawnRates() {
		Map<String, Double> spawnRates = new HashMap<>();
		for(Entry<String, Object> entry : ((Document) getData("spawnRates")).entrySet()) {
			spawnRates.put(entry.getKey(), (double) entry.getValue());
		}
		return spawnRates;
	}
	
	public double getSpawnRate(String npcClass) {
		for(Entry<String, Object> entry : ((Document) getData("spawnRates")).entrySet()) {
			if(entry.getKey().equalsIgnoreCase(npcClass)) {
				return (double)entry.getValue();
			}
		}
		return 0.0;
	}
	
	public void setSpawnRate(String npcClass, double spawnRate) {
		Document update = storageAccess.getDocument();
		Document spawnRates = (Document) update.get("spawnRates");
		spawnRates.append(npcClass, spawnRate);
		storageAccess.update(update);
	}
	
	public World getWorld() {
		return getMin().getWorld();
	}
	
	public Floor getFloor() {
		return floor;
	}
	
	public boolean containsXZ(Location test) {
		if(getWorld() != test.getWorld()) return false;
		return test.getX() >= getMin().getX() && test.getX() <= getMax().getX()
				&& test.getZ() >= getMin().getZ() && test.getZ() <= getMax().getZ();
	}
	
	public boolean contains(Location test) {
		return containsXZ(test) && test.getY() >= getMin().getY() && test.getY() <= getMax().getY();
	}
	
	public void updateCorners(Location corner1, Location corner2) {
		if(corner1.getWorld() != corner2.getWorld()) {
			throw new IllegalArgumentException("Corners must be in the same world");
		}
		storageAccess.set("world", corner1.getWorld().getName());
		storageAccess.set("corner1", StorageUtil.vecToDoc(corner1.toVector()));
		storageAccess.set("corner2", StorageUtil.vecToDoc(corner2.toVector()));
		regionData = new CachedRegionData(storageAccess);
		floor = floorLoader.fromWorld(getWorld());
	}
	

	
}
