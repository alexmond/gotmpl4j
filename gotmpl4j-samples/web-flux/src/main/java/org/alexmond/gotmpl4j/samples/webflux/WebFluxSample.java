package org.alexmond.gotmpl4j.samples.webflux;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot WebFlux sample. In a reactive web application the gotmpl4j starter
 * auto-configures a {@code GoTemplateReactiveViewResolver} instead of the servlet one, so
 * the controller looks identical to the MVC sample — only the underlying runtime differs.
 * Start it with:
 *
 * <pre>./mvnw -pl gotmpl4j-samples/web-flux -am spring-boot:run</pre>
 *
 * then open http://localhost:8081/.
 */
@SpringBootApplication
// UseUtilityClass: a Spring Boot app class is instantiated as a config bean, so it can't
// have a private constructor.
@SuppressWarnings("PMD.UseUtilityClass")
public class WebFluxSample {

	public static void main(String[] args) {
		SpringApplication.run(WebFluxSample.class, args);
	}

}
