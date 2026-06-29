package org.alexmond.gotmpl4j.exec;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.IOException;
import java.io.Writer;
import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.extern.slf4j.Slf4j;
import org.alexmond.gotmpl4j.Function;
import org.alexmond.gotmpl4j.Functions;
import org.alexmond.gotmpl4j.MissingKeyMode;
import org.alexmond.gotmpl4j.TemplateExecutionException;
import org.alexmond.gotmpl4j.TemplateNotFoundException;
import org.alexmond.gotmpl4j.parse.ActionNode;
import org.alexmond.gotmpl4j.parse.BoolNode;
import org.alexmond.gotmpl4j.parse.BreakNode;
import org.alexmond.gotmpl4j.parse.ChainNode;
import org.alexmond.gotmpl4j.parse.CommandNode;
import org.alexmond.gotmpl4j.parse.CommentNode;
import org.alexmond.gotmpl4j.parse.ContinueNode;
import org.alexmond.gotmpl4j.parse.DotNode;
import org.alexmond.gotmpl4j.parse.FieldNode;
import org.alexmond.gotmpl4j.parse.IdentifierNode;
import org.alexmond.gotmpl4j.parse.IfNode;
import org.alexmond.gotmpl4j.parse.ListNode;
import org.alexmond.gotmpl4j.parse.NilNode;
import org.alexmond.gotmpl4j.parse.Node;
import org.alexmond.gotmpl4j.parse.NumberNode;
import org.alexmond.gotmpl4j.parse.PipeNode;
import org.alexmond.gotmpl4j.parse.RangeNode;
import org.alexmond.gotmpl4j.parse.StringNode;
import org.alexmond.gotmpl4j.parse.TemplateNode;
import org.alexmond.gotmpl4j.parse.TextNode;
import org.alexmond.gotmpl4j.parse.VariableNode;
import org.alexmond.gotmpl4j.parse.WithNode;

@Slf4j
public class Executor {

	// Maximum nesting of {{template}}/{{block}} invocations before execution is aborted.
	// Mirrors Go's text/template maxExecDepth and bounds self-recursive templates such as
	// {{define "x"}}{{template "x" .}}{{end}} so they raise a TemplateExecutionException
	// instead of a StackOverflowError. A StackOverflowError backstop in execute() catches
	// the case where the JVM stack is exhausted before this limit is reached.
	private static final int MAX_TEMPLATE_DEPTH = 100_000;

	private final Map<String, Node> rootNodes;

	private final Map<String, Function> functions;

	// Current {{template}}-invocation nesting depth (see MAX_TEMPLATE_DEPTH).
	private int templateDepth;

	// Reflection caches keyed by data class. Owned by the GoTemplate (so they persist
	// across
	// the many short-lived Executors it spawns) and shared concurrently, hence
	// ConcurrentHashMap.
	private final Map<Class<?>, BeanInfo> beanInfoCache;

	private final Map<Class<?>, Map<String, Method>> accessorCache;

	// Variable storage for template execution context
	private final Map<String, Object> variables = new HashMap<>();

	// Block-scope undo logs. Each {{if}}/{{with}}/range-iteration pushes a frame; a `:=`
	// declaration inside records how to undo itself, so the declaration is unwound when
	// the
	// block exits (Go scopes `:=` to its block). `=` assignments are not logged and
	// persist.
	private final java.util.Deque<List<ScopeUndo>> scopeStack = new java.util.ArrayDeque<>();

	private void pushScope() {
		scopeStack.push(new ArrayList<>());
	}

	private void popScope() {
		List<ScopeUndo> frame = scopeStack.pop();
		for (int i = frame.size() - 1; i >= 0; i--) {
			ScopeUndo u = frame.get(i);
			if (u.existed()) {
				variables.put(u.name(), u.previous());
			}
			else {
				variables.remove(u.name());
			}
		}
	}

	// Root data for $ variable
	private Object rootData;

	// How a nil/absent value is rendered by a bare action (Go's missingkey option).
	private final MissingKeyMode missingKey;

	public Executor(Map<String, Node> rootNodes, Map<String, Function> functions) {
		this(rootNodes, functions, MissingKeyMode.DEFAULT);
	}

	public Executor(Map<String, Node> rootNodes, Map<String, Function> functions, MissingKeyMode missingKey) {
		this(rootNodes, functions, missingKey, new ConcurrentHashMap<>(), new ConcurrentHashMap<>());
	}

	/**
	 * Creates an executor that shares the engine-owned reflection caches, so
	 * introspection is amortised across every render of the owning template rather than
	 * rebuilt per execution.
	 * @param rootNodes the parsed template set
	 * @param functions the function table
	 * @param missingKey how a nil/absent value renders
	 * @param beanInfoCache shared {@code Class -> BeanInfo} cache
	 * @param accessorCache shared {@code Class -> (name -> read method)} cache
	 */
	public Executor(Map<String, Node> rootNodes, Map<String, Function> functions, MissingKeyMode missingKey,
			Map<Class<?>, BeanInfo> beanInfoCache, Map<Class<?>, Map<String, Method>> accessorCache) {
		this.rootNodes = rootNodes;
		this.functions = functions;
		this.missingKey = missingKey;
		this.beanInfoCache = beanInfoCache;
		this.accessorCache = accessorCache;
	}

	public void execute(String name, Object data, Writer writer)
			throws IOException, TemplateNotFoundException, TemplateExecutionException {
		Node node = rootNodes.get(name);
		if (node == null) {
			throw new TemplateNotFoundException(String.format("template '%s' not found", name));
		}

		if (node instanceof ListNode listNode) {
			try {
				// Store root data for $ variable
				this.rootData = data;
				variables.put("$", data);
				BeanInfo beanInfo = getBeanInfo(data);
				writeNode(writer, listNode, data, beanInfo);
			}
			catch (StackOverflowError ex) {
				throw new TemplateExecutionException("template recursion too deep while executing '" + name
						+ "' (possible self-referential template)", ex);
			}
			catch (IndexOutOfBoundsException ex) {
				if (log.isDebugEnabled()) {
					log.debug("Internal IndexOutOfBounds in '{}': {}", name, ex.getMessage(), ex);
				}
				throw new TemplateExecutionException("Internal IndexOutOfBounds in '" + name + "': " + ex.getMessage(),
						ex);
			}
			catch (Exception ex) {
				if (ex instanceof IOException || ex instanceof TemplateExecutionException
						|| ex instanceof TemplateNotFoundException) {
					throw ex;
				}
				if (log.isDebugEnabled()) {
					log.debug("Execution failure in template '{}': {}", name, ex.getMessage(), ex);
				}
				throw new TemplateExecutionException("Execution error in '" + name + "': " + ex.getMessage(), ex);
			}
		}
		else {
			throw new TemplateExecutionException(String.format("root node for '%s' is not a ListNode", name));
		}
	}

	private void writeNode(Writer writer, Node node, Object data, BeanInfo beanInfo)
			throws IOException, TemplateExecutionException, TemplateNotFoundException {
		if (node == null) {
			return;
		}
		if (node instanceof ListNode) {
			writeList(writer, (ListNode) node, data, beanInfo);
		}
		else if (node instanceof ActionNode actionNode) {
			writeAction(writer, actionNode, data, beanInfo);
		}
		else if (node instanceof CommentNode commentNode) {
			if (log.isTraceEnabled()) {
				log.trace("Skipping comment node: {}", commentNode);
			}
		}
		else if (node instanceof IfNode ifNode) {
			writeIf(writer, ifNode, data, beanInfo);
		}
		else if (node instanceof RangeNode rangeNode) {
			writeRange(writer, rangeNode, data, beanInfo);
		}
		else if (node instanceof TemplateNode templateNode) {
			writeTemplate(writer, templateNode, data, beanInfo);
		}
		else if (node instanceof TextNode textNode) {
			writeText(writer, textNode);
		}
		else if (node instanceof WithNode withNode) {
			writeWith(writer, withNode, data, beanInfo);
		}
		else if (node instanceof BreakNode) {
			throw new BreakSignal();
		}
		else if (node instanceof ContinueNode) {
			throw new ContinueSignal();
		}
		else {
			throw new TemplateExecutionException(String.format("unknown node: %s", node.toString()));
		}
	}

	private void writeAction(Writer writer, ActionNode actionNode, Object data, BeanInfo beanInfo)
			throws IOException, TemplateExecutionException {
		PipeNode pipeNode = actionNode.getPipeNode();
		Object value = executePipe(pipeNode, data, beanInfo);

		// Debug: log variable assignments
		if (pipeNode.getVariableCount() > 0) {
			for (VariableNode variable : pipeNode.getVariables()) {
				String varName = variable.getIdentifier(0);
				if (log.isDebugEnabled()) {
					log.debug("Action assigned variable: {} = {}", varName, value);
				}
			}
		}

		if (pipeNode.getVariableCount() == 0) {
			printValue(writer, value);
		}
	}

	private void writeIf(Writer writer, IfNode ifNode, Object data, BeanInfo beanInfo)
			throws IOException, TemplateExecutionException, TemplateNotFoundException {
		// `{{ if $x := … }}` scopes the declaration to the if/else; body `:=`
		// declarations
		// are block-scoped too. Push a scope over the whole construct and unwind on exit.
		pushScope();
		try {
			Object value = executePipe(ifNode.getPipeNode(), data, beanInfo);
			if (isTrue(value)) {
				writeNode(writer, ifNode.getIfListNode(), data, beanInfo);
			}
			else if (ifNode.getElseListNode() != null) {
				writeNode(writer, ifNode.getElseListNode(), data, beanInfo);
			}
		}
		finally {
			popScope();
		}
	}

	private void writeList(Writer writer, ListNode listNode, Object data, BeanInfo beanInfo)
			throws IOException, TemplateExecutionException, TemplateNotFoundException {
		int count = listNode.size();
		for (int i = 0; i < count; i++) {
			writeNode(writer, listNode.get(i), data, beanInfo);
		}
	}

	private void writeRange(Writer writer, RangeNode rangeNode, Object data, BeanInfo beanInfo)
			throws IOException, TemplateExecutionException, TemplateNotFoundException {
		PipeNode pipeNode = rangeNode.getPipeNode();
		List<VariableNode> rangeVars = pipeNode.getVariables();
		// `range $i := x` declares loop-local variables (saved/restored around the loop);
		// `range $i = x` ASSIGNS an existing outer variable, which keeps the last
		// iterated
		// value after the loop (and is unchanged for an empty range), so it is not
		// saved/restored.
		boolean declare = rangeVars.isEmpty() || pipeNode.isDeclare();

		// Save current variable values to restore later (declarations only).
		Map<String, Object> savedVars = new HashMap<>();
		if (declare) {
			for (VariableNode varNode : rangeVars) {
				String varName = varNode.getIdentifier(0);
				if (variables.containsKey(varName)) {
					savedVars.put(varName, variables.get(varName));
				}
			}
			// Initialize range variables to null in case the range is empty
			for (VariableNode varNode : rangeVars) {
				variables.put(varNode.getIdentifier(0), null);
			}
		}

		// Execute pipe but don't set variables yet - we'll set them per iteration
		Object arrayOrList = executePipeWithoutVariableAssignment(pipeNode, data, beanInfo);

		if (arrayOrList == null) {
			if (rangeNode.getElseListNode() != null) {
				writeNode(writer, rangeNode.getElseListNode(), data, beanInfo);
			}
			if (declare) {
				restoreVariables(rangeVars, savedVars);
			}
			return;
		}

		iterateRange(writer, rangeNode, rangeVars, arrayOrList, data, beanInfo, savedVars, declare);

		// Restore declared variables after range (assignments keep their last value).
		if (declare) {
			restoreVariables(rangeVars, savedVars);
		}
	}

	private void iterateRange(Writer writer, RangeNode rangeNode, List<VariableNode> rangeVars, Object arrayOrList,
			Object data, BeanInfo beanInfo, Map<String, Object> savedVars, boolean declare)
			throws IOException, TemplateExecutionException, TemplateNotFoundException {
		if (arrayOrList.getClass().isArray()) {
			int length = Array.getLength(arrayOrList);
			if (length == 0 && rangeNode.getElseListNode() != null) {
				writeNode(writer, rangeNode.getElseListNode(), data, beanInfo);
				if (declare) {
					restoreVariables(rangeVars, savedVars);
				}
				return;
			}
			for (int i = 0; i < length; i++) {
				Object value = Array.get(arrayOrList, i);
				setRangeVariables(rangeVars, i, value);
				if (writeRangeItem(writer, rangeNode, value)) {
					return;
				}
			}
		}
		else if (arrayOrList instanceof Collection<?> collection) {
			iterateCollection(writer, rangeNode, rangeVars, collection, data, beanInfo, savedVars, declare);
		}
		else if (arrayOrList instanceof Map<?, ?> map) {
			iterateMap(writer, rangeNode, rangeVars, map, data, beanInfo, savedVars, declare);
		}
		else {
			if (declare) {
				restoreVariables(rangeVars, savedVars);
			}
			throw new TemplateExecutionException(
					String.format("can't iterate over %s", arrayOrList.getClass().getName()));
		}
	}

	private void iterateCollection(Writer writer, RangeNode rangeNode, List<VariableNode> rangeVars,
			Collection<?> collection, Object data, BeanInfo beanInfo, Map<String, Object> savedVars, boolean declare)
			throws IOException, TemplateExecutionException, TemplateNotFoundException {
		if (collection.isEmpty() && rangeNode.getElseListNode() != null) {
			writeNode(writer, rangeNode.getElseListNode(), data, beanInfo);
			if (declare) {
				restoreVariables(rangeVars, savedVars);
			}
			return;
		}
		int index = 0;
		for (Object object : collection) {
			setRangeVariables(rangeVars, index++, object);
			if (writeRangeItem(writer, rangeNode, object)) {
				return;
			}
		}
	}

	private void iterateMap(Writer writer, RangeNode rangeNode, List<VariableNode> rangeVars, Map<?, ?> map,
			Object data, BeanInfo beanInfo, Map<String, Object> savedVars, boolean declare)
			throws IOException, TemplateExecutionException, TemplateNotFoundException {
		if (map.isEmpty() && rangeNode.getElseListNode() != null) {
			writeNode(writer, rangeNode.getElseListNode(), data, beanInfo);
			if (declare) {
				restoreVariables(rangeVars, savedVars);
			}
			return;
		}
		// Go text/template guarantees sorted-key iteration for maps with
		// comparable keys
		List<Map.Entry<?, ?>> sorted = new ArrayList<>(map.entrySet());
		sorted.sort(Comparator.comparing((e) -> String.valueOf(e.getKey())));
		for (Map.Entry<?, ?> entry : sorted) {
			setRangeVariables(rangeVars, entry.getKey(), entry.getValue());
			if (writeRangeItem(writer, rangeNode, entry.getValue())) {
				return;
			}
		}
	}

	private void setRangeVariables(List<VariableNode> rangeVars, Object keyOrIndex, Object value) {
		if (rangeVars.size() == 1) {
			// Single variable gets the value
			variables.put(rangeVars.get(0).getIdentifier(0), value);
		}
		else if (rangeVars.size() >= 2) {
			// Two variables: first is key/index, second is value
			variables.put(rangeVars.get(0).getIdentifier(0), keyOrIndex);
			variables.put(rangeVars.get(1).getIdentifier(0), value);
		}
	}

	private void restoreVariables(List<VariableNode> rangeVars, Map<String, Object> savedVars) {
		for (VariableNode varNode : rangeVars) {
			String varName = varNode.getIdentifier(0);
			if (savedVars.containsKey(varName)) {
				variables.put(varName, savedVars.get(varName));
			}
			else {
				variables.remove(varName);
			}
		}
	}

	/**
	 * Writes a single range iteration, handling break/continue signals.
	 * @return {@code true} if a break was signalled and the loop should exit
	 */
	private boolean writeRangeItem(Writer writer, RangeNode rangeNode, Object value)
			throws IOException, TemplateExecutionException, TemplateNotFoundException {
		// Each iteration body is its own scope, so a `:=` inside does not leak to the
		// next
		// iteration or past the range (the loop variables themselves are managed
		// separately).
		pushScope();
		try {
			writeRangeValue(writer, rangeNode, value);
		}
		catch (ContinueSignal ex) {
			return false;
		}
		catch (BreakSignal ex) {
			return true;
		}
		finally {
			popScope();
		}
		return false;
	}

	private void writeRangeValue(Writer writer, RangeNode rangeNode, Object value)
			throws IOException, TemplateExecutionException, TemplateNotFoundException {
		ListNode ifListNode = rangeNode.getIfListNode();
		BeanInfo itemBeanInfo = getBeanInfo(value);
		int count = ifListNode.size();
		for (int i = 0; i < count; i++) {
			writeNode(writer, ifListNode.get(i), value, itemBeanInfo);
		}
	}

	private void writeText(Writer writer, TextNode textNode) throws IOException {
		printText(writer, textNode.getText());
	}

	private void writeWith(Writer writer, WithNode withNode, Object data, BeanInfo beanInfo)
			throws IOException, TemplateExecutionException, TemplateNotFoundException {
		pushScope();
		try {
			Object value = executePipe(withNode.getPipeNode(), data, beanInfo);
			if (isTrue(value)) {
				BeanInfo valueBeanInfo = getBeanInfo(value);
				writeNode(writer, withNode.getIfListNode(), value, valueBeanInfo);
			}
			else if (withNode.getElseListNode() != null) {
				writeNode(writer, withNode.getElseListNode(), data, beanInfo);
			}
		}
		finally {
			popScope();
		}
	}

	private void writeTemplate(Writer writer, TemplateNode templateNode, Object data, BeanInfo beanInfo)
			throws IOException, TemplateExecutionException, TemplateNotFoundException {
		String name = templateNode.getName();

		ListNode listNode = (ListNode) rootNodes.get(name);
		if (listNode == null) {
			throw new TemplateExecutionException(String.format("template %s not defined", name));
		}

		if (++this.templateDepth > MAX_TEMPLATE_DEPTH) {
			this.templateDepth--;
			throw new TemplateExecutionException(
					"exceeded maximum template depth (" + MAX_TEMPLATE_DEPTH + ") invoking '" + name + "'");
		}

		Object value = executePipe(templateNode.getPipeNode(), data, beanInfo);
		BeanInfo valueBeanInfo = (value != null) ? getBeanInfo(value) : null;
		// Go resets $ and clears outer variables when entering a nested template; its
		// block
		// scopes belong to the callee, so swap in a fresh scope stack too.
		Map<String, Object> savedVariables = new HashMap<>(this.variables);
		java.util.Deque<List<ScopeUndo>> savedScopes = new java.util.ArrayDeque<>(this.scopeStack);
		this.variables.clear();
		this.scopeStack.clear();
		this.variables.put("$", value);
		try {
			writeNode(writer, listNode, value, valueBeanInfo);
		}
		finally {
			this.templateDepth--;
			this.variables.clear();
			this.variables.putAll(savedVariables);
			this.scopeStack.clear();
			this.scopeStack.addAll(savedScopes);
		}
	}

	private Object executePipe(PipeNode pipeNode, Object data, BeanInfo beanInfo) throws TemplateExecutionException {
		return executePipe(pipeNode, data, beanInfo, true);
	}

	private Object executePipeWithoutVariableAssignment(PipeNode pipeNode, Object data, BeanInfo beanInfo)
			throws TemplateExecutionException {
		return executePipe(pipeNode, data, beanInfo, false);
	}

	private Object executePipe(PipeNode pipeNode, Object data, BeanInfo beanInfo, boolean assignVariables)
			throws TemplateExecutionException {
		if (pipeNode == null) {
			return data;
		}

		// Start from NO_PIPELINE so the first command sees "no upstream input" — distinct
		// from an upstream command that produces null. Go threads a piped value as the
		// next command's final argument even when that value is nil.
		Object value = NO_PIPELINE;
		for (CommandNode command : pipeNode.getCommands()) {
			value = executeCommand(command, data, beanInfo, value);
		}
		if (value == NO_PIPELINE) {
			value = null;
		}

		// Store variables in execution context (e.g., {{$x := .Value}})
		if (assignVariables) {
			for (VariableNode variable : pipeNode.getVariables()) {
				if (variable != null) {
					String varName = variable.getIdentifier(0);
					// A `:=` declaration inside a block records an undo entry so it is
					// unwound on block exit (Go scopes it to the block). A top-level
					// declaration (no open scope) and any `=` assignment just persist.
					if (pipeNode.isDeclare() && !scopeStack.isEmpty()) {
						scopeStack.peek()
							.add(new ScopeUndo(varName, variables.containsKey(varName), variables.get(varName)));
					}
					// Store the value even if it's null or empty string, matching Helm
					// behavior
					variables.put(varName, value);
				}
			}
		}

		return value;
	}

	private Object executeCommand(CommandNode command, Object data, BeanInfo beanInfo, Object currentPipelineValue)
			throws TemplateExecutionException {
		if (command.getArgumentCount() == 0) {
			throw new TemplateExecutionException("empty command");
		}
		Node firstArgument = command.getFirstArgument();
		// A method call on the receiver applies when there are explicit arguments
		// (.obj.Method arg) or when an upstream pipeline value is threaded in as the
		// final argument (arg | .obj.Method).
		boolean methodCallPossible = command.getArgumentCount() > 1 || currentPipelineValue != NO_PIPELINE;
		if (firstArgument instanceof FieldNode fieldNode) {
			// Go templates: .obj.Method arg → call Method(arg) on obj
			if (methodCallPossible) {
				Object result = tryFieldMethodCall(fieldNode, command.getArguments(), data, beanInfo,
						currentPipelineValue);
				if (result != INVOKE_NOT_FOUND) {
					return result;
				}
			}
			return executeField(fieldNode, data);
		}
		if (firstArgument instanceof IdentifierNode) {
			return executeFunction((IdentifierNode) firstArgument, command.getArguments(), data, beanInfo,
					currentPipelineValue);
		}
		if (firstArgument instanceof ChainNode chainNode) {
			// Go templates: (.pipe).Method arg or .X.Y.Method arg → method call
			if (methodCallPossible) {
				Object result = tryChainMethodCall(chainNode, command.getArguments(), data, beanInfo,
						currentPipelineValue);
				if (result != INVOKE_NOT_FOUND) {
					return result;
				}
			}
			return executeChain(chainNode, data, beanInfo);
		}
		if (firstArgument instanceof VariableNode variableNode) {
			// Go templates: $var.Field.Method arg → call Method(arg) on $var.Field
			// (e.g. $.Files.Get "path" or arg | $.Files.Get). Without this, the method
			// name resolves as a missing field and the argument is dropped.
			if (methodCallPossible && variableNode.getIdentifiers().length > 1) {
				Object result = tryVariableMethodCall(variableNode, command.getArguments(), data, beanInfo,
						currentPipelineValue);
				if (result != INVOKE_NOT_FOUND) {
					return result;
				}
			}
			return executeVariable(variableNode);
		}

		if (firstArgument instanceof DotNode) {
			return data;
		}
		if (firstArgument instanceof StringNode) {
			return ((StringNode) firstArgument).getText();
		}
		if (firstArgument instanceof BoolNode) {
			return ((BoolNode) firstArgument).isValue();
		}
		if (firstArgument instanceof NumberNode) {
			NumberNode numberNode = (NumberNode) firstArgument;
			if (numberNode.isInt()) {
				return numberNode.getIntValue();
			}
			if (numberNode.isFloat()) {
				return numberNode.getFloatValue();
			}
			return 0;
		}
		if (firstArgument instanceof NilNode) {
			return null;
		}

		if (firstArgument instanceof PipeNode) {
			return executePipe((PipeNode) firstArgument, data, beanInfo);
		}

		throw new TemplateExecutionException(String.format("can't evaluate command %s", firstArgument));
	}

	private Object executeChain(ChainNode chainNode, Object data, BeanInfo beanInfo) throws TemplateExecutionException {
		Object current = executeArgument(chainNode.getNode(), data, beanInfo);
		for (String field : chainNode.getFields()) {
			current = getFieldValue(current, field);
		}
		return current;
	}

	private Object getFieldValue(Object current, String identifier) throws TemplateExecutionException {
		if (current == null) {
			return null;
		}
		if (current instanceof Map<?, ?> rawMap) {
			@SuppressWarnings("unchecked")
			Map<String, Object> map = (Map<String, Object>) rawMap;
			if (map.containsKey(identifier)) {
				return map.get(identifier);
			}
			// A type that is both a map and exposes helper methods (like Helm's .Files):
			// when the key is absent, fall back to a matching no-arg helper method. Plain
			// maps have no such custom methods, so their missing keys still return null.
			Object methodValue = invokeNoArgHelperMethod(current, identifier);
			return (methodValue != NO_METHOD) ? methodValue : map.get(identifier);
		}
		Method readMethod = accessorFor(current.getClass(), identifier);
		if (readMethod != null) {
			try {
				return readMethod.invoke(current);
			}
			catch (IllegalAccessException | InvocationTargetException ex) {
				throw new TemplateExecutionException(String.format("can't get value '%s' from data", identifier), ex);
			}
		}

		// No JavaBean getter matched. Go templates resolve `.Name` as a method named Name
		// first, then an exported struct field — so for plain POJOs (no getters) fall
		// back
		// to a no-arg method, then a public field, by exact name.
		Object methodValue = invokeNoArgHelperMethod(current, identifier);
		if (methodValue != NO_METHOD) {
			return methodValue;
		}
		Object fieldValue = readPublicField(current, identifier);
		if (fieldValue != NO_FIELD) {
			return fieldValue;
		}

		// In Helm, missing fields return nil instead of throwing an error
		return null;
	}

	/**
	 * Sentinel marking "no matching public field found" (distinct from a null field
	 * value).
	 */
	private static final Object NO_FIELD = new Object();

	/**
	 * Read a public field named {@code name} (exact match, including inherited) from the
	 * target, mirroring Go's access to an exported struct field. JDK-declared fields are
	 * skipped so engine internals are not exposed.
	 * @return the field value, or {@link #NO_FIELD} if no such accessible field exists
	 */
	private static Object readPublicField(Object target, String name) {
		try {
			java.lang.reflect.Field field = target.getClass().getField(name);
			if (field.getDeclaringClass().getName().startsWith("java.")) {
				return NO_FIELD;
			}
			return field.get(target);
		}
		catch (NoSuchFieldException | IllegalAccessException ex) {
			return NO_FIELD;
		}
	}

	/**
	 * Sentinel marking "no matching helper method found" (distinct from a null result).
	 */
	private static final Object NO_METHOD = new Object();

	/**
	 * Invoke a public no-arg method named {@code name} declared by the target's own class
	 * (not inherited from the JDK). Used so a map-like object that also exposes helper
	 * methods — e.g. Helm's {@code .Files} with {@code .AsConfig} — resolves the method
	 * when the map key is absent. JDK map/object methods (isEmpty, size, ...) are skipped
	 * so plain maps are unaffected.
	 * @return the method result, or {@link #NO_METHOD} if no such method exists
	 */
	private static Object invokeNoArgHelperMethod(Object target, String name) {
		for (Method method : target.getClass().getMethods()) {
			if (method.getParameterCount() == 0 && method.getName().equals(name)
					&& !method.getDeclaringClass().getName().startsWith("java.")) {
				try {
					return method.invoke(target);
				}
				catch (ReflectiveOperationException ex) {
					return NO_METHOD;
				}
			}
		}
		return NO_METHOD;
	}

	private Object executeField(FieldNode fieldNode, Object data) throws TemplateExecutionException {
		Object current = data;
		for (String identifier : fieldNode.getIdentifiers()) {
			current = getFieldValue(current, identifier);
		}
		return current;
	}

	// Sentinel value indicating that no matching method was found
	private static final Object INVOKE_NOT_FOUND = new Object();

	// Sentinel for "this command has no upstream pipeline input" — distinct from an
	// upstream command that produced null, which Go still threads as the final argument.
	private static final Object NO_PIPELINE = new Object();

	/**
	 * Try to invoke the last identifier in a field chain as a method call. Go templates
	 * support method calls on values: {@code .Obj.Method arg1 arg2} calls
	 * {@code Method(arg1, arg2)} on the result of {@code .Obj}.
	 * @return the method result, or {@link #INVOKE_NOT_FOUND} if no method matched
	 */
	private Object tryFieldMethodCall(FieldNode fieldNode, List<Node> cmdArgNodes, Object data, BeanInfo beanInfo,
			Object pipelineValue) throws TemplateExecutionException {
		String[] identifiers = fieldNode.getIdentifiers();
		if (identifiers.length == 0) {
			return INVOKE_NOT_FOUND;
		}

		// Resolve all identifiers except the last to get the receiver object
		Object receiver = data;
		for (int i = 0; i < identifiers.length - 1; i++) {
			receiver = getFieldValue(receiver, identifiers[i]);
		}

		String methodName = identifiers[identifiers.length - 1];
		return tryMethodInvoke(receiver, methodName, cmdArgNodes, data, beanInfo, pipelineValue);
	}

	/**
	 * Try to invoke the last field in a chain node as a method call. Handles the parser's
	 * ChainNode structure: {@code .X.Y.Method arg} where the parser creates
	 * {@code ChainNode(FieldNode("X"), ["Y", "Method"])}.
	 * @return the method result, or {@link #INVOKE_NOT_FOUND} if no method matched
	 */
	private Object tryChainMethodCall(ChainNode chainNode, List<Node> cmdArgNodes, Object data, BeanInfo beanInfo,
			Object pipelineValue) throws TemplateExecutionException {
		List<String> fields = chainNode.getFields();
		if (fields.isEmpty()) {
			return INVOKE_NOT_FOUND;
		}

		// Resolve the chain's base node and all fields except the last
		Object receiver = executeArgument(chainNode.getNode(), data, beanInfo);
		for (int i = 0; i < fields.size() - 1; i++) {
			receiver = getFieldValue(receiver, fields.get(i));
		}

		String methodName = fields.get(fields.size() - 1);
		return tryMethodInvoke(receiver, methodName, cmdArgNodes, data, beanInfo, pipelineValue);
	}

	/**
	 * Try to invoke the last identifier of a variable reference as a method call:
	 * {@code $var.Field.Method arg} calls {@code Method(arg)} on the result of
	 * {@code $var.Field}. Mirrors {@link #tryFieldMethodCall} for variable-rooted chains
	 * such as {@code $.Files.Get "path"}.
	 * @return the method result, or {@link #INVOKE_NOT_FOUND} if no method matched
	 */
	private Object tryVariableMethodCall(VariableNode variableNode, List<Node> cmdArgNodes, Object data,
			BeanInfo beanInfo, Object pipelineValue) throws TemplateExecutionException {
		String varName = variableNode.getIdentifier(0);
		if (!variables.containsKey(varName)) {
			return INVOKE_NOT_FOUND;
		}
		Object receiver = variables.get(varName);
		String[] identifiers = variableNode.getIdentifiers();
		// Resolve all identifiers except the last to get the receiver object
		for (int i = 1; i < identifiers.length - 1; i++) {
			receiver = getFieldValue(receiver, identifiers[i]);
		}
		String methodName = identifiers[identifiers.length - 1];
		return tryMethodInvoke(receiver, methodName, cmdArgNodes, data, beanInfo, pipelineValue);
	}

	/**
	 * Core method invocation logic shared by field and chain method calls.
	 */
	private Object tryMethodInvoke(Object receiver, String methodName, List<Node> cmdArgNodes, Object data,
			BeanInfo beanInfo, Object pipelineValue) throws TemplateExecutionException {
		if (receiver == null) {
			return INVOKE_NOT_FOUND;
		}

		// Evaluate the method arguments (skip the first arg which is the node itself).
		// In a chained pipeline the upstream value is passed as the method's final
		// argument (Go: "arg | .Method x" calls Method(x, arg)).
		int argCount = cmdArgNodes.size() - 1;
		int extra = (pipelineValue != NO_PIPELINE) ? 1 : 0;
		Object[] args = new Object[argCount + extra];
		executeArguments(data, beanInfo, cmdArgNodes, 1, args);
		if (extra == 1) {
			args[args.length - 1] = pipelineValue;
		}

		// Try to find a matching method via reflection
		for (Method m : receiver.getClass().getMethods()) {
			if (m.getName().equals(methodName) && m.getParameterCount() == args.length) {
				try {
					return m.invoke(receiver, args);
				}
				catch (IllegalAccessException | InvocationTargetException ex) {
					if (log.isDebugEnabled()) {
						log.debug("Method invocation failed: {}.{}(): {}", receiver.getClass().getSimpleName(),
								methodName, ex.getMessage());
					}
				}
			}
		}

		return INVOKE_NOT_FOUND;
	}

	private Object executeFunction(IdentifierNode identifierNode, List<Node> cmdArgNodes, Object data,
			BeanInfo beanInfo, Object finalValue) throws TemplateExecutionException {
		String identifier = identifierNode.getIdentifier();

		// Go special-cases and/or in the evaluator: operands are evaluated left to right
		// and
		// evaluation stops at the first falsy (and) / truthy (or) operand, so a later
		// operand
		// that would error is never reached. Intercept before the eager argument
		// evaluation
		// below so {{and false (index .s 99)}} matches Go instead of throwing.
		if (("and".equals(identifier) || "or".equals(identifier)) && functions.containsKey(identifier)) {
			return executeAndOr("and".equals(identifier), cmdArgNodes, data, beanInfo, finalValue);
		}

		if (functions.containsKey(identifier)) {
			Function function = functions.get(identifier);
			if (function == null) {
				throw new TemplateExecutionException("call of null for " + identifier);
			}

			// Arguments are cmdArgNodes[1..] (index 0 is the function name); evaluate
			// them
			// directly from that offset so the hot call path allocates no subList
			// wrapper.
			int argCount = cmdArgNodes.size() - 1;

			Object[] functionArgs;
			if (finalValue != NO_PIPELINE) {

				// per https://pkg.go.dev/text/template, "In a chained pipeline, the
				// result of
				// each command is passed as the last argument of the following command."
				// (This is necessary
				// when implementing functions like 'default', for example.) The value is
				// threaded even when it is null — Go passes a nil pipeline value through.

				functionArgs = new Object[argCount + 1];
				executeArguments(data, beanInfo, cmdArgNodes, 1, functionArgs);
				functionArgs[argCount] = finalValue;
			}
			else {
				functionArgs = new Object[argCount];
				executeArguments(data, beanInfo, cmdArgNodes, 1, functionArgs);
			}

			return function.invoke(functionArgs);
		}

		throw new TemplateExecutionException(String.format("%s is not a defined function", identifier));
	}

	/**
	 * Short-circuiting {@code and}/{@code or}. Operands (the command arguments, then the
	 * threaded pipeline value if any) are evaluated left to right; {@code and} returns
	 * the first falsy operand, {@code or} the first truthy one, otherwise the last
	 * operand — matching Go, which never evaluates operands past the deciding one.
	 */
	private Object executeAndOr(boolean isAnd, List<Node> cmdArgNodes, Object data, BeanInfo beanInfo,
			Object finalValue) throws TemplateExecutionException {
		Object result = Boolean.FALSE;
		// Operands are cmdArgNodes[1..]; iterate from that offset (no subList wrapper).
		for (int i = 1; i < cmdArgNodes.size(); i++) {
			result = executeArgument(cmdArgNodes.get(i), data, beanInfo);
			if (isTrue(result) != isAnd) {
				return result;
			}
		}
		if (finalValue != NO_PIPELINE) {
			result = finalValue;
		}
		return result;
	}

	// Evaluates args[from..] into argumentValues[0..]. Takes a start index rather than a
	// subList view so the hot function-call path allocates no AbstractList$SubList
	// wrapper;
	// with CommandNode backed by ArrayList, get(i) is O(1).
	private void executeArguments(Object data, BeanInfo beanInfo, List<Node> args, int from, Object[] argumentValues)
			throws TemplateExecutionException {
		int size = args.size();
		for (int i = from; i < size; i++) {
			argumentValues[i - from] = executeArgument(args.get(i), data, beanInfo);
		}
	}

	private Object executeArgument(Node argument, Object data, BeanInfo beanInfo) throws TemplateExecutionException {
		if (argument instanceof DotNode) {
			return data;
		}

		if (argument instanceof NilNode) {
			return null;
		}

		if (argument instanceof BoolNode) {
			BoolNode boolNode = (BoolNode) argument;
			return boolNode.isValue();
		}

		if (argument instanceof StringNode) {
			StringNode stringNode = (StringNode) argument;
			return stringNode.getText();
		}

		if (argument instanceof NumberNode) {
			NumberNode numberNode = (NumberNode) argument;
			if (numberNode.isInt()) {
				return numberNode.getIntValue();
			}
			if (numberNode.isFloat()) {
				return numberNode.getFloatValue();
			}
			return 0;
		}

		if (argument instanceof FieldNode) {
			FieldNode fieldNode = (FieldNode) argument;
			return executeField(fieldNode, data);
		}

		if (argument instanceof ChainNode) {
			ChainNode chainNode = (ChainNode) argument;
			return executeChain(chainNode, data, beanInfo);
		}

		if (argument instanceof PipeNode) {
			PipeNode pipeNode = (PipeNode) argument;
			return executePipe(pipeNode, data, beanInfo);
		}

		if (argument instanceof VariableNode) {
			return executeVariable((VariableNode) argument);
		}

		if (argument instanceof CommandNode) {
			CommandNode commandNode = (CommandNode) argument;
			return executeCommand(commandNode, data, beanInfo, NO_PIPELINE);
		}

		if (argument instanceof IdentifierNode) {
			IdentifierNode identifierNode = (IdentifierNode) argument;
			String identifier = identifierNode.getIdentifier();
			// Check if it's a function
			if (functions.containsKey(identifier)) {
				Function function = functions.get(identifier);
				if (function != null) {
					return function.invoke(new Object[0]);
				}
			}
			// Otherwise just return the identifier as a string
			return identifier;
		}

		throw new TemplateExecutionException(String.format("can't extract value of argument %s (type: %s)", argument,
				argument.getClass().getSimpleName()));
	}

	private Object executeVariable(VariableNode variableNode) throws TemplateExecutionException {
		String varName = variableNode.getIdentifier(0);
		if (!variables.containsKey(varName)) {
			throw new TemplateExecutionException(String.format("undefined variable: %s", varName));
		}
		Object current = variables.get(varName);
		// Chain through remaining identifiers (e.g., $config.expose.nested)
		String[] identifiers = variableNode.getIdentifiers();
		for (int i = 1; i < identifiers.length; i++) {
			current = getFieldValue(current, identifiers[i]);
		}
		return current;
	}

	/**
	 * Introspect the data object with caching for performance
	 * @param data Data object for the template
	 * @return BeanInfo telling the details of data object
	 */
	private BeanInfo getBeanInfo(Object data) {
		if (data == null) {
			return null;
		}
		if (data instanceof Map) {
			return null; // We handle Maps directly in field access
		}

		Class<?> type = data.getClass();

		return beanInfoCache.computeIfAbsent(type, (clazz) -> {
			try {
				return Introspector.getBeanInfo(clazz);
			}
			catch (IntrospectionException ex) {
				if (log.isDebugEnabled()) {
					log.debug("Failed to introspect class {}: {}", clazz.getName(), ex.getMessage());
				}
				return null;
			}
		});
	}

	/**
	 * Get go style property name
	 * @param propertyDescriptorName Name of property in an object
	 * @return Go style property name
	 */
	private String toGoStylePropertyName(String propertyDescriptorName) {
		return Character.toUpperCase(propertyDescriptorName.charAt(0)) + propertyDescriptorName.substring(1);
	}

	/**
	 * Resolve the read method for {@code identifier} on {@code type}, matched against
	 * both the JavaBean property name and its Go-style (capitalised) form. The per-class
	 * accessor map is built once and cached, replacing a linear
	 * {@link PropertyDescriptor} scan (with a per-property string allocation) on every
	 * field access.
	 * @param type the runtime type being accessed
	 * @param identifier the template field name
	 * @return the matching read method, or {@code null} if no property matches
	 */
	private Method accessorFor(Class<?> type, String identifier) {
		return accessorCache.computeIfAbsent(type, this::buildAccessors).get(identifier);
	}

	private Map<String, Method> buildAccessors(Class<?> type) {
		Map<String, Method> accessors = new HashMap<>();
		try {
			for (PropertyDescriptor descriptor : Introspector.getBeanInfo(type).getPropertyDescriptors()) {
				String name = descriptor.getName();
				Method readMethod = descriptor.getReadMethod();
				if ("class".equals(name) || readMethod == null) {
					continue;
				}
				// First descriptor wins on a key collision, matching the old linear scan.
				accessors.putIfAbsent(name, readMethod);
				accessors.putIfAbsent(toGoStylePropertyName(name), readMethod);
			}
		}
		catch (IntrospectionException ex) {
			if (log.isDebugEnabled()) {
				log.debug("Failed to introspect class {}: {}", type.getName(), ex.getMessage());
			}
		}
		return accessors;
	}

	/**
	 * Determine if a pipe evaluation returns a positive result, such as 'true' for a
	 * bool, a none-null value for an object, a none-empty array or list
	 * @param value The result of the pipe evaluation
	 * @return true if evaluation returns a positive result
	 */
	private boolean isTrue(Object value) {
		return Functions.isTrue(value);
	}

	private void printText(Writer writer, String text) throws IOException {
		writer.write(text);
	}

	private void printValue(Writer writer, Object value) throws IOException, TemplateExecutionException {
		ValuePrinter.printValue(writer, value, missingKey);
	}

	// One block-scoped `:=` declaration's undo record: restore {@code previous} if the
	// name
	// {@code existed} before the declaration, otherwise remove it, when the block exits.
	private record ScopeUndo(String name, boolean existed, Object previous) {
	}

}
