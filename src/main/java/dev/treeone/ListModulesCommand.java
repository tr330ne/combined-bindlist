package dev.treeone;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.feature.command.Command;
import org.rusherhack.core.command.annotations.CommandExecutor;


public class ListModulesCommand extends Command {

    public ListModulesCommand() {
        super("listmodules", "Lists all available modules from both RusherHack and Meteor Client");
    }

    @CommandExecutor
    private String allModules() {
        try {
            if (RusherHackAPI.getHudManager().getFeature("CombinedBindList").isPresent()) {
                CombinedBindListHudElement hudElement = (CombinedBindListHudElement) RusherHackAPI.getHudManager().getFeature("CombinedBindList").get();

                if (hudElement.modules.isEmpty()) {
                    return "No modules found. Make sure the HUD element is loaded properly.";
                }

                StringBuilder modules = new StringBuilder("Available modules (");
                modules.append(hudElement.modules.size()).append("): ");

                for (int i = 0; i < hudElement.modules.size(); i++) {
                    CombinedBindListHudElement.ModuleHolder module = hudElement.modules.get(i);
                    modules.append(module.getId());

                    if (i < hudElement.modules.size() - 1) {
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
}