package org.example.architecture.design;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.example.architecture.render.ArchitectureMarkdown;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/architecture")
@Tag(name = "Architecture", description = "Turn reviewed user stories and NFRs into architecture artifacts")
public class ArchitectureController {

	static final String TEXT_MARKDOWN = "text/markdown";

	private final ArchitectureAgent agent;

	private final ArchitectureSubmission submission;

	public ArchitectureController(ArchitectureAgent agent, ArchitectureSubmission submission) {
		this.agent = agent;
		this.submission = submission;
	}

	@Operation(summary = "Design the architecture",
			description = "Returns a validated architecture model as JSON: actors, external systems, containers, relationships, entities, flows, decisions, NFR tactics, deployment nodes, and the gaps the design leaves open.")
	@PostMapping
	public ArchitectureModel design(@RequestBody ArchitectureRequest request) {
		return agent.design(submission.prepare(request));
	}

	@Operation(summary = "Design the architecture as Markdown",
			description = "Returns the same model as one Markdown document with C4 context and container diagrams, sequence diagrams, a domain model, a deployment diagram, ADRs, and an NFR traceability matrix. Diagrams are Mermaid.")
	@PostMapping(path = "/markdown", produces = TEXT_MARKDOWN)
	public String designMarkdown(@RequestBody ArchitectureRequest request) {
		return ArchitectureMarkdown.render(design(request));
	}

	@Operation(summary = "Render an edited model as Markdown",
			description = "Takes a model returned by this API, after an architect has reviewed or edited it, and renders it as Markdown. Gaps are worked out again from the edited model. The model is not called.")
	@PostMapping(path = "/render", produces = TEXT_MARKDOWN)
	public String render(@Valid @RequestBody ArchitectureModel model) {
		return ArchitectureMarkdown.render(model.normalized());
	}

}
