package org.alexmond.gotmpl4j.exec;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shared, thread-safe index of a data class's reflective dispatch surface: its public
 * methods grouped by name, and its public (non-JDK) fields by name. Method/field access
 * then runs {@code getMethods()}/{@code getFields()} <em>once per class</em> and looks up
 * a tiny per-name list, instead of a fresh {@code Class[]} + linear scan on every access
 * (#117).
 *
 * <p>
 * Owned by the {@code GoTemplate} (or its {@code GoTemplateRegistry}) and shared across
 * every {@link Executor} it spawns and every render, mirroring the accessor cache — so it
 * warms once per data class and stays warm. Resolution is deterministic, so a benign
 * duplicate build under concurrent first-access is harmless; the built per-class indexes
 * are only read afterwards and are published through the {@link ConcurrentHashMap}.
 */
public final class DispatchCache {

	private final Map<Class<?>, Map<String, List<Method>>> methodsByClass = new ConcurrentHashMap<>();

	private final Map<Class<?>, Map<String, Field>> fieldsByClass = new ConcurrentHashMap<>();

	/**
	 * The public methods on {@code type} named {@code name}, in {@code getMethods()}
	 * order.
	 * @param type the receiver class
	 * @param name the method name
	 * @return the matching methods (empty if none)
	 */
	public List<Method> methodsNamed(Class<?> type, String name) {
		List<Method> named = this.methodsByClass.computeIfAbsent(type, DispatchCache::indexMethods).get(name);
		return (named != null) ? named : List.of();
	}

	/**
	 * The public, non-JDK-declared field named {@code name} on {@code type}.
	 * @param type the receiver class
	 * @param name the field name
	 * @return the field, or {@code null} if there is no such public field
	 */
	public Field publicField(Class<?> type, String name) {
		return this.fieldsByClass.computeIfAbsent(type, DispatchCache::indexFields).get(name);
	}

	private static Map<String, List<Method>> indexMethods(Class<?> type) {
		Map<String, List<Method>> index = new HashMap<>();
		for (Method method : type.getMethods()) {
			index.computeIfAbsent(method.getName(), (n) -> new ArrayList<>(2)).add(method);
		}
		return index;
	}

	private static Map<String, Field> indexFields(Class<?> type) {
		Map<String, Field> index = new HashMap<>();
		for (Field field : type.getFields()) {
			// Mirror readPublicField: ignore JDK-declared public fields so plain
			// collections are unaffected; first (most-derived) declaration wins.
			if (!field.getDeclaringClass().getName().startsWith("java.")) {
				index.putIfAbsent(field.getName(), field);
			}
		}
		return index;
	}

}
