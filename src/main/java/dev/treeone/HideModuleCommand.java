package dev.treeone;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.feature.command.Command;
import org.rusherhack.core.command.annotations.CommandExecutor;


public class HideModuleCommand extends Command {

    public HideModuleCommand() {
        super("hidemodule", "Manages hidden module list - commands: add, remove, list, clear");
    }

    @CommandExecutor(subCommand = "add")
    @CommandExecutor.Argument("string")
    private String addModule(String string) {
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
    }

    @CommandExecutor(subCommand = "remove")
    @CommandExecutor.Argument("string")
    private String removeModule(String string) {
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
    }

    @CommandExecutor(subCommand = "list")
    private String listHiddenModules() {
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
    }

    @CommandExecutor(subCommand = "clear")
    private String clearHiddenModules() {
        if (RusherHackAPI.getHudManager().getFeature("CombinedBindList").isPresent()) {
            CombinedBindListHudElement hudElement = (CombinedBindListHudElement) RusherHackAPI.getHudManager().getFeature("CombinedBindList").get();

            int clearedCount = CombinedBindListHudElement.hiddenModules.size();
            CombinedBindListHudElement.hiddenModules.clear();
            hudElement.save();

            return "Cleared " + clearedCount + " hidden modules - all modules are now visible";
        } else {
            return "CombinedBindList HUD element is not present";
        }
    }

    @CommandExecutor
    private String showHelp() {
        return "HideModule commands:\n" +
                "- hidemodule add <module_name> - Hide a module from the bind list\n" +
                "- hidemodule remove <module_name> - Show a hidden module again\n" +
                "- hidemodule list - Show all currently hidden modules\n" +
                "- hidemodule clear - Clear all hidden modules (make all visible)";
    }
}