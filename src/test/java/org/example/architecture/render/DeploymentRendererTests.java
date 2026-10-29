package org.example.architecture.render;

import java.util.List;

import org.example.architecture.design.ArchitectureModel.DeploymentNode;
import org.example.architecture.design.Samples;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.architecture.render.ModelBuilder.container;

class DeploymentRendererTests {

	@Test
	void rendersTheSampleDeployment() {
		assertThat(DeploymentRenderer.render(Samples.model())).isEqualTo("""
				flowchart TB
				    subgraph nD1 ["EKS cluster [Kubernetes]"]
				        nD1_nC1["Web app<br/>[React]"]
				        nD1_nC2["Order service<br/>[Spring Boot]"]
				    end
				    subgraph nD2 ["Amazon RDS [PostgreSQL 16]"]
				        nD2_nC3["Order database<br/>[PostgreSQL]"]
				    end
				""");
	}

	@Test
	void givesAContainerOnTwoNodesTwoNodeIdsAndSkipsUnknownContainers() {
		ModelBuilder model = new ModelBuilder();
		model.containers = List.of(container("C-1", "API"));
		model.deploymentNodes = List.of(new DeploymentNode("D-1", "Zone A", null, List.of("C-1", "C-9")),
				new DeploymentNode("D-2", "Zone B", null, List.of("C-1")), new DeploymentNode("D-3", "Spare", null, List.of()));

		assertThat(DeploymentRenderer.render(model.build())).isEqualTo("""
				flowchart TB
				    subgraph nD1 ["Zone A"]
				        nD1_nC1["API"]
				    end
				    subgraph nD2 ["Zone B"]
				        nD2_nC1["API"]
				    end
				    subgraph nD3 ["Spare"]
				    end
				""");
	}

}
