package dev.treeone;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.feature.command.Command;
import org.rusherhack.core.command.annotations.CommandExecutor;


public class ListModulesCommand extends Command {

    public ListModulesCommand() {
        super("listmodules", "description");
    }


    @CommandExecutor
    private String allModules() {
        if (RusherHackAPI.getHudManager().getFeature("CombinedBindList").isPresent()) {
            CombinedBindListHudElement hudElement = (CombinedBindListHudElement) RusherHackAPI.getHudManager().getFeature("CombinedBindList").get();
            StringBuilder modules = new StringBuilder();

            for (CombinedBindListHudElement.ModuleHolder module : hudElement.modules) {
                modules.append(module.getId()).append(", ");
            }
            return modules.toString();

        } else return "Hud element is not present";
    }
}