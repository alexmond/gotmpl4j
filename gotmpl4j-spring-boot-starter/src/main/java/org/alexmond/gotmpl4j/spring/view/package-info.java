/**
 * Spring web view integration for gotmpl4j.
 *
 * <p>
 * Provides {@code ViewResolver}/{@code View} implementations that render Go templates as
 * Spring views for both Spring MVC (servlet:
 * {@link org.alexmond.gotmpl4j.spring.view.GoTemplateViewResolver} and
 * {@link org.alexmond.gotmpl4j.spring.view.GoTemplateView}) and Spring WebFlux (reactive:
 * {@link org.alexmond.gotmpl4j.spring.view.GoTemplateReactiveViewResolver} and
 * {@link org.alexmond.gotmpl4j.spring.view.GoTemplateReactiveView}), so controllers can
 * return a template name and have the model rendered through the engine.
 */
package org.alexmond.gotmpl4j.spring.view;
