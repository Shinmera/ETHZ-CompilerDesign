package cd.backend.codegen;

import cd.ToDoException;
import cd.backend.codegen.RegisterManager.Register;
import cd.ir.Ast;
import cd.ir.Ast.Assign;
import cd.ir.Ast.BuiltInWrite;
import cd.ir.Ast.BuiltInWriteln;
import cd.ir.Ast.IfElse;
import cd.ir.Ast.MethodCall;
import cd.ir.Ast.MethodDecl;
import cd.ir.Ast.WhileLoop;
import cd.ir.AstVisitor;
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
            throw new RuntimeException("Not required");
        }
    }

    @Override
    public Register methodDecl(MethodDecl ast, Void arg) {
        {
            // Because we only handle very simple programs in HW1,
            // you can just emit the prologue here!
            throw new ToDoException();
        }
    }

    @Override
    public Register ifElse(IfElse ast, Void arg) {
        {
            throw new RuntimeException("Not required");
        }
    }

    @Override
    public Register whileLoop(WhileLoop ast, Void arg) {
        {
            throw new RuntimeException("Not required");
        }
    }

    @Override
    public Register assign(Assign ast, Void arg) {
        {
            // Because we only handle very simple programs in HW1,
            // you can just emit the prologue here!
            throw new ToDoException();
        }
    }

    private Register printf(String format, T...args){
        // Transform format string into word sized chunks of bytes.
        int[] words = new int[format.length/4+2];
        int space = words.length+2;
        for(int i=0; i<words.length; i++){
            
        }
        Register retval = cg.rm.getRegister();
        cg.emit.emit("movl", "%eax", retval);
        // Prepare stack and load up the format string.
        cg.emit.emit("subl", "$"+space, "%esp");
        for(int i=0; i<words.length; i++){
            cg.emit.emit("movl", "$"+words[i], (4*i)+"(%esp)");
        }
        // Load up the 
        for(int i=0; i<args.length; i++){
            cg.emit.emit("movl", args[i], "(%esp)");
        }
        cg.emit.emit("call", cd.Config.PRINTF);
        // Clean up and return.
        cg.emit.emit("addl", "$"+space, "%esp");
        cg.emit.emit("xchg", "%eax", retval);
        return retval;
    }

    @Override
    public Register builtInWrite(BuiltInWrite ast, Void arg) {
        // Save EAX in case it's used
        Register retval = cg.rm.getRegister();
        cg.emit.emit("movl", "%eax", retval);
        // Perform the call as usual
        Register value = this.visit(ast.arg(), arg);
        cg.emit.emit("subl", "$8", "%esp");
        cg.emit.emit("movl", value, "4(%esp)");
        cg.emit.emit("movl", "$printfinteger", "0(%esp)");
        cg.emit.emit("call", cd.Config.PRINTF);
        cg.emit.emit("addl", "$8", "%esp");
        cg.rm.releaseRegister(value);
        // Swap out the return value in EAX
        cg.emit.emit("xchg", retval, "%eax");
        return retval;
    }

    @Override
    public Register builtInWriteln(BuiltInWriteln ast, Void arg) {
        // Save EAX in case it's used
        Register retval = cg.rm.getRegister();
        cg.emit.emit("movl", "%eax", retval);
        // Perform the call as usual
        cg.emit.emit("subl", "$4", "%esp");
        cg.emit.emit("movl", "$printfnewline", "0(%esp)");
        cg.emit.emit("call", cd.Config.PRINTF);
        cg.emit.emit("addl", "$4", "%esp");
        // Swap out the return value in EAX
        cg.emit.emit("xchg", retval, "%eax");
        return value;
    }

}
