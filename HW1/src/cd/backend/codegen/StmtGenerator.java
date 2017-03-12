package cd.backend.codegen;

import cd.Config;
import cd.ToDoException;
import cd.backend.codegen.RegisterManager.Register;
import cd.ir.Ast;
import cd.ir.Ast.Assign;
import cd.ir.Ast.BuiltInWrite;
import cd.ir.Ast.BuiltInWriteln;
import cd.ir.Ast.IfElse;
import cd.ir.Ast.MethodCall;
import cd.ir.Ast.MethodDecl;
import cd.ir.Ast.Var;
import cd.ir.Ast.VarDecl;
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
        // Dumb, global space for now.
    	cg.emit.emitRaw(Config.DATA_STR_SECTION);
    	cg.emit.emitRaw("printfinteger: "+Config.DOT_STRING+" \"%d\"");
    	cg.emit.emitRaw("scanfinteger: "+Config.DOT_STRING+" \"%d\"");
    	cg.emit.emitRaw("printfnewline: "+Config.DOT_STRING+" \"\\n\"");
    	cg.emit.emitRaw(Config.DATA_INT_SECTION);
    	this.visit(ast.decls(), arg);
    	cg.emit.emitRaw(Config.TEXT_SECTION);
    	cg.emit.emitRaw(".global "+Config.MAIN);
    	cg.emit.emitRaw(Config.MAIN+":");
    	
    	cg.emit.emitRaw(".align 16");
    	
    	this.visit(ast.body(), arg);
    	cg.emit.emit("movl", "$0", "%eax");
    	cg.emit.emitRaw("ret");
    	return null;
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
    public Register varDecl(VarDecl ast, Void arg){
    	cg.emit.emitRaw("var"+ast.name+": "+Config.DOT_INT+" 0");
        return null;
    }

    @Override
    public Register assign(Assign ast, Void arg) {
    	// Compute value first to reduce register pressure.
        Register value = cg.eg.visit(ast.right(), arg);
        Register place = cg.sg.visit(ast.left(), arg);
        cg.emit.emit("movl", value, "("+place.repr+")");
        cg.rm.releaseRegister(place);
        cg.rm.releaseRegister(value);
        return null;
    }

    @Override
    public Register builtInWrite(BuiltInWrite ast, Void arg) {
    	// I had code here that returned the value from the PRINTF
    	// call, but apparently Registers from a top-level method
    	// expression are not being used in any way anyway, so...
        //
        // Hooray for built-in memory (register) leaks from the
        // provided framework.
    	cg.withRegistersSaved(()->{
                Register value = cg.eg.visit(ast.arg(), arg);
                cg.emit.emit("subl", "$8", "%esp");
                cg.emit.emit("movl", value, "4(%esp)");
                cg.emit.emit("movl", "$printfinteger", "0(%esp)");
                cg.emit.emit("call", cd.Config.PRINTF);
                cg.emit.emit("addl", "$8", "%esp");
                cg.rm.releaseRegister(value);
            }, new Register[]{}, new String[]{"%eax"});
        return null;
    }

    @Override
    public Register builtInWriteln(BuiltInWriteln ast, Void arg) {
        cg.withRegistersSaved(()->{
                cg.emit.emit("subl", "$16", "%esp");
                cg.emit.emit("movl", "$printfnewline", "0(%esp)");
                cg.emit.emit("call", cd.Config.PRINTF);
                cg.emit.emit("addl", "$16", "%esp");
            }, new Register[]{}, new String[]{"%eax"});
        return null;
    } 
    
    @Override
    public Register var(Var ast, Void arg) {
    	Register place = cg.rm.getRegister();
    	cg.emit.emit("leal", "var"+ast.name, place);
    	return place;
    }

}
