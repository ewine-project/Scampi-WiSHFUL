package wishful_framework;

import android.os.Environment;
import android.util.Log;

import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.LinkedList;
import java.util.Map;

import de.tu_berlin.tkn.ewine.WishFulService;


public class Configuration {

    public static final String TAG = "YamlConfiguration";

    public WishFulAgent agent;
    public WishFulBroker broker;
    public LinkedList<WishFulController> controllerList;
    public LinkedList<WishFulModule> moduleList;

    public Configuration() {
        agent   = null;
        broker  = null;
        controllerList  = new LinkedList<WishFulController>();
        moduleList      = new LinkedList<WishFulModule>();
    }

    public void print() {
        if (agent != null) {
            agent.print("");
        }
        if (broker != null) {
            broker.print("");
        }
        for (WishFulController controller : controllerList) {
            controller.print("");
        }
        for (WishFulModule module : moduleList) {
            module.print("");
        }
    }

    public static String loadConfig(WishFulConfig config) {
        if (config == null) {
            Log.e(TAG, "error loading yaml, config null");
            return null;
        }
        return config.yamlConfigString;
    }

    public static String readFile(String fileName, WishFulConfig config) {

        //Yaml Init
        String yamlString = new String("");
        try {

            // -- /storage/sdcard/
            File sdcard = Environment.getExternalStorageDirectory();
            File file = new File(sdcard, fileName);

            Log.v(TAG, "yaml file "+file.getPath()+" "+file.toString());

            BufferedReader br = new BufferedReader(new FileReader(file));
            String line;
            while ((line = br.readLine()) != null) {
                //Log.v(TAG, "read "+line);
                yamlString += line+"\n";
            }
            br.close();
        } catch (FileNotFoundException e) {
            Log.e(TAG, "error reading yaml, file not found "+fileName);
            if (config != null) {
                return loadConfig(config);
            }
            return null;
        } catch (IOException e) {
            Log.e(TAG, "error reading yaml file "+e);
            return null;
        }
        return yamlString;
    }

    public static Configuration parse(String filename, WishFulConfig config, WishFulService wishFulService) {

        String yamlConfig = readFile(filename, config);
        if (yamlConfig == null) {
            return null;
        }

        Configuration configuration = new Configuration();
        Yaml yaml = new Yaml();
        Map<String,Object> yamlMap = (Map<String,Object>)yaml.load(yamlConfig);

        Log.v(TAG,"Map "+yamlMap.toString());
        for (String key : yamlMap.keySet()) {

            if (key.equals("agent_config")) {
                if ((configuration.agent = WishFulAgent.parse(yamlMap.get(key))) == null) {
                    return null;
                }

            } else if (key.equals("broker")) {
                if ((configuration.broker = WishFulBroker.parse(yamlMap.get(key))) == null) {
                    return null;
                }

            } else if (key.equals("controllers")) {
                if (!(yamlMap.get(key) instanceof Map)) {
                    Log.e(TAG, "controller key is not map");
                    return null;
                }

                Map<String,Object> controllerMap = (Map<String,Object>)yamlMap.get(key);
                for (String controllerKey : controllerMap.keySet()) {

                    Object obj = controllerMap.get(controllerKey);
                    if (!(obj instanceof Map)) {
                        Log.e(TAG, " obj not a map");
                        return null;
                    }

                    WishFulController controller = (WishFulController)WishFulEntity.getEntityObject(wishFulService,
                                                                                                    WishFulController.class.getSimpleName(),
                                                                                                    controllerKey,
                                                                                                    (Map<String,Object>)obj);
                    if (controller == null) {
                        Log.e(TAG, "error parsing controller "+controllerKey);
                        return null;
                    }
                    controller.parse((Map<String,Object>)obj);
                    configuration.controllerList.add(controller);
                }

            } else if (key.equals("modules")) {
                if (!(yamlMap.get(key) instanceof Map)) {
                    Log.e(TAG, "modules key is not map");
                    return null;
                }

                Map<String,Object> moduleMap = (Map<String,Object>)yamlMap.get(key);
                for (String moduleKey : moduleMap.keySet()) {

                    Object obj = moduleMap.get(moduleKey);
                    if (!(obj instanceof Map)) {
                        Log.e(TAG, " obj not a map");
                        return null;
                    }

                    WishFulModule module = (WishFulModule)WishFulEntity.getEntityObject(wishFulService,
                            WishFulModule.class.getSimpleName(),
                            moduleKey,
                            (Map<String,Object>)obj);

                    if (module == null) {
                        Log.e(TAG, "error parsing controller "+moduleKey);
                        return null;
                    }
                    module.parse((Map<String,Object>)obj);
                    configuration.moduleList.add(module);
                }

            } else {
                Log.e(TAG, "unknown yaml key "+key);
                return null;
            }
        }

        return configuration;
    }
}
