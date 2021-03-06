package mc.dragons.core.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.floor.Floor;
import mc.dragons.core.gameobject.loader.FloorLoader;
import mc.dragons.core.gameobject.loader.GameObjectRegistry;
import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.util.PermissionUtil;

public class GoToFloorCommand implements CommandExecutor {

	private GameObjectRegistry registry;
	private FloorLoader floorLoader;
	
	public GoToFloorCommand(Dragons instance) {
		registry = instance.getGameObjectRegistry();
		floorLoader = (FloorLoader) GameObjectType.FLOOR.<Floor>getLoader();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command.");
			return true;
		}
		
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		
		if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.BUILDER, true)) return true;
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.YELLOW + "/gotofloor -listfloors");
			sender.sendMessage(ChatColor.YELLOW + "/gotofloor <FloorName>");
			return true;
		}
		
		
		if(args[0].equalsIgnoreCase("-listfloors")) {
			sender.sendMessage(ChatColor.GREEN + "Listing all floors:");
			for(GameObject gameObject : registry.getRegisteredObjects(GameObjectType.FLOOR)) {
				Floor floor = (Floor) gameObject;
				sender.sendMessage(ChatColor.GRAY + "- " + floor.getFloorName() + " [" + floor.getWorldName() + "] [Lv " + floor.getLevelMin() + "]");
			}
			return true;
		}
		
		Floor floor = floorLoader.fromFloorName(args[0]);
		if(floor == null) {
			sender.sendMessage(ChatColor.RED + "That floor does not exist!");
			return true;
		}
		
		user.sendToFloor(floor.getFloorName(), true);
		sender.sendMessage(ChatColor.GREEN + "Teleported to floor successfully.");
		
		return true;
	}

}
