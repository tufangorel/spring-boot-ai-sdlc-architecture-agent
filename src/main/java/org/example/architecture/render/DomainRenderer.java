package org.example.architecture.render;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.example.architecture.design.ArchitectureModel;
import org.example.architecture.design.ArchitectureModel.Attribute;
import org.example.architecture.design.ArchitectureModel.Entity;
import org.example.architecture.design.ArchitectureModel.EntityRelation;
import org.example.architecture.design.Cardinality;

/** Renders the entities and their relations as a Mermaid entity-relationship diagram. */
public final class DomainRenderer {

	private DomainRenderer() {
	}

	public static String render(ArchitectureModel model) {
		Map<String, String> identifiers = new HashMap<>();
		Set<String> used = new HashSet<>();
		StringBuilder diagram = new StringBuilder("erDiagram\n");
		for (Entity entity : model.entities()) {
			String identifier = Mermaid.identifier(entity.name(), "Entity");
			for (int suffix = 2; !used.add(identifier); suffix++) {
				identifier = Mermaid.identifier(entity.name(), "Entity") + "_" + suffix;
			}
			identifiers.put(entity.id(), identifier);
			diagram.append("    ").append(identifier);
			if (entity.attributes().isEmpty()) {
				diagram.append("\n");
				continue;
			}
			diagram.append(" {\n");
			for (Attribute attribute : entity.attributes()) {
				diagram.append("        ").append(Mermaid.identifier(attribute.type(), "string")).append(" ")
						.append(Mermaid.identifier(attribute.name(), "attribute")).append("\n");
			}
			diagram.append("    }\n");
		}
		for (EntityRelation relation : model.entityRelations()) {
			String from = identifiers.get(relation.from());
			String to = identifiers.get(relation.to());
			if (from == null || to == null) {
				continue;
			}
			diagram.append("    ").append(from).append(" ").append(symbol(relation.cardinality())).append(" ").append(to)
					.append(" : \"").append(Text.line(relation.label()).replace('"', '\'')).append("\"\n");
		}
		return diagram.toString();
	}

	private static String symbol(Cardinality cardinality) {
		if (cardinality == null) {
			return "}o--o{";
		}
		return switch (cardinality) {
			case ONE_TO_ONE -> "||--||";
			case ONE_TO_MANY -> "||--o{";
			case MANY_TO_ONE -> "}o--||";
			case MANY_TO_MANY -> "}o--o{";
		};
	}

}
