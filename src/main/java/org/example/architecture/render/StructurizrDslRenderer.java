package org.example.architecture.render;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.architecture.design.ArchitectureModel;
import org.example.architecture.design.ArchitectureModel.Container;
import org.example.architecture.design.ArchitectureModel.DeploymentNode;
import org.example.architecture.design.ArchitectureModel.Element;
import org.example.architecture.design.ArchitectureModel.Flow;
import org.example.architecture.design.ArchitectureModel.Relationship;
import org.example.architecture.design.ArchitectureModel.Step;

/** Renders an architecture model as a Structurizr DSL workspace. */
public final class StructurizrDslRenderer {

	static final String SYSTEM = "mainSystem";

	StructurizrDslRenderer() {
	}

	public static String render(ArchitectureModel model) {
		StringBuilder dsl = new StringBuilder();
		String systemName = Text.line(model.systemName());
		if (systemName.isEmpty()) {
			systemName = "System";
		}
		String summary = Text.line(model.summary());

		dsl.append("workspace \"").append(escape(systemName)).append("\"");
		if (!summary.isEmpty()) {
			dsl.append(" \"").append(escape(summary)).append("\"");
		}
		dsl.append(" {\n\n");

		Map<String, String> elementIds = new HashMap<>();
		model(dsl, model, systemName, summary, elementIds);
		views(dsl, model, elementIds);

		dsl.append("}\n");
		return dsl.toString();
	}

	private static void model(StringBuilder dsl, ArchitectureModel model, String systemName, String summary,
			Map<String, String> elementIds) {
		dsl.append("    model {\n");

		for (Element actor : model.actors()) {
			String id = id("actor", actor.id());
			elementIds.put(actor.id(), id);
			dsl.append("        ").append(id).append(" = person \"").append(escape(actor.name())).append("\"");
			String desc = Text.line(actor.description());
			if (!desc.isEmpty()) {
				dsl.append(" \"").append(escape(desc)).append("\"");
			}
			dsl.append("\n");
		}

		for (Element ext : model.externalSystems()) {
			String id = id("ext", ext.id());
			elementIds.put(ext.id(), id);
			dsl.append("        ").append(id).append(" = softwareSystem \"").append(escape(ext.name())).append("\"");
			String desc = Text.line(ext.description());
			dsl.append(" \"").append(escape(desc)).append("\" \"External\"\n");
		}

		dsl.append("        ").append(SYSTEM).append(" = softwareSystem \"").append(escape(systemName)).append("\"");
		if (!summary.isEmpty()) {
			dsl.append(" \"").append(escape(summary)).append("\"");
		}
		dsl.append(" {\n");

		for (Container container : model.containers()) {
			String id = id("c", container.id());
			elementIds.put(container.id(), id);
			dsl.append("            ").append(id).append(" = container \"").append(escape(container.name())).append("\"");
			String resp = Text.line(container.responsibility());
			String tech = Text.line(container.technology());
			boolean isDb = isDatabase(container);
			if (!resp.isEmpty() || !tech.isEmpty() || isDb) {
				dsl.append(" \"").append(escape(resp)).append("\"");
			}
			if (!tech.isEmpty() || isDb) {
				dsl.append(" \"").append(escape(tech)).append("\"");
			}
			if (isDb) {
				dsl.append(" \"Database\"");
			}
			dsl.append("\n");
		}

		dsl.append("        }\n");

		for (Relationship rel : model.relationships()) {
			String from = elementIds.get(rel.from());
			String to = elementIds.get(rel.to());
			if (from == null || to == null) {
				continue;
			}
			dsl.append("        ").append(from).append(" -> ").append(to);
			String desc = Text.line(rel.description());
			String proto = Text.line(rel.protocol());
			dsl.append(" \"").append(escape(desc)).append("\"");
			if (!proto.isEmpty()) {
				dsl.append(" \"").append(escape(proto)).append("\"");
			}
			dsl.append("\n");
		}

		if (!model.deploymentNodes().isEmpty()) {
			String target = Text.line(model.deploymentTarget());
			if (target.isEmpty()) {
				target = "Production";
			}
			dsl.append("\n        deploymentEnvironment \"").append(escape(target)).append("\" {\n");
			for (DeploymentNode node : model.deploymentNodes()) {
				String nodeId = id("node", node.id());
				dsl.append("            ").append(nodeId).append(" = deploymentNode \"")
						.append(escape(node.name())).append("\"");
				String tech = Text.line(node.technology());
				if (!tech.isEmpty()) {
					dsl.append(" \"\" \"").append(escape(tech)).append("\"");
				}
				dsl.append(" {\n");
				for (String cid : node.containerIds()) {
					String containerRef = elementIds.get(cid);
					if (containerRef != null) {
						dsl.append("                containerInstance ").append(containerRef).append("\n");
					}
				}
				dsl.append("            }\n");
			}
			dsl.append("        }\n");
		}

		dsl.append("    }\n\n");
	}

	private static void views(StringBuilder dsl, ArchitectureModel model, Map<String, String> elementIds) {
		dsl.append("    views {\n");
		dsl.append("        systemContext ").append(SYSTEM).append(" \"SystemContext\" {\n");
		dsl.append("            include *\n");
		dsl.append("            autoLayout\n");
		dsl.append("        }\n");

		dsl.append("\n        container ").append(SYSTEM).append(" \"Containers\" {\n");
		dsl.append("            include *\n");
		dsl.append("            autoLayout\n");
		dsl.append("        }\n");

		if (!model.deploymentNodes().isEmpty()) {
			String target = Text.line(model.deploymentTarget());
			if (target.isEmpty()) {
				target = "Production";
			}
			dsl.append("\n        deployment ").append(SYSTEM).append(" \"").append(escape(target))
					.append("\" \"Deployment\" {\n");
			dsl.append("            include *\n");
			dsl.append("            autoLayout\n");
			dsl.append("        }\n");
		}

		for (Flow flow : model.flows()) {
			boolean hasValidSteps = flow.steps().stream().anyMatch(
					s -> elementIds.containsKey(s.from()) && elementIds.containsKey(s.to()));
			if (!hasValidSteps) {
				continue;
			}
			String flowId = id("flow", flow.storyId());
			String title = Text.line(flow.title());
			dsl.append("\n        dynamic ").append(SYSTEM).append(" \"").append(flowId).append("\"");
			if (!title.isEmpty()) {
				dsl.append(" \"").append(escape(title)).append("\"");
			}
			dsl.append(" {\n");
			for (Step step : flow.steps()) {
				String from = elementIds.get(step.from());
				String to = elementIds.get(step.to());
				if (from == null || to == null) {
					continue;
				}
				String msg = Text.line(step.message());
				dsl.append("            ").append(from).append(" -> ").append(to).append(" \"")
						.append(escape(msg)).append("\"\n");
			}
			dsl.append("            autoLayout\n");
			dsl.append("        }\n");
		}

		dsl.append("\n        styles {\n");
		dsl.append("            element \"Person\" {\n");
		dsl.append("                shape Person\n");
		dsl.append("                background #08427b\n");
		dsl.append("                color #ffffff\n");
		dsl.append("            }\n");
		dsl.append("            element \"Software System\" {\n");
		dsl.append("                background #1168bd\n");
		dsl.append("                color #ffffff\n");
		dsl.append("            }\n");
		dsl.append("            element \"External\" {\n");
		dsl.append("                background #999999\n");
		dsl.append("                color #ffffff\n");
		dsl.append("            }\n");
		dsl.append("            element \"Container\" {\n");
		dsl.append("                background #438dd5\n");
		dsl.append("                color #ffffff\n");
		dsl.append("            }\n");
		dsl.append("            element \"Database\" {\n");
		dsl.append("                shape Cylinder\n");
		dsl.append("            }\n");
		dsl.append("        }\n");

		dsl.append("    }\n");
	}

	static String id(String prefix, String rawId) {
		String cleaned = rawId == null ? "" : rawId.replaceAll("[^A-Za-z0-9]", "");
		return prefix + "_" + (cleaned.isEmpty() ? "id" : cleaned);
	}

	private static final List<String> DB_MARKERS = List.of("database", "sql", "postgres", "mongo", "redis", "db");

	static boolean isDatabase(Container container) {
		String text = (Text.line(container.technology()) + " " + Text.line(container.name())).toLowerCase();
		for (String marker : DB_MARKERS) {
			if (text.contains(marker)) {
				return true;
			}
		}
		return false;
	}

	static String escape(String text) {
		return Text.line(text).replace("\\", "\\\\").replace("\"", "\\\"");
	}

}
