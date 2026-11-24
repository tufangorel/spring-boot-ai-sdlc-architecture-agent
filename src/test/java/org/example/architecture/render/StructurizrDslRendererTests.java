package org.example.architecture.render;

import java.util.List;
import java.nio.file.Path;

import com.structurizr.Workspace;
import com.structurizr.dsl.StructurizrDslParserException;
import org.example.architecture.design.ArchitectureModel;
import org.example.architecture.design.ArchitectureModel.Container;
import org.example.architecture.design.ArchitectureModel.DeploymentNode;
import org.example.architecture.design.ArchitectureModel.Element;
import org.example.architecture.design.ArchitectureModel.Flow;
import org.example.architecture.design.ArchitectureModel.Relationship;
import org.example.architecture.design.ArchitectureModel.Step;
import org.example.architecture.design.Samples;
import org.example.architecture.StructurizrDslParserTestSupport;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class StructurizrDslRendererTests {

	@TempDir
	Path tempDir;

	@Test
	void rendersTheSampleModelAsStructurizrDsl() {
		assertThat(StructurizrDslRenderer.render(Samples.model())).isEqualTo("""
				workspace "Web shop" "A web app calls a Spring Boot order service that stores orders in PostgreSQL and refunds through the payment gateway." {

				    model {
				        actor_A1 = person "Customer" "Buys and cancels orders"
				        ext_X1 = softwareSystem "Payment gateway" "Card payments and refunds" "External"
				        mainSystem = softwareSystem "Web shop" "A web app calls a Spring Boot order service that stores orders in PostgreSQL and refunds through the payment gateway." {
				            c_C1 = container "Web app" "Order pages for customers" "React"
				            c_C2 = container "Order service" "Cancels orders and issues refunds" "Spring Boot"
				            c_C3 = container "Order database" "Stores orders and refunds" "PostgreSQL" "Database"
				        }
				        actor_A1 -> c_C1 "Cancels orders" "HTTPS"
				        c_C1 -> c_C2 "Calls the order API" "HTTPS/JSON"
				        c_C2 -> c_C3 "Reads and writes orders" "JDBC"
				        c_C2 -> ext_X1 "Issues refunds" "HTTPS"

				        deploymentEnvironment "AWS EKS" {
				            node_D1 = deploymentNode "EKS cluster" "" "Kubernetes" {
				                containerInstance c_C1
				                containerInstance c_C2
				            }
				            node_D2 = deploymentNode "Amazon RDS" "" "PostgreSQL 16" {
				                containerInstance c_C3
				            }
				        }
				    }

				    views {
				        systemContext mainSystem "SystemContext" {
				            include *
				            autoLayout
				        }

				        container mainSystem "Containers" {
				            include *
				            autoLayout
				        }

				        deployment mainSystem "AWS EKS" "Deployment" {
				            include *
				            autoLayout
				        }

				        dynamic mainSystem "flow_US1" "Cancel an unshipped order" {
				            actor_A1 -> c_C1 "Cancel order"
				            c_C1 -> c_C2 "POST /orders/{id}/cancel"
				            c_C2 -> c_C3 "Set status to Cancelled"
				            autoLayout
				        }

				        styles {
				            element "Person" {
				                shape Person
				                background #08427b
				                color #ffffff
				            }
				            element "Software System" {
				                background #1168bd
				                color #ffffff
				            }
				            element "External" {
				                background #999999
				                color #ffffff
				            }
				            element "Container" {
				                background #438dd5
				                color #ffffff
				            }
				            element "Database" {
				                shape Cylinder
				            }
				        }
				    }
				}
				""");
	}

	@Test
	void generatedSampleDslParsesWithExpectedModelAndViews() throws Exception {
		Workspace workspace = StructurizrDslParserTestSupport.parse(StructurizrDslRenderer.render(Samples.model()), tempDir);

		assertThat(workspace.getModel().getSoftwareSystemWithName("Web shop")).isNotNull();
		assertThat(workspace.getModel().getSoftwareSystemWithName("Web shop").getContainers())
				.extracting("name").contains("Web app", "Order service", "Order database");
		assertThat(workspace.getModel().getRelationships()).extracting("description")
				.contains("Cancels orders", "Calls the order API", "Reads and writes orders", "Issues refunds");
		assertThat(workspace.getViews().getSystemContextViews()).hasSize(1);
		assertThat(workspace.getViews().getContainerViews()).hasSize(1);
		assertThat(workspace.getViews().getDeploymentViews()).hasSize(1);
		assertThat(workspace.getViews().getDynamicViews()).hasSize(1);
	}

	@Test
	void handlesMinimalModelAndFallbacks() {
		ModelBuilder builder = new ModelBuilder();
		builder.systemName = " ";
		builder.summary = null;
		builder.actors = List.of(new Element("A-1", "User", " "));
		builder.externalSystems = List.of(new Element("X-1", "Ext", null));
		builder.containers = List.of(
				new Container("C-1", "Simple Container", null, null, List.of()),
				new Container("C-2", "Backend", "Go", null, List.of()),
				new Container("C-3", "Auth Service", null, "Authenticates users", List.of()),
				new Container("C-4", "Redis Cache", "In-memory", null, List.of()),
				new Container("C-5", "Order DB", null, null, List.of()));
		builder.relationships = List.of(
				new Relationship("A-1", "C-1", "Interacts", null),
				new Relationship("C-1", "UNKNOWN", "Ghost", "HTTP"),
				new Relationship("UNKNOWN", "C-2", "Ghost", null));
		builder.flows = List.of(
				new Flow("US-1", null, List.of(
						new Step("A-1", "C-1", null),
						new Step("UNKNOWN_FROM", "C-1", "skip"),
						new Step("C-1", "UNKNOWN_TO", "skip"),
						new Step("GHOST1", "GHOST2", "skip"))),
				new Flow("US-2", "Ghost Flow", List.of(
						new Step("UNKNOWN1", "C-1", "Message"),
						new Step("C-1", "UNKNOWN2", "Message"))));
		builder.deploymentTarget = "";
		builder.deploymentNodes = List.of(
				new DeploymentNode("D-1", "Server", null, List.of("C-1", "C-UNKNOWN")));

		ArchitectureModel model = builder.build();
		String dsl = StructurizrDslRenderer.render(model);

		assertThat(dsl).startsWith("workspace \"System\" {\n\n");
		assertThat(dsl).contains("mainSystem = softwareSystem \"System\" {");
		assertThat(dsl).contains("actor_A1 = person \"User\"");
		assertThat(dsl).contains("c_C1 = container \"Simple Container\"");
		assertThat(dsl).contains("c_C2 = container \"Backend\" \"\" \"Go\"");
		assertThat(dsl).contains("c_C3 = container \"Auth Service\" \"Authenticates users\"");
		assertThat(dsl).contains("c_C4 = container \"Redis Cache\" \"\" \"In-memory\" \"Database\"");
		assertThat(dsl).contains("c_C5 = container \"Order DB\" \"\" \"\" \"Database\"");
		assertThat(dsl).contains("actor_A1 -> c_C1 \"Interacts\"");
		assertThat(dsl).doesNotContain("UNKNOWN");
		assertThat(dsl).contains("deploymentEnvironment \"Production\" {");
		assertThat(dsl).contains("node_D1 = deploymentNode \"Server\" {");
		assertThat(dsl).contains("dynamic mainSystem \"flow_US1\" {");
		assertThat(dsl).doesNotContain("Ghost Flow");
	}

	@Test
	void omitsDeploymentAndFlowsWhenAbsent() {
		ModelBuilder builder = new ModelBuilder();
		builder.systemName = "Billing";
		builder.summary = "Handles billing";
		builder.containers = List.of(new Container("C-1", "Billing App", "Java", "Invoicing", List.of()));
		builder.deploymentNodes = List.of();
		builder.flows = List.of();

		ArchitectureModel model = builder.build();
		String dsl = StructurizrDslRenderer.render(model);

		assertThat(dsl).contains("workspace \"Billing\" \"Handles billing\" {");
		assertThat(dsl).doesNotContain("deploymentEnvironment");
		assertThat(dsl).doesNotContain("deployment mainSystem");
		assertThat(dsl).doesNotContain("dynamic mainSystem");
	}

	@Test
	void generatedMinimalDslParsesWithoutDeploymentView() throws Exception {
		ModelBuilder builder = new ModelBuilder();
		builder.systemName = "Billing";
		builder.containers = List.of(new Container("C-1", "Billing App", "Java", "Invoicing", List.of()));
		Workspace workspace = StructurizrDslParserTestSupport.parse(StructurizrDslRenderer.render(builder.build()), tempDir);

		assertThat(workspace.getModel().getSoftwareSystemWithName("Billing").getContainers()).hasSize(1);
		assertThat(workspace.getViews().getDeploymentViews()).isEmpty();
		assertThat(workspace.getViews().getDynamicViews()).isEmpty();
	}

	@Test
	void generatedDslWithEscapedTextParses() throws Exception {
		ModelBuilder builder = new ModelBuilder();
		builder.systemName = "Billing \"Core\"";
		builder.containers = List.of(new Container("C-1", "API \\ Gateway", "Java", "Handles \"billing\"", List.of()));
		Workspace workspace = StructurizrDslParserTestSupport.parse(StructurizrDslRenderer.render(builder.build()), tempDir);

		assertThat(workspace.getModel().getSoftwareSystemWithName("Billing \"Core\"")).isNotNull();
		assertThat(workspace.getModel().getSoftwareSystemWithName("Billing \"Core\"").getContainers()).hasSize(1);
	}

	@Test
	void parserRejectsInvalidDsl() {
		assertThatThrownBy(() -> StructurizrDslParserTestSupport.parse("workspace { invalid", tempDir))
				.isInstanceOf(StructurizrDslParserException.class);
	}

	@Test
	void escapesQuotesAndBackslashes() {
		assertThat(StructurizrDslRenderer.escape("He said: \"Hello\\World\""))
				.isEqualTo("He said: \\\"Hello\\\\World\\\"");
		assertThat(StructurizrDslRenderer.escape(null)).isEmpty();
	}

	@Test
	void generatesSafeIds() {
		assertThat(StructurizrDslRenderer.id("actor", "A-1")).isEqualTo("actor_A1");
		assertThat(StructurizrDslRenderer.id("c", null)).isEqualTo("c_id");
		assertThat(StructurizrDslRenderer.id("c", "---")).isEqualTo("c_id");
	}

	@Test
	void recognizesDatabaseContainers() {
		assertThat(StructurizrDslRenderer.isDatabase(new Container("C-1", "Service", "database", null, List.of()))).isTrue();
		assertThat(StructurizrDslRenderer.isDatabase(new Container("C-2", "Storage", "sql", null, List.of()))).isTrue();
		assertThat(StructurizrDslRenderer.isDatabase(new Container("C-3", "Store", "postgres", null, List.of()))).isTrue();
		assertThat(StructurizrDslRenderer.isDatabase(new Container("C-4", "Document", "mongo", null, List.of()))).isTrue();
		assertThat(StructurizrDslRenderer.isDatabase(new Container("C-5", "Cache", "redis", null, List.of()))).isTrue();
		assertThat(StructurizrDslRenderer.isDatabase(new Container("C-6", "User DB", "C++", null, List.of()))).isTrue();
		assertThat(StructurizrDslRenderer.isDatabase(new Container("C-7", "Web frontend", "React", null, List.of()))).isFalse();
	}

	@Test
	void canInstantiatePackagePrivateConstructor() {
		assertThat(new StructurizrDslRenderer()).isNotNull();
	}

}
