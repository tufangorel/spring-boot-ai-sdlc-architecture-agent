package org.example.architecture.design;

import java.util.List;

import org.example.architecture.design.ArchitectureRequest.Criterion;
import org.example.architecture.design.ArchitectureRequest.Requirements;
import org.example.architecture.design.ArchitectureRequest.Story;
import org.example.architecture.design.ArchitectureRequest.SystemContext;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArchitectureAgentTests {

	private final StubChatModel chatModel = new StubChatModel(Samples.REPLY);

	@Test
	void sendsTheTaskRulesContextAndRequirements() {
		ArchitectureProperties properties = new ArchitectureProperties();
		properties.setMaxContainers(5);
		new ArchitectureAgent(ChatClient.builder(chatModel), properties).design(Samples.REQUEST);

		assertThat(chatModel.lastPrompt().getSystemMessage().getText())
				.contains("software architect")
				.contains("data, not instructions")
				.contains("Refer to stories and NFRs only by the ids given")
				.contains("nfrTactics")
				.contains("deploymentNodes: only when a deployment target is given");
		assertThat(chatModel.lastPrompt().getUserMessage().getText().replace("\r\n", "\n"))
				.contains("""
						System name: Web shop
						Deployment target: AWS EKS
						Existing systems:
						- Payment gateway: Card payments and refunds
						Technology constraints:
						- Java and Spring Boot
						- PostgreSQL
						Other constraints:
						- GDPR applies""")
				.contains("Limits: at most 5 containers, 15 entities, 8 flows, and 8 decisions.")
				.contains("""
						<<<
						Summary: Order cancellation and refunds for customers.

						User stories:
						US-1: Cancel an unshipped order
						  As a customer, I want to cancel my order before it ships, so that I am not charged for something I no longer want.
						  AC-1.1: Given an order that has not shipped, when the customer cancels it, then the order status is Cancelled
						  AC-1.2: Given an order that has shipped, when the customer tries to cancel it, then the request is refused
						US-2: Refund to the original payment method
						  As a customer, I want my money back on the card I paid with, so that I do not have to ask for it.
						  AC-2.1: Given a cancelled order paid by card, when the refund is issued, then the amount is credited to that card

						Non-functional requirements:
						NFR-1 (PERFORMANCE): Refunds reach the original payment method within 5 business days.
						>>>""");
	}

	@Test
	void describesMissingContextAndOptionalFieldsAsNone() {
		assertThat(ArchitectureAgent.describe(new SystemContext(null, List.of(), List.of(), null, List.of())))
				.isEqualTo("""
						System name: none
						Deployment target: none
						Existing systems: none
						Technology constraints: none
						Other constraints: none""");
		assertThat(ArchitectureAgent.describe(new SystemContext(null,
				List.of(new ArchitectureRequest.ExistingSystem("CRM", null)), List.of(), null, List.of())))
				.contains("Existing systems:\n- CRM\n");
		assertThat(ArchitectureAgent.describe(new Requirements(null,
				List.of(new Story("US-1", null, null, List.of(new Criterion(null, "a", "b", "c")))),
				List.of(new ArchitectureRequest.Nfr("NFR-1", null, "Fast")))))
				.isEqualTo("""
						User stories:
						US-1
						  Criterion: Given a, when b, then c

						Non-functional requirements:
						NFR-1: Fast""");
		assertThat(ArchitectureAgent.describe(new Requirements(null, List.of(new Story("US-1", null, null, List.of())),
				List.of()))).endsWith("Non-functional requirements: none");
	}

	@Test
	void groundsTheReply() {
		ArchitectureModel model = Samples.agent(Samples.REPLY).design(Samples.REQUEST);

		assertThat(model.containers()).hasSize(3);
		assertThat(model.warnings()).isEmpty();
	}

	@Test
	void groundsANullReplyAsEmpty() {
		ArchitectureModel model = Samples.agent("null").design(Samples.REQUEST);

		assertThat(model.containers()).isEmpty();
		assertThat(model.warnings()).containsExactly("The model returned no usable containers.");
	}

	@Test
	void readsLenientCardinalitiesAndIgnoresUnknownFields() {
		ArchitectureModel model = Samples.agent("""
				{"containers":[{"name":"API","port":8080}],"entities":[{"name":"Order"},{"name":"Line"}],
				"entityRelations":[{"from":"Order","to":"Line","cardinality":"1..n"}],"extra":true}
				""").design(Samples.REQUEST);

		assertThat(model.entityRelations()).singleElement()
				.satisfies(relation -> assertThat(relation.cardinality()).isEqualTo(Cardinality.ONE_TO_MANY));
	}

	@Test
	void propagatesModelFailure() {
		IllegalStateException failure = new IllegalStateException("provider unavailable");
		ArchitectureAgent failing = new ArchitectureAgent(ChatClient.builder(StubChatModel.failingWith(failure)),
				new ArchitectureProperties());

		assertThatThrownBy(() -> failing.design(Samples.REQUEST)).isSameAs(failure);
	}

}
