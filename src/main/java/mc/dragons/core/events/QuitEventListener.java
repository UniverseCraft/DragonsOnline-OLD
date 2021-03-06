package mc.dragons.core.events;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.user.User;

/**
 * Event handler for player quitting.
 * 
 * @author Rick
 *
 */
public class QuitEventListener implements Listener {

	//private UserLoader userLoader;
	
	public QuitEventListener() {
		//userLoader = (UserLoader) GameObjectType.USER.<User>getLoader();
	}
	
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		User user = UserLoader.fromPlayer(event.getPlayer());
		user.handleQuit();
		event.setQuitMessage(null);
	}
}
