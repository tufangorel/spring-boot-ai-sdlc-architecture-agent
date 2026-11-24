package org.example.architecture.design;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.structurizr.Workspace;
import org.example.architecture.StructurizrDslParserTestSupport;
import org.example.architecture.render.ArchitectureMarkdown;
import org.example.architecture.render.StructurizrDslRenderer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.assertj.core.api.Assertions.assertThat;

/** Keeps the documentation samples in step with the code. */
class ReadmeTests {

	@TempDir
	Path tempDir;

	@Test
	void readmeShowsTheTestedSamples() throws IOException {
		assertThat(read("README.md"))
				.contains("Structurizr DSL 6.2.3 parser in test scope (validates generated DSL workspaces)")
				.contains("generated Structurizr DSL is parsed with the official Java parser")
				.contains("```json\n" + Samples.REQUEST_JSON + "```\n")
				.contains("```json\n" + ArchitectureControllerTests.README_MODEL + "```\n")
				.contains("````markdown\n" + ArchitectureControllerTests.README_MARKDOWN + "````\n");
	}

	@Test
	void sampleDocumentIsTheRenderedSampleModel() throws IOException {
		String doc = read("docs/sample-architecture.md");
		assertThat(doc).startsWith(ArchitectureMarkdown.render(Samples.model()));
		assertThat(doc).contains("## Structurizr DSL\n\n```dsl\n" + StructurizrDslRenderer.render(Samples.model()) + "```");
	}

	@Test
	void sampleStructurizrDocumentIsTheRenderedSampleModelAndParses() throws Exception {
		String dsl = read("docs/sample-architecture.dsl");
		assertThat(dsl).isEqualTo(StructurizrDslRenderer.render(Samples.model()));

		Workspace workspace = StructurizrDslParserTestSupport.parse(dsl, tempDir);
		assertThat(workspace.getModel().getSoftwareSystemWithName("Web shop")).isNotNull();
		assertThat(workspace.getModel().getRelationships()).extracting("description")
				.contains("Cancels orders", "Calls the order API", "Reads and writes orders", "Issues refunds");
		assertThat(workspace.getViews().getDeploymentViews()).hasSize(1);
		assertThat(workspace.getViews().getDynamicViews()).hasSize(1);
	}

	private static String read(String file) throws IOException {
		return Files.readString(Path.of(file)).replace("\r\n", "\n");
	}

}
