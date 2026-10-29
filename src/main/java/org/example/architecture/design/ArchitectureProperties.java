package org.example.architecture.design;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "architecture")
public class ArchitectureProperties {

	private int maxRequirementCharacters = 20_000;

	private int maxContextCharacters = 4_000;

	private int maxStories = 30;

	private int maxNfrs = 30;

	private int maxContainers = 12;

	private int maxEntities = 15;

	private int maxFlows = 8;

	private int maxDecisions = 8;

	public int getMaxRequirementCharacters() {
		return maxRequirementCharacters;
	}

	public void setMaxRequirementCharacters(int maxRequirementCharacters) {
		this.maxRequirementCharacters = maxRequirementCharacters;
	}

	public int getMaxContextCharacters() {
		return maxContextCharacters;
	}

	public void setMaxContextCharacters(int maxContextCharacters) {
		this.maxContextCharacters = maxContextCharacters;
	}

	public int getMaxStories() {
		return maxStories;
	}

	public void setMaxStories(int maxStories) {
		this.maxStories = maxStories;
	}

	public int getMaxNfrs() {
		return maxNfrs;
	}

	public void setMaxNfrs(int maxNfrs) {
		this.maxNfrs = maxNfrs;
	}

	public int getMaxContainers() {
		return maxContainers;
	}

	public void setMaxContainers(int maxContainers) {
		this.maxContainers = maxContainers;
	}

	public int getMaxEntities() {
		return maxEntities;
	}

	public void setMaxEntities(int maxEntities) {
		this.maxEntities = maxEntities;
	}

	public int getMaxFlows() {
		return maxFlows;
	}

	public void setMaxFlows(int maxFlows) {
		this.maxFlows = maxFlows;
	}

	public int getMaxDecisions() {
		return maxDecisions;
	}

	public void setMaxDecisions(int maxDecisions) {
		this.maxDecisions = maxDecisions;
	}

}
