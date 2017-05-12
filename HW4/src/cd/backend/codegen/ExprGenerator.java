package cd.backend.codegen;

import static cd.Config.SCANF;
import static cd.backend.codegen.AssemblyEmitter.constant;
import static cd.backend.codegen.RegisterManager.STACK_REG;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

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
import cd.ir.Ast.MethodCallExpr;
import cd.ir.Ast.NewArray;
import cd.ir.Ast.NewObject;
import cd.ir.Ast.NullConst;
import cd.ir.Ast.ThisRef;
import cd.ir.Ast.UnaryOp;
import cd.ir.Ast.Var;
import cd.ir.ExprVisitor;
import cd.ir.Symbol.ClassSymbol;
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
        int leftRN = cg.rnv.calc(ast.left());
        int rightRN = cg.rnv.calc(ast.right());

        Register left, right;
        if (leftRN > rightRN) {
            left = gen(ast.left());
            right = gen(ast.right());
        } else {
            right = gen(ast.right());
            left = gen(ast.left());
        }

        cg.debug("Binary Op: %s (%s,%s)", ast, left, right);

        switch (ast.operator) {
        case B_TIMES:
            cg.emit.emit("imull", right, left);
            break;
        case B_PLUS:
            cg.emit.emit("addl", right, left);
            break;
        case B_MINUS:
            cg.emit.emit("subl", right, left);
            break;
        case B_DIV:
            // Save EAX, EBX, and EDX to the stack if they are not used
            // in this subtree (but are used elsewhere). We will be
            // changing them.
            List<Register> dontBother = Arrays.asList(right, left);
            Register[] affected = { Register.EAX, Register.EBX, Register.EDX };

            for (Register s : affected)
                if (!dontBother.contains(s) && cg.rm.isInUse(s))
                    cg.emit.emit("pushl", s);

            // FIXME check for zero division
            
            // Move the LHS (numerator) into eax
            // Move the RHS (denominator) into ebx
            cg.emit.emit("pushl", right);
            cg.emit.emit("pushl", left);
            cg.emit.emit("popl", Register.EAX);
            cg.emit.emit("popl", "%ebx");
            cg.emit.emitRaw("cltd"); // sign-extend %eax into %edx
            cg.emit.emit("idivl", "%ebx"); // division, result into edx:eax
            // Move the result into the LHS, and pop off anything we saved
            cg.emit.emit("movl", Register.EAX, left);
            for (int i = affected.length - 1; i >= 0; i--) {
                Register s = affected[i];
                if (!dontBother.contains(s) && cg.rm.isInUse(s))
                    cg.emit.emit("popl", s);
            }
            break;
        case B_AND:
            cg.emit.emit("andl", right, left);
            break;
        case B_OR:
            cg.emit.emit("orl", right, left);
            break;
        case B_EQUAL:
        case B_NOT_EQUAL:
        case B_LESS_THAN:
        case B_LESS_OR_EQUAL:
        case B_GREATER_THAN:
        case B_GREATER_OR_EQUAL:
            cg.emit.emit("cmpl", right, left);
            cg.emit.emit("movl", "$0", left);
            switch(ast.operator){
            case B_EQUAL:
                cg.emit.emit("cmove", "$1", left);
                break;
            case B_NOT_EQUAL:
                cg.emit.emit("cmovne", "$1", left);
                break;
            case B_LESS_THAN:
                cg.emit.emit("cmovl", "$1", left);
                break;
            case B_LESS_OR_EQUAL:
                cg.emit.emit("cmovle", "$1", left);
                break;
            case B_GREATER_THAN:
                cg.emit.emit("cmovg", "$1", left);
                break;
            case B_GREATER_OR_EQUAL:
                cg.emit.emit("cmovge", "$1", left);
                break;
            }
            break;
        }

        cg.rm.releaseRegister(right);
        return left;
    }

    @Override
    public Register builtInRead(BuiltInRead ast, Void arg) {
        {
            Register reg = cg.rm.getRegister();
            cg.emit.emit("sub", constant(16), STACK_REG);
            cg.emit.emit("leal", AssemblyEmitter.registerOffset(8, STACK_REG), reg);
            cg.emit.emitStore(reg, 4, STACK_REG);
            cg.emit.emitStore("$STR_D", 0, STACK_REG);
            cg.emit.emit("call", SCANF);
            cg.emit.emitLoad(8, STACK_REG, reg);
            cg.emit.emit("add", constant(16), STACK_REG);
            return reg;
        }
    }

    @Override
    public Register booleanConst(BooleanConst ast, Void arg) {
        Register reg = cg.rm.getRegister();
        cg.emit.emit("movl", (ast.value)? "$1" : "$0", reg);
        return reg;
    }

    @Override
    public Register intConst(IntConst ast, Void arg) {
        {
            Register reg = cg.rm.getRegister();
            cg.emit.emit("movl", "$" + ast.value, reg);
            return reg;
        }
    }

    @Override
    public Register nullConst(NullConst ast, Void arg) {
        Register reg = cg.rm.getRegister();
        cg.emit.emit("movl", "$0", reg);
        return reg;
    }

    @Override
    public Register cast(Cast ast, Void arg) {
        String checkLabel = cg.emit.uniqueLabel();
        String startLabel = cg.emit.uniqueLabel();
        Register object = cg.eg.gen(ast.arg());
        Register vtable = cg.rm.getRegister();
        
        cg.emit.emit("movl", object, vtable);
        cg.emit.emitLabel(startLabel);

        // Check whether it is null (no further parent / null ptr)
        cg.emit.emit("cmpl", vtable, "$0");
        cg.emit.emit("jne", checkLabel);

        // The cast has failed as we reached a null pointer
        cg.emit.emit("jmp", "Runtime.invalidDowncastExit");

        // Dereference the next vtable
        cg.emit.emitLabel(checkLabel);
        cg.emit.emit("movl", "0("+vtable+")", vtable);

        // Check whether it is the vtable we need
        cg.emit.emit("cmpl", vtable, ast.typeName);
        cg.emit.emit("jne", startLabel);
        
        // Success!
        return object;
    }

    @Override
    public Register newArray(NewArray ast, Void arg) {
        {
            throw new ToDoException();
        }
    }

    @Override
    public Register index(Index ast, Void arg) {
        {
            throw new ToDoException();
        }
    }

    @Override
    public Register newObject(NewObject ast, Void arg) {
        ClassSymbol symbol = cg.getClass(ast.typeName);
        // Allocate space on the heap.
        int size = (symbol.effectiveFields.size()+1)*4;
        Register ptr = cdeclCall("calloc", "$"+size, "$4");
        // Save a reference to the vtable in the object header
        Register tmp = cg.rm.getRegister();
        cg.emit.emit("leal", ast.typeName, tmp);
        cg.emit.emit("movl", tmp, "0("+ptr+")");
        cg.rm.releaseRegister(tmp);
        return ptr;
    }

    @Override
    public Register field(Field ast, Void arg) {
        Register reg = cg.eg.gen(ast.arg());
        // Check for null pointer
        cg.emit.emit("cmp", reg, "$0");
        cg.emit.emit("je", "Runtime.nullPointerExit");
        // Offset is +1 for the vtable.
        int offset = (ast.sym.getPosition()+1)*4;
        cg.emit.emit("movl", offset+"("+reg+")", reg);
        return reg;
    }

    @Override
    public Register thisRef(ThisRef ast, Void arg) {
        Register reg = cg.rm.getRegister();
        // Current arg is always stored closest to the EBP.
        // Offset is 4 for RET + 4 for EBP.
        cg.emit.emit("movl", "8(%ebp)", reg);
        return reg;
    }

    public Register cdeclCall(Object label, Object... args){
        System.out.println("CDECL "+label);
        // We use the cdecl x86 calling convention as it is compatible
        // with the libc routines and is widely supported, well
        // documented, and well understood. A brief description follows.
        //
        // The caller must ensure that the registers EAX, ECX, and EDX
        // are saved on the caller's side so that the callee is free to
        // use them.
        //
        // All the arguments for a call are passed on the stack and are
        // now pushed in reverse order.
        //
        // Once the arguments are prepared, a CALL is made. On the
        // callee's side, a new stack frame is then allocated by pushing
        // EBP and moving EBP into ESP. Upon return, the return value
        // is stored into EAX and EBP is popped from the stack. RET 0
        // then ends the call and returns to the callee.
        //
        // The callee then increases EBP again to recover the space lost
        // to the call's arguments. This completes the call, with the
        // return value still in EAX.
        Register target = RegisterManager.Register.EAX;
        
        // cdecl requires EAX, ECX, and EDX to be caller-saved.
        List<Register> callerSave = new ArrayList<Register>();
        if(cg.rm.isInUse(RegisterManager.Register.EAX))
            callerSave.add(RegisterManager.Register.EAX);
        if(cg.rm.isInUse(RegisterManager.Register.ECX))
            callerSave.add(RegisterManager.Register.ECX);
        if(cg.rm.isInUse(RegisterManager.Register.EDX))
            callerSave.add(RegisterManager.Register.EDX);
        
        for(Register saved : callerSave){
            cg.emit.emit("pushl", saved);
            cg.rm.releaseRegister(saved);
        }

        // Push all the arguments in reverse
        int stackSize = 0;
        // Java is annoying. Why is there not just a functional reverse?
        List<Object> rargs = Arrays.asList(Arrays.copyOf(args, args.length));
        Collections.reverse(rargs);
        for(Object arg : rargs){
            if(arg instanceof Expr){
                Register reg = cg.eg.gen((Expr)arg);
                cg.emit.emit("pushl", reg);
                cg.rm.releaseRegister(reg);
            }else{
                cg.emit.emit("pushl", ""+arg);
            }
            stackSize += 4;
        }

        // Perform the call
        cg.emit.emit("call", label+"");

        // Restore stack space lost to arguments.
        cg.emit.emit("addl", "$"+stackSize, "%esp");
        
        // Re-acquire saved registers.
        for(Register saved : callerSave){
            cg.rm.acquireRegister(saved);
        }

        // Read out the return value
        if(callerSave.contains(target)){
            target = cg.rm.getRegister();
            cg.emit.emit("movl", "%eax", target);
        }else{
            cg.rm.acquireRegister(target);
        }

        // Restore saved EAX, ECX, and EDX.
        for(Register saved : callerSave){
            cg.emit.emit("popl", saved);
        }

        return target;
    }

    @Override
    public Register methodCall(MethodCallExpr ast, Void arg) {
        // For method calls we extend the cdecl convention by requiring
        // the first argument (last on the stack) to be the object instance.
        Register object = cg.eg.gen(ast.receiver());
        // Offset is +1 for the parent pointer
        int offset = (ast.sym.getPosition()+1)*4;
        
        // Check for null pointer
        cg.emit.emit("cmp", object, "$0");
        cg.emit.emit("je", "Runtime.nullPointerExit");

        // Load the vtable from the object header.
        Register vtable = object;
        cg.emit.emit("movl", "("+object+")", vtable);
        
        // Load the method location from the vtable.
        Register methodLocation = object;
        cg.emit.emit("movl", offset+"("+vtable+")", methodLocation);

        // Perform the cdecl call.
        return cdeclCall(methodLocation, ast.allArguments());
    }

    @Override
    public Register unaryOp(UnaryOp ast, Void arg) {
        {
            Register argReg = gen(ast.arg());
            switch (ast.operator) {
            case U_PLUS:
                break;

            case U_MINUS:
                cg.emit.emit("negl", argReg);
                break;

            case U_BOOL_NOT:
                cg.emit.emit("negl", argReg);
                cg.emit.emit("incl", argReg);
                break;
            }

            return argReg;
        }
    }
	
    @Override
    public Register var(Var ast, Void arg) {
        Register reg = cg.rm.getRegister();
        // Offset is +2 for the ret and the esp.
        int offset = (ast.sym.getPosition()+2)*4;
        cg.emit.emit("movl", offset+"(%ebp)", reg);
        return reg;
    }
}