package org.alexmond.gotmpl4j.parse;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * A sequence of sibling nodes, such as the body of a template, an
 * {@code if}/{@code range} branch, or an {@code else} clause. Iterable over its child
 * {@link Node}s in document order.
 */
public class ListNode implements Node, Iterable<Node> {

	private final List<Node> nodes = new LinkedList<>();

	public void append(Node node) {
		nodes.add(node);
	}

	public Node getLast() {
		return (!nodes.isEmpty()) ? nodes.get(nodes.size() - 1) : null;
	}

	public void removeLast() {
		if (!nodes.isEmpty()) {
			nodes.remove(nodes.size() - 1);
		}
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		nodes.forEach(sb::append);
		return sb.toString();
	}

	@Override
	public Iterator<Node> iterator() {
		return nodes.iterator();
	}

}
