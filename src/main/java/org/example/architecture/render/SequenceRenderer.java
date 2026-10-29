package org.example.architecture.render;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.example.architecture.design.ArchitectureModel;
import org.example.architecture.design.ArchitectureModel.Flow;
import org.example.architecture.design.ArchitectureModel.Step;

/** Renders one flow as a Mermaid sequence diagram. */
public final class SequenceRenderer {

	private SequenceRenderer() {
	}

	/**
	 * @return the diagram, or {@code null} when no step of the flow connects two known participants
	 */
	public static String render(ArchitectureModel model, Flow flow) {
		Map<String, String> names = new HashMap<>();
		Set<String> actors = new LinkedHashSet<>();
		model.actors().forEach(actor -> {
			names.put(actor.id(), actor.name());
			actors.add(actor.id());
		});
		model.externalSystems().forEach(system -> names.put(system.id(), system.name()));
		model.containers().forEach(container -> names.put(container.id(), container.name()));

		Set<String> participants = new LinkedHashSet<>();
		StringBuilder messages = new StringBuilder();
		for (Step step : flow.steps()) {
			if (!names.containsKey(step.from()) || !names.containsKey(step.to())) {
				continue;
			}
			participants.add(step.from());
			participants.add(step.to());
			String message = Mermaid.text(step.message());
			messages.append("    ").append(Mermaid.node(step.from())).append("->>").append(Mermaid.node(step.to()))
					.append(": ").append(message.isEmpty() ? "(no message)" : message).append("\n");
		}
		if (participants.isEmpty()) {
			return null;
		}
		StringBuilder diagram = new StringBuilder("sequenceDiagram\n");
		for (String id : participants) {
			diagram.append("    ").append(actors.contains(id) ? "actor " : "participant ").append(Mermaid.node(id))
					.append(" as ").append(Mermaid.text(names.get(id))).append("\n");
		}
		return diagram.append(messages).toString();
	}

}
