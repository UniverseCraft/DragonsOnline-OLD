package mc.dragons.core.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.item.ItemClass;
import mc.dragons.core.gameobject.loader.GameObjectRegistry;
import mc.dragons.core.gameobject.loader.ItemClassLoader;
import mc.dragons.core.gameobject.loader.NPCClassLoader;
import mc.dragons.core.gameobject.loader.QuestLoader;
import mc.dragons.core.gameobject.loader.RegionLoader;
import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.npc.NPCClass;
import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.quest.QuestAction;
import mc.dragons.core.gameobject.quest.QuestAction.QuestActionType;
import mc.dragons.core.gameobject.quest.QuestStep;
import mc.dragons.core.gameobject.quest.QuestTrigger;
import mc.dragons.core.gameobject.quest.QuestTrigger.TriggerType;
import mc.dragons.core.gameobject.region.Region;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class QuestCommand implements CommandExecutor {
	private QuestLoader questLoader;
	private NPCClassLoader npcClassLoader;
	private ItemClassLoader itemClassLoader;
	private RegionLoader regionLoader;
	//private UserLoader userLoader;
	private GameObjectRegistry registry;
	
	public QuestCommand(Dragons instance) {
		questLoader = (QuestLoader) GameObjectType.QUEST.<Quest>getLoader();
		npcClassLoader = (NPCClassLoader) GameObjectType.NPC_CLASS.<NPCClass>getLoader();
		itemClassLoader = (ItemClassLoader) GameObjectType.ITEM_CLASS.<ItemClass>getLoader();
		regionLoader = (RegionLoader) GameObjectType.REGION.<Region>getLoader();
		//userLoader = (UserLoader) GameObjectType.USER.<User>getLoader();
		registry = instance.getGameObjectRegistry();
	}
	
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		Player player = null;
		User user = null;
		if(sender instanceof Player) {
			player = (Player) sender;
			user = UserLoader.fromPlayer(player);
			if(!PermissionUtil.verifyActivePermissionLevel(user, PermissionLevel.GM, true)) return true;
		}
		else {
			sender.sendMessage(ChatColor.RED + "This is an ingame-only command.");
			return true;
		}
		
		if(args.length == 0) {
			sender.sendMessage(ChatColor.YELLOW + "/quest -c <ShortName> <LvMin>" + ChatColor.GRAY + " create a new quest");
			sender.sendMessage(ChatColor.YELLOW + "/quest -l" + ChatColor.GRAY + " list all quests");
			sender.sendMessage(ChatColor.YELLOW + "/quest -s <ShortName>" + ChatColor.GRAY + " view quest information");
			sender.sendMessage(ChatColor.YELLOW + "/quest -s <ShortName> questname <FullQuestName>" + ChatColor.GRAY + " set full quest name");
			sender.sendMessage(ChatColor.YELLOW + "/quest -s <ShortName> lvmin <NewLvMin>" + ChatColor.GRAY + " set quest level min");
			sender.sendMessage(ChatColor.YELLOW + "/quest -s <ShortName> stages" + ChatColor.GRAY + " list all quest steps");
			sender.sendMessage(ChatColor.YELLOW + "/quest -s <ShortName> stage <Stage#>" + ChatColor.GRAY + " view quest stage");
			sender.sendMessage(ChatColor.YELLOW + "/quest -s <ShortName> stage <Stage#> name <QuestStageName>" + ChatColor.GRAY + " set name of quest stage");
			sender.sendMessage(ChatColor.YELLOW + "/quest -s <ShortName> stage <add|<Stage#> trigger> <TriggerType> [TriggerParam]" + ChatColor.GRAY + " update quest stage trigger");
			sender.sendMessage(ChatColor.YELLOW + "/quest -s <ShortName> stage <Stage#> action "
					+ "<add <ActionType> [ActionParams...]|"
					+ "dialogue add <Action#> <Message>|"
					+ "branch add <Action#> <GoToStage#> <TriggerType> [TriggerParams...]|"
					+ "del <Action#>>" + ChatColor.GRAY + " manage quest stage actions");
			sender.sendMessage(ChatColor.YELLOW + "/quest -s <ShortName> stage <Stage#> del" + ChatColor.GRAY + " delete quest stage");
			sender.sendMessage(ChatColor.YELLOW + "/quest -d <ShortName>" + ChatColor.GRAY + " delete quest");
			sender.sendMessage(ChatColor.YELLOW + "/testquest <ShortName>" + ChatColor.GRAY + " test quest");
			sender.sendMessage(ChatColor.DARK_GRAY + "" +  ChatColor.BOLD + "Note:" + ChatColor.DARK_GRAY + " Quest ShortNames must not contain spaces.");
			sender.sendMessage(ChatColor.DARK_GRAY + "" +  ChatColor.BOLD + "Note:" + ChatColor.DARK_GRAY + " All quests MUST end with a stage named \"Complete\" with trigger INSTANT and no actions.");
			return true;
		}
		
		if(args[0].equalsIgnoreCase("-c")) {
			if(args.length < 3) {
				sender.sendMessage(ChatColor.RED + "Insufficient arguments! /quest -c <ShortName> <LvMin>");
				return true;
			}
			questLoader.registerNew(args[1], "Unnamed Quest", Integer.valueOf(args[2]));
			sender.sendMessage(ChatColor.GREEN + "Quest created successfully.");
			return true;
		}
		
		if(args[0].equalsIgnoreCase("-l")) {
			sender.sendMessage(ChatColor.GREEN + "Listing all quests:");
			int nInvalid = 0;
			for(GameObject gameObject : registry.getRegisteredObjects(GameObjectType.QUEST)) {
				Quest quest = (Quest) gameObject;
				if(!quest.isValid()) nInvalid++;
				sender.sendMessage(ChatColor.GRAY + "- " + quest.getName() + " (" + quest.getQuestName() + ") [Lv " + quest.getLevelMin() + "] [" + quest.getSteps().size() + " steps]" + (quest.isValid() ? "" : ChatColor.RED + " (Incomplete Setup!)"));
			}
			if(nInvalid > 0) {
				sender.sendMessage(ChatColor.RED + "" + nInvalid + " invalid or incomplete quests found!");
			}
			return true;
		}
		
		Quest quest = questLoader.getQuestByName(args[1]);
		if(quest == null) {
			sender.sendMessage(ChatColor.RED + "No quest by that name exists! (Make sure you're using the short name)");
			return true;
		}
		
		if(args.length < 2) {
			sender.sendMessage(ChatColor.RED + "Insufficient arguments! /quest <-s|-d> <ShortName> [...]");
		}
		
		if(args[0].equalsIgnoreCase("-s")) {
			if(args.length == 2) {
				sender.sendMessage(ChatColor.GREEN + "=== Quest: " + quest.getName() + " ===");
				sender.sendMessage(ChatColor.GRAY + "Database identifier: " + ChatColor.GREEN + quest.getIdentifier().toString());
				sender.sendMessage(ChatColor.GRAY + "Full name: " + ChatColor.GREEN + quest.getQuestName());
				sender.sendMessage(ChatColor.GRAY + "Level min: " + ChatColor.GREEN + quest.getLevelMin());
				sender.sendMessage(ChatColor.GRAY + "# Stages: " + ChatColor.GREEN + quest.getSteps().size());
				if(!quest.isValid()) {
					sender.sendMessage(ChatColor.RED + "Warning: Invalid or incomplete quest! To be valid, the final stage must be named \"Complete\", have trigger type INSTANT, and have no actions.");
				}
				return true;
			}
			if(args[2].equalsIgnoreCase("questname")) {
				quest.setQuestName(StringUtil.concatArgs(args, 3));
				sender.sendMessage(ChatColor.GREEN + "Updated quest name successfully.");
				return true;
			}
			if(args[2].equalsIgnoreCase("lvmin")) {
				quest.setLevelMin(Integer.valueOf(args[3]));
				sender.sendMessage(ChatColor.GREEN + "Updated quest level min successfully.");
				return true;
			}
			if(args[2].equalsIgnoreCase("stages")) {
				sender.sendMessage(ChatColor.GREEN + "Listing quest stages:");
				int i = 0;
				for(QuestStep step : quest.getSteps()) {
					sender.sendMessage(ChatColor.YELLOW + "#" + i + ": " + ChatColor.GRAY + step.getStepName() + " [Trigger: " + step.getTrigger().getTriggerType() + "] [" + step.getActions().size() + " actions" + "]");
					i++;
				}
				return true;
			}
			if(args[2].equalsIgnoreCase("stage")) {
				if(args.length == 3) {
					sender.sendMessage(ChatColor.RED + "Insufficient arguments! /quest -s <ShortName> stage <Stage#> [name|trigger|action|del] [...]");
					return true;
				}
				
				if(args[3].equalsIgnoreCase("add")) {
					TriggerType type = null;
					try {
						type = TriggerType.valueOf(args[4]);
					}
					catch(Exception e) {
						sender.sendMessage(ChatColor.RED + "Invalid trigger type! Valid types are " + Arrays.asList(TriggerType.values())
							.stream()
							.map(t -> t.toString())
							.collect(Collectors.joining(", ")));
						return true;
					}
					QuestStep step = new QuestStep("Unnamed Step", makeTrigger(type, args.length > 5 ? Arrays.copyOfRange(args, 5, args.length) : null), new ArrayList<>(), quest);
					quest.addStep(step);
					sender.sendMessage(ChatColor.GREEN + "Added new quest stage successfully.");
					return true;
				}
				
				int stepNo = Integer.valueOf(args[3]);
				QuestStep step = quest.getSteps().get(stepNo);
				if(args.length == 4) {
					sender.sendMessage(ChatColor.GREEN + "Quest Stage #" + stepNo);
					sender.sendMessage(ChatColor.GRAY + "Objective: " + ChatColor.GREEN + step.getStepName());
					sender.sendMessage(ChatColor.GRAY + "Trigger: " + ChatColor.GREEN + step.getTrigger().getTriggerType());
					sender.sendMessage(ChatColor.GRAY + "- " + displayTrigger(step.getTrigger()));
					sender.sendMessage(ChatColor.GRAY + "Actions:");
					int i = 0;
					for(QuestAction action : step.getActions()) {
						sender.sendMessage(ChatColor.YELLOW + "#" + i + ": " + ChatColor.GRAY + action.getActionType().toString());
						sender.sendMessage(ChatColor.GRAY + "    - " + displayAction(action));
						i++;
					}
					return true;
				}
				
				
				if(args[4].equalsIgnoreCase("name")) {
					step.setStepName(StringUtil.concatArgs(args, 5));
					sender.sendMessage(ChatColor.GREEN + "Updated quest step name successfully.");
					return true;
				}
				if(args[4].equalsIgnoreCase("trigger")) {
					TriggerType type = null;
					try {
						type = TriggerType.valueOf(args[5]);
					}
					catch(Exception e) {
						sender.sendMessage(ChatColor.RED + "Invalid trigger type! Valid types are " + Arrays.asList(TriggerType.values())
							.stream()
							.map(t -> t.toString())
							.collect(Collectors.joining(", ")));
						return true;
					}
					step.setTrigger(makeTrigger(type, args[6]));
					sender.sendMessage(ChatColor.GREEN + "Updated quest stage trigger successfully.");
					return true;
				}
				if(args[4].equalsIgnoreCase("action")) {
					if(args.length == 5) {
						sender.sendMessage(ChatColor.RED + "Insufficient arguments!");
						sender.sendMessage(ChatColor.RED + "/quest -s <ShortName> stage <Stage#> action add TELEPORT_PLAYER");
						sender.sendMessage(ChatColor.RED + "/quest -s <ShortName> stage <Stage#> action add SPAWN_NPC <NpcClass> [NpcReferenceName]");
						sender.sendMessage(ChatColor.RED + "/quest -s <ShortName> stage <Stage#> action add TELEPORT_NPC <NpcReferenceName>");
						sender.sendMessage(ChatColor.RED + "/quest -s <ShortName> stage <Stage#> action add PATHFIND_NPC <NpcReferenceName> [GotoStage#WhenComplete]");
						sender.sendMessage(ChatColor.RED + "/quest -s <ShortName> stage <Stage#> action add BEGIN_DIALOGUE <NpcClass>");
						sender.sendMessage(ChatColor.RED + "/quest -s <ShortName> stage <Stage#> action add GIVE_XP <XPAmount>");
						sender.sendMessage(ChatColor.RED + "/quest -s <ShortName> stage <Stage#> action add <GIVE_ITEM|TAKE_ITEM> <ItemClass> <Amount>");
						sender.sendMessage(ChatColor.RED + "/quest -s <ShortName> stage <Stage#> action dialogue add <Action#> <Dialogue>");
						sender.sendMessage(ChatColor.RED + "/quest -s <ShortName> stage <Stage#> action branch add <TriggerType> <TriggerParam|NONE> <GoToStage#>");
						sender.sendMessage(ChatColor.RED + "/quest -s <ShortName> stage <Stage#> action del <Action#>");
						return true;
					}
					if(args[5].equalsIgnoreCase("add")) {
						if(args[6].equalsIgnoreCase("TELEPORT_PLAYER") || args[6].equalsIgnoreCase("TeleportPlayer")) {
							step.addAction(QuestAction.teleportPlayerAction(quest, user.getPlayer().getLocation()));
						}
						else if(args[6].equalsIgnoreCase("SPAWN_NPC") || args[6].equalsIgnoreCase("SpawnNPC")) {
							step.addAction(QuestAction.spawnNPCAction(quest, npcClassLoader.getNPCClassByClassName(args[7]), args.length == 8 ? "" : args[8]));
						}
						else if(args[6].equalsIgnoreCase("TELEPORT_NPC") || args[6].equalsIgnoreCase("TeleportNPC")) {
							step.addAction(QuestAction.teleportNPCAction(quest, args[7], user.getPlayer().getLocation()));
						}
						else if(args[6].equalsIgnoreCase("PATHFIND_NPC") || args[6].equalsIgnoreCase("PathfindNPC")) {
							step.addAction(QuestAction.pathfindNPCAction(quest, args[7], user.getPlayer().getLocation(), args.length == 8 ? -1 : Integer.valueOf(args[8])));
						}
						else if(args[6].equalsIgnoreCase("BEGIN_DIALOGUE") || args[6].equalsIgnoreCase("BeginDialogue")) {
							step.addAction(QuestAction.beginDialogueAction(quest, npcClassLoader.getNPCClassByClassName(args[7]), new ArrayList<>()));
						}
						else if(args[6].equalsIgnoreCase("GIVE_XP") || args[6].equalsIgnoreCase("GiveXP")) {
							step.addAction(QuestAction.giveXPAction(quest, Integer.valueOf(args[7])));
						}
						else if(args[6].equalsIgnoreCase("GOTO_STAGE") || args[6].equalsIgnoreCase("GoToStage")) {
							step.addAction(QuestAction.goToStageAction(quest, Integer.valueOf(args[7])));
						}
						else if(args[6].equalsIgnoreCase("TAKE_ITEM") || args[6].equalsIgnoreCase("TakeItem")) {
							step.addAction(QuestAction.takeItemAction(quest, itemClassLoader.getItemClassByClassName(args[7]), args.length == 8 ? 1 : Integer.valueOf(args[8])));
						}
						else if(args[6].equalsIgnoreCase("GIVE_ITEM") || args[6].equalsIgnoreCase("GiveItem")) {
							step.addAction(QuestAction.giveItemAction(quest, itemClassLoader.getItemClassByClassName(args[7]), args.length == 8 ? 1 : Integer.valueOf(args[8])));
						}
						else {
							sender.sendMessage(ChatColor.RED + "Invalid action type! Valid action types are " + Arrays.asList(QuestActionType.values())
								.stream()
								.map(t -> t.toString())
								.collect(Collectors.joining(", ")));
							return true;
						}
						sender.sendMessage(ChatColor.GREEN + "Added new action to quest stage successfully.");
						return true;
					}
					if(args[5].equalsIgnoreCase("dialogue")) {
						if(args[6].equalsIgnoreCase("add")) {
							step.addDialogue(Integer.valueOf(args[7]), StringUtil.concatArgs(args, 8));
							sender.sendMessage(ChatColor.GREEN + "Added dialogue to quest stage action successfully.");
							return true;
						}
					}
					if(args[5].equalsIgnoreCase("branch")) {
						if(args[6].equalsIgnoreCase("add")) {
							QuestTrigger trigger = makeTrigger(TriggerType.valueOf(args[8]), Arrays.copyOfRange(args, 9, args.length));
							QuestAction action = QuestAction.goToStageAction(quest, Integer.valueOf(args[7]));
							step.addBranchPoint(trigger, action);
							sender.sendMessage(ChatColor.GREEN + "Added branch point successfully.");
							return true;
						}
					}
					if(args[5].equalsIgnoreCase("del")) {
						step.deleteAction(Integer.valueOf(args[6]));
						sender.sendMessage(ChatColor.GREEN + "Removed action from quest stage successfully.");
						return true;
					}
				}
				if(args[4].equalsIgnoreCase("del")) {
					quest.delStep(stepNo);
					sender.sendMessage(ChatColor.GREEN + "Deleted quest stage successfully.");
					return true;
				}
			}
			return true;
		}
		if(args[0].equalsIgnoreCase("-d")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "Specify a quest to delete! /quest -d <ShortName>");
				return true;
			}
			registry.removeFromDatabase(quest);
			sender.sendMessage(ChatColor.GREEN + "Deleted quest successfully.");
		}
		
		
		return true;
	}
	
	private QuestTrigger makeTrigger(TriggerType type, String... params) {
		switch(type) {
		case INSTANT:
			return QuestTrigger.instant();
		case NEVER:
			return QuestTrigger.never();
		case KILL_NPC:
			return QuestTrigger.onKillNPC(npcClassLoader.getNPCClassByClassName(params[0]));
		case CLICK_NPC:
			return QuestTrigger.onClickNPC(npcClassLoader.getNPCClassByClassName(params[0]));
		case ENTER_REGION:
			return QuestTrigger.onEnterRegion(regionLoader.getRegionByName(params[0]));
		case EXIT_REGION:
			return QuestTrigger.onExitRegion(regionLoader.getRegionByName(params[0]));
		case BRANCH_CONDITIONAL:
			return QuestTrigger.branchConditional(new HashMap<>());
		case HAS_ITEM:
			return QuestTrigger.hasItem(itemClassLoader.getItemClassByClassName(params[0]), Integer.valueOf(params[1]));
		}
		return null;
	}
	
	private String displayTrigger(QuestTrigger trigger) {
		switch(trigger.getTriggerType()) {
		case INSTANT:
			return "Triggered immediately";
		case NEVER:
			return "Never triggered (limbo)";
		case KILL_NPC:
		case CLICK_NPC:
			return "Target NPC Class: " + ChatColor.GREEN + trigger.getNPCClass().getName() + " (" + trigger.getNPCClass().getClassName() + ")";
		case ENTER_REGION:
		case EXIT_REGION:
			return "Target Region: " + ChatColor.GREEN + trigger.getRegion().getName() + " (" + trigger.getRegion().getFlags().getString("fullname") + ")";
		case BRANCH_CONDITIONAL:
			String triggerMsg = "Branch Points:";
			for(Entry<QuestTrigger, QuestAction> conditional : trigger.getBranchPoints().entrySet()) {
				triggerMsg += "\n    - " + conditional.getKey().getTriggerType().toString() + " (" + displayTrigger(conditional.getKey()) + ChatColor.GRAY + ")" 
						+ " -> " + conditional.getValue().getActionType().toString() + " (" + displayAction(conditional.getValue()) + ChatColor.GRAY + ")";
			}
			return triggerMsg;
		case HAS_ITEM:
			return "Triggered immediately once player has " + ChatColor.GREEN + trigger.getQuantity() + ChatColor.GRAY + " of " + ChatColor.GREEN + trigger.getItemClass().getClassName();
		default:
			break;
		}
		return "";
	}
	
	private String displayAction(QuestAction action) {
		switch(action.getActionType()) {
		case TELEPORT_PLAYER:
			return "Target Location: " + ChatColor.GREEN + StringUtil.locToString(action.getLocation());
		case SPAWN_NPC:
			return "Target NPC Class: " + ChatColor.GREEN + action.getNPCClass().getName() + " (" + action.getNPCClass().getClassName() + ")\n"
					+ ChatColor.GRAY + (action.getNPCReferenceName() == null ? "    - No reference name" : "    - Reference name: " + ChatColor.GREEN + action.getNPCReferenceName());
		case TELEPORT_NPC:
			return "NPC Reference Name: " + ChatColor.GREEN + action.getNPCReferenceName() + "\n"
					+ ChatColor.GRAY + "    - To: " + ChatColor.GREEN + StringUtil.locToString(action.getLocation());
		case PATHFIND_NPC:
			return "NPC Reference Name: " + ChatColor.GREEN + action.getNPCReferenceName() + "\n"
					+ ChatColor.GRAY + "    - To: " + ChatColor.GREEN + StringUtil.locToString(action.getLocation()) + "\n"
					+ ChatColor.GRAY + (action.getGotoStage() == -1 ? "    - No special behavior upon completion" : "    - On completion: " + ChatColor.GREEN + "Go to stage " + action.getGotoStage());
		case BEGIN_DIALOGUE:
			String actionMsg = "Spoken By: " + ChatColor.GREEN + action.getNPCClass().getName() + " (" + action.getNPCClass().getClassName() + ")\n"
				+ ChatColor.GRAY + "    - Dialogue:";
			int j = 0;
			for(String line : action.getDialogue()) {
				actionMsg += "\n" + ChatColor.YELLOW + "      #" + j + ":  " + ChatColor.GRAY + line + "";
				j++;
			}
			return actionMsg;
		case GIVE_XP:
			return ChatColor.GRAY + "Amount: " + ChatColor.GREEN + action.getXPAmount();
		case GOTO_STAGE:
			return ChatColor.GRAY + "Stage: " + ChatColor.GREEN + action.getGotoStage();
		case TAKE_ITEM:
		case GIVE_ITEM:
			return ChatColor.GRAY + "Item Class: " + ChatColor.GREEN + action.getItemClass().getClassName() + ChatColor.GRAY + "; Amount: " + ChatColor.GREEN + action.getQuantity();
		}
		return "";
	}
}
