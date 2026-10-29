package org.example.architecture.render;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MermaidTests {

	@Test
	void buildsNodeIdsFromLettersAndDigits() {
		assertThat(Mermaid.node("C-1")).isEqualTo("nC1");
		assertThat(Mermaid.node("end")).isEqualTo("nend");
		assertThat(Mermaid.node(null)).isEqualTo("n");
	}

	@Test
	void escapesEveryCharacterMermaidTreatsAsSyntax() {
		assertThat(Mermaid.text("a #1 \"b\";\n<c>  `d`")).isEqualTo("a #35;1 #quot;b#quot;#59; #lt;c#gt; #96;d#96;");
		assertThat(Mermaid.text(null)).isEmpty();
	}

	@Test
	void joinsNonEmptyLabelPartsWithLineBreaks() {
		assertThat(Mermaid.label("API", null, " ", "[Container]")).isEqualTo("API<br/>[Container]");
		assertThat(Mermaid.label()).isEmpty();
	}

	@Test
	void buildsErIdentifiers() {
		assertThat(Mermaid.identifier("Order line", "Entity")).isEqualTo("Order_line");
		assertThat(Mermaid.identifier(" List<String> ", "string")).isEqualTo("List_String");
		assertThat(Mermaid.identifier("1st", "Entity")).isEqualTo("Entity_1st");
		assertThat(Mermaid.identifier("!!!", "Entity")).isEqualTo("Entity");
		assertThat(Mermaid.identifier(null, "string")).isEqualTo("string");
	}

	@Test
	void flattensTextAndEscapesTableCells() {
		assertThat(Text.line(" a\n  b ")).isEqualTo("a b");
		assertThat(Text.line(null)).isEmpty();
		assertThat(Text.cell("a | b\nc")).isEqualTo("a \\| b c");
	}

}
