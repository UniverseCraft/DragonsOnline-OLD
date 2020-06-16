package mc.dragons.dragons.core.tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.bukkit.Location;
import org.bukkit.World;

import mc.dragons.dragons.core.Dragons;
import mc.dragons.dragons.core.gameobject.GameObject;
import mc.dragons.dragons.core.gameobject.GameObjectType;
import mc.dragons.dragons.core.gameobject.loader.GameObjectRegistry;
import mc.dragons.dragons.core.gameobject.loader.NPCLoader;
import mc.dragons.dragons.core.gameobject.loader.RegionLoader;
import mc.dragons.dragons.core.gameobject.npc.NPC;
import mc.dragons.dragons.core.gameobject.player.User;
import mc.dragons.dragons.core.gameobject.region.Region;

/**
 * Repeating task that spawns region-based entities
 * around players.
 * 
 * @author Rick
 *
 */
public class SpawnEntityTask {

	private static SpawnEntityTask INSTANCE;

	private Dragons plugin;
	private GameObjectRegistry registry;
	private NPCLoader npcLoader;
	private RegionLoader regionLoader;
	
	private SpawnEntityTask(Dragons instance) {
		this.plugin = instance;
		this.registry = instance.getGameObjectRegistry();
		this.npcLoader = (NPCLoader) GameObjectType.NPC.<NPC>getLoader();
		this.regionLoader = (RegionLoader) GameObjectType.REGION.<Region>getLoader();
	}
	
	public synchronized static SpawnEntityTask getInstance(Dragons pluginInstance) {
		if(INSTANCE == null) {
			INSTANCE = new SpawnEntityTask(pluginInstance);
		}
		return INSTANCE;
	}
	
	public void run() {
		if(!plugin.getServerOptions().isCustomSpawningEnabled()) return;
		for(GameObject gameObject : registry.getRegisteredObjects(GameObjectType.USER)) {
			User user = (User)gameObject;
			World world = user.p().getWorld();
			
			Set<Region> regions = regionLoader.getRegionsByLocationXZ(user.p().getLocation());
			Map<String, Double> spawnRates = new HashMap<>();
			for(Region region : regions) {
				for(Entry<String, Double> entry : region.getSpawnRates().entrySet()) {
					if(entry.getValue() > spawnRates.getOrDefault(entry.getKey(), 0.0)) {
						spawnRates.put(entry.getKey(), entry.getValue());
					}
				}
			}
			for(Entry<String, Double> spawnRate : spawnRates.entrySet()) {
				boolean spawn = Math.random() <= spawnRate.getValue() / 100;
				if(spawn) {
					double xOffset = Math.signum(Math.random() - 0.5) * (5 + Math.random() * 10);
					double zOffset = Math.signum(Math.random() - 0.5) * (5 + Math.random() * 10);
					double yOffset = 2;
					Location loc = user.p().getLocation().add(xOffset, yOffset, zOffset);
					npcLoader.registerNew(world, loc, spawnRate.getKey());
					for(int i = 0; i < 50; i++) {
						if(loc.getBlock().getType().isSolid()) {
							loc.add(0, 1, 0);
						}
						else break;
					}
					
				}
			}
		}
	}
	
}
