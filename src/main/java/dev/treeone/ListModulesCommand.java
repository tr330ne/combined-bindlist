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
            CombinedBindListHudElement hudElement = (CombinedBindListHudElement)
                    RusherHackAPI.getHudManager().getFeature("CombinedBindList").orElse(null);

            if (hudElement == null) {
                return "CombinedBindList HUD element is not present";
            }

            if (hudElement.modules == null || hudElement.modules.isEmpty()) {
                return "No modules found. Make sure the HUD element is loaded properly.";
            }

            StringBuilder modules = new StringBuilder("Available modules (")
                    .append(hudElement.modules.size()).append("): ");

            for (int i = 0; i < hudElement.modules.size(); i++) {
                CombinedBindListHudElement.ModuleHolder module = hudElement.modules.get(i);
                if (module != null) {
                    modules.append(module.getId());
                    if (i < hudElement.modules.size() - 1) {
                        modules.append(", ");
                    }
                }
            }

            return modules.toString();
        } catch (Exception e) {
            return "Error listing modules: " + e.getMessage();
        }
    }
}