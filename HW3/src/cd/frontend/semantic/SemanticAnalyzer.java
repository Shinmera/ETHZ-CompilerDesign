package cd.frontend.semantic;

import java.util.List;

import cd.Main;
import cd.ToDoException;
import cd.ir.Ast.ClassDecl;

public class SemanticAnalyzer {
	
	public final Main main;
	SemanticAstVisitor visitor;
	
	public SemanticAnalyzer(Main main) {
		this.main = main;
	}
	
	/**
	 * 
	 * 
	 * @param classDecls
	 * @throws SemanticFailure
	 */
	public void check(List<ClassDecl> classDecls) 
	throws SemanticFailure {
		{
			//throw new ToDoException();
			
			/*
			 *  1. 
			 *  Collect type information: Traverse AST, typecheck() for each node and create nested 
			 *  symbol tables and context.
			 *  
			 *  For each class, type, variable etc. create new Symbol and add to corresponding symbol table.
			 */
						
			visitor = new SemanticAstVisitor();
			
			// 1.1 Traverse AST with visitor
			for ( ClassDecl c : classDecls) {
				c.accept(visitor, null);
			}
			
			// 2. Check for correctness: Traverse AST upwards and check if correct in context of parents.
		}
	}

}
