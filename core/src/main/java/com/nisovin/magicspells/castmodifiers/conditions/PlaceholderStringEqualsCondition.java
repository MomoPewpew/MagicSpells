package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;

public class PlaceholderStringEqualsCondition extends Condition {

	private static final Pattern PLACEHOLDER_STRING_MATCHER = Pattern.compile("(%..*%|\".*\")([:=])(%..*%|\".*\")");

	private ConfigData<String> expressionLeft;
	private ConfigData<String> expressionRight;

	@Override
	public boolean initialize(String var) {
		Matcher matcher = PLACEHOLDER_STRING_MATCHER.matcher(var);
		List<String> split = new ArrayList<>();

		for (int i = 1; i <= matcher.groupCount(); i++) {
			if (matcher.matches() && matcher.group(i) != null) {
				String group = matcher.group(i);
				if (group.startsWith("\"") && group.endsWith("\"")) {
					split.add(group.substring(1, group.length() - 1));
				} else {
					split.add(group);
				}
			}
		}

		if (split.size() != 3) {
			MagicSpells.error("Invalid placeholderstringequals ConditionVar: " + var);
			return false;
		}

		expressionLeft = ConfigDataUtil.getString(split.get(0));
		expressionRight = ConfigDataUtil.getString(split.get(2));

		return (expressionLeft != null && expressionRight != null);
	}

	@Override
	public boolean check(LivingEntity caster) {
		return compare(caster);
	}

	@Override
	public boolean check(LivingEntity caster, LivingEntity target) {
		return compare(target);
	}

	@Override
	public boolean check(LivingEntity caster, Location location) {
		return compare(caster);
	}

	private boolean compare(LivingEntity target) {
		String left = expressionLeft.get(target, 0, new String[0]);
		String right = expressionRight.get(target, 0, new String[0]);

		return left.equals(right);
	}
}
