package org.alexmond.gotmpl4j.samples.webflux;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Identical in shape to the MVC sample's controller — returning a view name plus a model
 * is the same programming model on WebFlux. The reactive ViewResolver renders
 * {@code home.gotmpl}.
 */
@Controller
public class HomeController {

	@GetMapping("/")
	public String home(Model model) {
		model.addAttribute("Title", "gotmpl4j on Spring WebFlux");
		model.addAttribute("User", "Grace");
		model.addAttribute("Items",
				List.of(Map.of("Name", "gateway", "Port", 8443), Map.of("Name", "stream", "Port", 9092)));
		return "home";
	}

}
