package dev.treeone;

import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.events.client.EventUpdate;
import org.rusherhack.client.api.events.world.EventLoadWorld;
import org.rusherhack.client.api.feature.hud.ListHudElement;
import org.rusherhack.client.api.feature.module.IModule;
import org.rusherhack.client.api.feature.module.ToggleableModule;
import org.rusherhack.client.api.setting.ColorSetting;
import org.rusherhack.core.event.subscribe.Subscribe;
import org.rusherhack.core.setting.BooleanSetting;
import org.rusherhack.core.setting.EnumSetting;
import org.rusherhack.core.bind.key.IKey;

import java.awt.Color;
import java.util.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class CombinedBindListHudElement extends ListHudElement {
    static List<String> hiddenModules = new ArrayList<>();
    static List<String> hiddenMetadataModules = new ArrayList<>();

    private final EnumSetting<CaseType> caseSetting = new EnumSetting<>("Case", "", CaseType.Default);
    private final EnumSetting<CaseType> keyCase = new EnumSetting<>("KeyCase", "", CaseType.Default);
    private final EnumSetting<CaseType> metaCase = new EnumSetting<>("MetaCase", "", CaseType.Default);
    private final BooleanSetting showKeys = new BooleanSetting("ShowKeys", "Show keybind text for modules with keybinds", true);
    private final BooleanSetting rawKeys = new BooleanSetting("RawKeys", "Display raw key names instead of formatted names", false);
    private final BooleanSetting activeUnbound = new BooleanSetting("ActiveUnbound", "Shows active modules without a keybind", false);
    private final BooleanSetting boundAsUnbound = new BooleanSetting("BoundAsUnbound", "Display keybound modules as unbound active modules", false);
    private final BooleanSetting hideBounded = new BooleanSetting("HideBounded", "Hides modules with a keybind", false);
    private final BooleanSetting boundedMeta = new BooleanSetting("BoundedMeta", "Show metadata for modules with keybinds", false);
    private final BooleanSetting unboundMeta = new BooleanSetting("UnboundMeta", "Show metadata for active unbound modules", false);
    private final BooleanSetting stateKeys = new BooleanSetting("StateKeys", "Use state color for keybind text", false);
    private final BooleanSetting stateBMeta = new BooleanSetting("StateBMeta", "Use state color for metadata of bound modules", true);
    private final BooleanSetting useStateBrackets = new BooleanSetting("StateBrackets", "Use state color for keybind brackets", true);
    private final BooleanSetting stateMBrackets = new BooleanSetting("StateMBrackets", "Use state color for metadata brackets of bound modules", true);
    private final EnumSetting<KeyBracketsStyle> keyBStyle = new EnumSetting<>("KeyBStyle", "Style of keybind brackets", KeyBracketsStyle.Square);
    private final EnumSetting<MetaBracketsStyle> metaBStyle = new EnumSetting<>("MetaBStyle", "Style of metadata brackets", MetaBracketsStyle.Round);
    private final ColorSetting enabledColor = new ColorSetting("Enabled", "Color for enabled modules", Color.GREEN)
            .setRainbowAllowed(true).setThemeSyncAllowed(true);
    private final ColorSetting disabledColor = new ColorSetting("Disabled", "Color for disabled modules", Color.RED)
            .setRainbowAllowed(true).setThemeSyncAllowed(true);
    private final ColorSetting unboundColor = new ColorSetting("Unbound", "Color for active modules without keybinds", Color.CYAN)
            .setRainbowAllowed(true).setThemeSyncAllowed(true);
    private final ColorSetting keybindsColor = new ColorSetting("Keybinds", "Color for keybind text", Color.WHITE)
            .setRainbowAllowed(true).setThemeSyncAllowed(true);
    private final ColorSetting bracketsColor = new ColorSetting("Brackets", "Color for keybind brackets", Color.BLUE)
            .setRainbowAllowed(true).setThemeSyncAllowed(true);
    private final ColorSetting mBoundColor = new ColorSetting("MBound", "Color for metadata text of bound modules", Color.YELLOW)
            .setRainbowAllowed(true).setThemeSyncAllowed(true);
    private final ColorSetting mUnboundColor = new ColorSetting("MUnbound", "Color for metadata text of unbound modules", Color.YELLOW)
            .setRainbowAllowed(true).setThemeSyncAllowed(true);
    private final ColorSetting mBrackets = new ColorSetting("MBrackets", "Color for metadata brackets of bound modules", Color.BLUE)
            .setRainbowAllowed(true).setThemeSyncAllowed(true);
    private final ColorSetting mUBrackets = new ColorSetting("MUBrackets", "Color for metadata brackets of unbound modules", Color.BLUE)
            .setRainbowAllowed(true).setThemeSyncAllowed(true);

    List<ModuleHolder> modules = new ArrayList<>();
    private long lastModuleLoadTime = 0;
    private static final long MODULE_LOAD_COOLDOWN = 1000;

    public enum CaseType { Default, Lowercase, Uppercase }
    public enum KeyBracketsStyle { Square, Round, Curly, Angle, Pipe, None }
    public enum MetaBracketsStyle { Round, Square, Curly, Angle, Pipe, None }

    public CombinedBindListHudElement() {
        super("CombinedBindList");
        enabledColor.setAlphaAllowed(false);
        disabledColor.setAlphaAllowed(false);
        unboundColor.setAlphaAllowed(false);
        keybindsColor.setAlphaAllowed(false);
        bracketsColor.setAlphaAllowed(false);
        mBrackets.setAlphaAllowed(false);
        mUBrackets.setAlphaAllowed(false);
        mBoundColor.setAlphaAllowed(false);
        mUnboundColor.setAlphaAllowed(false);
        registerSettings(caseSetting, keyCase, metaCase, showKeys, rawKeys, activeUnbound, boundAsUnbound, hideBounded, boundedMeta, unboundMeta, stateKeys, stateBMeta, useStateBrackets, stateMBrackets, keyBStyle, metaBStyle, enabledColor, disabledColor, unboundColor, keybindsColor, bracketsColor, mBoundColor, mUnboundColor, mBrackets, mUBrackets);
    }

    private String stripMinecraftColors(String text) {
        return text == null ? null : text.replaceAll("[Ã‚Â§&][0-9a-fk-orA-FK-OR]", "");
    }

    private static boolean checkMeteorAvailability() {
        try {
            Class.forName("meteordevelopment.meteorclient.systems.modules.Modules");
            return Modules.get() != null;
        } catch (Exception | LinkageError e) {
            return false;
        }
    }

    private static boolean checkRusherAvailability() {
        try {
            Class.forName("org.rusherhack.client.api.feature.module.ToggleableModule");
            return RusherHackAPI.getModuleManager() != null && RusherHackAPI.getModuleManager().getFeatures() != null;
        } catch (Exception e) {
            return false;
        }
    }

    public void load() {
        this.color.setHidden(true);
        this.color.setThemeSync(true);
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastModuleLoadTime < MODULE_LOAD_COOLDOWN) return;

        lastModuleLoadTime = currentTime;
        modules.clear();

        try {
            if (checkRusherAvailability()) {
                var features = RusherHackAPI.getModuleManager().getFeatures();
                if (features != null) {
                    for (IModule feature : features) {
                        if (feature instanceof ToggleableModule module) {
                            modules.add(new ModuleHolder(module));
                        }
                    }
                }
            }
            if (checkMeteorAvailability()) {
                var modulesList = Modules.get().getList();
                if (modulesList != null) {
                    for (Module module : modulesList) {
                        modules.add(new ModuleHolder(module));
                    }
                }
            }
            hiddenModules.clear();
            hiddenModules.addAll(CombinedBindListPlugin.loadConfig());
            hiddenMetadataModules.clear();
            hiddenMetadataModules.addAll(CombinedBindListPlugin.loadMetadataConfig());
        } catch (Exception ignored) {
        }
    }

    private void rebuildModuleList() {
        try {
            List<ListItem> itemsToRemove = new ArrayList<>();

            for (ListItem item : getMembers()) {
                if (item instanceof BindListItem bindItem) {
                    if (bindItem.shouldRemove()) {
                        itemsToRemove.add(item);
                    }
                } else if (item instanceof ExtraListItem extraItem) {
                    if (extraItem.shouldRemove()) {
                        itemsToRemove.add(item);
                    }
                }
            }

            for (ListItem item : itemsToRemove) {
                getMembers().remove(item);
            }

            if (!hideBounded.getValue()) {
                for (ModuleHolder module : modules) {
                    if (module != null && module.hasKeybind() && module.isVisible()) {
                        boolean alreadyExists = getMembers().stream()
                                .anyMatch(item -> item instanceof BindListItem bindItem &&
                                        bindItem.module.equals(module));
                        if (!alreadyExists) {
                            add(new BindListItem(module, this));
                        }
                    }
                }
            }

            if (activeUnbound.getValue()) {
                for (ModuleHolder module : modules) {
                    if (module != null && !module.hasKeybind() && module.isEnabled() && module.isVisible()) {
                        boolean alreadyExists = getMembers().stream()
                                .anyMatch(item -> item instanceof ExtraListItem extraItem &&
                                        extraItem.module.equals(module));
                        if (!alreadyExists) {
                            add(new ExtraListItem(module, this));
                        }
                    }
                }
            }
        } catch (Exception ignored) {
        }
    }

    public void save() {
        try {
            CombinedBindListPlugin.saveConfig(hiddenModules);
            CombinedBindListPlugin.saveMetadataConfig(hiddenMetadataModules);
        } catch (Exception ignored) {
        }
    }

    public Boolean isModuleLoaded(String moduleId) {
        try {
            return modules.stream().map(ModuleHolder::getId).anyMatch(id -> id.equals(moduleId.toLowerCase()));
        } catch (Exception e) {
            return false;
        }
    }

    public Set<String> getDuplicateModules() {
        try {
            Map<String, Integer> moduleCount = new HashMap<>();
            for (ModuleHolder module : modules) {
                String id = module.getId();
                moduleCount.put(id, moduleCount.getOrDefault(id, 0) + 1);
            }

            Set<String> duplicates = new HashSet<>();
            for (Map.Entry<String, Integer> entry : moduleCount.entrySet()) {
                if (entry.getValue() > 1) {
                    duplicates.add(entry.getKey());
                }
            }
            return duplicates;
        } catch (Exception e) {
            return new HashSet<>();
        }
    }

    @Override
    public void onEnable() {
        super.onEnable();
        try {
            load();
            rebuildModuleList();
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onDisable() {
        super.onDisable();
        save();
    }

    @Subscribe
    public void onLoadWorld(EventLoadWorld event) {
        try {
            getMembers().clear();
            load();
            rebuildModuleList();
        } catch (Exception ignored) {
        }
    }

    @Subscribe
    public void onTick(EventUpdate event) {
        try {
            int currentModuleCount = 0;

            if (checkRusherAvailability()) {
                var features = RusherHackAPI.getModuleManager().getFeatures();
                if (features != null) {
                    for (IModule feature : features) {
                        if (feature instanceof ToggleableModule) currentModuleCount++;
                    }
                }
            }

            if (checkMeteorAvailability()) {
                var modulesList = Modules.get().getList();
                if (modulesList != null) currentModuleCount += modulesList.size();
            }

            if (Math.abs(currentModuleCount - modules.size()) > 0) {
                getMembers().clear();
                lastModuleLoadTime = 0;
                load();
                return;
            }

            if (!hideBounded.getValue()) {
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
                        if (!foundModule) {
                            add(new BindListItem(module, this));
                        }
                    }
                }
            }

            if (activeUnbound.getValue()) {
                for (ModuleHolder module : modules) {
                    if (!module.hasKeybind() && module.isEnabled() && module.isVisible()) {
                        boolean foundModule = false;
                        for (ListItem member : getMembers()) {
                            if (member instanceof ExtraListItem extraListItem) {
                                if (extraListItem.module.equals(module)) {
                                    foundModule = true;
                                    break;
                                }
                            }
                        }
                        if (!foundModule) {
                            add(new ExtraListItem(module, this));
                        }
                    }
                }
            }

        } catch (Exception ignored) {
        }
    }

    static class ModuleHolder {
        public Module meteorModule;
        public ToggleableModule rusherModule;
        public ModuleType moduleType;

        private static final Map<String, String> RUSHER_KEY_MAP = new HashMap<>() {{
            put("MOUSE_1", "Mouse Left"); put("MOUSE_2", "Mouse Right"); put("MOUSE_3", "Middle");
            put("MOUSE_4", "Side Down"); put("MOUSE_5", "Side Up");
            put("KEY_LEFT_SHIFT", "Left Shift"); put("KEY_RIGHT_SHIFT", "Right Shift");
            put("KEY_LEFT_CONTROL", "Left Ctrl"); put("KEY_RIGHT_CONTROL", "Right Ctrl");
            put("KEY_LEFT_ALT", "Left Alt"); put("KEY_RIGHT_ALT", "Right Alt");
            put("KEY_CAPS_LOCK", "Caps Lock"); put("KEY_NUM_LOCK", "Num Lock");
            put("KEY_SCROLL_LOCK", "Scroll Lock"); put("KEY_PRINT_SCREEN", "Print Screen");
            put("KEY_PAGE_UP", "Page Up"); put("KEY_PAGE_DOWN", "Page Down");
            put("KEY_BACKSPACE", "Backspace"); put("KEY_ENTER", "Enter"); put("KEY_SPACE", "Space");
            put("KEY_TAB", "Tab"); put("KEY_DELETE", "Delete"); put("KEY_INSERT", "Insert");
            put("KEY_HOME", "Home"); put("KEY_END", "End"); put("KEY_UP", "Up"); put("KEY_DOWN", "Down");
            put("KEY_LEFT", "Left"); put("KEY_RIGHT", "Right"); put("KEY_PAUSE", "Pause");
            put("KEY_MENU", "Menu"); put("KEY_ESCAPE", "Escape"); put("KEY_GRAVE_ACCENT", "Grave");
            put("KEY_APOSTROPHE", "'"); put("KEY_COMMA", ","); put("KEY_MINUS", "-");
            put("KEY_PERIOD", "."); put("KEY_SLASH", "/"); put("KEY_SEMICOLON", ";");
            put("KEY_EQUAL", "="); put("KEY_LEFT_BRACKET", "["); put("KEY_BACKSLASH", "\\");
            put("KEY_RIGHT_BRACKET", "]");
        }};

        private static final Map<Integer, String> GLFW_KEY_MAP = new HashMap<>() {{
            put(-100, "Mouse Left"); put(-99, "Mouse Right"); put(-98, "Middle");
            put(-97, "Side Down"); put(-96, "Side Up");
            put(0, "Mouse Left"); put(1, "Mouse Right"); put(2, "Middle");
            put(3, "Side Down"); put(4, "Side Up");

            put(32, "Space"); put(257, "Enter"); put(258, "Tab"); put(259, "Backspace");
            put(260, "Insert"); put(261, "Delete"); put(262, "Right"); put(263, "Left");
            put(264, "Down"); put(265, "Up"); put(266, "Page Up"); put(267, "Page Down");
            put(268, "Home"); put(269, "End"); put(280, "Caps Lock"); put(281, "Scroll Lock");
            put(282, "Num Lock"); put(283, "Print Screen"); put(284, "Pause");
            put(340, "Left Shift"); put(341, "Left Ctrl"); put(342, "Left Alt");
            put(343, "Win"); put(344, "Right Shift"); put(345, "Right Ctrl");
            put(346, "Right Alt"); put(347, "Right Super"); put(348, "Menu");
            put(39, "'"); put(44, ","); put(45, "-"); put(46, "."); put(47, "/");
            put(59, ";"); put(61, "="); put(91, "["); put(92, "\\"); put(93, "]"); put(96, "Grave");
        }};

        static {
            for (int i = 0; i <= 9; i++) {
                RUSHER_KEY_MAP.put("KEY_KP_" + i, "Num " + i);
                GLFW_KEY_MAP.put(320 + i, "Num " + i);
                GLFW_KEY_MAP.put(48 + i, String.valueOf(i));
            }
            for (int i = 0; i < 26; i++) {
                char letter = (char) ('A' + i);
                GLFW_KEY_MAP.put(65 + i, String.valueOf(letter));
            }
            for (int i = 1; i <= 12; i++) {
                RUSHER_KEY_MAP.put("KEY_F" + i, "F" + i);
                GLFW_KEY_MAP.put(289 + i, "F" + i);
            }
            RUSHER_KEY_MAP.put("KEY_KP_ADD", "Num +");
            RUSHER_KEY_MAP.put("KEY_KP_DECIMAL", "Num .");
            RUSHER_KEY_MAP.put("KEY_KP_DIVIDE", "Num /");
            RUSHER_KEY_MAP.put("KEY_KP_ENTER", "Num Enter");
            RUSHER_KEY_MAP.put("KEY_KP_EQUAL", "Num =");
            RUSHER_KEY_MAP.put("KEY_KP_MULTIPLY", "Num *");
            RUSHER_KEY_MAP.put("KEY_KP_SUBTRACT", "Num -");

            GLFW_KEY_MAP.put(330, "Num .");
            GLFW_KEY_MAP.put(331, "Num /");
            GLFW_KEY_MAP.put(332, "Num *");
            GLFW_KEY_MAP.put(333, "Num -");
            GLFW_KEY_MAP.put(334, "Num +");
            GLFW_KEY_MAP.put(335, "Num Enter");
            GLFW_KEY_MAP.put(336, "Num =");
        }

        public enum ModuleType { RUSHER, METEOR }

        public ModuleHolder(Module meteorModule) {
            this.meteorModule = meteorModule;
            moduleType = ModuleType.METEOR;
        }

        public ModuleHolder(ToggleableModule rusherModule) {
            this.rusherModule = rusherModule;
            moduleType = ModuleType.RUSHER;
        }

        public boolean isEnabled() {
            try {
                return moduleType == ModuleType.METEOR ?
                        (meteorModule != null && meteorModule.isActive()) :
                        (rusherModule != null && rusherModule.isToggled());
            } catch (Exception e) {
                return false;
            }
        }

        public String getName() {
            try {
                return moduleType == ModuleType.METEOR ?
                        (meteorModule != null ? meteorModule.title : "Unknown") :
                        (rusherModule != null ? rusherModule.getName() : "Unknown");
            } catch (Exception e) {
                return "Unknown";
            }
        }

        public boolean hasKeybind() {
            try {
                if (moduleType == ModuleType.METEOR && meteorModule != null) {
                    return meteorModule.keybind != null && meteorModule.keybind.isSet();
                } else if (moduleType == ModuleType.RUSHER && rusherModule != null) {
                    final IKey key = RusherHackAPI.getBindManager().getBindRegistry().get(rusherModule);
                    if (key != null) {
                        String keyLabel = key.getLabel(false);
                        return keyLabel != null && !keyLabel.isEmpty() &&
                                !keyLabel.equalsIgnoreCase("NONE") &&
                                !keyLabel.equalsIgnoreCase("unbound") &&
                                !keyLabel.equalsIgnoreCase("unknown");
                    }
                }
                return false;
            } catch (Exception e) {
                return false;
            }
        }

        public String getKeybind() {
            try {
                if (moduleType == ModuleType.METEOR && meteorModule != null && meteorModule.keybind != null) {
                    return getMeteorKeybind();
                } else if (moduleType == ModuleType.RUSHER && rusherModule != null) {
                    return getRusherKeybind();
                }
            } catch (Exception ignored) {
            }
            return "unbound";
        }

        private String getMeteorKeybind() {
            try {
                var keybind = meteorModule.keybind;
                Method[] methods = keybind.getClass().getMethods();
                for (Method method : methods) {
                    if ((method.getName().equals("getKey") || method.getName().equals("getValue") ||
                            method.getName().equals("getGlfwKey")) && method.getParameterCount() == 0 &&
                            method.getReturnType() == int.class) {
                        int glfwKey = (int) method.invoke(keybind);
                        return formatGLFWKey(glfwKey);
                    }
                }

                Field keyField = keybind.getClass().getDeclaredField("key");
                keyField.setAccessible(true);
                int glfwKey = keyField.getInt(keybind);
                return formatGLFWKey(glfwKey);
            } catch (Exception e) {
                String rawKeybind = meteorModule.keybind.toString();
                return formatKeybind(rawKeybind);
            }
        }

        private String getRusherKeybind() {
            try {
                final IKey key = RusherHackAPI.getBindManager().getBindRegistry().get(rusherModule);
                if (key != null) {
                    String rawLabel = key.getLabel(true);
                    return formatRusherKeybind(rawLabel);
                }
            } catch (Exception ignored) {
            }
            return "unbound";
        }

        private String formatRusherKeybind(String keybind) {
            if (keybind == null || keybind.isEmpty()) return "unbound";

            String trimmed = keybind.trim();
            if (trimmed.equalsIgnoreCase("Left Super") || trimmed.equalsIgnoreCase("LEFTSUPER") ||
                    trimmed.contains("Left Super")) {
                return "Win";
            }

            String mapped = RUSHER_KEY_MAP.get(trimmed);
            if (mapped != null) return mapped;

            if (trimmed.startsWith("KEY_")) {
                String withoutPrefix = trimmed.substring(4);
                if (withoutPrefix.matches("^[0-9A-Z]$")) return withoutPrefix;
                return formatKeyName(withoutPrefix);
            }

            return trimmed;
        }

        private String formatKeyName(String keyName) {
            if (keyName == null || keyName.isEmpty()) return keyName;
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

        private String formatGLFWKey(int glfwKey) {
            String mapped = GLFW_KEY_MAP.get(glfwKey);
            if (mapped != null) return mapped;

            try {
                String keyName = org.lwjgl.glfw.GLFW.glfwGetKeyName(glfwKey, 0);
                if (keyName != null && !keyName.isEmpty()) {
                    return keyName.toUpperCase();
                }
            } catch (Exception ignored) {
            }
            return "KEY_" + glfwKey;
        }

        private String formatKeybind(String keybind) {
            if (keybind == null) return "unbound";

            Map<String, String> specialCases = new HashMap<>() {{
                put("KEY_0", "Mouse Left");
                put("KEY_1", "Mouse Right");
                put("KEY_2", "Middle");
                put("KEY_3", "Side Down");
                put("KEY_4", "Side Up");
                put("LEFT_SUPER", "Win");
                put("`", "Grave");
                put("LEFT_ALT", "Left Alt");
                put("RIGHT_ALT", "Right Alt");
                put("LEFT_CONTROL", "Left Ctrl");
                put("RIGHT_CONTROL", "Right Ctrl");
            }};

            String mapped = specialCases.get(keybind);
            if (mapped != null) return mapped;

            if (keybind.startsWith("KEY_KP_")) {
                String suffix = keybind.substring(7);
                if (suffix.matches("\\d")) return "Num " + suffix;
                return switch (suffix) {
                    case "ADD" -> "Num +";
                    case "DECIMAL" -> "Num .";
                    case "DIVIDE" -> "Num /";
                    case "ENTER" -> "Num Enter";
                    case "EQUAL" -> "Num =";
                    case "MULTIPLY" -> "Num *";
                    case "SUBTRACT" -> "Num -";
                    default -> keybind;
                };
            }

            return keybind.startsWith("KEY_") ? keybind.substring(4) : keybind;
        }

        public String getKeybindRaw() {
            try {
                if (moduleType == ModuleType.METEOR && meteorModule != null && meteorModule.keybind != null) {
                    String rawKeybind = meteorModule.keybind.toString();
                    if (rawKeybind == null || rawKeybind.isEmpty() || rawKeybind.equals("unbound")) {
                        return "unbound";
                    }
                    rawKeybind = rawKeybind.replaceAll("[\\p{Cntrl}\\p{So}]", "");
                    if (rawKeybind.matches("^[A-Z]$")) {
                        return rawKeybind;
                    }
                    return rawKeybind;
                } else if (moduleType == ModuleType.RUSHER && rusherModule != null) {
                    final IKey key = RusherHackAPI.getBindManager().getBindRegistry().get(rusherModule);
                    if (key != null) {
                        String rawLabel = key.getLabel(true);
                        if (rawLabel == null || rawLabel.isEmpty() || rawLabel.equals("unbound")) {
                            return "unbound";
                        }
                        return rawLabel.replaceAll("[\\p{Cntrl}\\p{So}]", "");
                    }
                }
            } catch (Exception ignored) {
            }
            return "unbound";
        }

        public String getMetadata() {
            try {
                String moduleId = this.getId();
                String clientSpecificId = moduleId + "_" + (moduleType == ModuleType.METEOR ? "meteor" : "rusher");

                if (hiddenMetadataModules.contains(clientSpecificId) || hiddenMetadataModules.contains(moduleId)) {
                    return null;
                }

                if (moduleType == ModuleType.METEOR && meteorModule != null) {
                    return meteorModule.getInfoString();
                } else if (moduleType == ModuleType.RUSHER && rusherModule != null) {
                    String metadata = rusherModule.getMetadata();
                    return metadata.isEmpty() ? null : metadata;
                }
            } catch (Exception ignored) {
            }
            return null;
        }

        public boolean isVisible() {
            try {
                String moduleId = this.getId();
                String clientSpecificId = moduleId + "_" + (moduleType == ModuleType.METEOR ? "meteor" : "rusher");

                if (hiddenModules.contains(clientSpecificId)) return false;
                if (hiddenModules.contains(moduleId)) return false;

                return moduleType != ModuleType.RUSHER || rusherModule == null || !rusherModule.isHidden();
            } catch (Exception e) {
                return true;
            }
        }

        public String getId() {
            try {
                String name = moduleType == ModuleType.METEOR ?
                        (meteorModule != null ? meteorModule.name : "") :
                        (rusherModule != null ? rusherModule.getName() : "");
                return name.toLowerCase().replaceAll("[-\\s]", "");
            } catch (Exception e) {
                return "unknown";
            }
        }

        public boolean equals(ModuleHolder moduleHolder) {
            if (moduleHolder == null) return false;
            if (this == moduleHolder) return true;
            try {
                if (moduleType != moduleHolder.moduleType) return false;
                return moduleType == ModuleType.METEOR ?
                        (meteorModule == moduleHolder.meteorModule) :
                        (rusherModule == moduleHolder.rusherModule);
            } catch (Exception e) {
                return false;
            }
        }

        @Override
        public int hashCode() {
            try {
                return moduleType == ModuleType.METEOR ?
                        (meteorModule != null ? meteorModule.hashCode() : 0) :
                        (rusherModule != null ? rusherModule.hashCode() : 0);
            } catch (Exception e) {
                return 0;
            }
        }
    }

    class BindListItem extends ListItem {
        public ModuleHolder module;

        public BindListItem(ModuleHolder module, ListHudElement parent) {
            super(parent);
            this.module = module;
        }

        public Component getText() {
            try {
                String name = formatName(module.getName());
                MutableComponent component;
                int nameColor;

                if (boundAsUnbound.getValue()) {
                    nameColor = unboundColor.getValue().getRGB();
                } else {
                    nameColor = module.isEnabled() ? enabledColor.getValue().getRGB() : disabledColor.getValue().getRGB();
                }
                component = Component.literal(name).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(nameColor)));

                if (!hideBounded.getValue() && showKeys.getValue()) {
                    String keybind = stripMinecraftColors(rawKeys.getValue() ? module.getKeybindRaw() : module.getKeybind());
                    if (!rawKeys.getValue()) {
                        keybind = formatKeybindCase(keybind);
                    }
                    int bracketColor = useStateBrackets.getValue() ?
                            (module.isEnabled() ? enabledColor.getValue().getRGB() : disabledColor.getValue().getRGB()) :
                            bracketsColor.getValue().getRGB();
                    int keyColor = stateKeys.getValue() ?
                            (module.isEnabled() ? enabledColor.getValue().getRGB() : disabledColor.getValue().getRGB()) :
                            keybindsColor.getValue().getRGB();
                    if (keyBStyle.getValue() != KeyBracketsStyle.None) {
                        String leftBracket = switch (keyBStyle.getValue()) {
                            case Square -> "[";
                            case Round -> "(";
                            case Curly -> "{";
                            case Angle -> "<";
                            case Pipe -> "|";
                            default -> "";
                        };
                        String rightBracket = switch (keyBStyle.getValue()) {
                            case Square -> "]";
                            case Round -> ")";
                            case Curly -> "}";
                            case Angle -> ">";
                            case Pipe -> "|";
                            default -> "";
                        };
                        component.append(Component.literal(" " + leftBracket).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(bracketColor))));
                        component.append(Component.literal(keybind).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(keyColor))));
                        component.append(Component.literal(rightBracket).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(bracketColor))));
                    } else {
                        component.append(Component.literal(" ").withStyle(Style.EMPTY));
                        component.append(Component.literal(keybind).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(keyColor))));
                    }
                }

                if (boundedMeta.getValue() && module.isEnabled()) {
                    String moduleMetadata = module.getMetadata();
                    if (moduleMetadata != null && !moduleMetadata.trim().isEmpty()) {
                        String cleanMetadata = stripMinecraftColors(moduleMetadata);
                        cleanMetadata = formatMetadataCase(cleanMetadata);
                        int metaColor = stateBMeta.getValue() ?
                                (module.isEnabled() ? enabledColor.getValue().getRGB() : disabledColor.getValue().getRGB()) :
                                mBoundColor.getValue().getRGB();
                        int metaBracketColor = stateMBrackets.getValue() ?
                                (module.isEnabled() ? enabledColor.getValue().getRGB() : disabledColor.getValue().getRGB()) :
                                mBrackets.getValue().getRGB();
                        if (metaBStyle.getValue() != MetaBracketsStyle.None) {
                            String leftBracket = switch (metaBStyle.getValue()) {
                                case Round -> "(";
                                case Square -> "[";
                                case Curly -> "{";
                                case Angle -> "<";
                                case Pipe -> "|";
                                default -> "";
                            };
                            String rightBracket = switch (metaBStyle.getValue()) {
                                case Round -> ")";
                                case Square -> "]";
                                case Curly -> "}";
                                case Angle -> ">";
                                case Pipe -> "|";
                                default -> "";
                            };
                            component.append(Component.literal(" " + leftBracket).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(metaBracketColor))));
                            component.append(Component.literal(cleanMetadata).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(metaColor))));
                            component.append(Component.literal(rightBracket).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(metaBracketColor))));
                        } else {
                            component.append(Component.literal(" ").withStyle(Style.EMPTY));
                            component.append(Component.literal(cleanMetadata).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(metaColor))));
                        }
                    }
                }

                return component;
            } catch (Exception e) {
                return Component.literal("Error").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(Color.RED.getRGB())));
            }
        }

        public boolean shouldRemove() {
            return !module.hasKeybind() || !module.isVisible() || hideBounded.getValue();
        }
    }

    class ExtraListItem extends ListItem {
        public ModuleHolder module;

        public ExtraListItem(ModuleHolder module, ListHudElement parent) {
            super(parent);
            this.module = module;
        }

        public Component getText() {
            try {
                String name = formatName(module.getName());
                MutableComponent component = Component.literal(name).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(unboundColor.getValue().getRGB())));

                if (unboundMeta.getValue()) {
                    String moduleMetadata = module.getMetadata();
                    if (moduleMetadata != null && !moduleMetadata.trim().isEmpty()) {
                        String cleanMetadata = stripMinecraftColors(moduleMetadata);
                        cleanMetadata = formatMetadataCase(cleanMetadata);
                        int metaColor = mUnboundColor.getValue().getRGB();
                        int metaBracketColor = mUBrackets.getValue().getRGB();
                        if (metaBStyle.getValue() != MetaBracketsStyle.None) {
                            String leftBracket = switch (metaBStyle.getValue()) {
                                case Round -> "(";
                                case Square -> "[";
                                case Curly -> "{";
                                case Angle -> "<";
                                case Pipe -> "|";
                                default -> "";
                            };
                            String rightBracket = switch (metaBStyle.getValue()) {
                                case Round -> ")";
                                case Square -> "]";
                                case Curly -> "}";
                                case Angle -> ">";
                                case Pipe -> "|";
                                default -> "";
                            };
                            component.append(Component.literal(" " + leftBracket).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(metaBracketColor))));
                            component.append(Component.literal(cleanMetadata).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(metaColor))));
                            component.append(Component.literal(rightBracket).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(metaBracketColor))));
                        } else {
                            component.append(Component.literal(" ").withStyle(Style.EMPTY));
                            component.append(Component.literal(cleanMetadata).withStyle(Style.EMPTY.withColor(TextColor.fromRgb(metaColor))));
                        }
                    }
                }

                return component;
            } catch (Exception e) {
                return Component.literal("Error").withStyle(Style.EMPTY.withColor(TextColor.fromRgb(Color.RED.getRGB())));
            }
        }

        public boolean shouldRemove() {
            return module.hasKeybind() || !module.isEnabled() || !module.isVisible() || !activeUnbound.getValue();
        }
    }

    private String formatName(String name) {
        if (name == null) return "Unknown";
        return switch (caseSetting.getValue()) {
            case Uppercase -> name.toUpperCase();
            case Lowercase -> name.toLowerCase();
            default -> name;
        };
    }

    private String formatKeybindCase(String keybind) {
        if (keybind == null) return "unbound";
        return switch (keyCase.getValue()) {
            case Uppercase -> keybind.toUpperCase();
            case Lowercase -> keybind.toLowerCase();
            default -> keybind;
        };
    }

    private String formatMetadataCase(String metadata) {
        if (metadata == null) return null;
        return switch (metaCase.getValue()) {
            case Uppercase -> metadata.toUpperCase();
            case Lowercase -> metadata.toLowerCase();
            default -> metadata;
        };
    }
}