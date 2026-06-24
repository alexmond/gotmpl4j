/**
 * Spring Boot auto-configuration for gotmpl4j.
 *
 * <p>
 * {@link org.alexmond.gotmpl4j.spring.Gotmpl4jAutoConfiguration} wires up a
 * ready-to-inject {@link org.alexmond.gotmpl4j.spring.GoTemplateService} backed by a
 * {@link org.alexmond.gotmpl4j.spring.GoTemplateFactory} and
 * {@link org.alexmond.gotmpl4j.spring.GoTemplateLoader}, driven by
 * {@link org.alexmond.gotmpl4j.spring.Gotmpl4jProperties}. Any
 * {@link org.alexmond.gotmpl4j.FunctionProvider} bean on the context is contributed to
 * the engine, so applications extend the function set the Spring-idiomatic way.
 */
package org.alexmond.gotmpl4j.spring;
