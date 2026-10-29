package org.example.architecture.design;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = { "spring.ai.openai.api-key=test-key", "architecture.max-stories=2" })
@AutoConfigureMockMvc
class ArchitectureIntegrationTests {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private StubChatModel chatModel;

	@BeforeEach
	void clearPrompts() {
		chatModel.prompts().clear();
	}

	@Test
	void designsTheArchitectureEndToEnd() throws Exception {
		mockMvc.perform(post("/api/architecture").contentType(MediaType.APPLICATION_JSON).content(Samples.REQUEST_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.containers", hasSize(3)))
				.andExpect(jsonPath("$.flows[0].title").value("Cancel an unshipped order"))
				.andExpect(jsonPath("$.gaps.storiesWithoutFlow[0]").value("US-2"))
				.andExpect(jsonPath("$.warnings", hasSize(0)));

		assertThat(chatModel.lastPrompt().getUserMessage().getText())
				.contains("Deployment target: AWS EKS")
				.contains("NFR-1 (PERFORMANCE)");
	}

	@Test
	void rendersMarkdownEndToEnd() throws Exception {
		mockMvc.perform(post("/api/architecture/markdown").contentType(MediaType.APPLICATION_JSON)
						.content(Samples.REQUEST_JSON))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("## Deployment: AWS EKS")))
				.andExpect(content().string(containsString("- **Stories without a flow:** US-2")));
	}

	@Test
	void rejectsTooManyStoriesWithoutCallingTheModel() throws Exception {
		mockMvc.perform(post("/api/architecture").contentType(MediaType.APPLICATION_JSON).content("""
						{"requirements":{"stories":[{"id":"US-1"},{"id":"US-2"},{"id":"US-3"}]}}
						"""))
				.andExpect(status().isBadRequest());

		assertThat(chatModel.prompts()).isEmpty();
	}

	@TestConfiguration
	static class StubChatModelConfig {

		@Bean
		@Primary
		StubChatModel stubChatModel() {
			return new StubChatModel(Samples.REPLY);
		}

	}

}
