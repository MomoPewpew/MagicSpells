package com.nisovin.magicspells.castmodifiers.conditions;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.Location;
import org.bukkit.entity.LivingEntity;

import com.nisovin.magicspells.castmodifiers.conditions.util.OperatorCondition;
import com.nisovin.magicspells.util.config.ConfigData;
import com.nisovin.magicspells.util.config.ConfigDataUtil;

public class ExpressionCondition extends OperatorCondition {

	private static final Pattern EXPRESSION_MATCHER = Pattern.compile("(%..*%|[^<>:=]*)([<>:=])(%..*%|[^<>:=]*)");

	private ConfigData<Double> expressionLeft;
	private ConfigData<Double> expressionRight;

	@Override
	public boolean initialize(String var) {
		Matcher matcher = EXPRESSION_MATCHER.matcher(var);
		List<String> split = new ArrayList<>();

		for (int i = 1; i <= 3; i++) {
			if (matcher.matches() && matcher.group(i) != null) split.add(matcher.group(i));
		}

		if (split.size() != 3) {
			return false;
		}

		expressionLeft = ConfigDataUtil.getDouble(split.get(0));
		expressionRight = ConfigDataUtil.getDouble(split.get(2));

		return (super.initialize(split.get(1)) && expressionLeft != null && expressionRight != null);
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
		Double valueLeft = expressionLeft.get(target, 0, new String[0]);
		Double valueRight = expressionRight.get(target, 0, new String[0]);

		if (equals) return (valueLeft == valueRight);
		else if (moreThan) return (valueLeft > valueRight);
		else if (lessThan) return (valueLeft < valueRight);
		return false;
	}
}
