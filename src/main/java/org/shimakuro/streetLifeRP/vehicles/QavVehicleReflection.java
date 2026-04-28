package org.shimakuro.streetLifeRP.vehicles;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.List;
import java.util.UUID;

final class QavVehicleReflection {
    private QavVehicleReflection() {}

    static Object vehicleEntityByEntity(Entity entity) {
        try {
            Class<?> api = Class.forName("me.zombie_striker.qav.api.QualityArmoryVehicles");
            Method m = api.getMethod("getVehicleEntityByEntity", Entity.class);
            return m.invoke(null, entity);
        } catch (Throwable ignored) {
            return null;
        }
    }

    static boolean isVehicleEntity(Entity entity) {
        try {
            Class<?> api = Class.forName("me.zombie_striker.qav.api.QualityArmoryVehicles");
            try {
                Method isVehicle = api.getMethod("isVehicle", Entity.class);
                Object out = isVehicle.invoke(null, entity);
                if (out instanceof Boolean b) return b;
            } catch (Throwable ignored) {
                // fallback below
            }
            return vehicleEntityByEntity(entity) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }

    static UUID vehicleUuid(Object vehicleEntity) {
        try {
            Method m = vehicleEntity.getClass().getMethod("getVehicleUUID");
            Object out = m.invoke(vehicleEntity);
            if (out instanceof UUID u) return u;
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    static double getHealth(Object vehicleEntity) {
        try {
            Method m = vehicleEntity.getClass().getMethod("getHealth");
            Object out = m.invoke(vehicleEntity);
            if (out instanceof Double d) return d;
            if (out instanceof Number n) return n.doubleValue();
            return 0.0;
        } catch (Throwable ignored) {
            return 0.0;
        }
    }

    static void setHealth(Object vehicleEntity, double value) {
        try {
            Method m = vehicleEntity.getClass().getMethod("setHealth", double.class);
            m.invoke(vehicleEntity, value);
        } catch (Throwable ignored) {
            // best effort
        }
    }

    static int getFuel(Object vehicleEntity) {
        try {
            Method m = vehicleEntity.getClass().getMethod("getFuel");
            Object out = m.invoke(vehicleEntity);
            if (out instanceof Integer i) return i;
            if (out instanceof Number n) return n.intValue();
            return 0;
        } catch (Throwable ignored) {
            return 0;
        }
    }

    static void setFuel(Object vehicleEntity, int value) {
        try {
            Method m = vehicleEntity.getClass().getMethod("setFuel", int.class);
            m.invoke(vehicleEntity, value);
        } catch (Throwable ignored) {
            // best effort
        }
    }

    static double maxHealth(Object vehicleEntity) {
        try {
            Method getType = vehicleEntity.getClass().getMethod("getType");
            Object type = getType.invoke(vehicleEntity);
            if (type == null) return 0.0;
            Method m = type.getClass().getMethod("getMaxHealth");
            Object out = m.invoke(type);
            if (out instanceof Double d) return d;
            if (out instanceof Number n) return n.doubleValue();
            return 0.0;
        } catch (Throwable ignored) {
            return 0.0;
        }
    }

    static List<?> getOwnedVehicles(UUID owner) {
        try {
            Class<?> api = Class.forName("me.zombie_striker.qav.api.QualityArmoryVehicles");
            Method m = api.getMethod("getOwnedVehicles", UUID.class);
            Object out = m.invoke(null, owner);
            if (out instanceof List<?> list) return list;
            return List.of();
        } catch (Throwable ignored) {
            return List.of();
        }
    }

    static List<?> getAllVehicles() {
        try {
            Class<?> main = Class.forName("me.zombie_striker.qav.Main");
            java.lang.reflect.Field vehiclesField = main.getField("vehicles");
            Object list = vehiclesField.get(null);
            if (list instanceof List<?> vehicles) return vehicles;
        } catch (Throwable ignored) {
            // best effort
        }
        return List.of();
    }

    static Object vehicleForPlayer(Player player) {
        Entity seat = player.getVehicle();
        if (seat == null) return null;
        return vehicleEntityByEntity(seat);
    }
}
