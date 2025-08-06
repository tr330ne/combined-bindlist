package dev.treeone;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.feature.command.Command;
import org.rusherhack.core.command.annotations.CommandExecutor;

import java.util.Optional;

public class HideModuleCommand extends Command {

    public HideModuleCommand() {
        super("combindlist", "Manages hidden modules and metadata - supports: add, remove, list, clear, meta commands");
    }

    private String normalizeModuleName(String name) {
        return name.toLowerCase().replaceAll("[-\\s]", "");
    }

    private String createClientSpecificId(String moduleId, String client) {
        return moduleId + "_" + client.toLowerCase();
    }

    private boolean hasDuplicates(String moduleId, CombinedBindListHudElement hudElement) {
        if (hudElement == null || hudElement.modules == null) return false;

        boolean hasRusher = false, hasMeteor = false;
        for (CombinedBindListHudElement.ModuleHolder module : hudElement.modules) {
            if (module != null && moduleId.equals(module.getId())) {
                if (module.moduleType == CombinedBindListHudElement.ModuleHolder.ModuleType.RUSHER) {
                    hasRusher = true;
                } else if (module.moduleType == CombinedBindListHudElement.ModuleHolder.ModuleType.METEOR) {
                    hasMeteor = true;
                }
            }
        }
        return hasRusher && hasMeteor;
    }

    private CombinedBindListHudElement getHudElement() {
        try {
            return (CombinedBindListHudElement) RusherHackAPI.getHudManager()
                    .getFeature("CombinedBindList").orElse(null);
        } catch (Exception e) {
            return null;
        }
    }

    @CommandExecutor
    private String showHelp() {
        return """
        CombinedBindList Commands:
        
        Module visibility:
        • add <name> [rusher|meteor] - Hide module
        • remove <name> [rusher|meteor] - Show module  
        • list - List hidden modules
        • clear - Show all modules
        
        Metadata visibility:
        • metaadd <name> [rusher|meteor] - Hide metadata
        • metaremove <name> [rusher|meteor] - Show metadata
        • metalist - List modules with hidden metadata  
        • metaclear - Show all metadata
        
        Note: Use [rusher|meteor] only for duplicate modules""";
    }

    @CommandExecutor(subCommand = "add")
    @CommandExecutor.Argument({"string", "string"})
    private String addModule(String moduleName, Optional<String> client) {
        CombinedBindListHudElement hudElement = getHudElement();
        if (hudElement == null) return "CombinedBindList HUD element is not present";

        String moduleId = normalizeModuleName(moduleName);
        if (!hudElement.isModuleLoaded(moduleId)) {
            return "No module with name matches '" + moduleName + "'";
        }

        if (client.isPresent()) {
            String clientType = client.get().toLowerCase();
            if (!clientType.equals("rusher") && !clientType.equals("meteor")) {
                return "Invalid client type '" + client.get() + "'. Use 'rusher' or 'meteor'";
            }
            if (!hasDuplicates(moduleId, hudElement)) {
                return "This module doesn't have a duplicate, use *combindlist add without rusher/meteor at the end";
            }
            String clientSpecificId = createClientSpecificId(moduleId, clientType);
            if (CombinedBindListHudElement.hiddenModules.contains(clientSpecificId)) {
                return "Module '" + moduleName + "' from " + clientType + " is already hidden";
            }
            CombinedBindListHudElement.hiddenModules.add(clientSpecificId);
            hudElement.save();
            return "Added '" + moduleName + "' from " + clientType + " to hidden module list";
        } else {
            if (CombinedBindListHudElement.hiddenModules.contains(moduleId)) {
                return "Module '" + moduleName + "' is already hidden";
            }
            CombinedBindListHudElement.hiddenModules.add(moduleId);
            hudElement.save();
            return "Added '" + moduleName + "' to hidden module list";
        }
    }

    @CommandExecutor(subCommand = "remove")
    @CommandExecutor.Argument({"string", "string"})
    private String removeModule(String moduleName, Optional<String> client) {
        CombinedBindListHudElement hudElement = getHudElement();
        if (hudElement == null) return "CombinedBindList HUD element is not present";

        String moduleId = normalizeModuleName(moduleName);

        if (client.isPresent()) {
            String clientType = client.get().toLowerCase();
            if (!clientType.equals("rusher") && !clientType.equals("meteor")) {
                return "Invalid client type '" + client.get() + "'. Use 'rusher' or 'meteor'";
            }
            String clientSpecificId = createClientSpecificId(moduleId, clientType);
            if (CombinedBindListHudElement.hiddenModules.remove(clientSpecificId)) {
                hudElement.save();
                return "Removed '" + moduleName + "' from " + clientType + " from hidden modules list";
            } else {
                return "Module '" + moduleName + "' from " + clientType + " is not in the hidden list";
            }
        } else {
            boolean removed = CombinedBindListHudElement.hiddenModules.remove(moduleId);
            removed |= CombinedBindListHudElement.hiddenModules.remove(createClientSpecificId(moduleId, "rusher"));
            removed |= CombinedBindListHudElement.hiddenModules.remove(createClientSpecificId(moduleId, "meteor"));

            if (removed) {
                hudElement.save();
                return "Removed '" + moduleName + "' from hidden modules list";
            } else {
                return "Module '" + moduleName + "' is not in the hidden list";
            }
        }
    }

    @CommandExecutor(subCommand = "list")
    private String listHiddenModules() {
        if (CombinedBindListHudElement.hiddenModules.isEmpty()) {
            return "No modules are currently hidden";
        }

        StringBuilder modules = new StringBuilder("Hidden modules: ");
        for (int i = 0; i < CombinedBindListHudElement.hiddenModules.size(); i++) {
            String hiddenModule = CombinedBindListHudElement.hiddenModules.get(i);

            if (hiddenModule.contains("_rusher")) {
                modules.append(hiddenModule.replace("_rusher", " (rusher)"));
            } else if (hiddenModule.contains("_meteor")) {
                modules.append(hiddenModule.replace("_meteor", " (meteor)"));
            } else {
                modules.append(hiddenModule);
            }

            if (i < CombinedBindListHudElement.hiddenModules.size() - 1) {
                modules.append(", ");
            }
        }
        return modules.toString();
    }

    @CommandExecutor(subCommand = "clear")
    private String clearHiddenModules() {
        CombinedBindListHudElement hudElement = getHudElement();
        if (hudElement == null) return "CombinedBindList HUD element is not present";

        int clearedCount = CombinedBindListHudElement.hiddenModules.size();
        CombinedBindListHudElement.hiddenModules.clear();
        hudElement.save();
        return "Cleared " + clearedCount + " hidden modules";
    }

    @CommandExecutor(subCommand = "metaadd")
    @CommandExecutor.Argument({"string", "string"})
    private String addMetadata(String moduleName, Optional<String> client) {
        return handleMetadataCommand(moduleName, client, true, "add");
    }

    @CommandExecutor(subCommand = "metaremove")
    @CommandExecutor.Argument({"string", "string"})
    private String removeMetadata(String moduleName, Optional<String> client) {
        return handleMetadataCommand(moduleName, client, false, "remove");
    }

    private String handleMetadataCommand(String moduleName, Optional<String> client, boolean isAdd, String action) {
        CombinedBindListHudElement hudElement = getHudElement();
        if (hudElement == null) return "CombinedBindList HUD element is not present";

        String moduleId = normalizeModuleName(moduleName);
        if (isAdd && !hudElement.isModuleLoaded(moduleId)) {
            return "No module with name matches '" + moduleName + "'";
        }

        if (client.isPresent()) {
            String clientType = client.get().toLowerCase();
            if (!clientType.equals("rusher") && !clientType.equals("meteor")) {
                return "Invalid client type '" + client.get() + "'. Use 'rusher' or 'meteor'";
            }
            if (isAdd && !hasDuplicates(moduleId, hudElement)) {
                return "This module doesn't have a duplicate, use *combindlist meta" + action + " without rusher/meteor at the end";
            }

            String clientSpecificId = createClientSpecificId(moduleId, clientType);
            boolean changed = isAdd ?
                    !CombinedBindListHudElement.hiddenMetadataModules.contains(clientSpecificId) &&
                            CombinedBindListHudElement.hiddenMetadataModules.add(clientSpecificId) :
                    CombinedBindListHudElement.hiddenMetadataModules.remove(clientSpecificId);

            if (changed) {
                hudElement.save();
                return (isAdd ? "Hidden" : "Showing") + " metadata for '" + moduleName + "' from " + clientType +
                        (isAdd ? "" : " again");
            } else {
                return "Metadata for module '" + moduleName + "' from " + clientType + " is " +
                        (isAdd ? "already hidden" : "not hidden");
            }
        } else {
            boolean changed = false;
            if (isAdd) {
                if (!CombinedBindListHudElement.hiddenMetadataModules.contains(moduleId)) {
                    CombinedBindListHudElement.hiddenMetadataModules.add(moduleId);
                    changed = true;
                }
            } else {
                changed = CombinedBindListHudElement.hiddenMetadataModules.remove(moduleId);
                changed |= CombinedBindListHudElement.hiddenMetadataModules.remove(createClientSpecificId(moduleId, "rusher"));
                changed |= CombinedBindListHudElement.hiddenMetadataModules.remove(createClientSpecificId(moduleId, "meteor"));
            }

            if (changed) {
                hudElement.save();
                return (isAdd ? "Hidden" : "Showing") + " metadata for '" + moduleName + "'" +
                        (isAdd ? "" : " again");
            } else {
                return "Metadata for module '" + moduleName + "' is " +
                        (isAdd ? "already hidden" : "not hidden");
            }
        }
    }

    @CommandExecutor(subCommand = "metalist")
    private String listHiddenMetadata() {
        if (CombinedBindListHudElement.hiddenMetadataModules.isEmpty()) {
            return "No modules have hidden metadata";
        }

        StringBuilder modules = new StringBuilder("Modules with hidden metadata: ");
        for (int i = 0; i < CombinedBindListHudElement.hiddenMetadataModules.size(); i++) {
            String hiddenModule = CombinedBindListHudElement.hiddenMetadataModules.get(i);

            if (hiddenModule.contains("_rusher")) {
                modules.append(hiddenModule.replace("_rusher", " (rusher)"));
            } else if (hiddenModule.contains("_meteor")) {
                modules.append(hiddenModule.replace("_meteor", " (meteor)"));
            } else {
                modules.append(hiddenModule);
            }

            if (i < CombinedBindListHudElement.hiddenMetadataModules.size() - 1) {
                modules.append(", ");
            }
        }
        return modules.toString();
    }

    @CommandExecutor(subCommand = "metaclear")
    private String clearHiddenMetadata() {
        CombinedBindListHudElement hudElement = getHudElement();
        if (hudElement == null) return "CombinedBindList HUD element is not present";

        int clearedCount = CombinedBindListHudElement.hiddenMetadataModules.size();
        CombinedBindListHudElement.hiddenMetadataModules.clear();
        hudElement.save();
        return "Cleared " + clearedCount + " hidden metadata modules";
    }
}