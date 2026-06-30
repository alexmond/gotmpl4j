package org.alexmond.gotmpl4j.samples.webmvc;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Spring Boot MVC sample. The gotmpl4j starter auto-configures a servlet
 * {@code GoTemplateViewResolver}, so returning the view name {@code "home"} from a
 * controller renders {@code classpath:/templates/home.gotmpl}. Start it with:
 *
 * <pre>./mvnw -pl gotmpl4j-samples/web-mvc -am spring-boot:run</pre>
 *
 * then open http://localhost:8080/.
 */
@SpringBootApplication
// UseUtilityClass: a Spring Boot app class is instantiated as a config bean, so it can't
// have a private constructor.
@SuppressWarnings("PMD.UseUtilityClass")
public class WebMvcSample {

	public static void main(String[] args) {
		SpringApplication.run(WebMvcSample.class, args);
	}

}
