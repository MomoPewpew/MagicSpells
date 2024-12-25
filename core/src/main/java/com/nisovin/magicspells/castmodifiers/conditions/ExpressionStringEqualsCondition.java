package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.MagicSpells;
import com.nisovin.magicspells.castmodifiers.Condition;
import com.nisovin.magicspells.util.SpellData;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;

public class ExpressionStringEqualsCondition extends Condition {

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
	public boolean checkCaster(SpellData data) {
		return compare(data);
	}

	@Override
	public boolean checkTarget(SpellData data) {
		SpellData newData = data.builder().build().invert();
		return compare(newData);
	}

	@Override
	public boolean checkLocation(SpellData data) {
		return false;
	}

	private boolean compare(SpellData data) {
		String left = expressionLeft.get(data);
		String right = expressionRight.get(data);
		return left.equals(right);
	}
}
