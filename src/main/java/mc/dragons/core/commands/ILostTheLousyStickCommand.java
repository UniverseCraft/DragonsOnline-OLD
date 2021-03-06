package mc.dragons.core.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.item.Item;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.gameobject.loader.ItemClassLoader;
import mc.dragons.core.gameobject.loader.ItemLoader;
import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.user.User;

public class ILostTheLousyStickCommand implements CommandExecutor  {
	
	private ItemLoader itemLoader;
	private ItemClassLoader itemClassLoader;
	
	private ItemClass lousyStickClass;
	
	public ILostTheLousyStickCommand() {
		itemLoader = (ItemLoader) GameObjectType.ITEM.<Item>getLoader();
		itemClassLoader = (ItemClassLoader) GameObjectType.ITEM_CLASS.<ItemClass>getLoader();
		lousyStickClass = itemClassLoader.getItemClassByClassName("LousyStick");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		
		if(!(sender instanceof Player)) {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command!");
			return true;
		}
		
		Player player = (Player) sender;
		User user = UserLoader.fromPlayer(player);
		
		for(ItemStack itemStack : player.getInventory().getContents()) {
			if(itemStack == null) continue;
			Item item = ItemLoader.fromBukkit(itemStack);
			if(item == null) continue;
			if(item.getClassName() == null) continue;
			if(item.getClassName().equals("LousyStick")) {
				sender.sendMessage("No you didn't");
				return true;
			}
		}
		
		user.giveItem(itemLoader.registerNew(lousyStickClass));
		sender.sendMessage("Be more careful next time...");
		
		return true;
	}
}
