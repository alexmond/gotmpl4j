package org.alexmond.gotmpl4j.spring.context;

import java.util.ArrayList;
import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration for the Spring-context template functions ({@code gotmpl4j.spring.*}).
 *
 * <p>
 * The {@code msg} and {@code env} functions are always on. {@code bean} is off by default
 * because exposing arbitrary beans to a template is a server-side template-injection risk
 * if the template is ever untrusted; enable it only for developer-authored templates, and
 * prefer to narrow it with {@link #getAllowedBeans()}.
 */
@ConfigurationProperties("gotmpl4j.spring")
public class GoTemplateSpringProperties {

	/**
	 * Whether the {@code bean} function may resolve beans from the
	 * {@code ApplicationContext}. Off by default (template-injection risk with untrusted
	 * templates).
	 */
	private boolean exposeBeans;

	/**
	 * Optional allow-list of bean names the {@code bean} function may resolve. Empty
	 * means "any bean" (only meaningful when {@link #isExposeBeans()} is {@code true}).
	 */
	private List<String> allowedBeans = new ArrayList<>();

	public boolean isExposeBeans() {
		return this.exposeBeans;
	}

	public void setExposeBeans(boolean exposeBeans) {
		this.exposeBeans = exposeBeans;
	}

	public List<String> getAllowedBeans() {
		return this.allowedBeans;
	}

	public void setAllowedBeans(List<String> allowedBeans) {
		this.allowedBeans = allowedBeans;
	}

}
