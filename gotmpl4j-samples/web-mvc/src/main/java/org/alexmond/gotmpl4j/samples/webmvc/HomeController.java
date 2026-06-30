package org.alexmond.gotmpl4j.samples.webmvc;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Adds attributes to the model and returns a logical view name; the gotmpl4j ViewResolver
 * does the rest. Model keys are addressed Go-style in the template as {@code .Title},
 * {@code .User}, {@code .Items}.
 */
@Controller
public class HomeController {

	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("Title", "gotmpl4j on Spring MVC");
		model.addAttribute("User", "Ada");
		model.addAttribute("Items", List.of(Map.of("Name", "api", "Port", 8443), Map.of("Name", "worker", "Port", 9000),
				Map.of("Name", "scheduler", "Port", 7000)));
		return "home";
	}

}
