package dev.treeone;

import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.feature.command.Command;
import org.rusherhack.core.command.annotations.CommandExecutor;

public class HideModuleCommand extends Command {

    public HideModuleCommand() {
        super("combindlist", "Manages hidden modules and metadata - supports: add, remove, list, clear, metadata");
    }

    // Method for normalizing the module name (same as in getId())
    private String normalizeModuleName(String name) {
        return name.toLowerCase().replaceAll("[-\\s]", "");
    }

    @CommandExecutor
    private String showHelp() {
        return "CombinedBindList commands:\n" +
                "*combindlist add <ModuleName> - Hide a module from the bind list\n" +
                "*combindlist remove <ModuleName> - Show a hidden module again\n" +
                "*combindlist list - Show all currently hidden modules\n" +
                "*combindlist clear - Clear all hidden modules (make all visible)\n" +
                "*combindlist metaadd <ModuleName> - Hide metadata for a specific module\n" +
                "*combindlist metaremove <ModuleName> - Show metadata for a hidden module again\n" +
                "*combindlist metalist - Show all modules with hidden metadata\n" +
                "*combindlist metaclear - Clear all hidden metadata";
    }

    // ========== DEFAULT MODULES ==========

    @CommandExecutor(subCommand = "add")
    @CommandExecutor.Argument("string")
    private String addModule(String string) {
        try {
            if (RusherHackAPI.getHudManager().getFeature("CombinedBindList").isPresent()) {
                CombinedBindListHudElement hudElement = (CombinedBindListHudElement) RusherHackAPI.getHudManager().getFeature("CombinedBindList").get();

                String moduleId = normalizeModuleName(string);

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

                String moduleId = normalizeModuleName(string);

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

    // ========== METADATA ==========

    @CommandExecutor(subCommand = "metaadd")
    @CommandExecutor.Argument("string")
    private String addMetadata(String string) {
        try {
            if (RusherHackAPI.getHudManager().getFeature("CombinedBindList").isPresent()) {
                CombinedBindListHudElement hudElement = (CombinedBindListHudElement) RusherHackAPI.getHudManager().getFeature("CombinedBindList").get();

                String moduleId = normalizeModuleName(string);

                if (!hudElement.isModuleLoaded(moduleId)) {
                    return "No module with name matches '" + string + "'";
                }

                if (CombinedBindListHudElement.hiddenMetadataModules.contains(moduleId)) {
                    return "Metadata for module '" + string + "' is already hidden";
                }

                CombinedBindListHudElement.hiddenMetadataModules.add(moduleId);
                hudElement.save();
                return "Hidden metadata for '" + string + "'";
            } else {
                return "CombinedBindList HUD element is not present";
            }
        } catch (Exception e) {
            return "Error hiding metadata: " + e.getMessage();
        }
    }

    @CommandExecutor(subCommand = "metaremove")
    @CommandExecutor.Argument("string")
    private String removeMetadata(String string) {
        try {
            if (RusherHackAPI.getHudManager().getFeature("CombinedBindList").isPresent()) {
                CombinedBindListHudElement hudElement = (CombinedBindListHudElement) RusherHackAPI.getHudManager().getFeature("CombinedBindList").get();

                String moduleId = normalizeModuleName(string);

                if (CombinedBindListHudElement.hiddenMetadataModules.remove(moduleId)) {
                    hudElement.save();
                    return "Showing metadata for '" + string + "' again";
                } else {
                    return "Metadata for module '" + string + "' is not hidden";
                }
            } else {
                return "CombinedBindList HUD element is not present";
            }
        } catch (Exception e) {
            return "Error showing metadata: " + e.getMessage();
        }
    }

    @CommandExecutor(subCommand = "metalist")
    private String listHiddenMetadata() {
        try {
            if (RusherHackAPI.getHudManager().getFeature("CombinedBindList").isPresent()) {
                if (CombinedBindListHudElement.hiddenMetadataModules.isEmpty()) {
                    return "No modules have hidden metadata";
                }

                StringBuilder modules = new StringBuilder("Modules with hidden metadata: ");
                for (int i = 0; i < CombinedBindListHudElement.hiddenMetadataModules.size(); i++) {
                    String hiddenModule = CombinedBindListHudElement.hiddenMetadataModules.get(i);
                    modules.append(hiddenModule);

                    if (i < CombinedBindListHudElement.hiddenMetadataModules.size() - 1) {
                        modules.append(", ");
                    }
                }
                return modules.toString();
            } else {
                return "CombinedBindList HUD element is not present";
            }
        } catch (Exception e) {
            return "Error listing metadata: " + e.getMessage();
        }
    }

    @CommandExecutor(subCommand = "metaclear")
    private String clearHiddenMetadata() {
        try {
            if (RusherHackAPI.getHudManager().getFeature("CombinedBindList").isPresent()) {
                CombinedBindListHudElement hudElement = (CombinedBindListHudElement) RusherHackAPI.getHudManager().getFeature("CombinedBindList").get();

                int clearedCount = CombinedBindListHudElement.hiddenMetadataModules.size();
                CombinedBindListHudElement.hiddenMetadataModules.clear();
                hudElement.save();

                return "Cleared " + clearedCount + " hidden metadata modules - metadata will show for all modules";
            } else {
                return "CombinedBindList HUD element is not present";
            }
        } catch (Exception e) {
            return "Error clearing metadata: " + e.getMessage();
        }
    }
}