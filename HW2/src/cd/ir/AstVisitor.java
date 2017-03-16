package cd.ir;

import cd.ir.Ast.Decl;
import cd.ir.Ast.Expr;
import cd.ir.Ast.Stmt;

/** A visitor that visits any kind of node */
public class AstVisitor<R,A> extends ExprVisitor<R,A> {
	
	/** 
	 * Recurse and process {@code ast}.  It is preferred to 
	 * call this rather than calling accept directly, since
	 * it can be overloaded to introduce memoization, 
	 * for example. */
	public R visit(Ast ast, A arg) {
		return ast.accept(this, arg);
	}
	
	/**
	 * A handy function which visits the children of {@code ast},
	 * providing "arg" to each of them.  It returns the result of
	 * the last child in the list.  It is invoked by the method 
	 * {@link #dflt(Ast, Object)} by default.
	 */
	public R visitChildren(Ast ast, A arg) {
		R lastValue = null;
		for (Ast child : ast.children())
			lastValue = visit(child, arg);
		return lastValue;
	}
	
	/** 
	 * The default action for default actions is to call this,
	 * which simply recurses to any children.  Also called
	 * by seq() by default. */
	protected R dflt(Ast ast, A arg) {
		return visitChildren(ast, arg);
	}
	
	/** 
	 * The default action for statements is to call this */
	protected R dfltStmt(Stmt ast, A arg) {
		return dflt(ast, arg);
	}
	
	/** 
	 * The default action for expressions is to call this */
	protected R dfltExpr(Expr ast, A arg) {
		return dflt(ast, arg);
	}

	/** 
	 * The default action for AST nodes representing declarations
	 * is to call this function */
	protected R dfltDecl(Decl ast, A arg) {
		return dflt(ast, arg);
	}
	
	public R assign(Ast.Assign ast, A arg) {
		return dfltStmt(ast, arg);
	}

	public R builtInWrite(Ast.BuiltInWrite ast, A arg) {
		return dfltStmt(ast, arg);
	}

	public R builtInWriteln(Ast.BuiltInWriteln ast, A arg) {
		return dfltStmt(ast, arg);
	}
	
	public R classDecl(Ast.ClassDecl ast, A arg) {
		return dfltDecl(ast, arg);
	}
	
	public R methodDecl(Ast.MethodDecl ast, A arg) {
		return dfltDecl(ast, arg);
	}
			
	public R varDecl(Ast.VarDecl ast, A arg) {
		return dfltDecl(ast, arg);
	}
	
	public R ifElse(Ast.IfElse ast, A arg) {
		return dfltStmt(ast, arg);
	}
	
	public R returnStmt(Ast.ReturnStmt ast, A arg) {
		return dfltStmt(ast, arg);
	}

	public R methodCall(Ast.MethodCall ast, A arg) {
		return dfltStmt(ast, arg);
	}

	public R nop(Ast.Nop ast, A arg) {
		return dfltStmt(ast, arg);
	}
	
	public R seq(Ast.Seq ast, A arg) {
		return dflt(ast, arg);
	}
	
	public R whileLoop(Ast.WhileLoop ast, A arg) {
		return dfltStmt(ast, arg);
	}
}
