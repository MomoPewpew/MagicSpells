package com.nisovin.magicspells.util;

public record TargetInfo<T>(T target, SpellData spellData, boolean cancelled) {

	public boolean empty() {
		return target == null;
	}

	public boolean noTarget() {
		return cancelled || target == null;
	}

	public T getTarget() {
		return target;
	}

	public float getPower() {
		return spellData.power();
	}

}