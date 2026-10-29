package org.example.architecture.design;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ArchitectureController.class)
class ArchitectureControllerTests {

	static final String README_MODEL = """
			{
			  "systemName": "Web shop",
			  "summary": "Order cancellation with refunds retried by a worker.",
			  "stories": [
			    { "id": "US-1", "title": "Cancel an unshipped order" },
			    { "id": "US-2", "title": "Refund to the original payment method" }
			  ],
			  "nonFunctionalRequirements": [
			    { "id": "NFR-1", "category": "PERFORMANCE", "statement": "Refunds reach the original payment method within 5 business days." }
			  ],
			  "actors": [ { "id": "A-1", "name": "Customer" } ],
			  "externalSystems": [ { "id": "X-1", "name": "Payment gateway" } ],
			  "containers": [
			    { "id": "C-1", "name": "Order service", "technology": "Spring Boot", "responsibility": "Cancels orders and issues refunds", "requirementIds": ["US-1", "US-2"] },
			    { "id": "C-2", "name": "Refund worker", "technology": "Spring Batch", "responsibility": "Retries failed refunds", "requirementIds": ["NFR-1"] }
			  ],
			  "relationships": [
			    { "from": "A-1", "to": "C-1", "description": "Cancels orders", "protocol": "HTTPS" },
			    { "from": "C-1", "to": "C-2", "description": "Queues refunds", "protocol": "AMQP" },
			    { "from": "C-2", "to": "X-1", "description": "Issues refunds", "protocol": "HTTPS" }
			  ],
			  "flows": [
			    {
			      "storyId": "US-2",
			      "title": "Refund to the original payment method",
			      "steps": [
			        { "from": "C-1", "to": "C-2", "message": "Queue refund" },
			        { "from": "C-2", "to": "X-1", "message": "Refund card" }
			      ]
			    }
			  ],
			  "nfrTactics": [
			    { "nfrId": "NFR-1", "tactic": "Refund worker retries failed refunds every hour", "containerIds": ["C-2"] }
			  ]
			}
			""";

	static final String README_MARKDOWN = """
			# Architecture: Web shop

			Order cancellation with refunds retried by a worker.

			## System context

			```mermaid
			flowchart TB
			    nA1["Customer<br/>[Person]"]
			    system["Web shop<br/>[Software System]"]
			    nX1["Payment gateway<br/>[External System]"]
			    nA1 -->|"Cancels orders<br/>[HTTPS]"| system
			    system -->|"Issues refunds<br/>[HTTPS]"| nX1
			```

			## Containers

			```mermaid
			flowchart TB
			    nA1["Customer<br/>[Person]"]
			    subgraph system ["Web shop"]
			        nC1["Order service<br/>[Container: Spring Boot]<br/>Cancels orders and issues refunds"]
			        nC2["Refund worker<br/>[Container: Spring Batch]<br/>Retries failed refunds"]
			    end
			    nX1["Payment gateway<br/>[External System]"]
			    nA1 -->|"Cancels orders<br/>[HTTPS]"| nC1
			    nC1 -->|"Queues refunds<br/>[AMQP]"| nC2
			    nC2 -->|"Issues refunds<br/>[HTTPS]"| nX1
			```

			| Id | Container | Technology | Responsibility | Requirements |
			|----|-----------|------------|----------------|--------------|
			| C-1 | Order service | Spring Boot | Cancels orders and issues refunds | US-1, US-2 |
			| C-2 | Refund worker | Spring Batch | Retries failed refunds | NFR-1 |

			## Key flows

			### US-2: Refund to the original payment method

			```mermaid
			sequenceDiagram
			    participant nC1 as Order service
			    participant nC2 as Refund worker
			    participant nX1 as Payment gateway
			    nC1->>nC2: Queue refund
			    nC2->>nX1: Refund card
			```

			## NFR traceability

			| NFR | Category | Statement | Tactic | Containers |
			|-----|----------|-----------|--------|------------|
			| NFR-1 | PERFORMANCE | Refunds reach the original payment method within 5 business days. | Refund worker retries failed refunds every hour | Refund worker |

			## Gaps

			- **Stories without a flow:** US-1
			""";

	@Autowired
	private MockMvc mockMvc;

	@MockitoBean
	private ArchitectureAgent agent;

	@MockitoBean
	private ArchitectureSubmission submission;

	@Test
	void returnsTheModelAsJson() throws Exception {
		given(submission.prepare(Samples.REQUEST)).willReturn(Samples.REQUEST);
		given(agent.design(Samples.REQUEST)).willReturn(Samples.model());

		mockMvc.perform(post("/api/architecture").contentType(MediaType.APPLICATION_JSON).content(Samples.REQUEST_JSON))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.systemName").value("Web shop"))
				.andExpect(jsonPath("$.containers[1].id").value("C-2"))
				.andExpect(jsonPath("$.containers[1].requirementIds[2]").value("NFR-1"))
				.andExpect(jsonPath("$.relationships[3].to").value("X-1"))
				.andExpect(jsonPath("$.entityRelations[0].cardinality").value("ONE_TO_MANY"))
				.andExpect(jsonPath("$.flows[0].steps[1].message").value("POST /orders/{id}/cancel"))
				.andExpect(jsonPath("$.decisions[0].id").value("ADR-1"))
				.andExpect(jsonPath("$.nfrTactics[0].containerIds[0]").value("C-2"))
				.andExpect(jsonPath("$.deploymentNodes[1].containerIds[0]").value("C-3"))
				.andExpect(jsonPath("$.gaps.storiesWithoutFlow[0]").value("US-2"))
				.andExpect(jsonPath("$.warnings").isEmpty());
	}

	@Test
	void returnsTheModelAsMarkdown() throws Exception {
		given(submission.prepare(Samples.REQUEST)).willReturn(Samples.REQUEST);
		given(agent.design(Samples.REQUEST)).willReturn(Samples.model());

		mockMvc.perform(post("/api/architecture/markdown").contentType(MediaType.APPLICATION_JSON)
						.content(Samples.REQUEST_JSON))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/markdown"))
				.andExpect(content().string(containsString("# Architecture: Web shop")))
				.andExpect(content().string(containsString("### ADR-1: Issue refunds asynchronously")));
	}

	@Test
	void returnsTheSubmissionErrorAsBadRequest() throws Exception {
		given(submission.prepare(any())).willThrow(
				new ResponseStatusException(HttpStatus.BAD_REQUEST, "At least one story is required"));

		mockMvc.perform(post("/api/architecture").contentType(MediaType.APPLICATION_JSON).content("{}"))
				.andExpect(status().isBadRequest());
		verifyNoInteractions(agent);
	}

	@Test
	void rendersTheReadmeModelWithoutCallingTheAgent() throws Exception {
		mockMvc.perform(post("/api/architecture/render").contentType(MediaType.APPLICATION_JSON).content(README_MODEL))
				.andExpect(status().isOk())
				.andExpect(content().contentTypeCompatibleWith("text/markdown"))
				.andExpect(content().string(README_MARKDOWN));

		verifyNoInteractions(agent, submission);
	}

	@Test
	void rendersAModelWithNullListsAndEntries() throws Exception {
		mockMvc.perform(post("/api/architecture/render").contentType(MediaType.APPLICATION_JSON).content("""
						{"containers":[null,{"id":"C-1","name":"API"}],"flows":[{"storyId":"US-1","steps":null}],
						"decisions":[{"id":"ADR-1","title":"Use REST","decision":"REST","alternatives":null}],"warnings":[null]}
						"""))
				.andExpect(status().isOk())
				.andExpect(content().string(containsString("### ADR-1: Use REST")))
				.andExpect(content().string(containsString("- **Containers not traced to a requirement:** C-1 (API)")));
	}

	@Test
	void rejectsAModelWithoutContainers() throws Exception {
		for (String body : new String[] { "{\"systemName\":\"Shop\"}", "{\"containers\":[]}", "{\"containers\":" }) {
			mockMvc.perform(post("/api/architecture/render").contentType(MediaType.APPLICATION_JSON).content(body))
					.andExpect(status().isBadRequest());
		}

		verifyNoInteractions(agent, submission);
	}

}
