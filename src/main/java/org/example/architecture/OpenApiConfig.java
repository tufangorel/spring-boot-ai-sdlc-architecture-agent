package org.example.architecture;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

	@Bean
	public OpenAPI architectureOpenApi() {
		return new OpenAPI().info(new Info()
				.title("Spring Boot AI Architecture Agent")
				.description("Submit reviewed user stories and non-functional requirements with system context, and get back a traceable architecture model with diagrams, decision records, and an NFR traceability matrix.")
				.version("0.0.1"));
	}

}
