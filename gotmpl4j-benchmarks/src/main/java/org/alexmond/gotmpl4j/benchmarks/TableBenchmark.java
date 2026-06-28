package org.alexmond.gotmpl4j.benchmarks;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
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
import org.alexmond.gotmpl4j.benchmarks.model.Stock;
import org.alexmond.gotmpl4j.benchmarks.model.StockData;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.StringTemplateResolver;

/**
 * Workload 2 — render a table of N stock rows (loop + field access), the classic "stocks"
 * template-benchmark workload. The hot path; render scales with N. gotmpl4j and Go share
 * the identical {@code table.gotmpl}.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@Warmup(iterations = 5, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
@State(Scope.Thread)
public class TableBenchmark {

	@Param({ "10", "100", "1000" })
	private int n;

	private List<Stock> stocks;

	private Map<String, Object> itemsModel;

	private GoTemplate gotmpl;

	private Map<String, Object> gotmplData;

	private Template freemarker;

	private TemplateEngine thymeleaf;

	private String thymeleafSource;

	private Mustache mustache;

	private PebbleTemplate pebble;

	@Setup
	public void setup() throws Exception {
		this.stocks = StockData.stocks(this.n);
		this.itemsModel = new HashMap<>();
		this.itemsModel.put("items", this.stocks);

		this.gotmpl = new GoTemplate();
		this.gotmpl.parse("table", Templates.load("table.gotmpl"));
		this.gotmplData = new HashMap<>();
		this.gotmplData.put("Items", this.stocks);

		Configuration cfg = new Configuration(Configuration.VERSION_2_3_32);
		cfg.setLogTemplateExceptions(false);
		cfg.setNumberFormat("computer");
		StringTemplateLoader loader = new StringTemplateLoader();
		loader.putTemplate("table", Templates.load("table.ftl"));
		cfg.setTemplateLoader(loader);
		this.freemarker = cfg.getTemplate("table");

		StringTemplateResolver resolver = new StringTemplateResolver();
		resolver.setTemplateMode(TemplateMode.HTML);
		resolver.setCacheable(true);
		this.thymeleaf = new TemplateEngine();
		this.thymeleaf.setTemplateResolver(resolver);
		this.thymeleafSource = Templates.load("table.thymeleaf.html");
		thymeleafRender();

		this.mustache = new DefaultMustacheFactory().compile(new StringReader(Templates.load("table.mustache")),
				"table");
		this.pebble = new PebbleEngine.Builder().autoEscaping(false)
			.build()
			.getLiteralTemplate(Templates.load("table.pebble"));
	}

	@Benchmark
	public String gotmpl4jRender() {
		return this.gotmpl.render("table", this.gotmplData);
	}

	@Benchmark
	public String freemarkerRender() throws Exception {
		StringWriter out = new StringWriter();
		this.freemarker.process(this.itemsModel, out);
		return out.toString();
	}

	@Benchmark
	public String thymeleafRender() {
		Context ctx = new Context();
		ctx.setVariable("items", this.stocks);
		return this.thymeleaf.process(this.thymeleafSource, ctx);
	}

	@Benchmark
	public String mustacheRender() throws Exception {
		StringWriter out = new StringWriter();
		this.mustache.execute(out, this.itemsModel).flush();
		return out.toString();
	}

	@Benchmark
	public String pebbleRender() throws Exception {
		StringWriter out = new StringWriter();
		this.pebble.evaluate(out, this.itemsModel);
		return out.toString();
	}

}
