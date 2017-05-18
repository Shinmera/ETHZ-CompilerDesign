package cd.frontend.semantic;

import cd.ir.Ast;
import cd.ir.Ast.IfElse;
import cd.ir.Ast.ReturnStmt;
import cd.ir.Ast.Seq;
import cd.ir.Ast.Stmt;
import cd.ir.Ast.WhileLoop;
import cd.ir.AstVisitor;



/**
 * Visitor that checks if all paths of a given sequence have a
 * return statement.
 * 
 * This visitor only needs to be used if are not using the Control Flow Graph.
 * 
 * @author Leo Buttiker
 */
public class ReturnCheckerVisitor extends AstVisitor<Boolean, Void> {	

	@Override
	protected Boolean dfltStmt(Stmt ast, Void arg) {
		return false;
	}
	
	@Override
	public Boolean returnStmt(ReturnStmt ast, Void arg) {
		return true;
	}

	@Override
	public Boolean ifElse(IfElse ast, Void arg) {
		boolean allPathHaveAReturnStmt = true;			
		allPathHaveAReturnStmt &= visit(ast.then(), null);
		allPathHaveAReturnStmt &= visit(ast.otherwise(), null);
		return allPathHaveAReturnStmt;
	}

	@Override
	public Boolean seq(Seq ast, Void arg) {
		
		boolean allPathHaveAReturnStmt = false;	
		for (Ast child : ast.children()) {
			allPathHaveAReturnStmt |= this.visit(child, null);
		}
		return allPathHaveAReturnStmt;
	}
	
	@Override
	public Boolean whileLoop(WhileLoop ast, Void arg) {
		return false;		
	}
	
}