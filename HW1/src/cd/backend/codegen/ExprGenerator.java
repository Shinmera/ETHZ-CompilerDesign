package cd.backend.codegen;

import cd.ToDoException;
import cd.backend.codegen.RegisterManager.Register;
import cd.ir.Ast.BinaryOp;
import cd.ir.Ast.BooleanConst;
import cd.ir.Ast.BuiltInRead;
import cd.ir.Ast.Cast;
import cd.ir.Ast.Expr;
import cd.ir.Ast.Field;
import cd.ir.Ast.Index;
import cd.ir.Ast.IntConst;
import cd.ir.Ast.NewArray;
import cd.ir.Ast.NewObject;
import cd.ir.Ast.NullConst;
import cd.ir.Ast.ThisRef;
import cd.ir.Ast.UnaryOp;
import cd.ir.Ast.Var;
import cd.ir.ExprVisitor;
import cd.util.debug.AstOneLine;

/**
 * Generates code to evaluate expressions. After emitting the code, returns a
 * String which indicates the register where the result can be found.
 */
class ExprGenerator extends ExprVisitor<Register, Void> {
    protected final AstCodeGenerator cg;

    ExprGenerator(AstCodeGenerator astCodeGenerator) {
        cg = astCodeGenerator;
    }

    public Register gen(Expr ast) {
        return visit(ast, null);
    }

    @Override
    public Register visit(Expr ast, Void arg) {
        try {
            cg.emit.increaseIndent("Emitting " + AstOneLine.toString(ast));
            return super.visit(ast, null);
        } finally {
            cg.emit.decreaseIndent();
        }

    }

    @Override
    public Register binaryOp(BinaryOp ast, Void arg) {
        Register left = this.visit(ast.left(), arg);
        Register right = this.visit(ast.right(), arg);

        switch(ast.operator){
        case B_TIMES:
            cg.emit.emit("imul", right, left);
            break;
        case B_DIV:
            cg.emit.emit("idiv", right, left);
            break;
        case B_MOD:
            break;
        case B_PLUS:
            cg.emit.emit("add", right, left);
            break;
        case B_MINUS:
            cg.emit.emit("sub", right, left);
            break;
        case B_AND:
            cg.emit.emit("and", right, left);
            break;
        case B_OR:
            cg.emit.emit("or", right, left);
            break;
        case B_EQUAL:
        case B_NOT_EQUAL:
        case B_LESS_THAN:
        case B_LESS_OR_EQUAL:
        case B_GREATER_THAN:
        case B_GREATER_OR_EQUAL:
        }

        cg.rm.releaseRegister(right);
        return left;
    }

    @Override
    public Register booleanConst(BooleanConst ast, Void arg) {
        {
            throw new RuntimeException("Not required");
        }
    }

    @Override
    public Register builtInRead(BuiltInRead ast, Void arg) {
        Register value = cg.rm.getRegister();
        // Reserve stack space and load variable address into register
        cg.emit.emit("subl", "$12", "%esp");
        cg.emit.emit("leal", "8(%esp)", value);
        // Preprare stack and call scanf
        cg.emit.emit("movl", value, "4(%esp)");
        cg.emit.emit("movl", "$scanfinteger", "0(%esp)");
        cg.emit.emit("call", cd.Config.SCANF);
        // Read value out of the stack and free the allocated space
        cg.emit.emit("movl", "8(%esp)", value);
        cg.emit.emit("addl", "$12", "%esp");
        return value;
    }

    @Override
    public Register cast(Cast ast, Void arg) {
        {
            throw new RuntimeException("Not required");
        }
    }

    @Override
    public Register index(Index ast, Void arg) {
        {
            throw new RuntimeException("Not required");
        }
    }

    @Override
    public Register intConst(IntConst ast, Void arg) {
        Register value = cg.rm.getRegister();
        cg.emit.emit("movl", "$"+ast.value, value);
        return value;
    }

    @Override
    public Register field(Field ast, Void arg) {
        {
            throw new RuntimeException("Not required");
        }
    }

    @Override
    public Register newArray(NewArray ast, Void arg) {
        {
            throw new RuntimeException("Not required");
        }
    }

    @Override
    public Register newObject(NewObject ast, Void arg) {
        {
            throw new RuntimeException("Not required");
        }
    }

    @Override
    public Register nullConst(NullConst ast, Void arg) {
        {
            throw new RuntimeException("Not required");
        }
    }

    @Override
    public Register thisRef(ThisRef ast, Void arg) {
        {
            throw new RuntimeException("Not required");
        }
    }

    @Override
    public Register unaryOp(UnaryOp ast, Void arg) {
    	Register value = this.visit(ast.arg(), arg);
    	switch(ast.operator){
    	case U_BOOL_NOT:
    		cg.emit.emit("not", value);
    		break;
    	case U_MINUS:
    		cg.emit.emit("neg", value);
    		break;
    	case U_PLUS:
    		break;
    	}
    	return value;
    }
        
    @Override
    public Register var(Var ast, Void arg) {
        {
            throw new ToDoException();
        }
    }

}
