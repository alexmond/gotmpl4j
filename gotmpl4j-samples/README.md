# gotmpl4j samples

Runnable examples of **how to use gotmpl4j** — the core API, the Spring Boot starter, and the
function-provider extension SPI. These modules are documentation: they are part of the build but
are **not published** to Maven Central.

| Module | Shows | Run |
|---|---|---|
| [`quickstart`](quickstart) | **Core API** — parse/render from plain Java, Map & bean models, Sprig auto-discovery, html escaping, Go-builtins-only | `./mvnw -q -pl gotmpl4j-samples/quickstart -am compile exec:java` |
| [`web-mvc`](web-mvc) | **Starter (servlet)** — a controller returns a view name; the auto-configured `GoTemplateViewResolver` renders `templates/home.gotmpl` | `./mvnw -pl gotmpl4j-samples/web-mvc -am spring-boot:run` → http://localhost:8080/ |
| [`web-flux`](web-flux) | **Starter (reactive)** — the same model on the reactive ViewResolver | `./mvnw -pl gotmpl4j-samples/web-flux -am spring-boot:run` → http://localhost:8081/ |
| [`custom-functions`](custom-functions) | **Extension SPI** — implement `FunctionProvider`, register via `META-INF/services`, override by priority | `./mvnw -q -pl gotmpl4j-samples/custom-functions -am compile exec:java` |
| [`spring-context`](spring-context) | **gotmpl4j-spring** — `msg` (i18n), `env`, security (`hasRole`/`username`), web (`param`/`csrf`); page changes by role + locale | `./mvnw -pl gotmpl4j-samples/spring-context -am spring-boot:run` → http://localhost:8082/ |

## Notes

- **quickstart** and **custom-functions** are plain `main(String[])` programs (no Spring); they
  print their output to stdout.
- **web-mvc** / **web-flux** / **spring-context** are Spring Boot apps; each binds a different
  port so they can run side by side.
- In **spring-context**, log in as `alice`/`alice` (ROLE_ADMIN) or `bob`/`bob` (ROLE_USER), and
  send `Accept-Language: es` to see the i18n messages switch to Spanish.
- All three web samples run with `cache: false` so edited templates re-render without a restart.
