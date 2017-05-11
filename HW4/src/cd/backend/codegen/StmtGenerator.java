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
class StmtGenerator extends AstVisitor<Register, ClassDecl> {
    protected final AstCodeGenerator cg;

    StmtGenerator(AstCodeGenerator astCodeGenerator) {
        cg = astCodeGenerator;
    }

    public void gen(Ast ast) {
        visit(ast, null);
    }

    @Override
    public Register visit(Ast ast, ClassDecl arg) {
        try {
            cg.emit.increaseIndent("Emitting " + AstOneLine.toString(ast));
            return super.visit(ast, arg);
        } finally {
            cg.emit.decreaseIndent();
        }
    }

    @Override
    public Register methodCall(MethodCall ast, ClassDecl dummy) {
        Register reg = stmt.eg.methodCall(ast.getMethodCallExpr(), dummy);
        cg.rm.releaseRegister(reg);
        return null;
    }

    public Register methodCall(MethodSymbol sym, List<Expr> allArguments) {
        throw new RuntimeException("Not required");
    }

    // Emit vtable for arrays of this class:
    @Override
    public Register classDecl(ClassDecl ast, ClassDecl arg) {
        {
            throw new ToDoException();
        }
    }

    @Override
    public Register methodDecl(MethodDecl ast, ClassDecl _class) {
        // // ------------------------------------------------------------
        // // Homework 1 Prologue Generation:
        // // Rather simplistic due to limited requirements!

        // if (!ast.name.equals("main"))
        //     throw new RuntimeException(
        //                                "Only expected one method named 'main'");

        // // Emit some useful string constants:
        // cg.emit.emitRaw(Config.DATA_STR_SECTION);
        // cg.emit.emitLabel("STR_NL");
        // cg.emit.emitRaw(Config.DOT_STRING + " \"\\n\"");
        // cg.emit.emitLabel("STR_D");
        // cg.emit.emitRaw(Config.DOT_STRING + " \"%d\"");

        // // Emit a label for each variable:
        // // Let the AST Visitor do the iteration for us.
        // cg.emit.emitRaw(Config.DATA_INT_SECTION);
        // ast.decls().accept(new AstVisitor<Void, Void>() {
        //         @Override
        //         public Void varDecl(VarDecl ast, Void arg) {
        //             if (!ast.type.equals("int"))
        //                 throw new RuntimeException(
        //                                            "Only int variables expected");
        //             cg.emit.emitLabel(AstCodeGenerator.VAR_PREFIX + ast.name);
        //             cg.emit.emitConstantData("0");
        //             return null;
        //         }
        //     }, null);

        // // Emit the main() method:
        // cg.emit.emitRaw(Config.TEXT_SECTION);
        // cg.emit.emitRaw(".globl " + MAIN);
        // cg.emit.emitLabel(MAIN);

        // cg.emit.emit("enter", "$8", "$0");
        // cg.emit.emit("and", -16, STACK_REG);
        // gen(ast.body());
        // cg.emitMethodSuffix(true);

        cg.emit.emitLabel(ast.name+"@"+_class.name);
            
        // Allocate a new stack frame
        cg.emit.emit("pushl", "%ebp");
        cg.emit.emit("movl", "%esp", "%ebp");

        // Read out the arguments.
        // FIXME

        // Write out the declarations.
        // FIXME
            
        // Generate the actual body.
        cg.sg.gen(ast.body());

        // Pop the stack frame and return. This is usually
        // already done by the return statement.
        cg.emit.emit("popl", "%ebp");
        cg.emit.emit("ret");
        return null;
    }

    @Override
    public Register ifElse(IfElse ast, ClassDecl arg) {
        Register reg;
        String elseLabel = cg.emit.uniqueLabel();

        reg = cg.eg.gen(ast.condition());
        // Anything greater than zero is "true"
        cg.emit.emit("cmp", reg, "$0");
        cg.emit.emit("jne", elseLabel);
        cg.rm.releaseRegister(reg);
        
        cg.sg.gen(ast.then());

        if(ast.otherwise() == null){
            cg.emit.emitLabel(elseLabel);
        }else{
            String endLabel = cg.emit.uniqueLabel();
            cg.emit.emit("jmp", endLabel);
            
            cg.emit.emitLabel(elseLabel);
            cg.sg.gen(ast.otherwise());

            cg.emit.emitLabel(endLabel);
        }
        return null;
    }

    @Override
    public Register whileLoop(WhileLoop ast, ClassDecl arg) {
        Register reg;
        String testLabel = cg.emit.uniqueLabel();
        String endLabel = cg.emit.uniqueLabel();

        cg.emit.emitLabel(testLabel);
        reg = cg.eg.gen(ast.condition());
        cg.emit.emit("cmpl", reg, "$0");
        cg.emit.emit("jne", endLabel);
        cg.rm.releaseRegister(reg);

        cg.sg.gen(ast.body());
        cg.emit.emit("jmp", testLabel);
        cg.emit.emitLabel(endLabel);

        return null;
    }

    @Override
    public Register assign(Assign ast, ClassDecl arg) {
        Register left = cg.eg.gen(ast.left());
        Register right = cg.eg.gen(ast.right());
        cg.emit.emit("movl", right, "("+left+")");
        cg.rm.releaseRegister(right);
        cg.rm.releaseRegister(left);
        return null;
    }

    @Override
    public Register builtInWrite(BuiltInWrite ast, ClassDecl arg) {
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
    public Register builtInWriteln(BuiltInWriteln ast, ClassDecl arg) {
        {
            cg.emit.emit("sub", constant(16), STACK_REG);
            cg.emit.emitStore("$STR_NL", 0, STACK_REG);
            cg.emit.emit("call", Config.PRINTF);
            cg.emit.emit("add", constant(16), STACK_REG);
            return null;
        }
    }

    @Override
    public Register returnStmt(ReturnStmt ast, ClassDecl arg) {
        if(ast.arg() == null){
            cg.emit.emit("movl", "$0", "%eax");
        }else{
            Register reg = cg.eg.gen(ast.arg());
            if(reg != Register.RegisterManager.EAX){
                cg.emit.emit("movl", reg, "%eax");
                cg.rm.releaseRegister(reg);
            }
        }
        cg.emit.emit("popl", "%ebp");
        cg.emit.emit("ret");
        return null;
    }

}
