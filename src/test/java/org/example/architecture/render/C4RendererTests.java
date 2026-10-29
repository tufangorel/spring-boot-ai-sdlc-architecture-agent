package org.example.architecture.render;

import java.util.List;

import org.example.architecture.design.ArchitectureModel.Element;
import org.example.architecture.design.ArchitectureModel.Relationship;
import org.example.architecture.design.Samples;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.architecture.render.ModelBuilder.container;

class C4RendererTests {

	@Test
	void rendersTheSampleContextDiagram() {
		assertThat(C4Renderer.context(Samples.model())).isEqualTo("""
				flowchart TB
				    nA1["Customer<br/>[Person]<br/>Buys and cancels orders"]
				    system["Web shop<br/>[Software System]"]
				    nX1["Payment gateway<br/>[External System]<br/>Card payments and refunds"]
				    nA1 -->|"Cancels orders<br/>[HTTPS]"| system
				    system -->|"Issues refunds<br/>[HTTPS]"| nX1
				""");
	}

	@Test
	void rendersTheSampleContainerDiagram() {
		assertThat(C4Renderer.containers(Samples.model())).isEqualTo("""
				flowchart TB
				    nA1["Customer<br/>[Person]<br/>Buys and cancels orders"]
				    subgraph system ["Web shop"]
				        nC1["Web app<br/>[Container: React]<br/>Order pages for customers"]
				        nC2["Order service<br/>[Container: Spring Boot]<br/>Cancels orders and issues refunds"]
				        nC3["Order database<br/>[Container: PostgreSQL]<br/>Stores orders and refunds"]
				    end
				    nX1["Payment gateway<br/>[External System]<br/>Card payments and refunds"]
				    nA1 -->|"Cancels orders<br/>[HTTPS]"| nC1
				    nC1 -->|"Calls the order API<br/>[HTTPS/JSON]"| nC2
				    nC2 -->|"Reads and writes orders<br/>[JDBC]"| nC3
				    nC2 -->|"Issues refunds<br/>[HTTPS]"| nX1
				""");
	}

	@Test
	void projectsContainersOntoTheSystemInTheContextDiagram() {
		ModelBuilder model = new ModelBuilder();
		model.actors = List.of(new Element("A-1", "Customer", null));
		model.externalSystems = List.of(new Element("X-1", "Bank", null));
		model.containers = List.of(container("C-1", "API"), container("C-2", "Worker"));
		model.relationships = List.of(new Relationship("A-1", "C-1", "Uses", null),
				new Relationship("A-1", "C-2", "Also uses", null), new Relationship("C-1", "C-2", "Queues", null),
				new Relationship("A-1", "X-1", null, null), new Relationship("C-9", "X-1", "Ghost", null),
				new Relationship("A-1", "X-9", "Ghost", null));

		assertThat(C4Renderer.context(model.build())).isEqualTo("""
				flowchart TB
				    nA1["Customer<br/>[Person]"]
				    system["Shop<br/>[Software System]"]
				    nX1["Bank<br/>[External System]"]
				    nA1 -->|"Uses"| system
				    nA1 --> nX1
				""");
	}

	@Test
	void skipsRelationshipsWithUnknownEndpointsInTheContainerDiagram() {
		ModelBuilder model = new ModelBuilder();
		model.containers = List.of(container("C-1", "API"), container("C-2", "Worker"));
		model.relationships = List.of(new Relationship("C-1", "C-2", null, "AMQP"), new Relationship("C-1", "C-9", "x", null),
				new Relationship("C-9", "C-1", "x", null));

		assertThat(C4Renderer.containers(model.build())).isEqualTo("""
				flowchart TB
				    subgraph system ["Shop"]
				        nC1["API<br/>[Container]"]
				        nC2["Worker<br/>[Container]"]
				    end
				    nC1 -->|"[AMQP]"| nC2
				""");
	}

}
