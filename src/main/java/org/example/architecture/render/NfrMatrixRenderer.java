package org.example.architecture.render;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.example.architecture.design.ArchitectureModel;
import org.example.architecture.design.ArchitectureModel.Container;
import org.example.architecture.design.ArchitectureModel.NfrRef;
import org.example.architecture.design.ArchitectureModel.NfrTactic;

/** Renders a Markdown table that traces every NFR to the tactics and containers that meet it. */
public final class NfrMatrixRenderer {

	static final String NOT_ADDRESSED = "**Not addressed**";

	private NfrMatrixRenderer() {
	}

	public static String render(ArchitectureModel model) {
		Map<String, String> containerNames = new HashMap<>();
		for (Container container : model.containers()) {
			containerNames.put(container.id(), Text.cell(container.name()));
		}
		StringBuilder table = new StringBuilder("| NFR | Category | Statement | Tactic | Containers |\n");
		table.append("|-----|----------|-----------|--------|------------|\n");
		for (NfrRef nfr : model.nonFunctionalRequirements()) {
			String prefix = "| " + Text.cell(nfr.id()) + " | " + Text.cell(nfr.category()) + " | "
					+ Text.cell(nfr.statement()) + " | ";
			List<NfrTactic> tactics = model.nfrTactics().stream().filter(t -> nfr.id() != null && nfr.id().equals(t.nfrId()))
					.toList();
			if (tactics.isEmpty()) {
				table.append(prefix).append(NOT_ADDRESSED).append(" |  |\n");
				continue;
			}
			for (NfrTactic tactic : tactics) {
				List<String> names = new ArrayList<>();
				for (String id : tactic.containerIds()) {
					if (containerNames.containsKey(id)) {
						names.add(containerNames.get(id));
					}
				}
				table.append(prefix).append(Text.cell(tactic.tactic())).append(" | ").append(String.join(", ", names))
						.append(" |\n");
			}
		}
		return table.toString();
	}

}
