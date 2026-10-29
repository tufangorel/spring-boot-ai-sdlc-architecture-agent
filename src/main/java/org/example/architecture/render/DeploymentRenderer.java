package org.example.architecture.render;

import java.util.HashMap;
import java.util.Map;

import org.example.architecture.design.ArchitectureModel;
import org.example.architecture.design.ArchitectureModel.Container;
import org.example.architecture.design.ArchitectureModel.DeploymentNode;

/** Renders deployment nodes as Mermaid subgraphs holding the containers they host. */
public final class DeploymentRenderer {

	private DeploymentRenderer() {
	}

	public static String render(ArchitectureModel model) {
		Map<String, Container> containers = new HashMap<>();
		model.containers().forEach(container -> containers.put(container.id(), container));
		StringBuilder diagram = new StringBuilder("flowchart TB\n");
		for (DeploymentNode node : model.deploymentNodes()) {
			String nodeId = Mermaid.node(node.id());
			diagram.append("    subgraph ").append(nodeId).append(" [\"")
					.append(Mermaid.text(Text.line(node.name())
							+ (node.technology() == null ? "" : " [" + node.technology() + "]")))
					.append("\"]\n");
			for (String containerId : node.containerIds()) {
				Container container = containers.get(containerId);
				if (container == null) {
					continue;
				}
				diagram.append("        ").append(nodeId).append("_").append(Mermaid.node(container.id())).append("[\"")
						.append(Mermaid.label(container.name(),
								container.technology() == null ? null : "[" + container.technology() + "]"))
						.append("\"]\n");
			}
			diagram.append("    end\n");
		}
		return diagram.toString();
	}

}
