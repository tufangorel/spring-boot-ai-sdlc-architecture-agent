package org.example.architecture;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import com.structurizr.Workspace;
import com.structurizr.dsl.StructurizrDslParser;

/** Helpers for validating generated DSL with Structurizr's official parser. */
public final class StructurizrDslParserTestSupport {

	private StructurizrDslParserTestSupport() {
	}

	public static Workspace parse(String dsl, Path directory) throws Exception {
		Path file = directory.resolve("workspace.dsl");
		Files.writeString(file, dsl);
		StructurizrDslParser parser = new StructurizrDslParser();
		parser.parse(file.toFile());
		return parser.getWorkspace();
	}

}
