package org.example.architecture.render;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds Mermaid ids and labels. Model text can contain anything, so it is flattened to one line and every character
 * Mermaid treats as syntax is replaced with an entity code.
 */
final class Mermaid {

	private Mermaid() {
	}

	/** A node id: letters and digits only, with a prefix so that it can never be a keyword such as {@code end}. */
	static String node(String id) {
		return "n" + (id == null ? "" : id.replaceAll("[^A-Za-z0-9]", ""));
	}

	/** Text for a quoted flowchart label, a sequence participant alias, or a sequence message. */
	static String text(String text) {
		StringBuilder escaped = new StringBuilder();
		for (char c : Text.line(text).toCharArray()) {
			escaped.append(switch (c) {
				case '#' -> "#35;";
				case '"' -> "#quot;";
				case ';' -> "#59;";
				case '<' -> "#lt;";
				case '>' -> "#gt;";
				case '`' -> "#96;";
				default -> String.valueOf(c);
			});
		}
		return escaped.toString();
	}

	/** Escapes each part and joins the non-empty ones with line breaks. */
	static String label(String... parts) {
		List<String> lines = new ArrayList<>();
		for (String part : parts) {
			String line = text(part);
			if (!line.isEmpty()) {
				lines.add(line);
			}
		}
		return String.join("<br/>", lines);
	}

	/** An identifier for an ER diagram entity, attribute name, or attribute type. */
	static String identifier(String text, String fallback) {
		String identifier = Text.line(text).replaceAll("[^A-Za-z0-9]+", "_").replaceAll("^_+|_+$", "");
		if (identifier.isEmpty()) {
			return fallback;
		}
		return Character.isLetter(identifier.charAt(0)) ? identifier : fallback + "_" + identifier;
	}

}
