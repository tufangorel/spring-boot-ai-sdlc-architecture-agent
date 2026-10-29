package org.example.architecture.design;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.example.architecture.design.ArchitectureDraft.Attribute;
import org.example.architecture.design.ArchitectureDraft.Container;
import org.example.architecture.design.ArchitectureDraft.Decision;
import org.example.architecture.design.ArchitectureDraft.DeploymentNode;
import org.example.architecture.design.ArchitectureDraft.Element;
import org.example.architecture.design.ArchitectureDraft.Entity;
import org.example.architecture.design.ArchitectureDraft.EntityRelation;
import org.example.architecture.design.ArchitectureDraft.Flow;
import org.example.architecture.design.ArchitectureDraft.NfrTactic;
import org.example.architecture.design.ArchitectureDraft.Relationship;
import org.example.architecture.design.ArchitectureDraft.Step;
import org.example.architecture.design.ArchitectureModel.Gaps;
import org.example.architecture.design.ArchitectureRequest.SystemContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ArchitectureGroundingTests {

	private static final ArchitectureRequest NO_TARGET = new ArchitectureRequest(Samples.REQUEST.requirements(),
			new SystemContext(null, List.of(), List.of(), null, List.of()));

	private final ArchitectureProperties properties = new ArchitectureProperties();

	@Test
	void groundsTheSampleReply() {
		ArchitectureModel model = Samples.model();

		assertThat(model.systemName()).isEqualTo("Web shop");
		assertThat(model.stories()).extracting(ArchitectureModel.StoryRef::id).containsExactly("US-1", "US-2");
		assertThat(model.nonFunctionalRequirements()).containsExactly(new ArchitectureModel.NfrRef("NFR-1",
				"PERFORMANCE", "Refunds reach the original payment method within 5 business days."));
		assertThat(model.actors()).containsExactly(new ArchitectureModel.Element("A-1", "Customer", "Buys and cancels orders"));
		assertThat(model.externalSystems()).extracting(ArchitectureModel.Element::id).containsExactly("X-1");
		assertThat(model.containers()).extracting(ArchitectureModel.Container::id).containsExactly("C-1", "C-2", "C-3");
		assertThat(model.containers().get(1).requirementIds()).containsExactly("US-1", "US-2", "NFR-1");
		assertThat(model.relationships()).contains(new ArchitectureModel.Relationship("C-2", "X-1", "Issues refunds", "HTTPS"));
		assertThat(model.entities()).extracting(ArchitectureModel.Entity::id).containsExactly("E-1", "E-2");
		assertThat(model.entityRelations()).containsExactly(
				new ArchitectureModel.EntityRelation("E-1", "E-2", Cardinality.ONE_TO_MANY, "is refunded by"));
		assertThat(model.flows()).singleElement().satisfies(flow -> {
			assertThat(flow.storyId()).isEqualTo("US-1");
			assertThat(flow.title()).isEqualTo("Cancel an unshipped order");
			assertThat(flow.steps()).hasSize(3);
		});
		assertThat(model.decisions()).extracting(ArchitectureModel.Decision::id).containsExactly("ADR-1");
		assertThat(model.nfrTactics()).singleElement()
				.satisfies(tactic -> assertThat(tactic.containerIds()).containsExactly("C-2"));
		assertThat(model.deploymentTarget()).isEqualTo("AWS EKS");
		assertThat(model.deploymentNodes()).extracting(ArchitectureModel.DeploymentNode::containerIds)
				.containsExactly(List.of("C-1", "C-2"), List.of("C-3"));
		assertThat(model.gaps()).isEqualTo(new Gaps(List.of(), List.of(), List.of("US-2"), List.of()));
		assertThat(model.openQuestions()).containsExactly("Who is alerted when a refund keeps failing?");
		assertThat(model.warnings()).isEmpty();
	}

	@Test
	void fillsInDefaultsWhenTheModelReturnsNothing() {
		ArchitectureModel model = ArchitectureGrounding.ground(NO_TARGET, null, properties);

		assertThat(model.systemName()).isEqualTo("System");
		assertThat(model.summary()).isEqualTo("Architecture for System with 0 containers.");
		assertThat(model.containers()).isEmpty();
		assertThat(model.warnings()).containsExactly("The model returned no usable containers.");
		assertThat(model.openQuestions()).containsExactly(ArchitectureGrounding.NO_DEPLOYMENT_TARGET);
		assertThat(model.gaps()).isEqualTo(new Gaps(List.of(), List.of("NFR-1"), List.of("US-1", "US-2"), List.of()));
	}

	@Test
	void usesOneNamespaceForActorsExternalSystemsAndContainers() {
		Draft draft = new Draft();
		draft.actors = list(null, new Element(" ", "x"), new Element(" Customer ", " Buys "), new Element("customer", null));
		draft.externalSystems = list(null, new Element(null, null), new Element("CUSTOMER", null), new Element("Bank", null));
		draft.containers = list(null, new Container(null, null, null, null), new Container("bank", null, null, null),
				new Container("API", " Java ", " ", list(null, " ", "us-1", "US-9", "US-1")));

		ArchitectureModel model = ArchitectureGrounding.ground(Samples.REQUEST, draft.build(), properties);

		assertThat(model.actors()).containsExactly(new ArchitectureModel.Element("A-1", "Customer", "Buys"));
		assertThat(model.externalSystems()).containsExactly(new ArchitectureModel.Element("X-1", "Bank", null));
		assertThat(model.containers()).containsExactly(
				new ArchitectureModel.Container("C-1", "API", "Java", null, List.of("US-1")));
		assertThat(model.summary()).isEqualTo("Architecture for Web shop with 1 container.");
		assertThat(model.warnings()).containsExactly(
				"Dropped an actor: its name is missing.",
				"Dropped actor \"customer\": its name is already used.",
				"Dropped an external system: its name is missing.",
				"Dropped external system \"CUSTOMER\": its name is already used.",
				"Dropped a container: its name is missing.",
				"Dropped container \"bank\": its name is already used.",
				"Container \"API\": ignored unknown requirement id \"US-9\".");
	}

	@Test
	void capsContainersEntitiesFlowsAndDecisions() {
		properties.setMaxContainers(1);
		properties.setMaxEntities(1);
		properties.setMaxFlows(1);
		properties.setMaxDecisions(1);
		Draft draft = new Draft();
		draft.actors = list(new Element("Customer", null));
		draft.containers = list(new Container("API", null, null, null), new Container("Worker", null, null, null),
				new Container("Database", null, null, null));
		draft.entities = list(new Entity("Order", null), new Entity("Refund", null));
		draft.flows = list(flow("US-1"), flow("US-2"));
		draft.decisions = list(new Decision("One", null, "Do it", null, null, null),
				new Decision("Two", null, "Do it", null, null, null));

		ArchitectureModel model = ArchitectureGrounding.ground(Samples.REQUEST, draft.build(), properties);

		assertThat(model.containers()).hasSize(1);
		assertThat(model.entities()).hasSize(1);
		assertThat(model.flows()).hasSize(1);
		assertThat(model.decisions()).hasSize(1);
		assertThat(model.warnings()).containsExactly(
				"Dropped 2 more containers over the limit of 1.",
				"Dropped 1 more entities over the limit of 1.",
				"Dropped 1 more flows over the limit of 1.",
				"Dropped 1 more decisions over the limit of 1.");
	}

	@Test
	void dropsRelationshipsWithUnknownEndpoints() {
		Draft draft = new Draft();
		draft.actors = list(new Element("Customer", null));
		draft.containers = list(new Container("API", null, null, null));
		draft.relationships = list(null, new Relationship("Ghost", "API", null, null),
				new Relationship("Customer", "Ghost", null, null), new Relationship("customer", "api", " Uses ", " "));

		ArchitectureModel model = ArchitectureGrounding.ground(Samples.REQUEST, draft.build(), properties);

		assertThat(model.relationships()).containsExactly(new ArchitectureModel.Relationship("A-1", "C-1", "Uses", null));
		assertThat(model.warnings()).containsExactly(
				"Dropped relationship \"Ghost\" -> \"API\": \"Ghost\" is not a known actor, external system, or container.",
				"Dropped relationship \"Customer\" -> \"Ghost\": \"Ghost\" is not a known actor, external system, or container.");
	}

	@Test
	void groundsEntitiesAndTheirRelations() {
		Draft draft = new Draft();
		draft.containers = list(new Container("API", null, null, null));
		draft.entities = list(null, new Entity(" ", null), new Entity("Order",
				list(null, new Attribute(" ", "int"), new Attribute(" id ", " UUID "), new Attribute("note", null))),
				new Entity("ORDER", null), new Entity("Refund", null));
		draft.entityRelations = list(null, new EntityRelation("Ghost", "Order", null, null),
				new EntityRelation("Order", "Ghost", null, null),
				new EntityRelation("order", "refund", Cardinality.ONE_TO_MANY, " has "));

		ArchitectureModel model = ArchitectureGrounding.ground(Samples.REQUEST, draft.build(), properties);

		assertThat(model.entities()).containsExactly(
				new ArchitectureModel.Entity("E-1", "Order", List.of(new ArchitectureModel.Attribute("id", "UUID"),
						new ArchitectureModel.Attribute("note", null))),
				new ArchitectureModel.Entity("E-2", "Refund", List.of()));
		assertThat(model.entityRelations()).containsExactly(
				new ArchitectureModel.EntityRelation("E-1", "E-2", Cardinality.ONE_TO_MANY, "has"));
		assertThat(model.warnings()).containsExactly(
				"Dropped an entity: its name is missing.",
				"Dropped entity \"ORDER\": its name is already used.",
				"Dropped entity relation \"Ghost\" -> \"Order\": \"Ghost\" is not a known entity.",
				"Dropped entity relation \"Order\" -> \"Ghost\": \"Ghost\" is not a known entity.");
	}

	@Test
	void keepsOneFlowPerKnownStoryWithValidSteps() {
		Draft draft = new Draft();
		draft.actors = list(new Element("Customer", null));
		draft.containers = list(new Container("API", null, null, null));
		draft.flows = list(null, flow("US-9"),
				new Flow("us-1", list(null, new Step("Ghost", "API", "Hello"), new Step("Customer", "Ghost", "Hi"),
						new Step("Customer", "API", " Cancel "))),
				flow("US-1"), new Flow("US-2", null), new Flow("US-2", list(new Step("Ghost", "API", "x"))));

		ArchitectureModel model = ArchitectureGrounding.ground(Samples.REQUEST, draft.build(), properties);

		assertThat(model.flows()).containsExactly(new ArchitectureModel.Flow("US-1", "Cancel an unshipped order",
				List.of(new ArchitectureModel.Step("A-1", "C-1", "Cancel"))));
		assertThat(model.warnings()).containsExactly(
				"Dropped flow for \"US-9\": it is not a known story id.",
				"Flow for US-1: dropped step \"Hello\": \"Ghost\" is not a known participant.",
				"Flow for US-1: dropped step \"Hi\": \"Ghost\" is not a known participant.",
				"Dropped a second flow for US-1.",
				"Dropped flow for US-2: it has no valid steps.",
				"Flow for US-2: dropped step \"x\": \"Ghost\" is not a known participant.",
				"Dropped flow for US-2: it has no valid steps.");
		assertThat(model.gaps().storiesWithoutFlow()).containsExactly("US-2");
	}

	@Test
	void requiresATitleAndADecision() {
		Draft draft = new Draft();
		draft.containers = list(new Container("API", null, null, null));
		draft.decisions = list(null, new Decision(" ", null, "Do it", null, null, null),
				new Decision("Use Kafka", null, " ", null, null, null),
				new Decision(" Use REST ", " ", " Expose a REST API ", list(null, "gRPC", " grpc ", " "), " ",
						list("NFR-1", "nfr-2")));

		ArchitectureModel model = ArchitectureGrounding.ground(Samples.REQUEST, draft.build(), properties);

		assertThat(model.decisions()).containsExactly(new ArchitectureModel.Decision("ADR-1", "Use REST", null,
				"Expose a REST API", List.of("gRPC"), null, List.of("NFR-1")));
		assertThat(model.warnings()).containsExactly(
				"Dropped decision \"untitled\": a title and a decision are both required.",
				"Dropped decision \"Use Kafka\": a title and a decision are both required.",
				"Decision \"Use REST\": ignored unknown requirement id \"nfr-2\".");
	}

	@Test
	void groundsTacticsAgainstNfrsAndContainers() {
		Draft draft = new Draft();
		draft.containers = list(new Container("API", null, null, null));
		draft.nfrTactics = list(null, new NfrTactic("NFR-9", "Cache", null), new NfrTactic("NFR-1", " ", null),
				new NfrTactic("nfr-1", " Retry refunds ", list("api", "Worker")));

		ArchitectureModel model = ArchitectureGrounding.ground(Samples.REQUEST, draft.build(), properties);

		assertThat(model.nfrTactics()).containsExactly(
				new ArchitectureModel.NfrTactic("NFR-1", "Retry refunds", List.of("C-1")));
		assertThat(model.gaps().unaddressedNfrs()).isEmpty();
		assertThat(model.warnings()).containsExactly(
				"Dropped tactic for \"NFR-9\": it is not a known NFR id.",
				"Dropped tactic for NFR-1: the tactic is missing.",
				"Tactic for NFR-1: ignored unknown container \"Worker\".");
	}

	@Test
	void groundsDeploymentNodesWhenATargetIsGiven() {
		Draft draft = new Draft();
		draft.containers = list(new Container("API", null, null, null), new Container("Database", null, null, null));
		draft.deploymentNodes = list(null, new DeploymentNode(" ", null, null),
				new DeploymentNode(" Cluster ", " Kubernetes ", list("api", "Ghost")),
				new DeploymentNode("cluster", null, null));

		ArchitectureModel model = ArchitectureGrounding.ground(Samples.REQUEST, draft.build(), properties);

		assertThat(model.deploymentNodes()).containsExactly(
				new ArchitectureModel.DeploymentNode("D-1", "Cluster", "Kubernetes", List.of("C-1")));
		assertThat(model.gaps().unhostedContainers()).containsExactly("C-2");
		assertThat(model.gaps().untracedContainers()).containsExactly("C-1", "C-2");
		assertThat(model.warnings()).containsExactly(
				"Dropped a deployment node: its name is missing.",
				"Deployment node \"Cluster\": ignored unknown container \"Ghost\".",
				"Dropped deployment node \"cluster\": its name is already used.");
		assertThat(model.openQuestions()).doesNotContain(ArchitectureGrounding.NO_DEPLOYMENT_TARGET);
	}

	@Test
	void ignoresDeploymentNodesWithoutATarget() {
		Draft draft = new Draft();
		draft.containers = list(new Container("API", null, null, null));
		draft.deploymentNodes = list(null, new DeploymentNode("Cluster", null, null));
		draft.openQuestions = list("Which region?", " which  region? ");

		ArchitectureModel one = ArchitectureGrounding.ground(NO_TARGET, draft.build(), properties);
		draft.deploymentNodes = list(new DeploymentNode("Cluster", null, null), new DeploymentNode("VM", null, null));
		ArchitectureModel two = ArchitectureGrounding.ground(NO_TARGET, draft.build(), properties);

		assertThat(one.deploymentNodes()).isEmpty();
		assertThat(one.warnings()).containsExactly("Ignored 1 deployment node: no deployment target was given.");
		assertThat(one.openQuestions()).containsExactly("Which region?", ArchitectureGrounding.NO_DEPLOYMENT_TARGET);
		assertThat(two.warnings()).containsExactly("Ignored 2 deployment nodes: no deployment target was given.");
	}

	private static Flow flow(String storyId) {
		return new Flow(storyId, List.of(new Step("Customer", "API", "Call")));
	}

	@SafeVarargs
	private static <T> List<T> list(T... items) {
		return new ArrayList<>(Arrays.asList(items));
	}

	private static final class Draft {

		String summary;

		List<Element> actors;

		List<Element> externalSystems;

		List<Container> containers;

		List<Relationship> relationships;

		List<Entity> entities;

		List<EntityRelation> entityRelations;

		List<Flow> flows;

		List<Decision> decisions;

		List<NfrTactic> nfrTactics;

		List<DeploymentNode> deploymentNodes;

		List<String> assumptions;

		List<String> openQuestions;

		ArchitectureDraft build() {
			return new ArchitectureDraft(summary, actors, externalSystems, containers, relationships, entities,
					entityRelations, flows, decisions, nfrTactics, deploymentNodes, assumptions, openQuestions);
		}

	}

}
