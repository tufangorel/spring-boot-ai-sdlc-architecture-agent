package org.example.architecture.design;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

public record ArchitectureRequest(
		@Schema(description = "The reviewed result of the requirements agent. Only summary, stories, and nonFunctionalRequirements are read; other fields are ignored.")
		Requirements requirements,
		@Schema(description = "What the requirements do not say: the system name, existing systems, technology and other constraints, and the deployment target")
		SystemContext context) {

	public record Requirements(String summary, List<Story> stories, List<Nfr> nonFunctionalRequirements) {
	}

	public record Story(String id, String title, String story, List<Criterion> acceptanceCriteria) {
	}

	public record Criterion(String id, String given, String when, String then) {
	}

	public record Nfr(String id, String category, String statement) {
	}

	public record SystemContext(
			@Schema(example = "Order Service") String systemName,
			List<ExistingSystem> existingSystems,
			@Schema(example = "[\"Java 25 and Spring Boot\", \"PostgreSQL\"]") List<String> technologyConstraints,
			@Schema(description = "Where the system will run. Without it, no deployment diagram is produced.", example = "AWS EKS")
			String deploymentTarget,
			List<String> constraints) {
	}

	public record ExistingSystem(String name, String description) {
	}

}
