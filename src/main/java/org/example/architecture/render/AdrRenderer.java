package org.example.architecture.render;

import org.example.architecture.design.ArchitectureModel.Decision;

/** Renders a decision as a Markdown architecture decision record in Michael Nygard's format. */
public final class AdrRenderer {

	private AdrRenderer() {
	}

	public static String render(Decision decision) {
		StringBuilder adr = new StringBuilder("### ");
		String id = Text.line(decision.id());
		String title = Text.line(decision.title());
		adr.append(id.isEmpty() ? title : title.isEmpty() ? id : id + ": " + title).append("\n\n");
		adr.append("- **Status:** Proposed\n");
		adr.append("- **Requirements:** ")
				.append(decision.requirementIds().isEmpty() ? "None" : String.join(", ", decision.requirementIds()))
				.append("\n");
		paragraph(adr, "Context", decision.context());
		paragraph(adr, "Decision", decision.decision());
		if (!decision.alternatives().isEmpty()) {
			adr.append("\n**Alternatives considered:**\n\n");
			for (String alternative : decision.alternatives()) {
				adr.append("- ").append(Text.line(alternative)).append("\n");
			}
		}
		paragraph(adr, "Consequences", decision.consequences());
		return adr.toString();
	}

	private static void paragraph(StringBuilder adr, String heading, String text) {
		String line = Text.line(text);
		if (!line.isEmpty()) {
			adr.append("\n**").append(heading).append(":** ").append(line).append("\n");
		}
	}

}
