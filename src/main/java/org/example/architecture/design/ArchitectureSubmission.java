package org.example.architecture.design;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

import org.example.architecture.design.ArchitectureRequest.Criterion;
import org.example.architecture.design.ArchitectureRequest.ExistingSystem;
import org.example.architecture.design.ArchitectureRequest.Nfr;
import org.example.architecture.design.ArchitectureRequest.Requirements;
import org.example.architecture.design.ArchitectureRequest.Story;
import org.example.architecture.design.ArchitectureRequest.SystemContext;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/**
 * Checks and cleans a request before the model is called: trims text, removes empty entries, and enforces the
 * limits in {@link ArchitectureProperties}.
 */
@Component
public class ArchitectureSubmission {

	private final ArchitectureProperties properties;

	public ArchitectureSubmission(ArchitectureProperties properties) {
		this.properties = properties;
	}

	public ArchitectureRequest prepare(ArchitectureRequest request) {
		Requirements requirements = request.requirements();
		if (requirements == null) {
			throw badRequest("requirements is required");
		}
		List<Story> stories = clean(requirements.stories(), story -> new Story(trim(story.id()), trim(story.title()),
				trim(story.story()), clean(story.acceptanceCriteria(), criterion -> new Criterion(trim(criterion.id()),
						trim(criterion.given()), trim(criterion.when()), trim(criterion.then())))));
		List<Nfr> nfrs = clean(requirements.nonFunctionalRequirements(),
				nfr -> new Nfr(trim(nfr.id()), trim(nfr.category()), trim(nfr.statement())));
		if (stories.isEmpty()) {
			throw badRequest("At least one story is required");
		}
		if (stories.size() > properties.getMaxStories()) {
			throw badRequest("At most " + properties.getMaxStories() + " stories are allowed");
		}
		if (nfrs.size() > properties.getMaxNfrs()) {
			throw badRequest("At most " + properties.getMaxNfrs() + " non-functional requirements are allowed");
		}
		Set<String> ids = new HashSet<>();
		for (Story story : stories) {
			requireUniqueId(story.id(), "story", ids);
		}
		for (Nfr nfr : nfrs) {
			requireUniqueId(nfr.id(), "non-functional requirement", ids);
			if (nfr.statement() == null) {
				throw badRequest("Non-functional requirement " + nfr.id() + " needs a statement");
			}
		}
		Requirements cleaned = new Requirements(trim(requirements.summary()), stories, nfrs);
		if (characters(cleaned) > properties.getMaxRequirementCharacters()) {
			throw badRequest("Requirements exceed " + properties.getMaxRequirementCharacters() + " characters");
		}
		SystemContext context = request.context() == null ? new SystemContext(null, null, null, null, null)
				: request.context();
		SystemContext cleanedContext = new SystemContext(trim(context.systemName()),
				clean(context.existingSystems(), system -> system.name() == null || system.name().isBlank() ? null
						: new ExistingSystem(system.name().trim(), trim(system.description()))),
				texts(context.technologyConstraints()), trim(context.deploymentTarget()), texts(context.constraints()));
		if (characters(cleanedContext) > properties.getMaxContextCharacters()) {
			throw badRequest("Context exceeds " + properties.getMaxContextCharacters() + " characters");
		}
		return new ArchitectureRequest(cleaned, cleanedContext);
	}

	private static void requireUniqueId(String id, String kind, Set<String> ids) {
		if (id == null) {
			throw badRequest("Every " + kind + " needs an id");
		}
		if (!ids.add(id.toLowerCase(Locale.ROOT))) {
			throw badRequest("Id " + id + " is used more than once");
		}
	}

	private static int characters(Requirements requirements) {
		int total = length(requirements.summary());
		for (Story story : requirements.stories()) {
			total += length(story.id()) + length(story.title()) + length(story.story());
			for (Criterion criterion : story.acceptanceCriteria()) {
				total += length(criterion.id()) + length(criterion.given()) + length(criterion.when())
						+ length(criterion.then());
			}
		}
		for (Nfr nfr : requirements.nonFunctionalRequirements()) {
			total += length(nfr.id()) + length(nfr.category()) + length(nfr.statement());
		}
		return total;
	}

	private static int characters(SystemContext context) {
		int total = length(context.systemName()) + length(context.deploymentTarget());
		for (ExistingSystem system : context.existingSystems()) {
			total += length(system.name()) + length(system.description());
		}
		for (String text : context.technologyConstraints()) {
			total += text.length();
		}
		for (String text : context.constraints()) {
			total += text.length();
		}
		return total;
	}

	private static <T> List<T> clean(List<T> items, Function<T, T> cleaner) {
		if (items == null) {
			return List.of();
		}
		List<T> result = new ArrayList<>();
		for (T item : items) {
			if (item != null) {
				result.add(cleaner.apply(item));
			}
		}
		return result.stream().filter(Objects::nonNull).toList();
	}

	private static List<String> texts(List<String> items) {
		return clean(items, ArchitectureSubmission::trim);
	}

	private static String trim(String text) {
		return text == null || text.isBlank() ? null : text.trim();
	}

	private static int length(String text) {
		return text == null ? 0 : text.length();
	}

	private static ResponseStatusException badRequest(String message) {
		return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
	}

}
