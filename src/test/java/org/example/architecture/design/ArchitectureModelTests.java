package org.example.architecture.design;

import java.util.Arrays;
import java.util.List;

import org.example.architecture.design.ArchitectureModel.Container;
import org.example.architecture.design.ArchitectureModel.Decision;
import org.example.architecture.design.ArchitectureModel.DeploymentNode;
import org.example.architecture.design.ArchitectureModel.Entity;
import org.example.architecture.design.ArchitectureModel.Flow;
import org.example.architecture.design.ArchitectureModel.Gaps;
import org.example.architecture.design.ArchitectureModel.NfrTactic;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureModelTests {

	@Test
	void normalizesMissingListsAndNullEntries() {
		ArchitectureModel model = new ArchitectureModel("Shop", null, null, null, null, null,
				Arrays.asList(null, new Container("C-1", "API", null, null, null)), null,
				Arrays.asList(null, new Entity("E-1", "Order", Arrays.asList(null, null))), null,
				Arrays.asList(null, new Flow("US-1", null, null)),
				Arrays.asList(new Decision("ADR-1", "T", null, "D", null, null, Arrays.asList("US-1", null))),
				Arrays.asList(new NfrTactic("NFR-1", "Cache", null)), null,
				Arrays.asList(new DeploymentNode("D-1", "VM", null, Arrays.asList(null, "C-1"))), null, null, null,
				null).normalized();

		assertThat(model.stories()).isEmpty();
		assertThat(model.containers()).containsExactly(new Container("C-1", "API", null, null, List.of()));
		assertThat(model.relationships()).isEmpty();
		assertThat(model.entities()).containsExactly(new Entity("E-1", "Order", List.of()));
		assertThat(model.flows()).containsExactly(new Flow("US-1", null, List.of()));
		assertThat(model.decisions().getFirst().alternatives()).isEmpty();
		assertThat(model.decisions().getFirst().requirementIds()).containsExactly("US-1");
		assertThat(model.nfrTactics().getFirst().containerIds()).isEmpty();
		assertThat(model.deploymentNodes()).containsExactly(new DeploymentNode("D-1", "VM", null, List.of("C-1")));
		assertThat(model.warnings()).isEmpty();
		assertThat(ArchitectureGaps.compute(model))
				.isEqualTo(new Gaps(List.of("C-1"), List.of(), List.of(), List.of()));
	}

	@Test
	void comparesNamesIgnoringCaseAndWhitespace() {
		assertThat(ArchitectureGrounding.key("  Order\n  Service ")).isEqualTo("order service");
		assertThat(ArchitectureGrounding.key(null)).isEmpty();
	}

}
