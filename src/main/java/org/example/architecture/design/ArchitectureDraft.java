package org.example.architecture.design;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * The model's raw reply. Elements refer to each other by name; {@link ArchitectureGrounding} checks every reference
 * and assigns ids before anything reaches the caller.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ArchitectureDraft(
		String summary,
		List<Element> actors,
		List<Element> externalSystems,
		List<Container> containers,
		List<Relationship> relationships,
		List<Entity> entities,
		List<EntityRelation> entityRelations,
		List<Flow> flows,
		List<Decision> decisions,
		List<NfrTactic> nfrTactics,
		List<DeploymentNode> deploymentNodes,
		List<String> assumptions,
		List<String> openQuestions) {

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Element(String name, String description) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Container(String name, String technology, String responsibility, List<String> requirementIds) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Relationship(String from, String to, String description, String protocol) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Entity(String name, List<Attribute> attributes) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Attribute(String name, String type) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record EntityRelation(String from, String to, Cardinality cardinality, String label) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Flow(String storyId, List<Step> steps) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Step(String from, String to, String message) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record Decision(String title, String context, String decision, List<String> alternatives,
			String consequences, List<String> requirementIds) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record NfrTactic(String nfrId, String tactic, List<String> containers) {
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public record DeploymentNode(String name, String technology, List<String> containers) {
	}

}
