package org.example.architecture.render;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.architecture.design.ArchitectureGaps;
import org.example.architecture.design.ArchitectureModel;
import org.example.architecture.design.ArchitectureModel.Container;
import org.example.architecture.design.ArchitectureModel.Decision;
import org.example.architecture.design.ArchitectureModel.Flow;
import org.example.architecture.design.ArchitectureModel.Gaps;

/** Assembles every artifact into one Markdown document. Sections with nothing to show are left out. */
public final class ArchitectureMarkdown {

	private ArchitectureMarkdown() {
	}

	/**
	 * @param model a model as returned by {@code normalized()}; gaps are recomputed from it, so edits are reflected
	 */
	public static String render(ArchitectureModel model) {
		StringBuilder markdown = new StringBuilder("# Architecture: ").append(C4Renderer.systemName(model)).append("\n");
		if (!Text.line(model.summary()).isEmpty()) {
			markdown.append("\n").append(Text.line(model.summary())).append("\n");
		}

		markdown.append("\n## System context\n\n");
		mermaid(markdown, C4Renderer.context(model));

		markdown.append("\n## Containers\n\n");
		mermaid(markdown, C4Renderer.containers(model));
		markdown.append("\n| Id | Container | Technology | Responsibility | Requirements |\n");
		markdown.append("|----|-----------|------------|----------------|--------------|\n");
		for (Container container : model.containers()) {
			markdown.append("| ").append(Text.cell(container.id())).append(" | ").append(Text.cell(container.name()))
					.append(" | ").append(Text.cell(container.technology())).append(" | ")
					.append(Text.cell(container.responsibility())).append(" | ")
					.append(Text.cell(String.join(", ", container.requirementIds()))).append(" |\n");
		}

		StringBuilder flows = new StringBuilder();
		for (Flow flow : model.flows()) {
			String diagram = SequenceRenderer.render(model, flow);
			if (diagram != null) {
				String title = Text.line(flow.title());
				flows.append("\n### ").append(Text.line(flow.storyId())).append(title.isEmpty() ? "" : ": " + title)
						.append("\n\n");
				mermaid(flows, diagram);
			}
		}
		if (!flows.isEmpty()) {
			markdown.append("\n## Key flows\n").append(flows);
		}

		if (!model.entities().isEmpty()) {
			markdown.append("\n## Domain model\n\n");
			mermaid(markdown, DomainRenderer.render(model));
		}

		if (!model.deploymentNodes().isEmpty()) {
			String target = Text.line(model.deploymentTarget());
			markdown.append("\n## Deployment").append(target.isEmpty() ? "" : ": " + target).append("\n\n");
			mermaid(markdown, DeploymentRenderer.render(model));
		}

		if (!model.decisions().isEmpty()) {
			markdown.append("\n## Architecture decisions\n");
			for (Decision decision : model.decisions()) {
				markdown.append("\n").append(AdrRenderer.render(decision));
			}
		}

		if (!model.nonFunctionalRequirements().isEmpty()) {
			markdown.append("\n## NFR traceability\n\n").append(NfrMatrixRenderer.render(model));
		}

		gaps(markdown, model);
		list(markdown, "Assumptions", model.assumptions());
		list(markdown, "Open questions", model.openQuestions());
		list(markdown, "Warnings", model.warnings());
		return markdown.toString();
	}

	private static void gaps(StringBuilder markdown, ArchitectureModel model) {
		Gaps gaps = ArchitectureGaps.compute(model);
		Map<String, String> names = new HashMap<>();
		model.containers().forEach(container -> names.put(container.id(), container.name()));
		StringBuilder lines = new StringBuilder();
		gap(lines, "Containers not traced to a requirement", gaps.untracedContainers(), names);
		gap(lines, "NFRs without a tactic", gaps.unaddressedNfrs(), Map.of());
		gap(lines, "Stories without a flow", gaps.storiesWithoutFlow(), Map.of());
		gap(lines, "Containers not deployed", gaps.unhostedContainers(), names);
		if (!lines.isEmpty()) {
			markdown.append("\n## Gaps\n\n").append(lines);
		}
	}

	private static void gap(StringBuilder lines, String label, List<String> ids, Map<String, String> names) {
		if (ids.isEmpty()) {
			return;
		}
		List<String> items = ids.stream()
				.map(id -> names.containsKey(id) ? id + " (" + Text.line(names.get(id)) + ")" : id)
				.toList();
		lines.append("- **").append(label).append(":** ").append(String.join(", ", items)).append("\n");
	}

	private static void list(StringBuilder markdown, String heading, List<String> items) {
		if (items.isEmpty()) {
			return;
		}
		markdown.append("\n## ").append(heading).append("\n\n");
		for (String item : items) {
			markdown.append("- ").append(Text.line(item)).append("\n");
		}
	}

	private static void mermaid(StringBuilder markdown, String diagram) {
		markdown.append("```mermaid\n").append(diagram).append("```\n");
	}

}
