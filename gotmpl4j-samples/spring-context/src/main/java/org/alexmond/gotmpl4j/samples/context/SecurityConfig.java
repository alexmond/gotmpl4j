package org.alexmond.gotmpl4j.samples.context;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Two in-memory users so the template's {@code hasRole}/{@code username} branches have
 * something to show: {@code alice} (ROLE_ADMIN) and {@code bob} (ROLE_USER). The home
 * page is public; the generated form-login page (no template needed) handles sign-in.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		http.authorizeHttpRequests((auth) -> auth.requestMatchers("/admin").hasRole("ADMIN").anyRequest().permitAll())
			.formLogin(Customizer.withDefaults())
			.httpBasic(Customizer.withDefaults());
		return http.build();
	}

	@Bean
	UserDetailsService users(PasswordEncoder encoder) {
		UserDetails alice = User.withUsername("alice").password(encoder.encode("alice")).roles("ADMIN").build();
		UserDetails bob = User.withUsername("bob").password(encoder.encode("bob")).roles("USER").build();
		return new InMemoryUserDetailsManager(alice, bob);
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

}
