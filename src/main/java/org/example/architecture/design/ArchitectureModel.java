package org.example.architecture.design;

import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

import jakarta.validation.constraints.NotEmpty;

/**
 * A validated architecture model. Elements refer to each other by id. Every diagram and document is rendered from
 * this one model, so they cannot contradict each other. An architect can edit it and send it to the render endpoint.
 */
public record ArchitectureModel(
		String systemName,
		String summary,
		List<StoryRef> stories,
		List<NfrRef> nonFunctionalRequirements,
		List<Element> actors,
		List<Element> externalSystems,
		@NotEmpty List<Container> containers,
		List<Relationship> relationships,
		List<Entity> entities,
		List<EntityRelation> entityRelations,
		List<Flow> flows,
		List<Decision> decisions,
		List<NfrTactic> nfrTactics,
		String deploymentTarget,
		List<DeploymentNode> deploymentNodes,
		Gaps gaps,
		List<String> assumptions,
		List<String> openQuestions,
		List<String> warnings) {

	public record StoryRef(String id, String title) {
	}

	public record NfrRef(String id, String category, String statement) {
	}

	public record Element(String id, String name, String description) {
	}

	public record Container(String id, String name, String technology, String responsibility,
			List<String> requirementIds) {
	}

	public record Relationship(String from, String to, String description, String protocol) {
	}

	public record Entity(String id, String name, List<Attribute> attributes) {
	}

	public record Attribute(String name, String type) {
	}

	public record EntityRelation(String from, String to, Cardinality cardinality, String label) {
	}

	public record Flow(String storyId, String title, List<Step> steps) {
	}

	public record Step(String from, String to, String message) {
	}

	public record Decision(String id, String title, String context, String decision, List<String> alternatives,
			String consequences, List<String> requirementIds) {
	}

	public record NfrTactic(String nfrId, String tactic, List<String> containerIds) {
	}

	public record DeploymentNode(String id, String name, String technology, List<String> containerIds) {
	}

	public record Gaps(List<String> untracedContainers, List<String> unaddressedNfrs, List<String> storiesWithoutFlow,
			List<String> unhostedContainers) {
	}

	/**
	 * Replaces missing lists with empty ones and removes {@code null} entries, so that renderers can rely on every
	 * list being present. Text values may still be {@code null}.
	 */
	public ArchitectureModel normalized() {
		return new ArchitectureModel(systemName, summary, list(stories, UnaryOperator.identity()),
				list(nonFunctionalRequirements, UnaryOperator.identity()), list(actors, UnaryOperator.identity()),
				list(externalSystems, UnaryOperator.identity()),
				list(containers, c -> new Container(c.id(), c.name(), c.technology(), c.responsibility(),
						list(c.requirementIds(), UnaryOperator.identity()))),
				list(relationships, UnaryOperator.identity()),
				list(entities, e -> new Entity(e.id(), e.name(), list(e.attributes(), UnaryOperator.identity()))),
				list(entityRelations, UnaryOperator.identity()),
				list(flows, f -> new Flow(f.storyId(), f.title(), list(f.steps(), UnaryOperator.identity()))),
				list(decisions, d -> new Decision(d.id(), d.title(), d.context(), d.decision(),
						list(d.alternatives(), UnaryOperator.identity()), d.consequences(),
						list(d.requirementIds(), UnaryOperator.identity()))),
				list(nfrTactics, t -> new NfrTactic(t.nfrId(), t.tactic(), list(t.containerIds(), UnaryOperator.identity()))),
				deploymentTarget,
				list(deploymentNodes, n -> new DeploymentNode(n.id(), n.name(), n.technology(),
						list(n.containerIds(), UnaryOperator.identity()))),
				gaps, list(assumptions, UnaryOperator.identity()), list(openQuestions, UnaryOperator.identity()),
				list(warnings, UnaryOperator.identity()));
	}

	public ArchitectureModel withGaps(Gaps newGaps) {
		return new ArchitectureModel(systemName, summary, stories, nonFunctionalRequirements, actors, externalSystems,
				containers, relationships, entities, entityRelations, flows, decisions, nfrTactics, deploymentTarget,
				deploymentNodes, newGaps, assumptions, openQuestions, warnings);
	}

	private static <T> List<T> list(List<T> items, UnaryOperator<T> copy) {
		return items == null ? List.of() : items.stream().filter(Objects::nonNull).map(copy).toList();
	}

}
