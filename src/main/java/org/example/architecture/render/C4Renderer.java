package org.example.architecture.render;

import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import org.example.architecture.design.ArchitectureModel;
import org.example.architecture.design.ArchitectureModel.Container;
import org.example.architecture.design.ArchitectureModel.Element;
import org.example.architecture.design.ArchitectureModel.Relationship;

/**
 * Renders C4 context and container diagrams as Mermaid flowcharts. Mermaid's own C4 syntax is still experimental, so
 * the C4 notation is shown in the labels instead.
 */
public final class C4Renderer {

	static final String SYSTEM = "system";

	private C4Renderer() {
	}

	/** The system as one box, with the people and external systems around it. */
	public static String context(ArchitectureModel model) {
		StringBuilder diagram = new StringBuilder("flowchart TB\n");
		people(diagram, model);
		diagram.append("    ").append(SYSTEM).append("[\"")
				.append(Mermaid.label(systemName(model), "[Software System]")).append("\"]\n");
		externals(diagram, model);
		Set<String> containers = new HashSet<>();
		model.containers().forEach(container -> containers.add(container.id()));
		Set<String> outside = outside(model);
		Set<String> edges = new LinkedHashSet<>();
		for (Relationship relationship : model.relationships()) {
			String from = containers.contains(relationship.from()) ? SYSTEM
					: outside.contains(relationship.from()) ? Mermaid.node(relationship.from()) : null;
			String to = containers.contains(relationship.to()) ? SYSTEM
					: outside.contains(relationship.to()) ? Mermaid.node(relationship.to()) : null;
			if (from == null || to == null || from.equals(to) || !edges.add(from + ">" + to)) {
				continue;
			}
			edge(diagram, from, to, relationship);
		}
		return diagram.toString();
	}

	/** The containers inside the system boundary, with the people and external systems that use them. */
	public static String containers(ArchitectureModel model) {
		StringBuilder diagram = new StringBuilder("flowchart TB\n");
		people(diagram, model);
		diagram.append("    subgraph ").append(SYSTEM).append(" [\"").append(Mermaid.label(systemName(model)))
				.append("\"]\n");
		Set<String> known = outside(model);
		for (Container container : model.containers()) {
			known.add(container.id());
			diagram.append("        ").append(Mermaid.node(container.id())).append("[\"")
					.append(Mermaid.label(container.name(), container.technology() == null ? "[Container]"
							: "[Container: " + container.technology() + "]", container.responsibility()))
					.append("\"]\n");
		}
		diagram.append("    end\n");
		externals(diagram, model);
		for (Relationship relationship : model.relationships()) {
			if (known.contains(relationship.from()) && known.contains(relationship.to())) {
				edge(diagram, Mermaid.node(relationship.from()), Mermaid.node(relationship.to()), relationship);
			}
		}
		return diagram.toString();
	}

	static String systemName(ArchitectureModel model) {
		String name = Text.line(model.systemName());
		return name.isEmpty() ? "System" : name;
	}

	private static void people(StringBuilder diagram, ArchitectureModel model) {
		for (Element actor : model.actors()) {
			diagram.append("    ").append(Mermaid.node(actor.id())).append("[\"")
					.append(Mermaid.label(actor.name(), "[Person]", actor.description())).append("\"]\n");
		}
	}

	private static void externals(StringBuilder diagram, ArchitectureModel model) {
		for (Element system : model.externalSystems()) {
			diagram.append("    ").append(Mermaid.node(system.id())).append("[\"")
					.append(Mermaid.label(system.name(), "[External System]", system.description())).append("\"]\n");
		}
	}

	private static Set<String> outside(ArchitectureModel model) {
		Set<String> ids = new HashSet<>();
		model.actors().forEach(actor -> ids.add(actor.id()));
		model.externalSystems().forEach(system -> ids.add(system.id()));
		return ids;
	}

	private static void edge(StringBuilder diagram, String from, String to, Relationship relationship) {
		String label = Mermaid.label(relationship.description(),
				relationship.protocol() == null ? null : "[" + relationship.protocol() + "]");
		diagram.append("    ").append(from).append(" -->");
		if (!label.isEmpty()) {
			diagram.append("|\"").append(label).append("\"|");
		}
		diagram.append(" ").append(to).append("\n");
	}

}
