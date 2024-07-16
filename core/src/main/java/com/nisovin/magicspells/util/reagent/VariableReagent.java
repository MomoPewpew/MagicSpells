package com.nisovin.magicspells.util.reagent;

import java.util.Map;
import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.managers.VariableManager;

public class VariableReagent extends Reagent {
    private Map<String, Double> variableMap;

    public VariableReagent() {
        this.variableMap = new HashMap<>();
    }

    public double get(String variableName) {
        return variableMap.getOrDefault(variableName, 0.0);
    }

    public void add(String variableName, double variableVal) {
        if (variableMap.containsKey(variableName)) {
            double currentVal = variableMap.get(variableName);
            variableMap.put(variableName, currentVal + variableVal);
        } else {
            variableMap.put(variableName, variableVal);
        }
    }

    public void set(String variableName, double variableVal) {
        variableMap.put(variableName, variableVal);
    }

    public void addValue(String variableName, double amount) {
        double currentVal = variableMap.getOrDefault(variableName, 0.0);
        variableMap.put(variableName, currentVal + amount);
    }

    public boolean isEmpty() {
        return variableMap.isEmpty();
    }

    @Override
    public boolean has(LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            if (!variableMap.isEmpty()) {
                VariableManager manager = MagicSpells.getVariableManager();
                if (manager == null) return false;
                for (Map.Entry<String, Double> var : variableMap.entrySet()) {
                    double val = var.getValue();
                    if (val > 0 && manager.getValue(var.getKey(), player) < val) return false;
                }
            }
        }
        return true;
    }

    @Override
    public void remove(LivingEntity livingEntity) {
        if (livingEntity instanceof Player player) {
            if (!variableMap.isEmpty()) {
                VariableManager manager = MagicSpells.getVariableManager();
                if (manager != null) {
                    for (Map.Entry<String, Double> var : variableMap.entrySet()) {
                        manager.set(var.getKey(), player, manager.getValue(var.getKey(), player) - var.getValue());
                    }
                }
            }
        }
    }

    @Override
    public void multiply(float multiplier) {
        for (String key : variableMap.keySet()) {
            double newVal = variableMap.get(key) * multiplier;
            variableMap.put(key, newVal);
        }
    }

    @Override
    public VariableReagent clone() {
        VariableReagent cloned = new VariableReagent();
        for (String key : variableMap.keySet()) {
            cloned.add(key, variableMap.get(key));
        }
        return cloned;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("VariableReagent{");
        for (String key : variableMap.keySet()) {
            sb.append(key).append("=").append(variableMap.get(key)).append(", ");
        }
        sb.delete(sb.length() - 2, sb.length()); // Remove last comma and space
        sb.append("}");
        return sb.toString();
    }
}
