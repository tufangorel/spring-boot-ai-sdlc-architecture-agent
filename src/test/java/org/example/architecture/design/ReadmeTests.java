package org.example.architecture.design;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.example.architecture.render.ArchitectureMarkdown;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/** Keeps the documentation samples in step with the code. */
class ReadmeTests {

	@Test
	void readmeShowsTheTestedSamples() throws IOException {
		assertThat(read("README.md"))
				.contains("```json\n" + Samples.REQUEST_JSON + "```\n")
				.contains("```json\n" + ArchitectureControllerTests.README_MODEL + "```\n")
				.contains("````markdown\n" + ArchitectureControllerTests.README_MARKDOWN + "````\n");
	}

	@Test
	void sampleDocumentIsTheRenderedSampleModel() throws IOException {
		assertThat(read("docs/sample-architecture.md")).isEqualTo(ArchitectureMarkdown.render(Samples.model()));
	}

	private static String read(String file) throws IOException {
		return Files.readString(Path.of(file)).replace("\r\n", "\n");
	}

}
