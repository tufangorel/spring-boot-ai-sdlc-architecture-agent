package org.example.architecture.render;

final class Text {

	private Text() {
	}

	/** Collapses line breaks and repeated whitespace to single spaces; {@code null} becomes empty. */
	static String line(String text) {
		return text == null ? "" : text.replaceAll("\\s+", " ").trim();
	}

	/** A Markdown table cell: one line, with pipes escaped. */
	static String cell(String text) {
		return line(text).replace("|", "\\|");
	}

}
