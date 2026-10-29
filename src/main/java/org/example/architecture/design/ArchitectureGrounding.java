package org.example.architecture.design;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.example.architecture.design.ArchitectureModel.Attribute;
import org.example.architecture.design.ArchitectureModel.Container;
import org.example.architecture.design.ArchitectureModel.Decision;
import org.example.architecture.design.ArchitectureModel.DeploymentNode;
import org.example.architecture.design.ArchitectureModel.Element;
import org.example.architecture.design.ArchitectureModel.Entity;
import org.example.architecture.design.ArchitectureModel.EntityRelation;
import org.example.architecture.design.ArchitectureModel.Flow;
import org.example.architecture.design.ArchitectureModel.NfrRef;
import org.example.architecture.design.ArchitectureModel.NfrTactic;
import org.example.architecture.design.ArchitectureModel.Relationship;
import org.example.architecture.design.ArchitectureModel.Step;
import org.example.architecture.design.ArchitectureModel.StoryRef;
import org.example.architecture.design.ArchitectureRequest.Story;

final class ArchitectureGrounding {

	static final String NO_DEPLOYMENT_TARGET = "Where will the system run? Set context.deploymentTarget to get a deployment diagram.";

	private ArchitectureGrounding() {
	}

	/**
	 * @param request a request prepared by {@link ArchitectureSubmission}
	 */
	static ArchitectureModel ground(ArchitectureRequest request, ArchitectureDraft draft,
			ArchitectureProperties properties) {
		ArchitectureDraft reply = draft == null
				? new ArchitectureDraft(null, null, null, null, null, null, null, null, null, null, null, null, null)
				: draft;
		List<String> warnings = new ArrayList<>();

		Map<String, StoryRef> stories = new LinkedHashMap<>();
		for (Story story : request.requirements().stories()) {
			stories.put(key(story.id()), new StoryRef(story.id(), story.title()));
		}
		Map<String, NfrRef> nfrs = new LinkedHashMap<>();
		for (ArchitectureRequest.Nfr nfr : request.requirements().nonFunctionalRequirements()) {
			nfrs.put(key(nfr.id()), new NfrRef(nfr.id(), nfr.category(), nfr.statement()));
		}
		Map<String, String> requirementIds = new HashMap<>();
		stories.values().forEach(story -> requirementIds.put(key(story.id()), story.id()));
		nfrs.values().forEach(nfr -> requirementIds.put(key(nfr.id()), nfr.id()));

		Map<String, String> participants = new HashMap<>();
		List<Element> actors = elements(reply.actors(), "A-", "actor", participants, warnings);
		List<Element> externalSystems = elements(reply.externalSystems(), "X-", "external system", participants,
				warnings);

		Map<String, String> containerIds = new HashMap<>();
		List<Container> containers = new ArrayList<>();
		int overLimit = 0;
		for (ArchitectureDraft.Container container : nullSafe(reply.containers())) {
			if (container == null) {
				continue;
			}
			if (!hasText(container.name())) {
				warnings.add("Dropped a container: its name is missing.");
				continue;
			}
			String name = container.name().trim();
			if (participants.containsKey(key(name))) {
				warnings.add("Dropped container \"" + name + "\": its name is already used.");
				continue;
			}
			if (containers.size() >= properties.getMaxContainers()) {
				overLimit++;
				continue;
			}
			String id = "C-" + (containers.size() + 1);
			participants.put(key(name), id);
			containerIds.put(key(name), id);
			containers.add(new Container(id, name, trim(container.technology()), trim(container.responsibility()),
					requirements(container.requirementIds(), "Container \"" + name + "\"", requirementIds, warnings)));
		}
		overLimit(overLimit, "containers", properties.getMaxContainers(), warnings);
		if (containers.isEmpty()) {
			warnings.add("The model returned no usable containers.");
		}

		List<Relationship> relationships = new ArrayList<>();
		for (ArchitectureDraft.Relationship relationship : nullSafe(reply.relationships())) {
			if (relationship == null) {
				continue;
			}
			String from = participants.get(key(relationship.from()));
			String to = participants.get(key(relationship.to()));
			if (from == null || to == null) {
				warnings.add("Dropped relationship \"" + relationship.from() + "\" -> \"" + relationship.to() + "\": \""
						+ (from == null ? relationship.from() : relationship.to())
						+ "\" is not a known actor, external system, or container.");
				continue;
			}
			relationships.add(new Relationship(from, to, trim(relationship.description()), trim(relationship.protocol())));
		}

		Map<String, String> entityIds = new HashMap<>();
		List<Entity> entities = new ArrayList<>();
		overLimit = 0;
		for (ArchitectureDraft.Entity entity : nullSafe(reply.entities())) {
			if (entity == null) {
				continue;
			}
			if (!hasText(entity.name())) {
				warnings.add("Dropped an entity: its name is missing.");
				continue;
			}
			String name = entity.name().trim();
			if (entityIds.containsKey(key(name))) {
				warnings.add("Dropped entity \"" + name + "\": its name is already used.");
				continue;
			}
			if (entities.size() >= properties.getMaxEntities()) {
				overLimit++;
				continue;
			}
			String id = "E-" + (entities.size() + 1);
			entityIds.put(key(name), id);
			List<Attribute> attributes = nullSafe(entity.attributes()).stream()
					.filter(attribute -> attribute != null && hasText(attribute.name()))
					.map(attribute -> new Attribute(attribute.name().trim(), trim(attribute.type())))
					.toList();
			entities.add(new Entity(id, name, attributes));
		}
		overLimit(overLimit, "entities", properties.getMaxEntities(), warnings);

		List<EntityRelation> entityRelations = new ArrayList<>();
		for (ArchitectureDraft.EntityRelation relation : nullSafe(reply.entityRelations())) {
			if (relation == null) {
				continue;
			}
			String from = entityIds.get(key(relation.from()));
			String to = entityIds.get(key(relation.to()));
			if (from == null || to == null) {
				warnings.add("Dropped entity relation \"" + relation.from() + "\" -> \"" + relation.to() + "\": \""
						+ (from == null ? relation.from() : relation.to()) + "\" is not a known entity.");
				continue;
			}
			entityRelations.add(new EntityRelation(from, to, relation.cardinality(), trim(relation.label())));
		}

		List<Flow> flows = new ArrayList<>();
		Set<String> flowStories = new LinkedHashSet<>();
		overLimit = 0;
		for (ArchitectureDraft.Flow flow : nullSafe(reply.flows())) {
			if (flow == null) {
				continue;
			}
			StoryRef story = stories.get(key(flow.storyId()));
			if (story == null) {
				warnings.add("Dropped flow for \"" + flow.storyId() + "\": it is not a known story id.");
				continue;
			}
			if (flowStories.contains(story.id())) {
				warnings.add("Dropped a second flow for " + story.id() + ".");
				continue;
			}
			List<Step> steps = new ArrayList<>();
			for (ArchitectureDraft.Step step : nullSafe(flow.steps())) {
				if (step == null) {
					continue;
				}
				String from = participants.get(key(step.from()));
				String to = participants.get(key(step.to()));
				if (from == null || to == null) {
					warnings.add("Flow for " + story.id() + ": dropped step \"" + step.message() + "\": \""
							+ (from == null ? step.from() : step.to()) + "\" is not a known participant.");
					continue;
				}
				steps.add(new Step(from, to, trim(step.message())));
			}
			if (steps.isEmpty()) {
				warnings.add("Dropped flow for " + story.id() + ": it has no valid steps.");
				continue;
			}
			if (flows.size() >= properties.getMaxFlows()) {
				overLimit++;
				continue;
			}
			flowStories.add(story.id());
			flows.add(new Flow(story.id(), story.title(), List.copyOf(steps)));
		}
		overLimit(overLimit, "flows", properties.getMaxFlows(), warnings);

		List<Decision> decisions = new ArrayList<>();
		overLimit = 0;
		for (ArchitectureDraft.Decision decision : nullSafe(reply.decisions())) {
			if (decision == null) {
				continue;
			}
			if (!hasText(decision.title()) || !hasText(decision.decision())) {
				warnings.add("Dropped decision \"" + (hasText(decision.title()) ? decision.title().trim() : "untitled")
						+ "\": a title and a decision are both required.");
				continue;
			}
			if (decisions.size() >= properties.getMaxDecisions()) {
				overLimit++;
				continue;
			}
			String title = decision.title().trim();
			decisions.add(new Decision("ADR-" + (decisions.size() + 1), title, trim(decision.context()),
					decision.decision().trim(), distinct(decision.alternatives()), trim(decision.consequences()),
					requirements(decision.requirementIds(), "Decision \"" + title + "\"", requirementIds, warnings)));
		}
		overLimit(overLimit, "decisions", properties.getMaxDecisions(), warnings);

		List<NfrTactic> tactics = new ArrayList<>();
		for (ArchitectureDraft.NfrTactic tactic : nullSafe(reply.nfrTactics())) {
			if (tactic == null) {
				continue;
			}
			NfrRef nfr = nfrs.get(key(tactic.nfrId()));
			if (nfr == null) {
				warnings.add("Dropped tactic for \"" + tactic.nfrId() + "\": it is not a known NFR id.");
				continue;
			}
			if (!hasText(tactic.tactic())) {
				warnings.add("Dropped tactic for " + nfr.id() + ": the tactic is missing.");
				continue;
			}
			tactics.add(new NfrTactic(nfr.id(), tactic.tactic().trim(),
					references(tactic.containers(), "Tactic for " + nfr.id(), "container", containerIds, warnings)));
		}

		List<String> openQuestions = new ArrayList<>(nullSafe(reply.openQuestions()));
		String deploymentTarget = request.context().deploymentTarget();
		List<DeploymentNode> deploymentNodes = new ArrayList<>();
		if (deploymentTarget == null) {
			long ignored = nullSafe(reply.deploymentNodes()).stream().filter(Objects::nonNull).count();
			if (ignored > 0) {
				warnings.add("Ignored " + ignored + " deployment " + (ignored == 1 ? "node" : "nodes")
						+ ": no deployment target was given.");
			}
			openQuestions.add(NO_DEPLOYMENT_TARGET);
		}
		else {
			Set<String> nodeNames = new LinkedHashSet<>();
			for (ArchitectureDraft.DeploymentNode node : nullSafe(reply.deploymentNodes())) {
				if (node == null) {
					continue;
				}
				if (!hasText(node.name())) {
					warnings.add("Dropped a deployment node: its name is missing.");
					continue;
				}
				String name = node.name().trim();
				if (!nodeNames.add(key(name))) {
					warnings.add("Dropped deployment node \"" + name + "\": its name is already used.");
					continue;
				}
				deploymentNodes.add(new DeploymentNode("D-" + (deploymentNodes.size() + 1), name, trim(node.technology()),
						references(node.containers(), "Deployment node \"" + name + "\"", "container", containerIds,
								warnings)));
			}
		}

		String systemName = request.context().systemName() == null ? "System" : request.context().systemName();
		String summary = hasText(reply.summary()) ? reply.summary().trim()
				: "Architecture for " + systemName + " with " + containers.size() + " "
						+ (containers.size() == 1 ? "container" : "containers") + ".";
		ArchitectureModel model = new ArchitectureModel(systemName, summary, List.copyOf(stories.values()),
				List.copyOf(nfrs.values()), actors, externalSystems, List.copyOf(containers),
				List.copyOf(relationships), List.copyOf(entities), List.copyOf(entityRelations), List.copyOf(flows),
				List.copyOf(decisions), List.copyOf(tactics), deploymentTarget, List.copyOf(deploymentNodes), null,
				distinct(reply.assumptions()), distinct(openQuestions), List.copyOf(warnings));
		return model.withGaps(ArchitectureGaps.compute(model));
	}

	private static List<Element> elements(List<ArchitectureDraft.Element> drafts, String prefix, String kind,
			Map<String, String> participants, List<String> warnings) {
		List<Element> kept = new ArrayList<>();
		for (ArchitectureDraft.Element element : nullSafe(drafts)) {
			if (element == null) {
				continue;
			}
			if (!hasText(element.name())) {
				warnings.add("Dropped an " + kind + ": its name is missing.");
				continue;
			}
			String name = element.name().trim();
			if (participants.containsKey(key(name))) {
				warnings.add("Dropped " + kind + " \"" + name + "\": its name is already used.");
				continue;
			}
			String id = prefix + (kept.size() + 1);
			participants.put(key(name), id);
			kept.add(new Element(id, name, trim(element.description())));
		}
		return List.copyOf(kept);
	}

	private static List<String> requirements(List<String> ids, String owner, Map<String, String> known,
			List<String> warnings) {
		return references(ids, owner, "requirement id", known, warnings);
	}

	private static List<String> references(List<String> names, String owner, String kind, Map<String, String> known,
			List<String> warnings) {
		Set<String> resolved = new LinkedHashSet<>();
		for (String name : nullSafe(names)) {
			if (!hasText(name)) {
				continue;
			}
			String id = known.get(key(name));
			if (id == null) {
				warnings.add(owner + ": ignored unknown " + kind + " \"" + name.trim() + "\".");
				continue;
			}
			resolved.add(id);
		}
		return List.copyOf(resolved);
	}

	private static void overLimit(int count, String noun, int limit, List<String> warnings) {
		if (count > 0) {
			warnings.add("Dropped " + count + " more " + noun + " over the limit of " + limit + ".");
		}
	}

	private static List<String> distinct(List<String> items) {
		Set<String> seen = new LinkedHashSet<>();
		List<String> result = new ArrayList<>();
		for (String item : nullSafe(items)) {
			if (hasText(item) && seen.add(key(item))) {
				result.add(item.trim());
			}
		}
		return List.copyOf(result);
	}

	static String key(String text) {
		return text == null ? "" : text.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
	}

	private static String trim(String text) {
		return hasText(text) ? text.trim() : null;
	}

	private static boolean hasText(String text) {
		return text != null && !text.isBlank();
	}

	private static <T> List<T> nullSafe(List<T> items) {
		return items == null ? List.of() : items;
	}

}
