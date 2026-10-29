package org.example.architecture.design;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.example.architecture.design.ArchitectureModel.Container;
import org.example.architecture.design.ArchitectureModel.DeploymentNode;
import org.example.architecture.design.ArchitectureModel.Flow;
import org.example.architecture.design.ArchitectureModel.Gaps;
import org.example.architecture.design.ArchitectureModel.NfrRef;
import org.example.architecture.design.ArchitectureModel.NfrTactic;
import org.example.architecture.design.ArchitectureModel.StoryRef;

/**
 * Works out what a model leaves open. It only reads the model, so it gives the same answer for a freshly generated
 * model and for one an architect has edited.
 */
public final class ArchitectureGaps {

	private ArchitectureGaps() {
	}

	/**
	 * @param model a {@linkplain ArchitectureModel#normalized() normalized} model
	 */
	public static Gaps compute(ArchitectureModel model) {
		List<String> untraced = model.containers().stream()
				.filter(container -> container.requirementIds().isEmpty())
				.map(Container::id)
				.toList();
		Set<String> tactics = new HashSet<>();
		for (NfrTactic tactic : model.nfrTactics()) {
			tactics.add(tactic.nfrId());
		}
		List<String> unaddressed = model.nonFunctionalRequirements().stream()
				.map(NfrRef::id)
				.filter(id -> !tactics.contains(id))
				.toList();
		Set<String> flows = new HashSet<>();
		for (Flow flow : model.flows()) {
			flows.add(flow.storyId());
		}
		List<String> withoutFlow = model.stories().stream()
				.map(StoryRef::id)
				.filter(id -> !flows.contains(id))
				.toList();
		List<String> unhosted = List.of();
		if (!model.deploymentNodes().isEmpty()) {
			Set<String> hosted = new HashSet<>();
			for (DeploymentNode node : model.deploymentNodes()) {
				hosted.addAll(node.containerIds());
			}
			unhosted = model.containers().stream()
					.map(Container::id)
					.filter(id -> !hosted.contains(id))
					.toList();
		}
		return new Gaps(untraced, unaddressed, withoutFlow, unhosted);
	}

}
