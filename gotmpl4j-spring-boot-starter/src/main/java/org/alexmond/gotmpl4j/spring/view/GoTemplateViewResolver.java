package org.alexmond.gotmpl4j.spring.view;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.alexmond.gotmpl4j.spring.GoTemplateService;

import org.springframework.web.servlet.view.AbstractTemplateViewResolver;
import org.springframework.web.servlet.view.AbstractUrlBasedView;

/**
 * Spring MVC {@link org.springframework.web.servlet.ViewResolver ViewResolver} that
 * resolves a view name to a {@link GoTemplateView}. The view name is used as the template
 * name directly (the loader already applies the prefix/suffix), so this resolver keeps
 * {@code UrlBasedViewResolver}'s empty prefix/suffix defaults.
 *
 * @since 1.0
 */
public class GoTemplateViewResolver extends AbstractTemplateViewResolver {

	private final GoTemplateService service;

	private Charset charset = StandardCharsets.UTF_8;

	/**
	 * Creates a resolver that renders views through the given service.
	 * @param service the gotmpl4j rendering service propagated to each view
	 */
	public GoTemplateViewResolver(GoTemplateService service) {
		this.service = service;
	}

	/**
	 * Set the charset propagated to each {@link GoTemplateView} for response encoding.
	 * @param charset the response charset
	 */
	public void setCharset(Charset charset) {
		this.charset = charset;
	}

	@Override
	protected Class<?> getViewClass() {
		return GoTemplateView.class;
	}

	@Override
	protected AbstractUrlBasedView buildView(String viewName) throws Exception {
		GoTemplateView view = (GoTemplateView) super.buildView(viewName);
		view.setService(this.service);
		view.setCharset(this.charset);
		return view;
	}

}
