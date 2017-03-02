package cd.util.debug;

import cd.ir.Ast;
import cd.ir.Ast.Assign;
import cd.ir.Ast.BinaryOp;
import cd.ir.Ast.BooleanConst;
import cd.ir.Ast.BuiltInRead;
import cd.ir.Ast.BuiltInWrite;
import cd.ir.Ast.BuiltInWriteln;
import cd.ir.Ast.Cast;
import cd.ir.Ast.ClassDecl;
import cd.ir.Ast.Field;
import cd.ir.Ast.IfElse;
import cd.ir.Ast.Index;
import cd.ir.Ast.IntConst;
import cd.ir.Ast.MethodDecl;
import cd.ir.Ast.NewArray;
import cd.ir.Ast.NewObject;
import cd.ir.Ast.Nop;
import cd.ir.Ast.NullConst;
import cd.ir.Ast.Seq;
import cd.ir.Ast.ThisRef;
import cd.ir.Ast.UnaryOp;
import cd.ir.Ast.Var;
import cd.ir.Ast.VarDecl;
import cd.ir.Ast.WhileLoop;
import cd.ir.AstVisitor;

public class AstOneLine {
	
	public static String toString(Ast ast) {
		return new Visitor().visit(ast, null);
	}

	protected static class Visitor extends AstVisitor<String, Void> {
		
		public String str(Ast ast) {
			return ast.accept(this, null);
		}

		@Override
		public String assign(Assign ast, Void arg) {
			return String.format("%s = %s", str(ast.left()), str(ast.right()));
		}

		@Override
		public String binaryOp(BinaryOp ast, Void arg) {
			return String.format("(%s %s %s)", 
					str(ast.left()), ast.operator.repr, str(ast.right()));
		}

		@Override
		public String booleanConst(BooleanConst ast, Void arg) {
			return Boolean.toString(ast.value);
		}

		@Override
		public String builtInRead(BuiltInRead ast, Void arg) {
			return String.format("read()");
		}
		
		@Override
		public String builtInWrite(BuiltInWrite ast, Void arg) {
			return String.format("write(%s)", str(ast.arg()));
		}
		
		@Override
		public String builtInWriteln(BuiltInWriteln ast, Void arg) {
			return String.format("writeln()");
		}

		@Override
		public String cast(Cast ast, Void arg) {
			return String.format("(%s)(%s)", ast.typeName, str(ast.arg()));
		}

		@Override
		public String classDecl(ClassDecl ast, Void arg) {
			return String.format("class %s {...}", ast.name);
		}

		@Override
		public String field(Field ast, Void arg) {
			return String.format("%s.%s", str(ast.arg()), ast.fieldName);
		}

		@Override
		public String ifElse(IfElse ast, Void arg) {
			return String.format("if (%s) {...} else {...}", str(ast.condition()));
		}

		@Override
		public String index(Index ast, Void arg) {
			return String.format("%s[%s]", str(ast.left()), str(ast.right()));
		}

		@Override
		public String intConst(IntConst ast, Void arg) {
			return Integer.toString(ast.value);
		}

		@Override
		public String methodDecl(MethodDecl ast, Void arg) {
			return String.format("%s %s(...) {...}", ast.returnType, ast.name);
		}

		@Override
		public String newArray(NewArray ast, Void arg) {
			return String.format("new %s[%s]", ast.typeName, str(ast.arg()));
		}

		@Override
		public String newObject(NewObject ast, Void arg) {
			return String.format("new %s()", ast.typeName);
		}

		@Override
		public String nop(Nop ast, Void arg) {
			return "nop";
		}

		@Override
		public String nullConst(NullConst ast, Void arg) {
			return "null";
		}

		@Override
		public String seq(Seq ast, Void arg) {
			return "(...)";
		}

		@Override
		public String thisRef(ThisRef ast, Void arg) {
			return "this";
		}
		
		@Override
		public String unaryOp(UnaryOp ast, Void arg) {
			return String.format("%s(%s)", ast.operator.repr, str(ast.arg()));
		}

		@Override
		public String var(Var ast, Void arg) {
			{
				return ast.name;
			}
		}

		@Override
		public String varDecl(VarDecl ast, Void arg) {
			return String.format("%s %s", ast.type, ast.name);
		}

		@Override
		public String whileLoop(WhileLoop ast, Void arg) {
			return String.format("while (%s) {...}", str(ast.condition()));
		}
		
	}
}
