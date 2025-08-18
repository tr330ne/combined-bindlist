package treeone.combinedbindlist;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.rusherhack.client.api.RusherHackAPI;
import org.rusherhack.client.api.plugin.Plugin;

import java.io.*;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CombinedBindListPlugin extends Plugin {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final String CONFIG_FILE = RusherHackAPI.getConfigPath() + "/combinedbindlist.json";
    private static final String METADATA_CONFIG_FILE = RusherHackAPI.getConfigPath() + "/combinedbindlist_metadata.json";
    private static final Type LIST_TYPE = new TypeToken<ArrayList<String>>() {
    }.getType();

    @Override
    public void onLoad() {
        try {
            final CombinedBindListHudElement hudElement = new CombinedBindListHudElement();
            RusherHackAPI.getHudManager().registerFeature(hudElement);
            hudElement.load();

            RusherHackAPI.getCommandManager().registerFeature(new HideModuleCommand());
            RusherHackAPI.getCommandManager().registerFeature(new ListModulesCommand());
        } catch (Exception e) {
            getLogger().error("Failed to load CombinedBindList plugin", e);
        }
    }

    @Override
    public void onUnload() {
    }

    static void saveConfig(List<String> list) {
        saveToFile(CONFIG_FILE, list);
    }

    static List<String> loadConfig() {
        return loadFromFile(CONFIG_FILE);
    }

    static void saveMetadataConfig(List<String> list) {
        saveToFile(METADATA_CONFIG_FILE, list);
    }

    static List<String> loadMetadataConfig() {
        return loadFromFile(METADATA_CONFIG_FILE);
    }

    private static void saveToFile(String filePath, List<String> list) {
        try (Writer writer = new FileWriter(filePath)) {
            GSON.toJson(list, writer);
        } catch (IOException ignored) {
        }
    }

    private static List<String> loadFromFile(String filePath) {
        List<String> result = new ArrayList<>();
        try (Reader reader = new FileReader(filePath)) {
            List<String> loaded = GSON.fromJson(reader, LIST_TYPE);
            if (loaded != null) result.addAll(loaded);
        } catch (FileNotFoundException e) {
            saveToFile(filePath, new ArrayList<>());
        } catch (IOException ignored) {
        }
        return result;
    }
}