package cd.backend.codegen;

import static cd.Config.MAIN;
import static cd.backend.codegen.AssemblyEmitter.constant;
import static cd.backend.codegen.RegisterManager.STACK_REG;

import java.util.List;

import cd.Config;
import cd.ToDoException;
import cd.backend.codegen.RegisterManager.Register;
import cd.ir.Ast;
import cd.ir.Ast.Assign;
import cd.ir.Ast.BuiltInWrite;
import cd.ir.Ast.BuiltInWriteln;
import cd.ir.Ast.ClassDecl;
import cd.ir.Ast.Expr;
import cd.ir.Ast.IfElse;
import cd.ir.Ast.MethodCall;
import cd.ir.Ast.MethodDecl;
import cd.ir.Ast.ReturnStmt;
import cd.ir.Ast.Var;
import cd.ir.Ast.VarDecl;
import cd.ir.Ast.WhileLoop;
import cd.ir.AstVisitor;
import cd.ir.Symbol.MethodSymbol;
import cd.util.debug.AstOneLine;

/**
 * Generates code to process statements and declarations.
 */
class StmtGenerator extends AstVisitor<Register, Void> {
	protected final AstCodeGenerator cg;

	StmtGenerator(AstCodeGenerator astCodeGenerator) {
		cg = astCodeGenerator;
	}

	public void gen(Ast ast) {
		visit(ast, null);
	}

	@Override
	public Register visit(Ast ast, Void arg) {
		try {
			cg.emit.increaseIndent("Emitting " + AstOneLine.toString(ast));
			return super.visit(ast, arg);
		} finally {
			cg.emit.decreaseIndent();
		}
	}

	@Override
	public Register methodCall(MethodCall ast, Void dummy) {
		{
			throw new ToDoException();
		}
	}

	public Register methodCall(MethodSymbol sym, List<Expr> allArguments) {
		throw new RuntimeException("Not required");
	}

	// Emit vtable for arrays of this class:
	@Override
	public Register classDecl(ClassDecl ast, Void arg) {
		{
			if (!ast.name.equals("Main"))
				throw new RuntimeException(
						"Only expected one class, named 'main'");
			return visitChildren(ast, arg);
		}
	}

	@Override
	public Register methodDecl(MethodDecl ast, Void arg) {
		{
			// ------------------------------------------------------------
			// Homework 1 Prologue Generation:
			// Rather simplistic due to limited requirements!

			if (!ast.name.equals("main"))
				throw new RuntimeException(
						"Only expected one method named 'main'");

			// Emit some useful string constants:
			cg.emit.emitRaw(Config.DATA_STR_SECTION);
			cg.emit.emitLabel("STR_NL");
			cg.emit.emitRaw(Config.DOT_STRING + " \"\\n\"");
			cg.emit.emitLabel("STR_D");
			cg.emit.emitRaw(Config.DOT_STRING + " \"%d\"");

			// Emit a label for each variable:
			// Let the AST Visitor do the iteration for us.
			cg.emit.emitRaw(Config.DATA_INT_SECTION);
			ast.decls().accept(new AstVisitor<Void, Void>() {
				@Override
				public Void varDecl(VarDecl ast, Void arg) {
					if (!ast.type.equals("int"))
						throw new RuntimeException(
								"Only int variables expected");
					cg.emit.emitLabel(AstCodeGenerator.VAR_PREFIX + ast.name);
					cg.emit.emitConstantData("0");
					return null;
				}
			}, null);

			// Emit the main() method:
			cg.emit.emitRaw(Config.TEXT_SECTION);
			cg.emit.emitRaw(".globl " + MAIN);
			cg.emit.emitLabel(MAIN);

			cg.emit.emit("enter", "$8", "$0");
			cg.emit.emit("and", -16, STACK_REG);
			gen(ast.body());
			cg.emitMethodSuffix(true);
			return null;
		}
	}

	@Override
	public Register ifElse(IfElse ast, Void arg) {
		{
			throw new ToDoException();
		}
	}

	@Override
	public Register whileLoop(WhileLoop ast, Void arg) {
		{
			throw new ToDoException();
		}
	}

	@Override
	public Register assign(Assign ast, Void arg) {
		{
			if (!(ast.left() instanceof Var))
				throw new RuntimeException("LHS must be var in HW1");
			Var var = (Var) ast.left();
			Register rhsReg = cg.eg.gen(ast.right());
			cg.emit.emit("movl", rhsReg, AstCodeGenerator.VAR_PREFIX + var.name);
			cg.rm.releaseRegister(rhsReg);
			return null;
		}
	}

	@Override
	public Register builtInWrite(BuiltInWrite ast, Void arg) {
		{
			Register reg = cg.eg.gen(ast.arg());
			cg.emit.emit("sub", constant(16), STACK_REG);
			cg.emit.emitStore(reg, 4, STACK_REG);
			cg.emit.emitStore("$STR_D", 0, STACK_REG);
			cg.emit.emit("call", Config.PRINTF);
			cg.emit.emit("add", constant(16), STACK_REG);
			cg.rm.releaseRegister(reg);
			return null;
		}
	}

	@Override
	public Register builtInWriteln(BuiltInWriteln ast, Void arg) {
		{
			cg.emit.emit("sub", constant(16), STACK_REG);
			cg.emit.emitStore("$STR_NL", 0, STACK_REG);
			cg.emit.emit("call", Config.PRINTF);
			cg.emit.emit("add", constant(16), STACK_REG);
			return null;
		}
	}

	@Override
	public Register returnStmt(ReturnStmt ast, Void arg) {
		{
			throw new ToDoException();
		}
	}

}
