package cd.frontend.parser;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.misc.NotNull;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;

import cd.ToDoException;
import cd.frontend.parser.JavaliParser.ClassDeclContext;
import cd.frontend.parser.JavaliParser.DeclarationContext;
import cd.frontend.parser.JavaliParser.FormalParameterListContext;
import cd.frontend.parser.JavaliParser.MethodDeclarationContext;
import cd.frontend.parser.JavaliParser.StatementContext;
import cd.frontend.parser.JavaliParser.VariableDeclarationContext;
import cd.ir.Ast;
import cd.ir.Ast.ClassDecl;
import cd.ir.Ast.Decl;
import cd.ir.Ast.MethodDecl;
import cd.ir.Ast.Seq;
import cd.ir.Ast.VarDecl;
import cd.util.Pair;

public final class JavaliAstVisitor extends JavaliBaseVisitor<Void> {

	public List<ClassDecl> classDecls = new ArrayList<>();
	public List<Decl> decls = new ArrayList<>();
	public List<VarDecl> varDecls = new ArrayList<>();
	public List<MethodDecl> methodDecls = new ArrayList<>();
	public List<Pair<String>> formalParameters = new ArrayList<Pair<String>>();

	@Override
	public Void visitClassDecl(ClassDeclContext ctx) {

		// Superclass.
		String sClass = "Object";

		/*
		 * 1. Get Tokens
		 */
		// Class name
		TerminalNode name = ctx.Identifier(0);

		// Superclass
		TerminalNode superclass = ctx.Identifier(1);
		if (superclass != null) {
			sClass = superclass.getText();
		}

		// Get the list of all members.
		for (DeclarationContext c : ctx.declaration()) {
			c.accept(this);
		}

		/*
		 * 2. Create AST
		 */
		/*
		 * // Test VarDecl decl2 = new VarDecl("int", "i"); VarDecl decl3 = new
		 * VarDecl("int", "j"); List <Decl> decls2 = new ArrayList<Decl>();
		 * decls2.add(decl2); decls2.add(decl3);
		 */

		// TODO decls List<Decl> not working. See ClassDecl definition of the
		// field 'members'.
		Ast.ClassDecl decl = new Ast.ClassDecl(name.getText(), sClass, decls);
		classDecls.add(decl);

		return null;
	}

	@Override
	public Void visitDeclaration(DeclarationContext ctx) {

		// List<Decl> list = new ArrayList<Ast.Decl>();

		// Variable
		ctx.variableDeclaration().accept(this);

		// Method
		ctx.methodDeclaration().accept(this);

		decls.addAll(varDecls);
		decls.addAll(methodDecls);

		return null;

	}

	@Override
	public Void visitVariableDeclaration(VariableDeclarationContext ctx) {

		// TEST for test case 6.
		/*
		 * Ast.VarDecl varDecl = new Ast.VarDecl("int", "i");
		 * varDecls.add(varDecl);
		 */

		// Type
		TerminalNode type = ctx.Type();

		// Identifiers
		List<TerminalNode> identifiers = ctx.Identifier();
		
		// Add to list.
		for (TerminalNode t : identifiers) {
			varDecls.add(new Ast.VarDecl(type.getText(), t.getText()));
		}

		return null;
	}

	@Override
	public Void visitMethodDeclaration(MethodDeclarationContext ctx) {

		// return type
		TerminalNode returnType = ctx.ReturnType();

		// identifier
		TerminalNode name = ctx.Identifier();

		// Parameterlist
		ctx.formalParameterList().accept(this);

		// Variable declarations.
		for (VariableDeclarationContext c : ctx.variableDeclaration()) {
			c.accept(this);
		}

		// Statements.
		for (StatementContext c : ctx.statement()) {
			c.accept(this);
		}

		// Add to list.

		return null;
	}

	@Override
	public Void visitADD(@NotNull JavaliParser.ADDContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public Void visitSUB(@NotNull JavaliParser.SUBContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public Void visitExpression(@NotNull JavaliParser.ExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public Void visitOR(@NotNull JavaliParser.ORContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public Void visitAssignment(@NotNull JavaliParser.AssignmentContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public Void visitNewExpression(@NotNull JavaliParser.NewExpressionContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public Void visitFormalParameterList(@NotNull JavaliParser.FormalParameterListContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public Void visitINT(@NotNull JavaliParser.INTContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public Void visitREAD(@NotNull JavaliParser.READContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public Void visitDIV(@NotNull JavaliParser.DIVContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public Void visitUnit(@NotNull JavaliParser.UnitContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public Void visitNOT(@NotNull JavaliParser.NOTContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public Void visitIDENT(@NotNull JavaliParser.IDENTContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public Void visitMULT(@NotNull JavaliParser.MULTContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public Void visitBOOL(@NotNull JavaliParser.BOOLContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public Void visitAND(@NotNull JavaliParser.ANDContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public Void visitStatement(@NotNull JavaliParser.StatementContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public Void visitUNARY(@NotNull JavaliParser.UNARYContext ctx) {
		return visitChildren(ctx);
	}

	@Override
	public Void visitWrite(@NotNull JavaliParser.WriteContext ctx) {
		return visitChildren(ctx);
	}
}
