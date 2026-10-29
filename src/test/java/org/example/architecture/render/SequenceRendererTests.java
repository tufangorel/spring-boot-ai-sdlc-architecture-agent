package org.example.architecture.render;

import java.util.List;

import org.example.architecture.design.ArchitectureModel;
import org.example.architecture.design.ArchitectureModel.Element;
import org.example.architecture.design.ArchitectureModel.Flow;
import org.example.architecture.design.ArchitectureModel.Step;
import org.example.architecture.design.Samples;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.architecture.render.ModelBuilder.container;

class SequenceRendererTests {

	@Test
	void rendersTheSampleFlow() {
		ArchitectureModel model = Samples.model();

		assertThat(SequenceRenderer.render(model, model.flows().getFirst())).isEqualTo("""
				sequenceDiagram
				    actor nA1 as Customer
				    participant nC1 as Web app
				    participant nC2 as Order service
				    participant nC3 as Order database
				    nA1->>nC1: Cancel order
				    nC1->>nC2: POST /orders/{id}/cancel
				    nC2->>nC3: Set status to Cancelled
				""");
	}

	@Test
	void declaresParticipantsInOrderOfAppearanceAndSkipsUnknownSteps() {
		ModelBuilder builder = new ModelBuilder();
		builder.actors = List.of(new Element("A-1", "Customer", null));
		builder.externalSystems = List.of(new Element("X-1", "Bank; \"main\"", null));
		builder.containers = List.of(container("C-1", "API"));
		ArchitectureModel model = builder.build();
		Flow flow = new Flow("US-1", "Pay", List.of(new Step("C-1", "X-1", "Charge #1"), new Step("C-9", "C-1", "Ghost"),
				new Step("A-1", "C-1", null)));

		assertThat(SequenceRenderer.render(model, flow)).isEqualTo("""
				sequenceDiagram
				    participant nC1 as API
				    participant nX1 as Bank#59; #quot;main#quot;
				    actor nA1 as Customer
				    nC1->>nX1: Charge #35;1
				    nA1->>nC1: (no message)
				""");
	}

	@Test
	void returnsNullWhenNoStepConnectsKnownParticipants() {
		ModelBuilder builder = new ModelBuilder();
		builder.containers = List.of(container("C-1", "API"));

		assertThat(SequenceRenderer.render(builder.build(),
				new Flow("US-1", null, List.of(new Step("C-1", "C-9", "Ghost"))))).isNull();
	}

}
