package com.nisovin.magicspells.util.managers;

import com.nisovin.magicspells.MagicSpells;
import org.bukkit.Bukkit;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class OfflineVariableManager {

    public String getOfflineVariableString(String variableName, String uniqueId) {
        uniqueId = uniqueId.replace("-","");
        if (Bukkit.getPlayer(uniqueId) != null) return null;

        File folder = new File(MagicSpells.getInstance().getDataFolder(), "vars");
        File file = new File(folder, "PLAYER_" + uniqueId + ".txt");
        if (!file.exists()) return null;

        try {
            Scanner scanner = new Scanner(file, StandardCharsets.UTF_8);
            while (scanner.hasNext()) {
                String line = scanner.nextLine().trim();
                if (!line.isEmpty()) {
                    String[] s = line.split("=", 2);
                    if (s.length == 2 && s[0].equals(variableName)) {
                        scanner.close();
                        return s[1];
                    }
                }
            }
            scanner.close();

        } catch (Exception e) {
            MagicSpells.error("ERROR LOADING PLAYER VARIABLES FOR " + uniqueId);
            MagicSpells.handleException(e);
        }

        return null;
    }
}
