package org.alexmond.gotmpl4j.benchmarks;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import io.pebbletemplates.pebble.PebbleEngine;
import io.pebbletemplates.pebble.template.PebbleTemplate;
import org.alexmond.gotmpl4j.GoTemplate;
import org.alexmond.gotmpl4j.benchmarks.model.Person;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * Workload 1 — single interpolation ({@code Hello <name>}). Measures the per-render
 * dispatch floor of each engine with a pre-parsed template.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Thread)
public class InterpolationBenchmark {

	private static final String HELLO = "hello";

	private final Person person = new Person("World");

	private GoTemplate gotmpl;

	private Template freemarker;

	private TemplateEngine thymeleaf;

	private String thymeleafSource;

	private Mustache mustache;

	private PebbleTemplate pebble;

	@Setup
	public void setup() throws Exception {
		this.gotmpl = new GoTemplate();
		this.gotmpl.parse(HELLO, Templates.load("hello.gotmpl"));

		Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
		cfg.setLogTemplateExceptions(false);
		StringTemplateLoader loader = new StringTemplateLoader();
		loader.putTemplate(HELLO, Templates.load("hello.ftl"));
		cfg.setTemplateLoader(loader);
		this.freemarker = cfg.getTemplate(HELLO);

		StringTemplateResolver resolver = new StringTemplateResolver();
		resolver.setTemplateMode(TemplateMode.TEXT);
		resolver.setCacheable(true);
		this.thymeleaf = new TemplateEngine();
		this.thymeleaf.setTemplateResolver(resolver);
		this.thymeleafSource = Templates.load("hello.thymeleaf.txt");
		// warm the parsed-template cache
		thymeleafRender();

		this.mustache = new DefaultMustacheFactory().compile(new StringReader(Templates.load("hello.mustache")), HELLO);
		this.pebble = new PebbleEngine.Builder().autoEscaping(false)
			.build()
			.getLiteralTemplate(Templates.load("hello.pebble"));
	}

	@Benchmark
	public String gotmpl4jRender() {
		return this.gotmpl.render("hello", this.person);
	}

	@Benchmark
	public String freemarkerRender() throws Exception {
		StringWriter out = new StringWriter();
		this.freemarker.process(this.person, out);
		return out.toString();
	}

	@Benchmark
	public String thymeleafRender() {
		Context ctx = new Context();
		ctx.setVariable("name", this.person.getName());
		return this.thymeleaf.process(this.thymeleafSource, ctx);
	}

	@Benchmark
	public String mustacheRender() throws Exception {
		StringWriter out = new StringWriter();
		this.mustache.execute(out, this.person).flush();
		return out.toString();
	}

	@Benchmark
	public String pebbleRender() throws Exception {
		StringWriter out = new StringWriter();
		this.pebble.evaluate(out, Map.of("name", this.person.getName()));
		return out.toString();
	}

}
