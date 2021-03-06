package mc.dragons.core.commands;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.gameobject.user.User.PunishmentData;
import mc.dragons.core.gameobject.user.User.PunishmentType;
import mc.dragons.core.util.PermissionUtil;

public class ViewPunishmentsCommand implements CommandExecutor {
	private UserLoader userLoader;
	
	public ViewPunishmentsCommand() {
		userLoader = (UserLoader) GameObjectType.USER.<User>getLoader();
	}
	
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		User user = null;
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.MOD, true)) return true;
		}
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.RED + "Please specify a player! /viewpunishments <player>");
			return true;
		}
		
		String username = args[0];
		Player targetPlayer = Bukkit.getPlayerExact(username);
		
		User targetUser = userLoader.loadObject(username);
		
		if(targetUser == null) {
			sender.sendMessage(ChatColor.RED + "That player does not exist!");
			return true;
		}
		

		PunishmentData banData = targetUser.getActivePunishmentData(PunishmentType.BAN);
		PunishmentData muteData = targetUser.getActivePunishmentData(PunishmentType.MUTE);
		

		sender.sendMessage(ChatColor.GOLD + "Punishment History for User " + targetUser.getName());
		if(targetPlayer == null) {
			sender.sendMessage(ChatColor.YELLOW + "" + ChatColor.ITALIC + "This player is offline. Showing cached data.");
		}
		sender.sendMessage(ChatColor.YELLOW + "Active Punishments:");
		if(banData == null) {
			sender.sendMessage(ChatColor.WHITE + "- Not banned");
		}
		else {
			sender.sendMessage(ChatColor.WHITE + "- Banned: " + banData.getReason() + " (" + (banData.isPermanent() ? "Permanent" : "Until " + banData.getExpiry().toString()) + ")");
		}
		if(muteData == null) {
			sender.sendMessage(ChatColor.WHITE + "- Not muted");
		}
		else {
			sender.sendMessage(ChatColor.WHITE + "- Muted: " + muteData.getReason() + " (" + (muteData.isPermanent() ? "Permanent" : "Until " + muteData.getExpiry().toString()) + ")");
		}
		
		sender.sendMessage(ChatColor.YELLOW + "Past Punishments:");
		
		int i = 1;
		for(PunishmentData entry : targetUser.getPunishmentHistory()) {
			sender.sendMessage(ChatColor.DARK_GREEN + "#" + i + ": " + ChatColor.RED + entry.getType() + ": " + ChatColor.WHITE + entry.getReason() + " (" + (entry.isPermanent() ? "Permanent" : "Until " + entry.getExpiry().toString()) + ")");
			i++;
		}
		
		if(targetPlayer == null) {
			// User was only constructed for querying purposes. Since they're not really online, remove them from local registry
			userLoader.unregister(targetUser);
		}
		
		return true;
	}
}
