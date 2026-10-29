package org.example.architecture.render;

import java.util.List;

import org.example.architecture.design.ArchitectureModel.NfrRef;
import org.example.architecture.design.ArchitectureModel.NfrTactic;
import org.example.architecture.design.Samples;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.architecture.render.ModelBuilder.container;

class NfrMatrixRendererTests {

	@Test
	void rendersTheSampleMatrix() {
		assertThat(NfrMatrixRenderer.render(Samples.model())).isEqualTo("""
				| NFR | Category | Statement | Tactic | Containers |
				|-----|----------|-----------|--------|------------|
				| NFR-1 | PERFORMANCE | Refunds reach the original payment method within 5 business days. | Retry failed refunds with a scheduled job and alert after 3 days | Order service |
				""");
	}

	@Test
	void showsOneRowPerTacticAndMarksNfrsWithoutOne() {
		ModelBuilder model = new ModelBuilder();
		model.containers = List.of(container("C-1", "API | v2"), container("C-2", "Cache"));
		model.nfrs = List.of(new NfrRef("NFR-1", "PERFORMANCE", "p95 < 200 ms"), new NfrRef("NFR-2", null, "Audit | log"),
				new NfrRef(null, null, "No id"));
		model.tactics = List.of(new NfrTactic("NFR-1", "Cache reads", List.of("C-2", "C-1")),
				new NfrTactic("NFR-1", "Scale out", List.of("C-9")), new NfrTactic(null, "Orphan", List.of()));

		assertThat(NfrMatrixRenderer.render(model.build())).isEqualTo("""
				| NFR | Category | Statement | Tactic | Containers |
				|-----|----------|-----------|--------|------------|
				| NFR-1 | PERFORMANCE | p95 < 200 ms | Cache reads | Cache, API \\| v2 |
				| NFR-1 | PERFORMANCE | p95 < 200 ms | Scale out |  |
				| NFR-2 |  | Audit \\| log | **Not addressed** |  |
				|  |  | No id | **Not addressed** |  |
				""");
	}

}
