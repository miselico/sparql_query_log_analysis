package nl.cochez.query_processing.metadata;

import java.util.Collection;
import java.util.Map.Entry;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;

public class EquivalenceClasses {
	public static final String PROPERTY_GROUP = "property";
	public static final String ENTITY_GROUP = "entity";

	private static SetMultimap<String, String> EQUIVALENCES = HashMultimap.create();

	static {
		/*
		 * Warning: the group 'literal' is already used in other code. It is assumed
		 * that this does not occur here.
		 */
//		EQUIVALENCES.putAll(ENTITY_GROUP, Sets.newHashSet("wd", "wdv", "wdno", "wdref"));
//		EQUIVALENCES.putAll(PROPERTY_GROUP, Sets.newHashSet("p", "wdt", "ps"));
//		EQUIVALENCES.putAll("qualifierproperty", Sets.newHashSet("pq", "pr"));
//		EQUIVALENCES.putAll("statement", Sets.newHashSet("ps"));
//
//		EQUIVALENCES.putAll("qualifier_blank", Sets.newHashSet("wds"));
//
//		EQUIVALENCES.putAll("statement-value", Sets.newHashSet("psv", "psn"));
//		EQUIVALENCES.putAll("qualifier-value", Sets.newHashSet("pqv", "pqn", "prv", "prn"));

	}

	/**
	 * Gives a String which is an identifier for the equivalence class of the input.
	 * If the input is not in the known equivalence classes, it is assumed to be on
	 * its own.
	 * 
	 * @param originalPrefix
	 * @return
	 */
	public static String getEquivalentOrDefault(String originalPrefix, String theDefault) {
		for (Entry<String, Collection<String>> equivalenceClass : EQUIVALENCES.asMap().entrySet()) {
			if (equivalenceClass.getValue().contains(originalPrefix)) {
				return equivalenceClass.getKey();
			}
		}
		return theDefault;
	}

}
