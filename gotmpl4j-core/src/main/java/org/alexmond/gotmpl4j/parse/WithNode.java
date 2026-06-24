package org.alexmond.gotmpl4j.parse;

/**
 * A {@code {{with}}} action: a {@link BranchNode} that rebinds the dot to its pipeline's
 * value and renders the body when that value is non-empty, otherwise the {@code else}
 * body.
 */
public class WithNode extends BranchNode {

}
