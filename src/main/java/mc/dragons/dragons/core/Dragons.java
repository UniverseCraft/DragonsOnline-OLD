package mc.dragons.dragons.core;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

import mc.dragons.dragons.core.bridge.Bridge;
import mc.dragons.dragons.core.bridge.impl.Bridge_Spigot1_8_R3;
import mc.dragons.dragons.core.events.DeathEventListener;
import mc.dragons.dragons.core.events.JoinEventListener;
import mc.dragons.dragons.core.events.QuitEventListener;
import mc.dragons.dragons.core.gameobject.loader.GameObjectRegistry;
import mc.dragons.dragons.core.storage.StorageManager;
import mc.dragons.dragons.core.storage.impl.MongoConfig;
import mc.dragons.dragons.core.storage.impl.MongoStorageManager;
import mc.dragons.dragons.core.tasks.AutoSaveTask;

/**
 * The main plugin class for Dragons RPG.
 * 
 * @author Rick
 *
 */
public class Dragons extends JavaPlugin {
	private static Dragons INSTANCE;
	private Bridge bridge;
	
	private StorageManager storageManager;
	private GameObjectRegistry gameObjectRegistry;
	
	private AutoSaveTask autoSaveTask;
	
	private ServerOptions serverOptions;
	
	public static final String serverName = Bukkit.getServer().getClass().getPackage().getName();
	public static final String serverVersion = serverName.substring(serverName.lastIndexOf(".") + 1, serverName.length()).substring(1);
	
	
	// JTN's first comment
	
	@Override
	public void onLoad() {
		synchronized(this) {
			if(INSTANCE == null) {
				INSTANCE = this;
			}
		}
	}
	
	@Override
	public void onEnable() {
		switch(serverVersion) {
		case "1_8_R3":
			bridge = new Bridge_Spigot1_8_R3();
			break;
		default:
			getLogger().severe("Incompatible server version (" + serverVersion + ")");
			getLogger().severe("Cannot run Dragons.");
			getServer().getPluginManager().disablePlugin(this);
			return;
		}
		
		storageManager = new MongoStorageManager(this, MongoConfig.HOST, MongoConfig.PORT, MongoConfig.USER, MongoConfig.PASSWORD, MongoConfig.AUTH_DB);
		gameObjectRegistry = new GameObjectRegistry(this, storageManager);
		
		autoSaveTask = AutoSaveTask.getInstance(this);
		
		serverOptions = new ServerOptions();
		
		
		getServer().getPluginManager().registerEvents(new JoinEventListener(this), this);
		getServer().getPluginManager().registerEvents(new QuitEventListener(), this);
		getServer().getPluginManager().registerEvents(new DeathEventListener(this), this);
		
		Bukkit.getScheduler().runTaskTimer(this, () -> autoSaveTask.run(false), 0, serverOptions.getAutoSavePeriodTicks());
	}
	
	@Override
	public void onDisable() {
		autoSaveTask.run(true);
	}
	
	public static Dragons getInstance() {
		return INSTANCE;
	}
	
	public StorageManager getStorageManager() {
		return storageManager;
	}
	
	public GameObjectRegistry getGameObjectRegistry() {
		return gameObjectRegistry;
	}
	
	public ServerOptions getServerOptions() {
		return serverOptions;
	}
	
	public Bridge getBridge() {
		return bridge;
	}
}
