package cd.backend.codegen;

import static cd.Config.SCANF;
import static cd.backend.codegen.AssemblyEmitter.constant;
import static cd.backend.codegen.RegisterManager.STACK_REG;

import java.util.Arrays;
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
    public Register booleanConst(BooleanConst ast, Void arg) {
        Register reg = cg.rm.getRegister();
        cg.emit.emit("movl", (ast.value)? "$1" : "$0", reg);
        return reg;
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
    public Register cast(Cast ast, Void arg) {
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
    public Register intConst(IntConst ast, Void arg) {
        {
            Register reg = cg.rm.getRegister();
            cg.emit.emit("movl", "$" + ast.value, reg);
            return reg;
        }
    }

    @Override
    public Register field(Field ast, Void arg) {
        {
            throw new ToDoException();
        }
    }

    @Override
    public Register newArray(NewArray ast, Void arg) {
        {
            throw new ToDoException();
        }
    }

    @Override
    public Register newObject(NewObject ast, Void arg) {
        {
            throw new ToDoException();
        }
    }

    @Override
    public Register nullConst(NullConst ast, Void arg) {
        Register reg = cg.rm.getRegister();
        cg.emit.emit("movl", "$0", reg);
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

    @Override
    public Register methodCall(MethodCallExpr ast, Void arg) {
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
        //
        // We extend this convention for object methods by requiring the
        // first argument (last on the stack) to be the object instance.
        Register target = RegisterManager.Register.EAX;

        // Figure out where the method is located
        // FIXME
        Register methodLocation;
        
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
        for(Expr arg : Lists.reverse(expr.allArguments())){
            Register reg = cg.eg.gen(arg);
            cg.emit.emit("pushl", reg);
            cg.rm.releaseRegister(reg);
            stackSize += 4;
        }

        // Perform the call
        cg.emit.emit("call", methodLocation);

        // Restore stack space lost to arguments.
        cg.emit.emit("addl", stackSize, "%esp");

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
            cg.rm.acquireRegister(saved);
        }
        
        return target;
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
        {
            throw new ToDoException();
        }
    }
}
