package dev.treeone;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.feature.command.Command;
import org.rusherhack.core.command.annotations.CommandExecutor;


public class HideModuleCommand extends Command {

    public HideModuleCommand() {
        super("combindlist", "Manages hidden module list - commands: add, remove, list, clear");
    }

    @CommandExecutor
    private String showHelp() {
        return "CombinedBindList commands:\n" +
                "*combindlist add <module name> - Hide a module from the bind list\n" +
                "*combindlist remove <module name> - Show a hidden module again\n" +
                "*combindlist list - Show all currently hidden modules\n" +
                "*combindlist clear - Clear all hidden modules (make all visible)";
    }

    @CommandExecutor(subCommand = "add")
    @CommandExecutor.Argument("string")
    private String addModule(String string) {
        try {
            if (RusherHackAPI.getHudManager().getFeature("CombinedBindList").isPresent()) {
                CombinedBindListHudElement hudElement = (CombinedBindListHudElement) RusherHackAPI.getHudManager().getFeature("CombinedBindList").get();

                String moduleId = string.toLowerCase();

                if (!hudElement.isModuleLoaded(moduleId)) {
                    return "No module with name matches '" + string + "'";
                }

                if (CombinedBindListHudElement.hiddenModules.contains(moduleId)) {
                    return "Module '" + string + "' is already hidden";
                }

                CombinedBindListHudElement.hiddenModules.add(moduleId);
                hudElement.save();
                return "Added '" + string + "' to hidden module list";
            } else {
                return "CombinedBindList HUD element is not present";
            }
        } catch (Exception e) {
            return "Error adding module: " + e.getMessage();
        }
    }

    @CommandExecutor(subCommand = "remove")
    @CommandExecutor.Argument("string")
    private String removeModule(String string) {
        try {
            if (RusherHackAPI.getHudManager().getFeature("CombinedBindList").isPresent()) {
                CombinedBindListHudElement hudElement = (CombinedBindListHudElement) RusherHackAPI.getHudManager().getFeature("CombinedBindList").get();

                String moduleId = string.toLowerCase();

                if (CombinedBindListHudElement.hiddenModules.remove(moduleId)) {
                    hudElement.save();
                    return "Removed '" + string + "' from hidden modules list";
                } else {
                    return "Module '" + string + "' is not in the hidden list";
                }
            } else {
                return "CombinedBindList HUD element is not present";
            }
        } catch (Exception e) {
            return "Error removing module: " + e.getMessage();
        }
    }

    @CommandExecutor(subCommand = "list")
    private String listHiddenModules() {
        try {
            if (RusherHackAPI.getHudManager().getFeature("CombinedBindList").isPresent()) {
                if (CombinedBindListHudElement.hiddenModules.isEmpty()) {
                    return "No modules are currently hidden";
                }

                StringBuilder modules = new StringBuilder("Hidden modules: ");
                for (int i = 0; i < CombinedBindListHudElement.hiddenModules.size(); i++) {
                    String hiddenModule = CombinedBindListHudElement.hiddenModules.get(i);
                    modules.append(hiddenModule);

                    if (i < CombinedBindListHudElement.hiddenModules.size() - 1) {
                        modules.append(", ");
                    }
                }
                return modules.toString();
            } else {
                return "CombinedBindList HUD element is not present";
            }
        } catch (Exception e) {
            return "Error listing modules: " + e.getMessage();
        }
    }

    @CommandExecutor(subCommand = "clear")
    private String clearHiddenModules() {
        try {
            if (RusherHackAPI.getHudManager().getFeature("CombinedBindList").isPresent()) {
                CombinedBindListHudElement hudElement = (CombinedBindListHudElement) RusherHackAPI.getHudManager().getFeature("CombinedBindList").get();

                int clearedCount = CombinedBindListHudElement.hiddenModules.size();
                CombinedBindListHudElement.hiddenModules.clear();
                hudElement.save();

                return "Cleared " + clearedCount + " hidden modules - all modules are now visible";
            } else {
                return "CombinedBindList HUD element is not present";
            }
        } catch (Exception e) {
            return "Error clearing modules: " + e.getMessage();
        }
    }
}