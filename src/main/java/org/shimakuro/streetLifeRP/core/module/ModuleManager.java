package org.shimakuro.streetLifeRP.core.module;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class ModuleManager {
    private final Logger logger;
    private final List<Module> modules = new ArrayList<>();
    private final List<Module> enabledModules = new ArrayList<>();

    public ModuleManager(Logger logger) {
        this.logger = logger;
    }

    public void register(Module module) {
        modules.add(module);
    }

    public List<Module> modules() {
        return Collections.unmodifiableList(modules);
    }

    public void enableAll() {
        for (Module module : modules) {
            try {
                module.enable();
                enabledModules.add(module);
                logger.info("[StreetLifeRP] Module enabled: " + module.name());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "[StreetLifeRP] Module failed: " + module.name(), e);
                disableAll();
                throw e;
            }
        }
    }

    public void disableAll() {
        for (int i = enabledModules.size() - 1; i >= 0; i--) {
            Module module = enabledModules.get(i);
            try {
                module.disable();
                logger.info("[StreetLifeRP] Module disabled: " + module.name());
            } catch (Exception e) {
                logger.log(Level.SEVERE, "[StreetLifeRP] Module disable failed: " + module.name(), e);
            }
        }
        enabledModules.clear();
    }
}

