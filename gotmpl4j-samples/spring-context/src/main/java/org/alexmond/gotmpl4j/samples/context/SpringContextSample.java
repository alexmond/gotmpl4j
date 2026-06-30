package org.alexmond.gotmpl4j.samples.context;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring context-functions sample. With gotmpl4j-spring on the classpath the {@code msg},
 * {@code env}, {@code bean}, security and web functions are auto-configured into every
 * rendered template. Start it with:
 *
 * <pre>./mvnw -pl gotmpl4j-samples/spring-context -am spring-boot:run</pre>
 *
 * then open http://localhost:8082/ (log in as {@code alice}/{@code alice} for ROLE_ADMIN
 * or {@code bob}/{@code bob} for ROLE_USER; send {@code Accept-Language: es} for
 * Spanish).
 */
@SpringBootApplication
// UseUtilityClass: a Spring Boot app class is instantiated as a config bean, so it can't
// have a private constructor.
@SuppressWarnings("PMD.UseUtilityClass")
public class SpringContextSample {

	public static void main(String[] args) {
		SpringApplication.run(SpringContextSample.class, args);
	}

}
