package org.example.architecture;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = "spring.ai.openai.api-key=test-key")
@AutoConfigureMockMvc
class ApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void exposesHealthAndInfoOnly() throws Exception {
		mockMvc.perform(get("/actuator/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"));
		mockMvc.perform(get("/actuator"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$._links.health").exists())
				.andExpect(jsonPath("$._links.info").exists())
				.andExpect(jsonPath("$._links.env").doesNotExist())
				.andExpect(jsonPath("$._links.beans").doesNotExist());
	}

	@Test
	void documentsTheArchitectureEndpoints() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Spring Boot AI Architecture Agent"))
				.andExpect(jsonPath("$.paths['/api/architecture'].post.summary").value("Design the architecture"))
				.andExpect(jsonPath("$.paths['/api/architecture'].post.tags[0]").value("Architecture"))
				.andExpect(jsonPath("$.paths['/api/architecture/markdown'].post.summary")
						.value("Design the architecture as Markdown"))
				.andExpect(jsonPath("$.paths['/api/architecture/render'].post.summary")
						.value("Render an edited model as Markdown"));
	}

	@Test
	void mainStartsWithEitherProvider() {
		assertThatNoException().isThrownBy(() -> SpringBootAiSdlcArchitectureAgentApplication.main(new String[] {
				"--spring.main.web-application-type=none", "--spring.ai.openai.api-key=test-key" }));
		assertThatNoException().isThrownBy(() -> SpringBootAiSdlcArchitectureAgentApplication.main(new String[] {
				"--spring.main.web-application-type=none", "--AI_PROVIDER=anthropic",
				"--spring.ai.anthropic.api-key=test-key" }));
	}

}
