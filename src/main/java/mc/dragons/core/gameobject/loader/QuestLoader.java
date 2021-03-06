package mc.dragons.core.gameobject.loader;

import java.util.ArrayList;
import java.util.UUID;

import org.bson.Document;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.storage.StorageAccess;
import mc.dragons.core.storage.StorageManager;

public class QuestLoader extends GameObjectLoader<Quest> {

	private static QuestLoader INSTANCE;
	private GameObjectRegistry masterRegistry;
	private boolean allLoaded = false;
	
	private QuestLoader(Dragons instance, StorageManager storageManager) {
		super(instance, storageManager);
		masterRegistry = instance.getGameObjectRegistry();
	}
	
	public synchronized static QuestLoader getInstance(Dragons instance, StorageManager storageManager) {
		if(INSTANCE == null) {
			INSTANCE = new QuestLoader(instance, storageManager);
		}
		return INSTANCE;
	}
	
	@Override
	public Quest loadObject(StorageAccess storageAccess) {
		lazyLoadAll();
		Quest quest = new Quest(storageManager, storageAccess);
		masterRegistry.getRegisteredObjects().add(quest);
		return quest;
	}
	
	public Quest getQuestByName(String questName) {
		lazyLoadAll();
		for(GameObject gameObject : masterRegistry.getRegisteredObjects(GameObjectType.QUEST)) {
			Quest quest = (Quest) gameObject;
			if(quest.getName().equalsIgnoreCase(questName)) {
				return quest;
			}
		}
		return null;
	}
	
	public Quest registerNew(String name, String questName, int lvMin) {
		lazyLoadAll();
		Document data = new Document("_id", UUID.randomUUID())
				.append("name", name)
				.append("questName", questName)
				.append("lvMin", lvMin )
				.append("steps", new ArrayList<Document>());
		StorageAccess storageAccess = storageManager.getNewStorageAccess(GameObjectType.QUEST, data);
		Quest quest = new Quest(storageManager, storageAccess);
		masterRegistry.getRegisteredObjects().add(quest);
		return quest;
	}	
	
	public void loadAll(boolean force) {
		if(allLoaded && !force) return;
		allLoaded = true; // must be here to prevent infinite recursion -> stack overflow -> death
		masterRegistry.removeFromRegistry(GameObjectType.QUEST);
		storageManager.getAllStorageAccess(GameObjectType.QUEST).stream().forEach((storageAccess) -> {
			masterRegistry.getRegisteredObjects().add(loadObject(storageAccess));	
		});
	}
	
	public void lazyLoadAll() {
		loadAll(false);
	}
	
}
