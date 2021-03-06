package mc.dragons.core.commands;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import mc.dragons.core.Dragons;
import mc.dragons.core.gameobject.GameObject;
import mc.dragons.core.gameobject.GameObjectType;
import mc.dragons.core.gameobject.loader.GameObjectRegistry;
import mc.dragons.core.gameobject.loader.NPCClassLoader;
import mc.dragons.core.gameobject.loader.NPCLoader;
import mc.dragons.core.gameobject.loader.QuestLoader;
import mc.dragons.core.gameobject.loader.UserLoader;
import mc.dragons.core.gameobject.npc.NPC;
import mc.dragons.core.gameobject.npc.NPC.NPCType;
import mc.dragons.core.gameobject.npc.NPCAction;
import mc.dragons.core.gameobject.npc.NPCAction.NPCActionType;
import mc.dragons.core.gameobject.npc.NPCClass;
import mc.dragons.core.gameobject.npc.NPCCondition;
import mc.dragons.core.gameobject.npc.NPCCondition.NPCConditionType;
import mc.dragons.core.gameobject.npc.NPCConditionalActions;
import mc.dragons.core.gameobject.npc.NPCConditionalActions.NPCTrigger;
import mc.dragons.core.gameobject.quest.Quest;
import mc.dragons.core.gameobject.user.PermissionLevel;
import mc.dragons.core.gameobject.user.User;
import mc.dragons.core.util.PermissionUtil;
import mc.dragons.core.util.StringUtil;

public class NPCCommand implements CommandExecutor {
	//private UserLoader userLoader;
	private NPCLoader npcLoader;
	private NPCClassLoader npcClassLoader;
	private QuestLoader questLoader;
	private GameObjectRegistry gameObjectRegistry;
	
	public NPCCommand(Dragons instance) {
		//userLoader = (UserLoader) GameObjectType.USER.<User>getLoader();
		npcLoader = (NPCLoader) GameObjectType.NPC.<NPC>getLoader();
		npcClassLoader = (NPCClassLoader) GameObjectType.NPC_CLASS.<NPCClass>getLoader();
		questLoader = (QuestLoader) GameObjectType.QUEST.<Quest>getLoader();
		gameObjectRegistry = instance.getGameObjectRegistry();
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
			sender.sendMessage(ChatColor.YELLOW + "/npc class -c <ClassName> <EntityType> <MaxHealth> <Level> <NPCType>" + ChatColor.GRAY + " create a new NPC class");
			sender.sendMessage(ChatColor.GRAY + "Valid NPCTypes are: HOSTILE, NEUTRAL, QUEST, and SHOP.");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -l" + ChatColor.GRAY + " list all NPC classes");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName>" + ChatColor.GRAY + " view information about NPC class");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> type <EntityType>" + ChatColor.GRAY + " change type of NPC class");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> name <DisplayName>" + ChatColor.GRAY + " set NPC class display name");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> health <MaxHealth>" + ChatColor.GRAY + " set NPC class max health");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> level <Level>" + ChatColor.GRAY + " set NPC level");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> npctype <NPCType>" + ChatColor.GRAY + " set NPC type");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> ai <HasAI>" + ChatColor.GRAY + " set whether the NPC has AI");
			sender.sendMessage(ChatColor.DARK_GRAY + " * Pathfinding behavior will still be enabled when triggered by the RPG");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> immortal <IsImmortal>" + ChatColor.GRAY + " set whether the NPC is immortal");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> loot [<RegionName> <ItemClassName> <Chance%|DEL>]" + ChatColor.GRAY + " manage NPC class loot table");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> behavior|b [<CLICK|HIT> <add|remove <#>>]" + ChatColor.GRAY + " add/remove/view NPC behaviors");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> behavior|b <CLICK|HIT> <#> condition <add [!]<ConditionType> <ConditionParams...>|remove <#>>" + ChatColor.GRAY + " add/remove conditions on an NPC behavior");
			sender.sendMessage(ChatColor.DARK_GRAY + " * Adding a ! before the ConditionType will negate the condition.");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -s <ClassName> behavior|b <CLICK|HIT> <#> action <add <ActionType> <ActionParams...>|remove <#>>" + ChatColor.GRAY + " add/remove actions on an NPC behavior");
			sender.sendMessage(ChatColor.YELLOW + "/npc class -d <ClassName>" + ChatColor.GRAY + " delete NPC class");
			sender.sendMessage(ChatColor.YELLOW + "/npc spawn <ClassName>  [-phase <Player>]" + ChatColor.GRAY + " spawn a new NPC of the given class");
			sender.sendMessage(ChatColor.DARK_GRAY + "" +  ChatColor.BOLD + "Note:" + ChatColor.DARK_GRAY + " Class names must not contain spaces.");
			return true;
		}
		
		if(args[0].equalsIgnoreCase("class")) {
			if(args.length == 1) {
				sender.sendMessage(ChatColor.RED + "Insufficient arguments!");
				return true;
			}
			if(args[1].equalsIgnoreCase("-c")) {
				if(args.length < 7) {
					sender.sendMessage(ChatColor.RED + "Insufficient arguments! /npc class -c <ClassName> <EntityType> <MaxHealth> <Level> <NPCType>");
					return true;
				}
				String npcClassName = args[2];
				EntityType type = EntityType.valueOf(args[3].toUpperCase());
				double maxHealth = Double.valueOf(args[4]);
				int level = Integer.valueOf(args[5]);
				NPCType npcType = NPCType.valueOf(args[6]);
				NPCClass npcClass = npcClassLoader.registerNew(npcClassName, "Unnamed Entity", type, maxHealth, level, npcType);
				if(npcClass == null) {
					sender.sendMessage(ChatColor.RED + "An error occurred! Does a class by this name already exist?");
					return true;
				}
				sender.sendMessage(ChatColor.GREEN + "Successfully created NPC class " + npcClassName);
				return true;
			}
			if(args[1].equalsIgnoreCase("-l")) {
				sender.sendMessage(ChatColor.GREEN + "Listing all NPC classes:");
				for(GameObject gameObject : Dragons.getInstance().getGameObjectRegistry().getRegisteredObjects(GameObjectType.NPC_CLASS)) {
					NPCClass npcClass = (NPCClass) gameObject;
					sender.sendMessage(ChatColor.GRAY + "- " + npcClass.getClassName() + " [Lv " + npcClass.getLevel() + "]");
				}
				return true;
			}
			if(args[1].equalsIgnoreCase("-s")) {
				if(args.length < 3) {
					sender.sendMessage(ChatColor.RED + "Insufficient arguments!");
					return true;
				}
				NPCClass npcClass = npcClassLoader.getNPCClassByClassName(args[2]);
				if(npcClass == null) {
					sender.sendMessage(ChatColor.RED + "That's not a valid NPC class name!");
					return true;
				}
				if(args.length == 3) {
					sender.sendMessage(ChatColor.GREEN + "=== NPC Class: " + npcClass.getClassName() + " ===");
					sender.sendMessage(ChatColor.GRAY + "Database identifier: " + ChatColor.GREEN + npcClass.getIdentifier().toString());
					sender.sendMessage(ChatColor.GRAY + "Display name: " + ChatColor.GREEN + npcClass.getName());
					sender.sendMessage(ChatColor.GRAY + "Entity type: " + ChatColor.GREEN + npcClass.getEntityType().toString());
					sender.sendMessage(ChatColor.GRAY + "Max health: " + ChatColor.GREEN + npcClass.getMaxHealth());
					sender.sendMessage(ChatColor.GRAY + "Level: " + ChatColor.GREEN + npcClass.getLevel());
					sender.sendMessage(ChatColor.GRAY + "NPC type: " + ChatColor.GREEN + npcClass.getNPCType().toString());
					sender.sendMessage(ChatColor.GRAY + "AI: " + ChatColor.GREEN + npcClass.hasAI());
					sender.sendMessage(ChatColor.GRAY + "Immortal: " + ChatColor.GREEN + npcClass.isImmortal());
					sender.sendMessage(ChatColor.GRAY + "/npc class -s " + npcClass.getClassName() + " loot" + ChatColor.YELLOW + " to view loot table");
					sender.sendMessage(ChatColor.GRAY + "/npc class -s " + npcClass.getClassName() + " behavior" + ChatColor.YELLOW + " to view behaviors");
					return true;
				}
				if(args[3].equalsIgnoreCase("type")) {
					EntityType type = EntityType.valueOf(args[4].toUpperCase());
					npcClass.setEntityType(type);
					sender.sendMessage(ChatColor.GREEN + "Updated entity type successfully.");
					return true;
				}
				if(args[3].equalsIgnoreCase("name")) {
					npcClass.setName(StringUtil.concatArgs(args, 4));
					sender.sendMessage(ChatColor.GREEN + "Updated entity display name successfully.");
					return true;
				}
				if(args[3].equalsIgnoreCase("health")) {
					npcClass.setMaxHealth(Double.valueOf(args[4]));
					sender.sendMessage(ChatColor.GREEN + "Updated entity max health successfully.");
					return true;
				}
				if(args[3].equalsIgnoreCase("level")) {
					npcClass.setLevel(Integer.valueOf(args[4]));
					sender.sendMessage(ChatColor.GREEN + "Updated entity level successfully.");
					return true;
				}
				if(args[3].equalsIgnoreCase("npctype")) {
					npcClass.setNPCType(NPCType.valueOf(args[4]));
					sender.sendMessage(ChatColor.GREEN + "Updated NPC type successfully.");
					if((npcClass.getNPCType() == NPCType.QUEST || npcClass.getNPCType() == NPCType.SHOP) && npcClass.hasAI()) {
						npcClass.setAI(false);
						sender.sendMessage(ChatColor.GREEN + "Automatically toggled off AI for this class based on the NPC type.");
					}
					return true;
				}
				if(args[3].equalsIgnoreCase("ai")) {
					npcClass.setAI(Boolean.valueOf(args[4]));
					sender.sendMessage(ChatColor.GREEN + "Updated entity AI successfully.");
					return true;
				}
				if(args[3].equalsIgnoreCase("immortal")) {
					npcClass.setImmortal(Boolean.valueOf(args[4]));
					sender.sendMessage(ChatColor.GREEN + "Updated entity immortality successfully.");
					return true;
				}
				if(args[3].equalsIgnoreCase("loot")) {
					if(args.length == 4) {
						sender.sendMessage(ChatColor.GREEN + "Loot Table:");
						for(Entry<String, Map<String, Double>> regionLoot : npcClass.getLootTable().asMap().entrySet()) {
							sender.sendMessage(ChatColor.GRAY + "- Region: " + regionLoot.getKey());
							for(Entry<String, Double> itemLoot : regionLoot.getValue().entrySet()) {
								sender.sendMessage(ChatColor.GRAY + "   - " + itemLoot.getKey() + ": " + itemLoot.getValue() + "%");
							}
						}
						return true;
					}
					if(args[6].equalsIgnoreCase("del")) {
						npcClass.deleteFromLootTable(args[4], args[5]);
						sender.sendMessage(ChatColor.GREEN + "Removed from entity loot table successfully.");
						return true;
					}
					npcClass.updateLootTable(args[4], args[5], Double.valueOf(args[6]));
					sender.sendMessage(ChatColor.GREEN + "Updated entity loot table successfully.");
					return true;
				}
				if(args[3].equalsIgnoreCase("behavior") || args[3].equalsIgnoreCase("b")) {
					if(args.length == 4) {
						sender.sendMessage(ChatColor.GREEN + "NPC Behaviors:");
						for(NPCTrigger trigger : NPCTrigger.values()) {
							sender.sendMessage(ChatColor.GRAY + "- Trigger: " + trigger);
							int i = 0;
							for(Entry<List<NPCCondition>, List<NPCAction>> entry : npcClass.getConditionalActions(trigger).getConditionals().entrySet()) {
								sender.sendMessage(ChatColor.GRAY + "  - Behavior " + ChatColor.DARK_GREEN + "#" + i + ChatColor.GRAY + ":");
								sender.sendMessage(ChatColor.GRAY + "    - Conditions:");
								int j = 0;
								for(NPCCondition condition : entry.getKey()) {
									sender.sendMessage(ChatColor.DARK_GREEN + "      #" + j + ": " + ChatColor.GRAY + displayNPCCondition(condition));
									j++;
								}
								sender.sendMessage(ChatColor.GRAY + "    - Actions:");
								j = 0;
								for(NPCAction action : entry.getValue()) {
									sender.sendMessage(ChatColor.DARK_GREEN + "      #" + j + ": " + ChatColor.GRAY + displayNPCAction(action));
								}
								i++;
							}
						}
						return true;
					}
					NPCTrigger trigger = NPCTrigger.valueOf(args[4]);
					NPCConditionalActions behaviorsLocal = npcClass.getConditionalActions(trigger);
					NPCConditionalActions parsedBehaviors = npcClass.getConditionalActions(trigger);
					Document conditionals = npcClass.getStorageAccess().getDocument().get("conditionals", Document.class);
					List<Document> behaviors = conditionals
							.getList(trigger.toString(), Document.class);
					if(args[5].equalsIgnoreCase("add")) {
						behaviors.add(new Document("conditions", new ArrayList<Document>()).append("actions", new ArrayList<Document>()));
						npcClass.getStorageAccess().update(new Document("conditionals", conditionals));
						parsedBehaviors.addLocalEntry();
						sender.sendMessage(ChatColor.GREEN + "Successfully added a new conditional with trigger " + trigger);
						return true;
					}
					else if(args[5].equalsIgnoreCase("remove")) {
						behaviors.remove((int) Integer.valueOf(args[6]));
						npcClass.getStorageAccess().update(new Document("conditionals", conditionals));
						parsedBehaviors.removeLocalEntry(Integer.valueOf(args[6]));
						sender.sendMessage(ChatColor.GREEN + "Successfully removed conditional #" + args[6] + " with trigger " + trigger);
						return true;
					}
					else {
						int behaviorNo = Integer.valueOf(args[5]);
						//Entry<List<NPCCondition>, List<NPCAction>> conditions = behaviorsParsed.getConditional(behaviorNo);
						Document behavior = behaviors.get(behaviorNo);
						if(args[6].equalsIgnoreCase("condition") || args[6].equalsIgnoreCase("c")) {
							List<Document> conditions = behavior.getList("conditions", Document.class);
							if(args[7].equalsIgnoreCase("add")) {
								boolean inverted = args[8].startsWith("!");
								if(inverted) args[8] = args[8].replace("!", "");
								NPCConditionType condType = NPCConditionType.valueOf(args[8]);
								NPCCondition cond = null;
								switch(condType) {
								case HAS_COMPLETED_QUEST:
									cond = NPCCondition.hasCompletedQuest(questLoader.getQuestByName(args[9]), inverted);
									break;
								case HAS_QUEST_STAGE:
									cond = NPCCondition.hasQuestStage(questLoader.getQuestByName(args[9]), Integer.valueOf(args[10]), inverted);
									break;
								case HAS_GOLD:
									cond = NPCCondition.hasGold(Double.valueOf(args[9]), inverted);
									break;
								case HAS_LEVEL:
									cond = NPCCondition.hasLevel(Integer.valueOf(args[9]), inverted);
									break;
								}
								conditions.add(cond.toDocument());
								behaviorsLocal.getConditional(behaviorNo).getKey().add(cond);
							}
							else if(args[7].equalsIgnoreCase("remove")) {
								conditions.remove((int) Integer.valueOf(args[8]));
								behaviorsLocal.getConditional(behaviorNo).getKey().remove((int) Integer.valueOf(args[8]));
							}
							behavior.append("conditions", conditions);
						}
						else if(args[6].equalsIgnoreCase("action") || args[6].equalsIgnoreCase("a")) {
							List<Document> actions = behavior.getList("actions", Document.class);
							if(args[7].equalsIgnoreCase("add")) {
								NPCActionType actionType = NPCActionType.valueOf(args[8]);
								NPCAction action = null;
								switch(actionType) {
								case BEGIN_DIALOGUE:
									action = NPCAction.beginDialogue(npcClass, Arrays.asList(StringUtil.concatArgs(args, 9).split(Pattern.quote("|"))));
									break;
								case BEGIN_QUEST:
									action = NPCAction.beginQuest(npcClass, questLoader.getQuestByName(args[9]));
									break;
								}
								actions.add(action.toDocument());
								behaviorsLocal.getConditional(behaviorNo).getValue().add(action);
							}
							else if(args[7].equalsIgnoreCase("remove")) {
								actions.remove((int) Integer.valueOf(args[8]));
								behaviorsLocal.getConditional(behaviorNo).getValue().remove((int) Integer.valueOf(args[8]));
							}
							behavior.append("actions", actions);
						}
						npcClass.getStorageAccess().update(new Document("conditionals", conditionals));
						sender.sendMessage(ChatColor.GREEN + "Updated behavior successfully.");
						return true;
					}
					
				}
				return true;
			}
			if(args[1].equalsIgnoreCase("-d")) {
				if(args.length == 2) {
					sender.sendMessage(ChatColor.RED + "Specify a class name to delete! /npc class -d <ClassName>");
					return true;
				}
				NPCClass npcClass = npcClassLoader.getNPCClassByClassName(args[2]);
				if(npcClass == null) {
					sender.sendMessage(ChatColor.RED + "That's not a valid NPC class name!");
					return true;
				}
				gameObjectRegistry.removeFromDatabase(npcClass);
				sender.sendMessage(ChatColor.GREEN + "Successfully deleted NPC class.");
				return true;
			}
		}
		if(args[0].equalsIgnoreCase("spawn")) {
			NPC npc = npcLoader.registerNew(player.getWorld(), player.getLocation(), args[1]);
			sender.sendMessage(ChatColor.GREEN + "Spawned an NPC of class " + args[1] + " at your location.");
			if(args.length >= 3) {
				if(args[2].equalsIgnoreCase("-phase")) {
					Player phaseFor = Bukkit.getPlayerExact(args[3]);
					npc.phase(phaseFor);
					sender.sendMessage(ChatColor.GREEN + "Phased NPC successfully.");
				}
			}
			return true;
		}
		
		return true;
	}
	
	private String displayNPCCondition(NPCCondition cond) {
		String result = (cond.isInverse() ? ChatColor.RED + "NOT " + ChatColor.GRAY : "") + cond.getType() + " (";
		switch(cond.getType()) {
		case HAS_COMPLETED_QUEST:
			result += cond.getQuest().getName();
			break;
		case HAS_QUEST_STAGE:
			result += cond.getQuest().getName() + " - stage " + cond.getStageRequirement();
			break;
		case HAS_LEVEL:
			result += "Lv " + cond.getLevelRequirement();
			break;
		case HAS_GOLD:
			result += cond.getGoldRequirement() + " Gold";
			break;
		}
		result += ")";
		return result;
	}
	
	private String displayNPCAction(NPCAction action) {
		String result = action.getType() + " (";
		switch(action.getType()) {
		case BEGIN_DIALOGUE:
			result += action.getDialogue().stream().map(line -> ChatColor.GREEN + line).collect(Collectors.joining(ChatColor.GRAY + " // "));
			break;
		case BEGIN_QUEST:
			result += action.getQuest().getName();
			break;
		default:
			break;
		
		}
		result += ChatColor.GRAY + ")";
		return result;
	}

}
