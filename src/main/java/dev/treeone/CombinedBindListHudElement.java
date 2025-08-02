package dev.treeone;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.network.chat.Component;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.events.world.EventLoadWorld;
import org.rusherhack.client.api.feature.hud.ListHudElement;
import org.rusherhack.client.api.feature.module.IModule;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.setting.ColorSetting;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.bind.key.IKey;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class CombinedBindListHudElement extends ListHudElement {
    static List<String> hiddenModules = new ArrayList<>();
    private final BooleanSetting lowercase = new BooleanSetting("Lowercase", false);
    private final ColorSetting enabledColor = new ColorSetting("Enabled Color", "Color for enabled modules", Color.GREEN);
    private final ColorSetting disabledColor = new ColorSetting("Disabled Color", "Color for disabled modules", Color.RED);
    List<ModuleHolder> modules = new ArrayList<>();

    public CombinedBindListHudElement() {
        super("CombinedBindList");
        // Disable the opacity slider for color settings
        enabledColor.setAlphaAllowed(false);
        disabledColor.setAlphaAllowed(false);
        registerSettings(lowercase, enabledColor, disabledColor);
    }

    public void load() {
        this.color.setHidden(true); // Hide the default Color parameter
        modules.clear();

        try {
            if (RusherHackAPI.getModuleManager().getFeatures() != null)
                for (IModule feature : RusherHackAPI.getModuleManager().getFeatures()) {
                    if (feature instanceof ToggleableModule module) {
                        modules.add(new ModuleHolder(module));
                    }
                }

            if (Modules.get() != null)
                for (Module module : Modules.get().getList()) {
                    modules.add(new ModuleHolder(module));
                }

            hiddenModules.clear();
            hiddenModules.addAll(CombinedBindListPlugin.loadConfig());
        } catch (Exception e) {}
    }

    public void save() {
        CombinedBindListPlugin.saveConfig(hiddenModules);
    }

    public Boolean isModuleLoaded(String moduleId) {
        return modules.stream().map(ModuleHolder::getId).toList().contains(moduleId.toLowerCase());
    }

    @Override
    public void onEnable() {
        super.onEnable();
        load();
    }

    @Override
    public void onDisable() {
        super.onDisable();
        save();
    }

    @Subscribe
    public void onLoadWorld(EventLoadWorld event) {
        load();
    }

    @Subscribe
    public void onTick(EventUpdate event) {
        // Searching for modules whose binds are missing from the list
        for (ModuleHolder module : modules) {
            if (module.hasKeybind() && module.isVisible()) {
                boolean foundModule = false;
                for (ListItem member : getMembers()) {
                    if (member instanceof BindListItem bindListItem) {
                        if (bindListItem.module.equals(module)) {
                            foundModule = true;
                            break;
                        }
                    }
                }
                // If the module with the bind is not found in the list, add it
                if (!foundModule) {
                    add(new BindListItem(module, this));
                }
            }
        }
    }

    static class ModuleHolder {
        public Module meteorModule;
        public ToggleableModule rusherModule;
        public ModuleType moduleType;

        public ModuleHolder(Module meteorModule) {
            this.meteorModule = meteorModule;
            moduleType = ModuleType.METEOR;
        }

        public ModuleHolder(ToggleableModule rusherModule) {
            this.rusherModule = rusherModule;
            moduleType = ModuleType.RUSHER;
        }

        public boolean isEnabled() {
            if (moduleType == ModuleType.METEOR) {
                return meteorModule.isActive();
            } else if (moduleType == ModuleType.RUSHER) {
                return rusherModule.isToggled();
            }
            throw new RuntimeException("Type not supported");
        }

        public String getName() {
            if (moduleType == ModuleType.METEOR) {
                return meteorModule.title;
            } else if (moduleType == ModuleType.RUSHER) {
                return rusherModule.getName();
            }
            throw new RuntimeException("Type not supported");
        }

        public boolean hasKeybind() {
            if (moduleType == ModuleType.METEOR) {
                return meteorModule.keybind.isSet();
            } else if (moduleType == ModuleType.RUSHER) {
                try {
                    final IKey key = RusherHackAPI.getBindManager().getBindRegistry().get(rusherModule);
                    if (key != null) {
                        String keyLabel = key.getLabel(false);
                        return keyLabel != null && !keyLabel.isEmpty() &&
                                !keyLabel.equalsIgnoreCase("NONE") &&
                                !keyLabel.equalsIgnoreCase("unbound") &&
                                !keyLabel.equalsIgnoreCase("unknown");
                    }
                    return false;
                } catch (Exception e) {
                    return false;
                }
            }
            throw new RuntimeException("Type not supported");
        }

        public String getKeybind() {
            if (moduleType == ModuleType.METEOR) {
                try {
                    var keybind = meteorModule.keybind;

                    // Try to find public methods to get the GLFW code
                    Method[] methods = keybind.getClass().getMethods();

                    for (Method method : methods) {
                        if ((method.getName().equals("getKey") ||
                                method.getName().equals("getValue") ||
                                method.getName().equals("getGlfwKey")) &&
                                method.getParameterCount() == 0 &&
                                method.getReturnType() == int.class) {

                            int glfwKey = (int) method.invoke(keybind);
                            return formatGLFWKey(glfwKey);
                        }
                    }

                    // If no suitable method is found, use field reflection
                    Field keyField = keybind.getClass().getDeclaredField("key");
                    keyField.setAccessible(true);
                    int glfwKey = keyField.getInt(keybind);
                    return formatGLFWKey(glfwKey);

                } catch (Exception e) {
                    // If nothing works, use the default method
                    String rawKeybind = meteorModule.keybind.toString();
                    return formatKeybind(rawKeybind);
                }
            } else if (moduleType == ModuleType.RUSHER) {
                try {
                    final IKey key = RusherHackAPI.getBindManager().getBindRegistry().get(rusherModule);
                    if (key != null) {
                        String rawLabel = key.getLabel(true); // Get the raw name
                        return formatRusherKeybind(rawLabel);
                    }
                } catch (Exception e) {
                    // Ignore errors
                }
                return "unbound";
            }
            throw new RuntimeException("Type not supported");
        }

        // New method for formatting RusherHack binds
        private String formatRusherKeybind(String keybind) {
            if (keybind == null || keybind.isEmpty()) return "unbound";

            // Check numpad keys with the KEY_KP_ prefix
            switch (keybind) {
                case "KEY_KP_0": return "Num 0";
                case "KEY_KP_1": return "Num 1";
                case "KEY_KP_2": return "Num 2";
                case "KEY_KP_3": return "Num 3";
                case "KEY_KP_4": return "Num 4";
                case "KEY_KP_5": return "Num 5";
                case "KEY_KP_6": return "Num 6";
                case "KEY_KP_7": return "Num 7";
                case "KEY_KP_8": return "Num 8";
                case "KEY_KP_9": return "Num 9";
                case "KEY_KP_ADD": return "Num +";
                case "KEY_KP_DECIMAL": return "Num .";
                case "KEY_KP_DIVIDE": return "Num /";
                case "KEY_KP_ENTER": return "Num Enter";
                case "KEY_KP_EQUAL": return "Num =";
                case "KEY_KP_MULTIPLY": return "Num *";
                case "KEY_KP_SUBTRACT": return "Num -";

                // Special keys
                case "KEY_LEFT_SHIFT": return "Left Shift";
                case "KEY_RIGHT_SHIFT": return "Right Shift";
                case "KEY_LEFT_CONTROL": return "Left Ctrl";
                case "KEY_RIGHT_CONTROL": return "Right Ctrl";
                case "KEY_LEFT_ALT": return "Left Alt";
                case "KEY_RIGHT_ALT": return "Right Alt";
                case "KEY_LEFT_SUPER": return "Left Super";
                case "KEY_RIGHT_SUPER": return "Right Super";
                case "KEY_CAPS_LOCK": return "Caps Lock";
                case "KEY_NUM_LOCK": return "Num Lock";
                case "KEY_SCROLL_LOCK": return "Scroll Lock";
                case "KEY_PRINT_SCREEN": return "Print Screen";
                case "KEY_PAGE_UP": return "Page Up";
                case "KEY_PAGE_DOWN": return "Page Down";
                case "KEY_BACKSPACE": return "Backspace";
                case "KEY_ENTER": return "Enter";
                case "KEY_SPACE": return "Space";
                case "KEY_TAB": return "Tab";
                case "KEY_DELETE": return "Delete";
                case "KEY_INSERT": return "Insert";
                case "KEY_HOME": return "Home";
                case "KEY_END": return "End";
                case "KEY_UP": return "Up";
                case "KEY_DOWN": return "Down";
                case "KEY_LEFT": return "Left";
                case "KEY_RIGHT": return "Right";
                case "KEY_PAUSE": return "Pause";
                case "KEY_MENU": return "Menu";
                case "KEY_ESCAPE": return "Escape";

                // Function keys
                case "KEY_F1": return "F1"; case "KEY_F2": return "F2";
                case "KEY_F3": return "F3"; case "KEY_F4": return "F4";
                case "KEY_F5": return "F5"; case "KEY_F6": return "F6";
                case "KEY_F7": return "F7"; case "KEY_F8": return "F8";
                case "KEY_F9": return "F9"; case "KEY_F10": return "F10";
                case "KEY_F11": return "F11"; case "KEY_F12": return "F12";

                // Symbols
                case "KEY_APOSTROPHE": return "'";
                case "KEY_COMMA": return ",";
                case "KEY_MINUS": return "-";
                case "KEY_PERIOD": return ".";
                case "KEY_SLASH": return "/";
                case "KEY_SEMICOLON": return ";";
                case "KEY_EQUAL": return "=";
                case "KEY_LEFT_BRACKET": return "[";
                case "KEY_BACKSLASH": return "\\";
                case "KEY_RIGHT_BRACKET": return "]";
                case "KEY_GRAVE_ACCENT": return "`";

                // Remove the KEY_ prefix for the remaining keys
                default:
                    if (keybind.startsWith("KEY_")) {
                        String withoutPrefix = keybind.substring(4); // Remove "KEY_"

                        // If it's a digit or letter, return as is
                        if (withoutPrefix.matches("^[0-9A-Z]$")) {
                            return withoutPrefix;
                        }

                        // For the rest, apply pretty formatting
                        return formatKeyName(withoutPrefix);
                    }
                    return keybind;
            }
        }

        // Helper method for nicely formatting key names
        private String formatKeyName(String keyName) {
            if (keyName == null || keyName.isEmpty()) return keyName;

            // Replace underscores with spaces and capitalize the first letter
            String[] parts = keyName.toLowerCase().split("_");
            StringBuilder result = new StringBuilder();

            for (int i = 0; i < parts.length; i++) {
                if (i > 0) result.append(" ");
                if (!parts[i].isEmpty()) {
                    result.append(Character.toUpperCase(parts[i].charAt(0)));
                    if (parts[i].length() > 1) {
                        result.append(parts[i].substring(1));
                    }
                }
            }

            return result.toString();
        }

        // Method for formatting GLFW keys
        private String formatGLFWKey(int glfwKey) {
            // GLFW constants for numpad
            switch (glfwKey) {
                case 320: return "Num 0";  // GLFW_KEY_KP_0
                case 321: return "Num 1";  // GLFW_KEY_KP_1
                case 322: return "Num 2";  // GLFW_KEY_KP_2
                case 323: return "Num 3";  // GLFW_KEY_KP_3
                case 324: return "Num 4";  // GLFW_KEY_KP_4
                case 325: return "Num 5";  // GLFW_KEY_KP_5
                case 326: return "Num 6";  // GLFW_KEY_KP_6
                case 327: return "Num 7";  // GLFW_KEY_KP_7
                case 328: return "Num 8";  // GLFW_KEY_KP_8
                case 329: return "Num 9";  // GLFW_KEY_KP_9
                case 330: return "Num .";  // GLFW_KEY_KP_DECIMAL
                case 331: return "Num /";  // GLFW_KEY_KP_DIVIDE
                case 332: return "Num *";  // GLFW_KEY_KP_MULTIPLY
                case 333: return "Num -";  // GLFW_KEY_KP_SUBTRACT
                case 334: return "Num +";  // GLFW_KEY_KP_ADD
                case 335: return "Num Enter"; // GLFW_KEY_KP_ENTER
                case 336: return "Num =";  // GLFW_KEY_KP_EQUAL

                // Regular digits
                case 49: return "1"; // GLFW_KEY_1
                case 50: return "2"; // GLFW_KEY_2
                case 51: return "3"; // GLFW_KEY_3
                case 52: return "4"; // GLFW_KEY_4
                case 53: return "5"; // GLFW_KEY_5
                case 54: return "6"; // GLFW_KEY_6
                case 55: return "7"; // GLFW_KEY_7
                case 56: return "8"; // GLFW_KEY_8
                case 57: return "9"; // GLFW_KEY_9
                case 48: return "0"; // GLFW_KEY_0

                // Letters A-Z
                case 65: return "A"; case 66: return "B"; case 67: return "C"; case 68: return "D";
                case 69: return "E"; case 70: return "F"; case 71: return "G"; case 72: return "H";
                case 73: return "I"; case 74: return "J"; case 75: return "K"; case 76: return "L";
                case 77: return "M"; case 78: return "N"; case 79: return "O"; case 80: return "P";
                case 81: return "Q"; case 82: return "R"; case 83: return "S"; case 84: return "T";
                case 85: return "U"; case 86: return "V"; case 87: return "W"; case 88: return "X";
                case 89: return "Y"; case 90: return "Z";

                // Function keys
                case 290: return "F1"; case 291: return "F2"; case 292: return "F3"; case 293: return "F4";
                case 294: return "F5"; case 295: return "F6"; case 296: return "F7"; case 297: return "F8";
                case 298: return "F9"; case 299: return "F10"; case 300: return "F11"; case 301: return "F12";

                // Special keys
                case 32: return "SPACE";
                case 257: return "ENTER";
                case 258: return "TAB";
                case 259: return "BACKSPACE";
                case 260: return "INSERT";
                case 261: return "DELETE";
                case 262: return "RIGHT";
                case 263: return "LEFT";
                case 264: return "DOWN";
                case 265: return "UP";
                case 266: return "PAGE_UP";
                case 267: return "PAGE_DOWN";
                case 268: return "HOME";
                case 269: return "END";
                case 280: return "CAPS_LOCK";
                case 281: return "SCROLL_LOCK";
                case 282: return "NUM_LOCK";
                case 283: return "PRINT_SCREEN";
                case 284: return "PAUSE";
                case 340: return "LEFT_SHIFT";
                case 341: return "LEFT_CONTROL";
                case 342: return "LEFT_ALT";
                case 343: return "LEFT_SUPER";
                case 344: return "RIGHT_SHIFT";
                case 345: return "RIGHT_CONTROL";
                case 346: return "RIGHT_ALT";
                case 347: return "RIGHT_SUPER";
                case 348: return "MENU";

                // Symbols
                case 39: return "'";
                case 44: return ",";
                case 45: return "-";
                case 46: return ".";
                case 47: return "/";
                case 59: return ";";
                case 61: return "=";
                case 91: return "[";
                case 92: return "\\";
                case 93: return "]";
                case 96: return "`";

                // For unknown keys, try using GLFW
                default:
                    try {
                        String keyName = org.lwjgl.glfw.GLFW.glfwGetKeyName(glfwKey, 0);
                        if (keyName != null && !keyName.isEmpty()) {
                            return keyName.toUpperCase();
                        }
                    } catch (Exception e) {
                        // Ignore
                    }
                    return "KEY_" + glfwKey;
            }
        }

        private String formatKeybind(String keybind) {
            if (keybind == null) return "unbound";

            // Checking numpad keys with the KEY_KP_ prefix
            switch (keybind) {
                case "KEY_KP_0": return "Num 0";
                case "KEY_KP_1": return "Num 1";
                case "KEY_KP_2": return "Num 2";
                case "KEY_KP_3": return "Num 3";
                case "KEY_KP_4": return "Num 4";
                case "KEY_KP_5": return "Num 5";
                case "KEY_KP_6": return "Num 6";
                case "KEY_KP_7": return "Num 7";
                case "KEY_KP_8": return "Num 8";
                case "KEY_KP_9": return "Num 9";
                case "KEY_KP_ADD": return "Num +";
                case "KEY_KP_DECIMAL": return "Num .";
                case "KEY_KP_DIVIDE": return "Num /";
                case "KEY_KP_ENTER": return "Num Enter";
                case "KEY_KP_EQUAL": return "Num =";
                case "KEY_KP_MULTIPLY": return "Num *";
                case "KEY_KP_SUBTRACT": return "Num -";

                // Removing the KEY_ prefix for regular keys
                default:
                    if (keybind.startsWith("KEY_")) {
                        return keybind.substring(4); // Remove "KEY_"
                    }
                    return keybind;
            }
        }

        public String getMetadata() {
            if (moduleType == ModuleType.METEOR) {
                return meteorModule.getInfoString();
            } else if (moduleType == ModuleType.RUSHER) {
                if (rusherModule.getMetadata().isEmpty()) return null;
                else return rusherModule.getMetadata();
            }
            throw new RuntimeException("Type not supported");
        }

        public boolean isVisible() {
            if (moduleType == ModuleType.METEOR) {
                return !hiddenModules.contains(this.getId());
            } else if (moduleType == ModuleType.RUSHER) {
                return !(hiddenModules.contains(this.getId()) || rusherModule.isHidden());
            }
            throw new RuntimeException("Type not supported");
        }

        public String getId() {
            if (moduleType == ModuleType.METEOR) {
                return meteorModule.name.toLowerCase().replaceAll("-", "");
            } else if (moduleType == ModuleType.RUSHER) {
                return rusherModule.getName().toLowerCase();
            }
            throw new RuntimeException("Type not supported");
        }

        public boolean equals(ModuleHolder moduleHolder) {
            if (moduleType == ModuleType.METEOR) {
                return meteorModule == moduleHolder.meteorModule;
            } else if (moduleType == ModuleType.RUSHER) {
                return rusherModule == moduleHolder.rusherModule;
            }
            throw new RuntimeException("Type not supported");
        }

        public enum ModuleType {
            RUSHER,
            METEOR
        }
    }

    class BindListItem extends ListItem {
        public ModuleHolder module;

        public BindListItem(ModuleHolder module, ListHudElement parent) {
            super(parent);
            this.module = module;
        }

        @Override
        public Component getText() {
            String name = lowercase.getValue() ? module.getName().toLowerCase() : module.getName();
            String keybind = module.getKeybind();
            // Using custom colors from ColorSetting
            int color = module.isEnabled() ? enabledColor.getValueRGB() : disabledColor.getValueRGB();
            String displayText = name + " [" + keybind + "]";
            return Component.literal(displayText).withColor(color);
        }

        @Override
        public boolean shouldRemove() {
            // Remove if module doesn't have keybind or is not visible
            return !module.hasKeybind() || !module.isVisible();
        }
    }
}