package org.alexmond.gotmpl4j.spring.context;

import org.alexmond.gotmpl4j.FunctionProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.MessageSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Registers {@link SpringContextFunctions} as a {@link FunctionProvider} bean so a
 * gotmpl4j engine in the same context (e.g. wired by
 * {@code gotmpl4j-spring-boot-starter}) can resolve {@code msg}/{@code env}/{@code bean}.
 * Active whenever the core {@link FunctionProvider} SPI is on the classpath; opt out with
 * {@code gotmpl4j.spring.enabled=false}.
 *
 * <p>
 * When Spring Security is also present, {@link SpringSecurityFunctions} is registered too
 * ({@code hasRole}/{@code isAuthenticated}/…) — {@code spring-security-core} is an
 * optional dependency, so apps without it pull nothing extra and those functions simply
 * don't appear.
 */
@AutoConfiguration
@ConditionalOnClass(FunctionProvider.class)
@ConditionalOnProperty(prefix = "gotmpl4j.spring", name = "enabled", matchIfMissing = true)
@EnableConfigurationProperties(GoTemplateSpringProperties.class)
public class GoTemplateSpringAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public SpringContextFunctions springContextFunctions(MessageSource messageSource, Environment environment,
			ApplicationContext applicationContext, GoTemplateSpringProperties properties) {
		return new SpringContextFunctions(messageSource, environment, applicationContext, properties);
	}

	/**
	 * Security functions, registered only when Spring Security is on the classpath. The
	 * nested {@code @ConditionalOnClass} keeps {@link SpringSecurityFunctions} from being
	 * loaded at all when {@code spring-security-core} is absent.
	 */
	@Configuration(proxyBeanMethods = false)
	@ConditionalOnClass(SecurityContextHolder.class)
	static class SpringSecurityFunctionsConfiguration {

		@Bean
		@ConditionalOnMissingBean
		SpringSecurityFunctions springSecurityFunctions() {
			return new SpringSecurityFunctions();
		}

	}

}
