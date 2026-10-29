package org.example.architecture.design;

import java.util.Arrays;
import java.util.List;

import org.example.architecture.design.ArchitectureRequest.Criterion;
import org.example.architecture.design.ArchitectureRequest.ExistingSystem;
import org.example.architecture.design.ArchitectureRequest.Nfr;
import org.example.architecture.design.ArchitectureRequest.Requirements;
import org.example.architecture.design.ArchitectureRequest.Story;
import org.example.architecture.design.ArchitectureRequest.SystemContext;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ArchitectureSubmissionTests {

	private static final Story STORY = new Story("US-1", "Cancel", null, null);

	private final ArchitectureProperties properties = new ArchitectureProperties();

	private final ArchitectureSubmission submission = new ArchitectureSubmission(properties);

	@Test
	void keepsAPreparedRequestAsItIs() {
		assertThat(submission.prepare(Samples.REQUEST)).isEqualTo(Samples.REQUEST);
	}

	@Test
	void trimsTextAndRemovesEmptyEntries() {
		ArchitectureRequest request = new ArchitectureRequest(
				new Requirements("  ", Arrays.asList(null,
						new Story(" US-1 ", " Cancel ", " ", Arrays.asList(null, new Criterion(" AC-1.1 ", " a ", " b ", " ")))),
						Arrays.asList(null, new Nfr(" NFR-1 ", " ", " Fast "))),
				new SystemContext(" ", Arrays.asList(null, new ExistingSystem(" ", "x"), new ExistingSystem(null, "y"),
						new ExistingSystem(" Bank ", " "), new ExistingSystem("CRM", " Customers ")),
						Arrays.asList(null, " ", " Java "), " ", null));

		ArchitectureRequest prepared = submission.prepare(request);

		assertThat(prepared).isEqualTo(new ArchitectureRequest(
				new Requirements(null,
						List.of(new Story("US-1", "Cancel", null, List.of(new Criterion("AC-1.1", "a", "b", null)))),
						List.of(new Nfr("NFR-1", null, "Fast"))),
				new SystemContext(null, List.of(new ExistingSystem("Bank", null), new ExistingSystem("CRM", "Customers")),
						List.of("Java"), null, List.of())));
	}

	@Test
	void treatsAMissingContextAsEmpty() {
		ArchitectureRequest prepared = submission.prepare(
				new ArchitectureRequest(new Requirements(null, List.of(STORY), null), null));

		assertThat(prepared.context()).isEqualTo(new SystemContext(null, List.of(), List.of(), null, List.of()));
		assertThat(prepared.requirements().nonFunctionalRequirements()).isEmpty();
		assertThat(prepared.requirements().stories().getFirst().acceptanceCriteria()).isEmpty();
	}

	@Test
	void rejectsMissingRequirementsOrStories() {
		assertRejected(new ArchitectureRequest(null, null), "requirements is required");
		assertRejected(new ArchitectureRequest(new Requirements("x", null, null), null), "At least one story is required");
		assertRejected(new ArchitectureRequest(new Requirements("x", Arrays.asList((Story) null), null), null),
				"At least one story is required");
	}

	@Test
	void rejectsMissingOrDuplicateIds() {
		assertRejected(requirements(List.of(new Story(" ", "x", null, null)), List.of()), "Every story needs an id");
		assertRejected(requirements(List.of(STORY), List.of(new Nfr(null, null, "Fast"))),
				"Every non-functional requirement needs an id");
		assertRejected(requirements(List.of(STORY, new Story("us-1", "x", null, null)), List.of()),
				"Id us-1 is used more than once");
		assertRejected(requirements(List.of(STORY), List.of(new Nfr("US-1", null, "Fast"))),
				"Id US-1 is used more than once");
	}

	@Test
	void rejectsAnNfrWithoutAStatement() {
		assertRejected(requirements(List.of(STORY), List.of(new Nfr("NFR-1", "SECURITY", " "))),
				"Non-functional requirement NFR-1 needs a statement");
	}

	@Test
	void enforcesTheCountLimits() {
		properties.setMaxStories(1);
		properties.setMaxNfrs(1);

		assertRejected(requirements(List.of(STORY, new Story("US-2", null, null, null)), List.of()),
				"At most 1 stories are allowed");
		assertRejected(requirements(List.of(STORY), List.of(new Nfr("NFR-1", null, "a"), new Nfr("NFR-2", null, "b"))),
				"At most 1 non-functional requirements are allowed");
	}

	@Test
	void enforcesTheCharacterLimits() {
		properties.setMaxRequirementCharacters(Samples.REQUEST.toString().length());
		properties.setMaxContextCharacters(10);
		assertRejected(Samples.REQUEST, "Context exceeds 10 characters");

		properties.setMaxRequirementCharacters(20);
		assertRejected(Samples.REQUEST, "Requirements exceed 20 characters");

		properties.setMaxRequirementCharacters(18);
		properties.setMaxContextCharacters(0);
		ArchitectureRequest exact = new ArchitectureRequest(new Requirements("1234",
				List.of(new Story("US-1", "Ab", "C", List.of(new Criterion("A", "b", "c", "d")))),
				List.of(new Nfr("N", "P", "S"))), null);
		assertThat(submission.prepare(exact)).isNotNull();
		properties.setMaxRequirementCharacters(17);
		assertRejected(exact, "Requirements exceed 17 characters");
	}

	private static ArchitectureRequest requirements(List<Story> stories, List<Nfr> nfrs) {
		return new ArchitectureRequest(new Requirements(null, stories, nfrs), null);
	}

	private void assertRejected(ArchitectureRequest request, String message) {
		assertThatThrownBy(() -> submission.prepare(request))
				.isInstanceOf(ResponseStatusException.class)
				.hasMessageContaining(message)
				.extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
				.isEqualTo(HttpStatus.BAD_REQUEST);
	}

}
