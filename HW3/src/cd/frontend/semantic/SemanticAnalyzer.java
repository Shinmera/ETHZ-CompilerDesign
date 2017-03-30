package cd.frontend.semantic;

import java.util.List;

import cd.Main;
import cd.ToDoException;
import cd.ir.Ast.ClassDecl;

public class SemanticAnalyzer {
	
	public final Main main;
	
	public SemanticAnalyzer(Main main) {
		this.main = main;
	}
	
	public void check(List<ClassDecl> classDecls) 
	throws SemanticFailure {
		{
			throw new ToDoException();
		}
	}

}
