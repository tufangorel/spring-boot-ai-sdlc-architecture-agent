package org.example.architecture.render;

import java.util.List;

import org.example.architecture.design.ArchitectureModel;
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
import org.example.architecture.design.ArchitectureModel.StoryRef;

/** Builds hand-made models for renderer tests. Every list starts empty. */
final class ModelBuilder {

	String systemName = "Shop";

	String summary;

	List<StoryRef> stories = List.of();

	List<NfrRef> nfrs = List.of();

	List<Element> actors = List.of();

	List<Element> externalSystems = List.of();

	List<Container> containers = List.of();

	List<Relationship> relationships = List.of();

	List<Entity> entities = List.of();

	List<EntityRelation> entityRelations = List.of();

	List<Flow> flows = List.of();

	List<Decision> decisions = List.of();

	List<NfrTactic> tactics = List.of();

	String deploymentTarget;

	List<DeploymentNode> deploymentNodes = List.of();

	List<String> assumptions = List.of();

	List<String> openQuestions = List.of();

	List<String> warnings = List.of();

	ArchitectureModel build() {
		return new ArchitectureModel(systemName, summary, stories, nfrs, actors, externalSystems, containers,
				relationships, entities, entityRelations, flows, decisions, tactics, deploymentTarget, deploymentNodes,
				null, assumptions, openQuestions, warnings);
	}

	static Container container(String id, String name) {
		return new Container(id, name, null, null, List.of());
	}

}
