package com.nisovin.magicspells.util.config;

import com.nisovin.magicspells.util.SpellData;

public interface ConfigData<T> {

	T get(SpellData data);

	default boolean isConstant() {
		return true;
	}

}
