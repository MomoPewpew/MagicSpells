package com.nisovin.magicspells.util.magicitems;

import java.util.Map;
import java.util.Set;
import java.util.List;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Objects;
import java.util.Collection;

import com.google.common.collect.Multimap;

import net.kyori.adventure.text.Component;

import org.bukkit.*;
import org.bukkit.potion.PotionData;
import org.bukkit.attribute.Attribute;
import org.bukkit.potion.PotionEffect;
import org.bukkit.block.banner.Pattern;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.attribute.AttributeModifier;

import com.nisovin.magicspells.util.Util;
import com.nisovin.magicspells.util.TxtUtil;

public class MagicItemData {

    private final EnumMap<MagicItemAttribute, Object> itemAttributes = new EnumMap<>(MagicItemAttribute.class);
    private final EnumSet<MagicItemAttribute> blacklistedAttributes = EnumSet.noneOf(MagicItemAttribute.class);
    private final EnumSet<MagicItemAttribute> ignoredAttributes = EnumSet.noneOf(MagicItemAttribute.class);

    private boolean strictEnchantLevel = true;
    private boolean strictDurability = true;
    private boolean strictEnchants = true;

    public Object getAttribute(MagicItemAttribute attr) {
        return itemAttributes.get(attr);
    }

    public void setAttribute(MagicItemAttribute attr, Object obj) {
        if (obj == null) return;
        if (!attr.getDataType().isAssignableFrom(obj.getClass())) return;

        itemAttributes.put(attr, obj);
    }

    public void removeAttribute(MagicItemAttribute attr) {
        itemAttributes.remove(attr);
    }

    public boolean hasAttribute(MagicItemAttribute atr) {
        return itemAttributes.containsKey(atr);
    }

    public EnumSet<MagicItemAttribute> getBlacklistedAttributes() {
        return blacklistedAttributes;
    }

    public EnumSet<MagicItemAttribute> getIgnoredAttributes() {
        return ignoredAttributes;
    }

    public boolean isStrictEnchantLevel() {
        return strictEnchantLevel;
    }

    public void setStrictEnchantLevel(boolean strictEnchantLevel) {
        this.strictEnchantLevel = strictEnchantLevel;
    }

    public boolean isStrictDurability() {
        return strictDurability;
    }

    public void setStrictDurability(boolean strictDurability) {
        this.strictDurability = strictDurability;
    }

    public boolean isStrictEnchants() {
        return strictEnchants;
    }

    public void setStrictEnchants(boolean strictEnchants) {
        this.strictEnchants = strictEnchants;
    }

    private boolean hasEqualAttributes(MagicItemData other) {
        Multimap<Attribute, AttributeModifier> attrSelf = (Multimap<Attribute, AttributeModifier>) itemAttributes.get(MagicItemAttribute.ATTRIBUTES);
        Multimap<Attribute, AttributeModifier> attrOther = (Multimap<Attribute, AttributeModifier>) other.itemAttributes.get(MagicItemAttribute.ATTRIBUTES);

        Set<Attribute> keysSelf = attrSelf.keySet();
        Set<Attribute> keysOther = attrOther.keySet();
        if (!keysSelf.equals(keysOther)) return false;

        record AttributeModifierData(String name, double amt, AttributeModifier.Operation op, EquipmentSlot slot) {

            public AttributeModifierData(AttributeModifier mod) {
                this(mod.getName(), mod.getAmount(), mod.getOperation(), mod.getSlot());
            }

        }

        for (Attribute attr : keysSelf) {
            Collection<AttributeModifier> modsSelf = attrSelf.get(attr);
            Collection<AttributeModifier> modsOther = attrOther.get(attr);
            if (modsSelf.size() != modsOther.size()) return false;

            HashMap<AttributeModifierData, Integer> freq = new HashMap<>();
            for (AttributeModifier mod : modsSelf) {
                AttributeModifierData data = new AttributeModifierData(mod);
                Integer count = freq.get(data);

                if (count == null) count = 0;
                freq.put(data, count + 1);
            }

            for (AttributeModifier mod : modsOther) {
                AttributeModifierData data = new AttributeModifierData(mod);
                Integer count = freq.get(data);

                if (count == null) return false;
                if (count == 1) freq.remove(data);
                else freq.put(data, count - 1);
            }
        }

        return true;
    }

    public boolean matches(MagicItemData data) {
        if (this == data) return true;

        Set<MagicItemAttribute> keysSelf = itemAttributes.keySet();
        Set<MagicItemAttribute> keysOther = data.itemAttributes.keySet();

        for (MagicItemAttribute attr : keysSelf) {
            if (ignoredAttributes.contains(attr)) continue;
            if (!keysOther.contains(attr)) return false;
        }

        for (MagicItemAttribute attr : blacklistedAttributes) {
            if (keysOther.contains(attr)) return false;
        }

        for (MagicItemAttribute attr : keysSelf) {
            if (ignoredAttributes.contains(attr)) continue;

            switch (attr) {
                case ATTRIBUTES -> {
                    if (!hasEqualAttributes(data)) return false;
                }
                case DURABILITY -> {
                    Integer durabilitySelf = (Integer) itemAttributes.get(attr);
                    Integer durabilityOther = (Integer) data.itemAttributes.get(attr);

                    int compare = durabilitySelf.compareTo(durabilityOther);
                    if (strictDurability ? compare != 0 : compare < 0) return false;
                }
                case ENCHANTS -> {
                    if (strictEnchants && strictEnchantLevel) {
                        if (!itemAttributes.get(attr).equals(data.itemAttributes.get(attr)))
                            return false;

                        continue;
                    }

                    Map<Enchantment, Integer> enchantsSelf = (Map<Enchantment, Integer>) itemAttributes.get(attr);
                    Map<Enchantment, Integer> enchantsOther = (Map<Enchantment, Integer>) data.itemAttributes.get(attr);

                    if (strictEnchants && enchantsSelf.size() != enchantsOther.size()) return false;

                    for (Enchantment enchant : enchantsSelf.keySet()) {
                        if (!enchantsOther.containsKey(enchant)) return false;

                        Integer levelSelf = enchantsSelf.get(enchant);
                        Integer levelOther = enchantsOther.get(enchant);

                        int compare = levelSelf.compareTo(levelOther);
                        if (strictEnchantLevel ? compare != 0 : compare > 0) return false;
                    }
                }
                case NAME -> {
                    Component nameSelf = (Component) itemAttributes.get(attr);
                    Component nameOther = (Component) data.itemAttributes.get(attr);
                    return Util.getLegacyFromComponent(nameSelf).equals(Util.getLegacyFromComponent(nameOther));
                }
                case LORE -> {
                    List<Component> loreSelf = (List<Component>) itemAttributes.get(attr);
                    List<Component> loreOther = (List<Component>) data.itemAttributes.get(attr);
                    if (loreSelf.size() != loreOther.size()) return false;

                    for (int i = 0; i < loreSelf.size(); i++) {
                        String self = Util.getLegacyFromComponent(loreSelf.get(i));
                        String other = Util.getLegacyFromComponent(loreOther.get(i));
                        if (!self.equals(other)) return false;
                    }
                    return true;
                }
                default -> {
                    if (!itemAttributes.get(attr).equals(data.itemAttributes.get(attr))) return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MagicItemData other)) return false;
        return itemAttributes.equals(other.itemAttributes)
            && ignoredAttributes.equals(other.ignoredAttributes)
            && blacklistedAttributes.equals(other.blacklistedAttributes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(itemAttributes, ignoredAttributes, blacklistedAttributes);
    }

    @Override
    public MagicItemData clone() {
        MagicItemData data = new MagicItemData();

        if (!itemAttributes.isEmpty()) data.itemAttributes.putAll(itemAttributes);
        if (!ignoredAttributes.isEmpty()) data.ignoredAttributes.addAll(ignoredAttributes);
        if (!blacklistedAttributes.isEmpty()) data.blacklistedAttributes.addAll(blacklistedAttributes);

        return data;
    }

    public enum MagicItemAttribute {

        TYPE(Material.class),
        NAME(Component.class),
        AMOUNT(Integer.class),
        DURABILITY(Integer.class),
        REPAIR_COST(Integer.class),
        CUSTOM_MODEL_DATA(Integer.class),
        POWER(Integer.class),
        UNBREAKABLE(Boolean.class),
        HIDE_TOOLTIP(Boolean.class),
        FAKE_GLINT(Boolean.class),
        POTION_DATA(PotionData.class),
        COLOR(Color.class),
        FIREWORK_EFFECT(FireworkEffect.class),
        TITLE(String.class),
        AUTHOR(String.class),
        UUID(String.class),
        TEXTURE(String.class),
        SIGNATURE(String.class),
        SKULL_OWNER(String.class),
        ENCHANTS(Map.class),
        LORE(List.class),
        PAGES(List.class),
        POTION_EFFECTS(List.class),
        PATTERNS(List.class),
        FIREWORK_EFFECTS(List.class),
        ATTRIBUTES(Multimap.class);

        private final Class<?> dataType;
        private final String asString;

        MagicItemAttribute(Class<?> dataType) {
            this.dataType = dataType;
            asString = name().toLowerCase().replace('_', '-');
        }

        public Class<?> getDataType() {
            return dataType;
        }

        @Override
        public String toString() {
            return asString;
        }

    }

    @Override
    public String toString() {
        StringBuilder output = new StringBuilder();
        boolean previous = false;

        if (hasAttribute(MagicItemAttribute.TYPE))
            output.append(((Material) getAttribute(MagicItemAttribute.TYPE)).name());

        if (hasAttribute(MagicItemAttribute.NAME)) {
            output.append('{');

            output
                .append("\"name\":\"")
                .append(TxtUtil.escapeJSON(Util.getStringFromComponent((Component) getAttribute(MagicItemAttribute.NAME))))
                .append('"');

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.AMOUNT)) {
            if (previous) output.append(',');
            else output.append('{');

            output
                .append("\"amount\":")
                .append((int) getAttribute(MagicItemAttribute.AMOUNT));

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.DURABILITY)) {
            if (previous) output.append(',');
            else output.append('{');

            output
                .append("\"durability\":")
                .append((int) getAttribute(MagicItemAttribute.DURABILITY));

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.REPAIR_COST)) {
            if (previous) output.append(',');
            else output.append('{');

            output
                .append("\"repair-cost\":")
                .append((int) getAttribute(MagicItemAttribute.REPAIR_COST));

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.CUSTOM_MODEL_DATA)) {
            if (previous) output.append(',');
            else output.append('{');

            output
                .append("\"custom-model-data\":")
                .append((int) getAttribute(MagicItemAttribute.CUSTOM_MODEL_DATA));

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.POWER)) {
            if (previous) output.append(',');
            else output.append('{');

            output
                .append("\"power\":")
                .append((int) getAttribute(MagicItemAttribute.POWER));

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.UNBREAKABLE)) {
            if (previous) output.append(',');
            else output.append('{');

            output
                .append("\"unbreakable\":")
                .append((boolean) getAttribute(MagicItemAttribute.UNBREAKABLE));

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.HIDE_TOOLTIP)) {
            if (previous) output.append(',');
            else output.append('{');

            output
                .append("\"hide-tooltip\":")
                .append((boolean) getAttribute(MagicItemAttribute.HIDE_TOOLTIP));

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.COLOR)) {
            if (previous) output.append(',');
            else output.append('{');

            Color color = (Color) getAttribute(MagicItemAttribute.COLOR);
            String hex = String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());

            output
                .append("\"color\":\"")
                .append(hex)
                .append('"');

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.POTION_DATA)) {
            if (previous) output.append(',');
            else output.append('{');

            PotionData potionData = (PotionData) getAttribute(MagicItemAttribute.POTION_DATA);

            output
                .append("\"potion-data\":\"")
                .append(potionData.getType());

            if (potionData.isExtended()) output.append(" extended");
            else if (potionData.isUpgraded()) output.append(" upgraded");

            output.append('"');

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.FIREWORK_EFFECT)) {
            if (previous) output.append(',');
            else output.append('{');

            FireworkEffect effect = (FireworkEffect) getAttribute(MagicItemAttribute.FIREWORK_EFFECT);

            output
                .append("\"firework-effect\":\"")
                .append(effect.getType())
                .append(' ')
                .append(effect.hasTrail())
                .append(' ')
                .append(effect.hasFlicker());

            boolean previousColor = false;
            if (!effect.getColors().isEmpty()) {
                output.append(' ');
                for (Color color : effect.getColors()) {
                    if (previousColor) output.append(',');

                    String hex = String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
                    output.append(hex);

                    previousColor = true;
                }

                if (!effect.getFadeColors().isEmpty()) {
                    output.append(' ');
                    previousColor = false;
                    for (Color color : effect.getFadeColors()) {
                        if (previousColor) output.append(',');

                        String hex = String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
                        output.append(hex);

                        previousColor = true;
                    }
                }
            }

            output.append('"');
            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.SKULL_OWNER)) {
            if (previous) output.append(',');
            else output.append('{');

            output
                .append("\"skull-owner\":\"")
                .append((String) getAttribute(MagicItemAttribute.SKULL_OWNER))
                .append('"');

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.TITLE)) {
            if (previous) output.append(',');
            else output.append('{');

            output
                .append("\"title\":\"")
                .append(TxtUtil.escapeJSON((String) getAttribute(MagicItemAttribute.TITLE)))
                .append('"');

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.AUTHOR)) {
            if (previous) output.append(',');
            else output.append('{');

            output
                .append("\"author\":\"")
                .append(TxtUtil.escapeJSON((String) getAttribute(MagicItemAttribute.AUTHOR)))
                .append('"');

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.UUID)) {
            if (previous) output.append(',');
            else output.append('{');

            output
                .append("\"uuid\":\"")
                .append(((String) getAttribute(MagicItemAttribute.UUID)))
                .append('"');

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.TEXTURE)) {
            if (previous) output.append(',');
            else output.append('{');

            output
                .append("\"texture\":\"")
                .append(((String) getAttribute(MagicItemAttribute.TEXTURE)))
                .append('"');

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.SIGNATURE)) {
            if (previous) output.append(',');
            else output.append('{');

            output
                .append("\"signature\":\"")
                .append(((String) getAttribute(MagicItemAttribute.SIGNATURE)))
                .append('"');

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.ENCHANTS)) {
            if (previous) output.append(',');
            else output.append('{');

            Map<Enchantment, Integer> enchantments = (Map<Enchantment, Integer>) getAttribute(MagicItemAttribute.ENCHANTS);
            boolean previousEnchantment = false;
            output.append("\"enchants\":{");
            for (Enchantment enchantment : enchantments.keySet()) {
                if (previousEnchantment) output.append(',');

                output
                    .append('"')
                    .append(enchantment.getKey().getKey())
                    .append("\":")
                    .append(enchantments.get(enchantment));

                previousEnchantment = true;
            }
            output.append('}');
            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.FAKE_GLINT)) {
            if (previous) output.append(',');
            else output.append('{');

            output
                .append("\"fake-glint\":")
                .append((boolean) getAttribute(MagicItemAttribute.FAKE_GLINT));

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.ATTRIBUTES)) {
            if (previous) output.append(',');
            else output.append('{');

            Multimap<Attribute, AttributeModifier> attributes = (Multimap<Attribute, AttributeModifier>) getAttribute(MagicItemAttribute.ATTRIBUTES);
            boolean previousAttribute = false;
            output.append("\"attributes\":[");
            for (Map.Entry<Attribute, AttributeModifier> entries : attributes.entries()) {
                if (previousAttribute) output.append(',');

                AttributeModifier modifier = entries.getValue();

                output
                    .append('"')
                    .append(modifier.getName())
                    .append(' ')
                    .append(modifier.getAmount())
                    .append(' ')
                    .append(modifier.getOperation().name().toLowerCase());

                EquipmentSlot slot = modifier.getSlot();
                if (slot != null) {
                    output
                        .append(' ')
                        .append(slot.name().toLowerCase());
                }

                output.append('"');
                previousAttribute = true;
            }
            output.append(']');

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.LORE)) {
            if (previous) output.append(',');
            else output.append('{');

            List<Component> lore = (List<Component>) getAttribute(MagicItemAttribute.LORE);
            boolean previousLore = false;
            output.append("\"lore\":[");
            for (Component line : lore) {
                if (previousLore) output.append(',');

                output
                    .append('"')
                    .append(TxtUtil.escapeJSON(Util.getStringFromComponent(line)))
                    .append('"');

                previousLore = true;
            }
            output.append(']');

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.PAGES)) {
            if (previous) output.append(',');
            else output.append('{');

            List<Component> pages = (List<Component>) getAttribute(MagicItemAttribute.PAGES);
            boolean previousPages = false;
            output.append("\"pages\":[");
            for (Component page : pages) {
                if (previousPages) output.append(',');

                output
                    .append('"')
                    .append(TxtUtil.escapeJSON(Util.getStringFromComponent(page)))
                    .append('"');

                previousPages = true;
            }
            output.append(']');

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.PATTERNS)) {
            if (previous) output.append(',');
            else output.append('{');

            output.append("\"patterns\":[");
            List<Pattern> patterns = (List<Pattern>) getAttribute(MagicItemAttribute.PATTERNS);
            boolean previousPattern = false;
            for (Pattern pattern : patterns) {
                if (previousPattern) output.append(',');

                output
                    .append('"')
                    .append(pattern.getPattern().name())
                    .append(' ')
                    .append(pattern.getColor().name())
                    .append('"');

                previousPattern = true;
            }
            output.append(']');

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.POTION_EFFECTS)) {
            if (previous) output.append(',');
            else output.append('{');

            output.append("\"potion-effects\":[");
            List<PotionEffect> effects = (List<PotionEffect>) getAttribute(MagicItemAttribute.POTION_EFFECTS);
            boolean previousEffect = false;
            for (PotionEffect effect : effects) {
                if (previousEffect) output.append(',');

                output
                    .append('"')
                    .append(effect.getType().getName())
                    .append(' ')
                    .append(effect.getAmplifier())
                    .append(' ')
                    .append(effect.getDuration())
                    .append('"');

                previousEffect = true;
            }
            output.append(']');

            previous = true;
        }

        if (hasAttribute(MagicItemAttribute.FIREWORK_EFFECTS)) {
            List<FireworkEffect> effects = (List<FireworkEffect>) getAttribute(MagicItemAttribute.FIREWORK_EFFECTS);

            if (previous) output.append(',');
            else output.append('{');

            output.append("\"firework-effects\":[");
            boolean previousEffect = false;
            for (FireworkEffect effect : effects) {
                if (previousEffect) output.append(',');

                output
                    .append('"')
                    .append(effect.getType())
                    .append(' ')
                    .append(effect.hasTrail())
                    .append(' ')
                    .append(effect.hasFlicker());

                boolean previousColor = false;
                if (!effect.getColors().isEmpty()) {
                    output.append(' ');
                    for (Color color : effect.getColors()) {
                        if (previousColor) output.append(',');
                        String hex = String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
                        output.append(hex);
                        previousColor = true;
                    }

                    if (!effect.getFadeColors().isEmpty()) {
                        output.append(' ');
                        previousColor = false;
                        for (Color color : effect.getFadeColors()) {
                            if (previousColor) output.append(',');
                            String hex = String.format("%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
                            output.append(hex);
                            previousColor = true;
                        }
                    }
                }

                output.append('"');
                previousEffect = true;
            }

            output.append(']');
            previous = true;
        }

        if (!ignoredAttributes.isEmpty()) {
            if (previous) output.append(",");
            else output.append('{');

            output.append("\"ignored-attributes\":[");
            boolean previousAttribute = false;
            for (MagicItemAttribute attr : ignoredAttributes) {
                if (previousAttribute) output.append(',');

                output
                    .append('"')
                    .append(attr.name())
                    .append('"');

                previousAttribute = true;
            }

            output.append(']');
            previous = true;
        }

        if (!blacklistedAttributes.isEmpty()) {
            if (previous) output.append(",");
            else output.append('{');

            output.append("\"blacklisted-attributes\":[");
            boolean previousAttribute = false;
            for (MagicItemAttribute attr : blacklistedAttributes) {
                if (previousAttribute) output.append(',');

                output
                    .append('"')
                    .append(attr.name())
                    .append('"');

                previousAttribute = true;
            }

            output.append(']');
            previous = true;
        }

        if (!strictEnchants) {
            if (previous) output.append(",");
            else output.append('{');

            output.append("\"strict-enchants\": false");
            previous = true;
        }

        if (!strictDurability) {
            if (previous) output.append(',');
            else output.append('{');

            output.append("\"strict-durability\": false");
            previous = true;
        }

        if (!strictEnchantLevel) {
            if (previous) output.append(",");
            else output.append('{');

            output.append("\"strict-enchant-level\": false");
            previous = true;
        }

        if (previous) output.append('}');

        return output.toString();
    }

}
