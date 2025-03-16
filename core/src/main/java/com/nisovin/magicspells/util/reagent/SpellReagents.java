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

        for (String costVal : costList) {
            try {
                String[] data = costVal.split(" ");
                Reagent reagent = switch (data[0].toLowerCase()) {
                    case "mana" -> new ManaReagent(Integer.parseInt(data[1]));
                    case "health" -> new HealthReagent(Double.parseDouble(data[1]));
                    case "hunger" -> new HungerReagent(Integer.parseInt(data[1]));
                    case "experience" -> new ExperienceReagent(Integer.parseInt(data[1]));
                    case "levels" -> new LevelReagent(Integer.parseInt(data[1]));
                    case "durability" -> new DurabilityReagent(Integer.parseInt(data[1]));
                    case "money" -> new MoneyReagent(Float.parseFloat(data[1]));
                    case "variable" -> {
                        VariableReagent varReagent = new VariableReagent();
                        varReagent.add(data[1], Double.parseDouble(data[2]));
                        yield varReagent;
                    }
                    default -> {
                        int quantity = 1;
                        if (data.length > 1) quantity = (int) Float.parseFloat(data[1]);

                        MagicItemData itemData = MagicItems.getMagicItemDataFromString(data[0]);
                        if (itemData == null) {
                            MagicSpells.error("Failed to process cost value for " + internalName + " spell: " + costVal);
                            yield null;
                        }
                        ItemReagent itemReagent = new ItemReagent();
                        itemReagent.add(itemData, quantity);
                        yield itemReagent;
                    }
                };

                if (reagent != null) {
                    if (reagent instanceof VariableReagent varReagent && !varReagent.isEmpty()) {
                        spellReagents.addReagent(varReagent);
                    } else if (reagent instanceof ItemReagent itemReagent && !itemReagent.isEmpty()) {
                        spellReagents.addReagent(itemReagent);
                    } else if (reagent.get().doubleValue() > 0) {
                        spellReagents.addReagent(reagent);
                    }
                }
            }
            catch (Exception e) {
                MagicSpells.error("Failed to process cost value for " + internalName + " spell: " + costVal);
            }
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

