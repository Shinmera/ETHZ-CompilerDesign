package cd.ir;

import java.util.ListIterator;

public class AstRewriteVisitor<A> extends AstVisitor<Ast, A> {

	@Override
	public Ast visitChildren(Ast ast, A arg) {
		ListIterator<Ast> children = ast.rwChildren.listIterator();
		while (children.hasNext()) {
			Ast child = children.next();
			if (child != null) {
				Ast replace = visit(child, arg);
				if (replace != child) {
					children.set(replace);
					nodeReplaced(child, replace);
				}
			}
		}
		return ast;
	}
	
	/**
	 * This method is called when a node is replaced. Subclasses can override it to do some
	 * bookkeeping.
	 * <p>
	 * The default implementation does nothing.
	 */
    protected void nodeReplaced(Ast oldNode, Ast newNode) {}

}
