package com.joshlong.mogul.utils;

import org.jspecify.annotations.NonNull;

import java.lang.reflect.Type;

/**
 * passing type names back and forth to the client requires that we have a single,
 * standard place in which to do the mapping
 */
public class TypeUtils {

	static public String typeName(@NonNull Type clazz) {
		return simpleName(clazz.getTypeName());
	}

	static public String typeName(@NonNull Class<?> clazz) {
		return simpleName(clazz.getSimpleName());
	}

	private static String simpleName(String sn) {
		return sn.substring(0, 1).toLowerCase() + sn.substring(1);
	}

}
