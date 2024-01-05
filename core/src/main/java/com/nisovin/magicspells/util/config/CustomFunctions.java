package com.nisovin.magicspells.util.config;

import java.util.Random;

import de.slikey.exp4j.function.Function;

public class CustomFunctions {

	private static final Function[] functions = {
			new Function("rand", 2) {
				private final Random random = new Random();

				@Override
				public double apply(double... args) {
					return random.nextDouble() * (args[1] - args[0]) + args[0];
				}
			},
			new Function("prob", 3) {
				private final Random random = new Random();

				@Override
				public double apply(double... args) {
					return random.nextDouble() < args[0] ? args[1] : args[2];
				}
			},
			new Function("min", 2) {
				@Override
				public double apply(double... args) {
					return Math.min(args[0], args[1]);
				}
			},
			new Function("max", 2) {
				@Override
				public double apply(double... args) {
					return Math.max(args[0], args[1]);
				}
			},
			new Function("select", 4) {
				@Override
				public double apply(double... args) {
					if (args[0] < 0) return args[1];
					else if (args[0] == 0) return args[2];
					return args[3];
				}
			}
	};

	public static Function[] getFunctions() {
		return functions;
	}

}
