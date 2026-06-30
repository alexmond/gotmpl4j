package org.alexmond.gotmpl4j.samples.context;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * The page carries no model — everything it shows comes from the gotmpl4j-spring
 * functions reading the live Spring context (messages, environment, security principal,
 * request). See {@code templates/home.gotmpl}.
 */
@Controller
public class HomeController {

	@GetMapping("/")
	public String home() {
		return "home";
	}

}
