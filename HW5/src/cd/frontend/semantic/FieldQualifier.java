package cd.frontend.semantic;

import cd.ir.Ast;
import cd.ir.Ast.Var;
import cd.ir.AstRewriteVisitor;

/**
 * Runs after the semantic check and rewrites expressions to be more normalized.
 * References to a field {@code foo} are rewritten to always use
 * {@link Ast.Field} objects (i.e., {@code this.foo}.
 */
public class FieldQualifier extends AstRewriteVisitor<Void> {

	@Override
	public Ast var(Var ast, Void arg) {
		switch (ast.sym.kind) {
			case PARAM :
			case LOCAL :
				// Leave params or local variables alone
				return ast;
			case FIELD :
				// Convert an implicit field reference to "this.foo"
				Ast.Field f = new Ast.Field(new Ast.ThisRef(), ast.name);
				f.sym = ast.sym;
				f.type = ast.type;
				return f;
		}
		throw new RuntimeException("Unknown kind of var");
	}
}