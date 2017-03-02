package cd.ir;

import cd.ir.Ast.Expr;

/**
 * A visitor that only visits {@link Expr} nodes.
 */
public class ExprVisitor<R,A> {
	/** 
	 * Recurse and process {@code ast}.  It is preferred to 
	 * call this rather than calling accept directly, since
	 * it can be overloaded to introduce memoization, 
	 * for example. */
	public R visit(Expr ast, A arg) {
		return ast.accept(this, arg);
	}
	
	/**
	 * Visits all children of the expression.  Relies on the fact
	 * that {@link Expr} nodes only contain other {@link Expr} nodes.
	 */
	public R visitChildren(Expr ast, A arg) {
		R lastValue = null;
		for (Ast child : ast.children())
			lastValue = visit((Expr)child, arg);
		return lastValue;
	}
	
	/** 
	 * The default action for default actions is to call this,
	 * which simply recurses to any children.  Also called
	 * by seq() by default. */
	protected R dfltExpr(Expr ast, A arg) {
		return visitChildren(ast, arg);
	}
	
	public R binaryOp(Ast.BinaryOp ast, A arg) {
		return dfltExpr(ast, arg);
	}

	public R booleanConst(Ast.BooleanConst ast, A arg) {
		return dfltExpr(ast, arg);
	}
	
	public R builtInRead(Ast.BuiltInRead ast, A arg) {
		return dfltExpr(ast, arg);
	}
		
	public R cast(Ast.Cast ast, A arg) {
		return dfltExpr(ast, arg);
	}
	
	public R field(Ast.Field ast, A arg) {
		return dfltExpr(ast, arg);
	}

	public R index(Ast.Index ast, A arg) {
		return dfltExpr(ast, arg);
	}

	public R intConst(Ast.IntConst ast, A arg) {
		return dfltExpr(ast, arg);
	}
		
	public R methodCall(Ast.MethodCallExpr ast, A arg) {
		return dfltExpr(ast, arg);
	}

	public R newObject(Ast.NewObject ast, A arg) {
		return dfltExpr(ast, arg);
	}

	public R newArray(Ast.NewArray ast, A arg) {
		return dfltExpr(ast, arg);
	}
	
	public R nullConst(Ast.NullConst ast, A arg) {
		return dfltExpr(ast, arg);
	}

	public R thisRef(Ast.ThisRef ast, A arg) {
		return dfltExpr(ast, arg);
	}

	public R unaryOp(Ast.UnaryOp ast, A arg) {
		return dfltExpr(ast, arg);
	}

	public R var(Ast.Var ast, A arg) {
		return dfltExpr(ast, arg);
	}
}
