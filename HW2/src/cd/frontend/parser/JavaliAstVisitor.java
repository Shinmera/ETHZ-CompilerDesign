package cd.frontend.parser;

import java.util.ArrayList;
import java.util.List;

import cd.ToDoException;
import cd.frontend.parser.JavaliParser.ClassDeclContext;
import cd.ir.Ast.ClassDecl;

public final class JavaliAstVisitor extends JavaliBaseVisitor<Void> {
	
	public List<ClassDecl> classDecls = new ArrayList<>();
	@Override
	public Void visitClassDecl(ClassDeclContext ctx) {
		{
			// classDecls = ...;
			throw new ToDoException();
		}
		//return null;
	}
}
