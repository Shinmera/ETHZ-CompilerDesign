package cd.backend.codegen;

import java.io.Writer;
import java.util.HashMap;
import java.util.List;

import static cd.Config.MAIN;

import cd.Config;
import cd.Main;
import cd.backend.codegen.RegisterManager.Register;
import cd.ir.Ast.ClassDecl;
import cd.ir.Ast.NewObject;
import cd.ir.Symbol.ClassSymbol;

public class AstCodeGenerator {

    protected RegsNeededVisitor rnv;
	
    protected ExprGenerator eg;
    protected StmtGenerator sg;
    protected HashMap<String, ClassSymbol> classes = new HashMap<String, ClassSymbol>();
	
    protected final Main main;
	
    protected final AssemblyEmitter emit;
    protected final RegisterManager rm = new RegisterManager();

	
    protected static final String VAR_PREFIX = "var_";

    AstCodeGenerator(Main main, Writer out) {
        {
            initMethodData();
        }
		
        this.emit = new AssemblyEmitter(out);
        this.main = main;
        this.rnv = new RegsNeededVisitor();

        this.eg = new ExprGenerator(this);
        this.sg = new StmtGenerator(this);
    }

    protected void debug(String format, Object... args) {
        this.main.debug(format, args);
    }

    public static AstCodeGenerator createCodeGenerator(Main main, Writer out) {
        return new AstCodeGenerator(main, out);
    }

    public ClassSymbol getClass(String name){
        return classes.get(name);
    }
	
    /**
     * Main method. Causes us to emit x86 assembly corresponding to {@code ast}
     * into {@code file}. Throws a {@link RuntimeException} should any I/O error
     * occur.
     * 
     * <p>
     * The generated file will be divided into two sections:
     * <ol>
     * <li>Prologue: Generated by {@link #emitPrefix()}. This contains any
     * introductory declarations and the like.
     * <li>Body: Generated by {@link ExprGenerator}. This contains the main
     * method definitions.
     * </ol>
     */
    public void go(List<? extends ClassDecl> astRoots) {
        for(ClassDecl decl : astRoots){
            classes.put(decl.name, decl.sym);
            decl.sym.finalizeInheritance();
        }
        
        // Emit some useful string constants:
        emit.emitRaw(Config.DATA_STR_SECTION);
        emit.emitLabel("STR_NL");
        emit.emitRaw(Config.DOT_STRING + " \"\\n\"");
        emit.emitLabel("STR_D");
        emit.emitRaw(Config.DOT_STRING + " \"%d\"");

        emit.emitRaw(Config.TEXT_SECTION);
        
        for (ClassDecl ast : astRoots) {
            sg.gen(ast);
        }

        emit.emitComment("> Runtime");
        emit.emitRaw(".globl "+MAIN);
        emit.emitLabel(MAIN);

        emit.emit("enter", "$8", "$0");
        emit.emit("and", "$-16", "%esp");

        Register reg = eg.gen(new NewObject("Main"));
        eg.cdeclCall("Main.main", reg);
        
        emitMethodSuffix(true);

        emitExits();
    }


    protected void initMethodData() {
        {
            rm.initRegisters();
        }
    }


    protected void emitMethodSuffix(boolean returnNull) {
        if (returnNull)
            emit.emit("movl", "$0", Register.EAX);
        emit.emitRaw("leave");
        emit.emitRaw("ret");
    }

    protected void emitExits(){
        emit.emitLabel("Runtime.invalidDowncastExit");
        rm.releaseRegister(eg.cdeclCall("exit", "$1"));
        
        emit.emitLabel("Runtime.invalidArrayStoreExit");
        rm.releaseRegister(eg.cdeclCall("exit", "$2"));
        
        emit.emitLabel("Runtime.invalidArrayBoundsExit");
        rm.releaseRegister(eg.cdeclCall("exit", "$3"));
        
        emit.emitLabel("Runtime.nullPointerExit");
        rm.releaseRegister(eg.cdeclCall("exit", "$4"));
        
        emit.emitLabel("Runtime.invalidArraySizeExit");
        rm.releaseRegister(eg.cdeclCall("exit", "$5"));
        
        emit.emitLabel("Runtime.possibleInfiniteLoopExit");
        rm.releaseRegister(eg.cdeclCall("exit", "$6"));
        
        emit.emitLabel("Runtime.divisonByZeroExit");
        rm.releaseRegister(eg.cdeclCall("exit", "$7"));
        
        emit.emitLabel("Runtime.internalErrorExit");
        rm.releaseRegister(eg.cdeclCall("exit", "$22"));
    }
}
