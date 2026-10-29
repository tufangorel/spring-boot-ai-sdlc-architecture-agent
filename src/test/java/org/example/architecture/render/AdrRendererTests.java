package org.example.architecture.render;

import java.util.List;

import org.example.architecture.design.ArchitectureModel.Decision;
import org.example.architecture.design.Samples;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AdrRendererTests {

	@Test
	void rendersTheSampleDecision() {
		assertThat(AdrRenderer.render(Samples.model().decisions().getFirst())).isEqualTo("""
				### ADR-1: Issue refunds asynchronously

				- **Status:** Proposed
				- **Requirements:** US-2, NFR-1

				**Context:** The payment gateway can be slow or unavailable, and refunds must arrive within 5 business days.

				**Decision:** The order service records a pending refund and a scheduled job sends it to the payment gateway, retrying on failure.

				**Alternatives considered:**

				- Refund synchronously during cancellation

				**Consequences:** Cancellation stays fast; refunds need monitoring.
				""");
	}

	@Test
	void leavesOutMissingParts() {
		assertThat(AdrRenderer.render(new Decision(null, "Use REST", null, "Expose a\nREST API", List.of(), " ", List.of())))
				.isEqualTo("""
						### Use REST

						- **Status:** Proposed
						- **Requirements:** None

						**Decision:** Expose a REST API
						""");
		assertThat(AdrRenderer.render(new Decision("ADR-2", null, null, null, List.of(), null, List.of())))
				.startsWith("### ADR-2\n");
	}

}
