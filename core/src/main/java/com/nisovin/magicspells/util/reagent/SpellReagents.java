package com.nisovin.magicspells.util.reagent;

import java.util.List;
import java.util.ArrayList;

import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.util.magicitems.MagicItems;
import com.nisovin.magicspells.util.magicitems.MagicItemData;

public class SpellReagents {

    private List<Reagent> reagents;

    public SpellReagents() {
        reagents = new ArrayList<>();
    }

    public SpellReagents(SpellReagents other) {
        reagents = new ArrayList<>();
        for (Reagent reagent : other.reagents) {
            reagents.add(reagent.clone());
        }
    }

    public List<Reagent> getReagents() {
        return reagents;
    }

    public void setReagents(List<Reagent> newReagents) {
        if (newReagents == null || newReagents.isEmpty()) reagents = new ArrayList<>();
        else reagents = new ArrayList<>(newReagents);
    }

    public void addReagent(Reagent reagent) {
        reagents.add(reagent);
    }

    public static SpellReagents fromList(List<String> costList, String internalName) {

        SpellReagents spellReagents = new SpellReagents();
        if (costList == null || costList.isEmpty()) return spellReagents;

        ManaReagent manaReagent = new ManaReagent(0);
        HealthReagent healthReagent = new HealthReagent(0);
        HungerReagent hungerReagent = new HungerReagent(0);
        ExperienceReagent experienceReagent = new ExperienceReagent(0);
        LevelReagent levelReagent = new LevelReagent(0);
        DurabilityReagent durabilityReagent = new DurabilityReagent(0);
        MoneyReagent moneyReagent = new MoneyReagent(0);
        VariableReagent variableReagent = new VariableReagent();
        ItemReagent itemReagent = new ItemReagent();

        for (String costVal : costList) {
            try {
                String[] data = costVal.split(" ");
                switch (data[0].toLowerCase()) {
                    case "mana" -> manaReagent.add(Integer.parseInt(data[1]));
                    case "health" -> healthReagent.add(Double.parseDouble(data[1]));
                    case "hunger" -> hungerReagent.add(Integer.parseInt(data[1]));
                    case "experience" -> experienceReagent.add(Integer.parseInt(data[1]));
                    case "levels" -> levelReagent.add(Integer.parseInt(data[1]));
                    case "durability" -> durabilityReagent.add(Integer.parseInt(data[1]));
                    case "money" -> moneyReagent.add(Float.parseFloat(data[1]));
                    case "variable" -> variableReagent.add(data[1], Double.parseDouble(data[2]));
                    default -> {

                        int quantity = 1;
                        if (data.length > 1) quantity = (int) Float.parseFloat(data[1]);

                        MagicItemData itemData = MagicItems.getMagicItemDataFromString(data[0]);
                        if (itemData == null) {
                            MagicSpells.error("Failed to process cost value for " + internalName + " spell: " + costVal);
                            continue;
                        }
                        itemReagent.add(itemData, quantity);
                    }
                }
            }
            catch (Exception e) {
                MagicSpells.error("Failed to process cost value for " + internalName + " spell: " + costVal);
            }
        }

        if (manaReagent.get() > 0) {
            spellReagents.addReagent(manaReagent);
        }
        if (healthReagent.get() > 0) {
            spellReagents.addReagent(healthReagent);
        }
        if (hungerReagent.get() > 0) {
            spellReagents.addReagent(hungerReagent);
        }
        if (experienceReagent.get() > 0) {
            spellReagents.addReagent(experienceReagent);
        }
        if (levelReagent.get() > 0) {
            spellReagents.addReagent(levelReagent);
        }
        if (durabilityReagent.get() > 0) {
            spellReagents.addReagent(durabilityReagent);
        }
        if (moneyReagent.get() > 0) {
            spellReagents.addReagent(moneyReagent);
        }
        if (!variableReagent.isEmpty()) {
            spellReagents.addReagent(variableReagent);
        }
        if (!itemReagent.isEmpty()) {
            spellReagents.addReagent(itemReagent);
        }
        return spellReagents;
    }

    public boolean hasAll(LivingEntity livingEntity) {
        if (reagents != null && !reagents.isEmpty()) {
            for (Reagent reagent : reagents) {
                if (!reagent.has(livingEntity)) {
                    return false;
                }
            }
        }
        return true;
    }

    public void removeAll(LivingEntity livingEntity) {
        if (reagents != null && !reagents.isEmpty()) {
            for (Reagent reagent : reagents) {
                reagent.remove(livingEntity);
            }
        }
    }

    public SpellReagents multiply(float x) {
        SpellReagents other = new SpellReagents();
        if (reagents != null && !reagents.isEmpty()) {
            for (Reagent reagent : reagents) {
                Reagent multipliedReagent = reagent.clone();
                multipliedReagent.multiply(x);
                other.addReagent(multipliedReagent);
            }
        }
        return other;
    }

    @Override
    public SpellReagents clone() {
        return new SpellReagents(this);
    }


    @Override
    public String toString() {
        return "SpellReagents{" +
                "reagents=" + reagents +
                '}';
    }
}

