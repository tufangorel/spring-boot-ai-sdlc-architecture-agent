package org.example.architecture.design;

import java.util.List;

import org.example.architecture.design.ArchitectureRequest.Criterion;
import org.example.architecture.design.ArchitectureRequest.ExistingSystem;
import org.example.architecture.design.ArchitectureRequest.Nfr;
import org.example.architecture.design.ArchitectureRequest.Requirements;
import org.example.architecture.design.ArchitectureRequest.Story;
import org.example.architecture.design.ArchitectureRequest.SystemContext;
import org.springframework.ai.chat.client.ChatClient;

public final class Samples {

	public static final ArchitectureRequest REQUEST = new ArchitectureRequest(
			new Requirements("Order cancellation and refunds for customers.",
					List.of(new Story("US-1", "Cancel an unshipped order",
							"As a customer, I want to cancel my order before it ships, so that I am not charged for something I no longer want.",
							List.of(new Criterion("AC-1.1", "an order that has not shipped", "the customer cancels it",
									"the order status is Cancelled"),
									new Criterion("AC-1.2", "an order that has shipped", "the customer tries to cancel it",
											"the request is refused"))),
							new Story("US-2", "Refund to the original payment method",
									"As a customer, I want my money back on the card I paid with, so that I do not have to ask for it.",
									List.of(new Criterion("AC-2.1", "a cancelled order paid by card", "the refund is issued",
											"the amount is credited to that card")))),
					List.of(new Nfr("NFR-1", "PERFORMANCE",
							"Refunds reach the original payment method within 5 business days."))),
			new SystemContext("Web shop", List.of(new ExistingSystem("Payment gateway", "Card payments and refunds")),
					List.of("Java and Spring Boot", "PostgreSQL"), "AWS EKS", List.of("GDPR applies")));

	public static final String REQUEST_JSON = """
			{
			  "requirements": {
			    "summary": "Order cancellation and refunds for customers.",
			    "stories": [
			      {
			        "id": "US-1",
			        "title": "Cancel an unshipped order",
			        "story": "As a customer, I want to cancel my order before it ships, so that I am not charged for something I no longer want.",
			        "acceptanceCriteria": [
			          { "id": "AC-1.1", "given": "an order that has not shipped", "when": "the customer cancels it", "then": "the order status is Cancelled" },
			          { "id": "AC-1.2", "given": "an order that has shipped", "when": "the customer tries to cancel it", "then": "the request is refused" }
			        ]
			      },
			      {
			        "id": "US-2",
			        "title": "Refund to the original payment method",
			        "story": "As a customer, I want my money back on the card I paid with, so that I do not have to ask for it.",
			        "acceptanceCriteria": [
			          { "id": "AC-2.1", "given": "a cancelled order paid by card", "when": "the refund is issued", "then": "the amount is credited to that card" }
			        ]
			      }
			    ],
			    "nonFunctionalRequirements": [
			      { "id": "NFR-1", "category": "PERFORMANCE", "statement": "Refunds reach the original payment method within 5 business days." }
			    ]
			  },
			  "context": {
			    "systemName": "Web shop",
			    "existingSystems": [ { "name": "Payment gateway", "description": "Card payments and refunds" } ],
			    "technologyConstraints": [ "Java and Spring Boot", "PostgreSQL" ],
			    "deploymentTarget": "AWS EKS",
			    "constraints": [ "GDPR applies" ]
			  }
			}
			""";

	public static final String REPLY = """
			{
			  "summary": "A web app calls a Spring Boot order service that stores orders in PostgreSQL and refunds through the payment gateway.",
			  "actors": [ { "name": "Customer", "description": "Buys and cancels orders" } ],
			  "externalSystems": [ { "name": "Payment gateway", "description": "Card payments and refunds" } ],
			  "containers": [
			    { "name": "Web app", "technology": "React", "responsibility": "Order pages for customers", "requirementIds": ["US-1"] },
			    { "name": "Order service", "technology": "Spring Boot", "responsibility": "Cancels orders and issues refunds", "requirementIds": ["US-1", "US-2", "NFR-1"] },
			    { "name": "Order database", "technology": "PostgreSQL", "responsibility": "Stores orders and refunds", "requirementIds": ["US-1", "US-2"] }
			  ],
			  "relationships": [
			    { "from": "Customer", "to": "Web app", "description": "Cancels orders", "protocol": "HTTPS" },
			    { "from": "Web app", "to": "Order service", "description": "Calls the order API", "protocol": "HTTPS/JSON" },
			    { "from": "Order service", "to": "Order database", "description": "Reads and writes orders", "protocol": "JDBC" },
			    { "from": "Order service", "to": "Payment gateway", "description": "Issues refunds", "protocol": "HTTPS" }
			  ],
			  "entities": [
			    { "name": "Order", "attributes": [ { "name": "id", "type": "UUID" }, { "name": "status", "type": "OrderStatus" } ] },
			    { "name": "Refund", "attributes": [ { "name": "id", "type": "UUID" }, { "name": "amount", "type": "Money" } ] }
			  ],
			  "entityRelations": [ { "from": "Order", "to": "Refund", "cardinality": "ONE_TO_MANY", "label": "is refunded by" } ],
			  "flows": [
			    {
			      "storyId": "US-1",
			      "steps": [
			        { "from": "Customer", "to": "Web app", "message": "Cancel order" },
			        { "from": "Web app", "to": "Order service", "message": "POST /orders/{id}/cancel" },
			        { "from": "Order service", "to": "Order database", "message": "Set status to Cancelled" }
			      ]
			    }
			  ],
			  "decisions": [
			    {
			      "title": "Issue refunds asynchronously",
			      "context": "The payment gateway can be slow or unavailable, and refunds must arrive within 5 business days.",
			      "decision": "The order service records a pending refund and a scheduled job sends it to the payment gateway, retrying on failure.",
			      "alternatives": ["Refund synchronously during cancellation"],
			      "consequences": "Cancellation stays fast; refunds need monitoring.",
			      "requirementIds": ["US-2", "NFR-1"]
			    }
			  ],
			  "nfrTactics": [
			    { "nfrId": "NFR-1", "tactic": "Retry failed refunds with a scheduled job and alert after 3 days", "containers": ["Order service"] }
			  ],
			  "deploymentNodes": [
			    { "name": "EKS cluster", "technology": "Kubernetes", "containers": ["Web app", "Order service"] },
			    { "name": "Amazon RDS", "technology": "PostgreSQL 16", "containers": ["Order database"] }
			  ],
			  "assumptions": ["Refunds are only issued to cards"],
			  "openQuestions": ["Who is alerted when a refund keeps failing?"]
			}
			""";

	private Samples() {
	}

	/** The model the agent builds for {@link #REQUEST} when the chat model answers with {@link #REPLY}. */
	public static ArchitectureModel model() {
		return agent(REPLY).design(REQUEST);
	}

	static ArchitectureAgent agent(String reply) {
		return new ArchitectureAgent(ChatClient.builder(new StubChatModel(reply)), new ArchitectureProperties());
	}

}
