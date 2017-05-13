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
import cd.ir.Symbol.VariableSymbol;
import cd.util.debug.AstOneLine;

/**
 * Generates code to process statements and declarations.
 */
class StmtGenerator extends AstVisitor<Register, Object> {
    protected final AstCodeGenerator cg;

    StmtGenerator(AstCodeGenerator astCodeGenerator) {
        cg = astCodeGenerator;
    }

    public void gen(Ast ast) {
        gen(ast, null);
    }

    public void gen(Ast ast, Object arg){
        visit(ast, arg);
    }

    @Override
    public Register visit(Ast ast, Object arg) {
        try {
            cg.emit.increaseIndent("Emitting " + AstOneLine.toString(ast));
            return super.visit(ast, arg);
        } finally {
            cg.emit.decreaseIndent();
        }
    }

    @Override
    public Register methodCall(MethodCall ast, Object dummy) {
        Register reg = cg.eg.methodCall(ast.getMethodCallExpr(), null);
        cg.rm.releaseRegister(reg);
        return null;
    }

    public Register methodCall(MethodSymbol sym, List<Expr> allArguments) {
        throw new RuntimeException("Not required");
    }

    @Override
    public Register classDecl(ClassDecl ast, Object arg) {
        // Emit vtable
        cg.emit.emitRaw(Config.DATA_INT_SECTION);
        cg.emit.emitLabel(ast.name);
        // Header with parent pointer.
        if(ast.sym.superClass != null){
            cg.emit.emitRaw(Config.DOT_INT+" "+ast.sym.superClass.name);
        }else{
            cg.emit.emitRaw(Config.DOT_INT+" 0");
        }
        // All the method pointers.
        for(MethodSymbol method : ast.sym.effectiveMethods){
            cg.emit.emitRaw(Config.DOT_INT+" "+method.getLabel());
        }
        // Emit all method bodies.
        cg.emit.emitRaw(Config.TEXT_SECTION);
        for(MethodDecl method : ast.methods()){
            cg.sg.gen(method);
        }
        return null;
    }

    @Override
    public Register methodDecl(MethodDecl ast, Object arg) {
        cg.emit.emitRaw(".globl "+ast.sym.getLabel());
        cg.emit.emitLabel(ast.sym.getLabel());
            
        // Allocate a new stack frame
        cg.emit.emit("pushl", "%ebp");
        cg.emit.emit("movl", "%esp", "%ebp");
        
        // Write out the declarations.
        int stackSize = 0;
        for(VariableSymbol var : ast.sym.locals.values()){
            cg.emit.emit("pushl", "$0");
            stackSize += 4;
        }

        // Conservatively push callee-saved registers.
        // This could be improved by only saving the register
        // once it is actually being used. This would require
        // tracking which registers have already been "touched"
        // within a method declaration and the point at which
        // they are released for the last time.
        for(Register save : cg.rm.CALLEE_SAVE){
            cg.emit.emit("pushl", save);
        }
            
        // Generate the actual body.
        cg.sg.gen(ast.body(), ast.sym);
        
        // Generate exit label.
        cg.emit.emitLabel(ast.sym.getLabel()+".exit");

        // Pop callee-saved registers.
        for(Register save : cg.rm.CALLEE_SAVE){
            cg.emit.emit("popl", save);
        }
        
        // Restore stack size lost to local variables
        cg.emit.emit("addl", "$"+stackSize, "%esp");
        
        cg.emit.emit("popl", "%ebp");
        cg.emit.emitRaw("ret");
        return null;
    }

    @Override
    public Register ifElse(IfElse ast, Object arg) {
        Register reg;
        String elseLabel = cg.emit.uniqueLabel();

        reg = cg.eg.gen(ast.condition());
        // Anything greater than zero is "true"
        cg.emit.emit("cmpl", "$0", reg);
        cg.emit.emit("je", elseLabel);
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
    public Register whileLoop(WhileLoop ast, Object arg) {
        Register reg;
        String testLabel = cg.emit.uniqueLabel();
        String endLabel = cg.emit.uniqueLabel();

        cg.emit.emitLabel(testLabel);
        reg = cg.eg.gen(ast.condition());
        cg.emit.emit("cmpl", "$0", reg);
        cg.emit.emit("je", endLabel);
        cg.rm.releaseRegister(reg);

        cg.sg.gen(ast.body());
        cg.emit.emit("jmp", testLabel);
        cg.emit.emitLabel(endLabel);

        return null;
    }

    @Override
    public Register assign(Assign ast, Object arg) {
        Register left = cg.eg.gen(ast.left(), true);
        Register right = cg.eg.gen(ast.right());
        cg.emit.emit("movl", right, "("+left+")");
        cg.rm.releaseRegister(right);
        cg.rm.releaseRegister(left);
        return null;
    }

    @Override
    public Register builtInWrite(BuiltInWrite ast, Object arg) {
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
    public Register builtInWriteln(BuiltInWriteln ast, Object arg) {
        {
            cg.emit.emit("sub", constant(16), STACK_REG);
            cg.emit.emitStore("$STR_NL", 0, STACK_REG);
            cg.emit.emit("call", Config.PRINTF);
            cg.emit.emit("add", constant(16), STACK_REG);
            return null;
        }
    }

    @Override
    public Register returnStmt(ReturnStmt ast, Object arg) {
        if(ast.arg() == null){
            cg.emit.emit("movl", "$0", "%eax");
        }else{
            Register reg = cg.eg.gen(ast.arg());
            if(reg != RegisterManager.Register.EAX){
                cg.emit.emit("movl", reg, "%eax");
                cg.rm.releaseRegister(reg);
            }
        }
        cg.emit.emit("jmp", ((MethodSymbol)arg).getLabel()+".exit");
        return null;
    }
}
