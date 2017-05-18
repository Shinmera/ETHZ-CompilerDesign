package cd.backend.codegen;

import static cd.backend.codegen.AssemblyEmitter.constant;
import static cd.backend.codegen.AssemblyEmitter.labelAddress;
import static cd.backend.codegen.RegisterManager.BASE_REG;

import java.util.Arrays;
import java.util.List;

import cd.Config;
import cd.ToDoException;
import cd.backend.codegen.RegisterManager.Register;
import cd.ir.Ast.BinaryOp;
import cd.ir.Ast.BinaryOp.BOp;
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
import cd.ir.Ast.UnaryOp.UOp;
import cd.ir.Ast.Var;
import cd.ir.ExprVisitor;
import cd.ir.Symbol.ArrayTypeSymbol;
import cd.ir.Symbol.ClassSymbol;
import cd.ir.Symbol.PrimitiveTypeSymbol;
import cd.ir.Symbol.TypeSymbol;
import cd.util.Pair;
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
		throw new ToDoException();
	}

	@Override
	public Register booleanConst(BooleanConst ast, Void arg) {
		throw new ToDoException();
	}

	@Override
	public Register builtInRead(BuiltInRead ast, Void arg) {
		throw new ToDoException();
	}

	@Override
	public Register cast(Cast ast, Void arg) {
		throw new ToDoException();
	}

	@Override
	public Register index(Index ast, Void arg) {
		throw new ToDoException();
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
		throw new ToDoException();
	}

	@Override
	public Register newArray(NewArray ast, Void arg) {
		throw new ToDoException();
	}

	@Override
	public Register newObject(NewObject ast, Void arg) {
		throw new ToDoException();
	}

	@Override
	public Register nullConst(NullConst ast, Void arg) {
		throw new ToDoException();
	}

	@Override
	public Register thisRef(ThisRef ast, Void arg) {
		throw new ToDoException();
	}

	@Override
	public Register methodCall(MethodCallExpr ast, Void arg) {
		throw new ToDoException();
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
		throw new ToDoException();
	}

}

/*
 * This is the subclass of ExprGenerator containing the reference solution
 */
class ExprGeneratorRef extends ExprGenerator {

	/* cg and cgRef are the same instance. cgRef simply
	 * provides a wider interface */
	protected final AstCodeGeneratorRef cgRef;

	ExprGeneratorRef(AstCodeGeneratorRef astCodeGenerator) {
		super(astCodeGenerator);
		this.cgRef = astCodeGenerator;
	}

	/**
	 * This routine handles register shortages. It generates a value for
	 * {@code right}, while keeping the value in {@code leftReg} live. However,
	 * if there are insufficient registers, it may temporarily store the value
	 * in {@code leftReg} to the stack. In this case, it will be restored into
	 * another register once {@code right} has been evaluated, but the register
	 * may not be the same as {@code leftReg}. Therefore, this function returns
	 * a pair of registers, the first of which stores the left value, and the
	 * second of which stores the right value.
	 * 
	 */
	public Pair<Register> genPushing(Register leftReg, Expr right) {
		Register newLeftReg = leftReg;
		boolean pop = false;

		if (cgRef.rnv.calc(right) > cgRef.rm.availableRegisters()) {
			cgRef.push(newLeftReg.repr);
			cgRef.rm.releaseRegister(newLeftReg);
			pop = true;
		}

		Register rightReg = gen(right);

		if (pop) {
			newLeftReg = cgRef.rm.getRegister();
			cgRef.pop(newLeftReg.repr);
		}

		return new Pair<Register>(newLeftReg, rightReg);

	}

	@Override
	public Register binaryOp(BinaryOp ast, Void arg) {
		Register leftReg = null;
		Register rightReg = null;

		{

			leftReg = gen(ast.left());
			Pair<Register> regs = genPushing(leftReg, ast.right());
			leftReg = regs.a;
			rightReg = regs.b;

		}

		assert leftReg != null && rightReg != null;

		new OperandsDispatcher() {

			@Override
			public void booleanOp(Register leftReg, BOp op, Register rightReg) {
				integerOp(leftReg, op, rightReg);
			}

			@Override
			public void integerOp(Register leftReg, BOp op, Register rightReg) {

				switch (op) {
				case B_TIMES:
					cgRef.emit.emit("imull", rightReg, leftReg);
					break;
				case B_PLUS:
					cgRef.emit.emit("addl", rightReg, leftReg);
					break;
				case B_MINUS:
					cgRef.emit.emit("subl", rightReg, leftReg);
					break;
				case B_DIV:
					emitDivMod(Register.EAX, leftReg, rightReg);
					break;
				case B_MOD:
					emitDivMod(Register.EDX, leftReg, rightReg);
					break;
				case B_AND:
					cgRef.emit.emit("andl", rightReg, leftReg);
					break;
				case B_OR:
					cgRef.emit.emit("orl", rightReg, leftReg);
					break;
				case B_EQUAL:
					emitCmp("sete", leftReg, rightReg);
					break;
				case B_NOT_EQUAL:
					emitCmp("setne", leftReg, rightReg);
					break;
				case B_LESS_THAN:
					emitCmp("setl", leftReg, rightReg);
					break;
				case B_LESS_OR_EQUAL:
					emitCmp("setle", leftReg, rightReg);
					break;
				case B_GREATER_THAN:
					emitCmp("setg", leftReg, rightReg);
					break;
				case B_GREATER_OR_EQUAL:
					emitCmp("setge", leftReg, rightReg);
					break;
				default:
					throw new AssemblyFailedException(
							"Invalid binary operator for "
									+ PrimitiveTypeSymbol.intType + " or "
									+ PrimitiveTypeSymbol.booleanType);
				}

			}

		}.binaryOp(ast, leftReg, rightReg);

		cgRef.rm.releaseRegister(rightReg);

		return leftReg;
	}

	private void emitCmp(String opname, Register leftReg, Register rightReg) {

		cgRef.emit.emit("cmpl", rightReg, leftReg);

		if (leftReg.hasLowByteVersion()) {
			cgRef.emit.emit("movl", "$0", leftReg);
			cgRef.emit.emit(opname, leftReg.lowByteVersion().repr);
		} else {
			cgRef.push(Register.EAX.repr);
			cgRef.emit.emit("movl", "$0", Register.EAX);
			cgRef.emit.emit(opname, "%al");
			cgRef.emit.emit("movl", Register.EAX, leftReg);
			cgRef.pop(Register.EAX.repr);
		}

	}

	private void emitDivMod(Register whichResultReg, Register leftReg,
			Register rightReg) {

		// Compare right reg for 0
		int padding = cgRef.emitCallPrefix(null, 1);
		cgRef.push(rightReg.repr);
		cgRef.emit.emit("call", AstCodeGeneratorRef.CHECK_NON_ZERO);
		cgRef.emitCallSuffix(null, 1, padding);

		// Save EAX, EBX, and EDX to the stack if they are not used
		// in this subtree (but are used elsewhere). We will be
		// changing them.
		List<Register> dontBother = Arrays.asList(rightReg, leftReg);
		Register[] affected = { Register.EAX, Register.EBX, Register.EDX };
		for (Register s : affected)
			if (!dontBother.contains(s) && cgRef.rm.isInUse(s))
				cgRef.emit.emit("pushl", s);

		// Move the LHS (numerator) into eax
		// Move the RHS (denominator) into ebx
		cgRef.emit.emit("pushl", rightReg);
		cgRef.emit.emit("pushl", leftReg);
		cgRef.emit.emit("popl", Register.EAX);
		cgRef.emit.emit("popl", "%ebx");
		cgRef.emit.emitRaw("cltd"); // sign-extend %eax into %edx
		cgRef.emit.emit("idivl", "%ebx"); // division, result into edx:eax

		// Move the result into the LHS, and pop off anything we saved
		cgRef.emit.emit("movl", whichResultReg, leftReg);
		for (int i = affected.length - 1; i >= 0; i--) {
			Register s = affected[i];
			if (!dontBother.contains(s) && cgRef.rm.isInUse(s))
				cgRef.emit.emit("popl", s);
		}
	}

	@Override
	public Register booleanConst(BooleanConst ast, Void arg) {
		Register reg = cgRef.rm.getRegister();
		cgRef.emit.emit("movl", ast.value ? "$1" : "$0", reg);
		return reg;
	}

	@Override
	public Register builtInRead(BuiltInRead ast, Void arg) {
		Register reg = cgRef.rm.getRegister();
		int padding = cgRef.emitCallPrefix(reg, 0);
		cgRef.emit.emit("call", AstCodeGeneratorRef.READ_INTEGER);
		cgRef.emitCallSuffix(reg, 0, padding);
		return reg;
	}

	@Override
	public Register cast(Cast ast, Void arg) {
		// Invoke the helper function. If it does not exit,
		// the cast succeeded!
		Register objReg = gen(ast.arg());
		int padding = cgRef.emitCallPrefix(null, 2);
		cgRef.push(objReg.repr);
		cgRef.push(AssemblyEmitter.labelAddress(cgRef.vtable(ast.type)));
		cgRef.emit.emit("call", AstCodeGeneratorRef.CHECK_CAST);
		cgRef.emitCallSuffix(null, 2, padding);
		return objReg;
	}

	@Override
	public Register index(Index ast, Void arg) {
		Register arr = gen(ast.left());
		int padding = cgRef.emitCallPrefix(null, 1);
		cgRef.push(arr.repr);
		cgRef.emit.emit("call", AstCodeGeneratorRef.CHECK_NULL);
		cgRef.emitCallSuffix(null, 1, padding);
		Pair<Register> pair = genPushing(arr, ast.right());
		arr = pair.a;
		Register idx = pair.b;

		// Check array bounds
		padding = cgRef.emitCallPrefix(null, 2);
		cgRef.push(idx.repr);
		cgRef.push(arr.repr);
		cgRef.emit.emit("call", AstCodeGeneratorRef.CHECK_ARRAY_BOUNDS);
		cgRef.emitCallSuffix(null, 2, padding);

		cgRef.emit.emitMove(AssemblyEmitter.arrayAddress(arr, idx), idx);
		cgRef.rm.releaseRegister(arr);
		return idx;
	}

	@Override
	public Register field(Field ast, Void arg) {
		Register reg = gen(ast.arg());
		int padding = cgRef.emitCallPrefix(null, 1);
		cgRef.push(reg.repr);
		cgRef.emit.emit("call", AstCodeGeneratorRef.CHECK_NULL);
		cgRef.emitCallSuffix(null, 1, padding);
		assert ast.sym.offset != -1;
		cgRef.emit.emitLoad(ast.sym.offset, reg, reg);
		return reg;
	}

	@Override
	public Register newArray(NewArray ast, Void arg) {
		// Size of the array = 4 + 4 + elemsize * num elem.
		// Compute that into reg, store it into the stack as
		// an argument to Javali$Alloc(), and then use it to store final
		// result.
		ArrayTypeSymbol arrsym = (ArrayTypeSymbol) ast.type;
		Register reg = gen(ast.arg());

		// Check for negative array sizes
		int padding = cgRef.emitCallPrefix(null, 1);
		cgRef.push(reg.repr);
		cgRef.emit.emit("call", AstCodeGeneratorRef.CHECK_ARRAY_SIZE);
		cgRef.emitCallSuffix(null, 1, padding);

		Register lenReg = cgRef.rm.getRegister();
		cgRef.emit.emit("movl", reg, lenReg); // save length

		cgRef.emit.emit("imul", Config.SIZEOF_PTR, reg);
		cgRef.emit.emit("addl", 2 * Config.SIZEOF_PTR, reg);

		int allocPadding = cgRef.emitCallPrefix(reg, 1);
		cgRef.push(reg.repr);
		cgRef.emit.emit("call", AstCodeGeneratorRef.ALLOC);
		cgRef.emitCallSuffix(reg, 1, allocPadding);

		// store vtable ptr and array length
		cgRef.emit.emitStore(AssemblyEmitter.labelAddress(cgRef.vtable(arrsym)), 0, reg);
		cgRef.emit.emitStore(lenReg, Config.SIZEOF_PTR, reg);
		cgRef.rm.releaseRegister(lenReg);

		return reg;
	}

	@Override
	public Register newObject(NewObject ast, Void arg) {
		ClassSymbol clssym = (ClassSymbol) ast.type;
		Register reg = cgRef.rm.getRegister();
		int allocPadding = cgRef.emitCallPrefix(reg, 1);
		cgRef.push(constant(clssym.sizeof));
		cgRef.emit.emit("call", AstCodeGeneratorRef.ALLOC);
		cgRef.emitCallSuffix(reg, 1, allocPadding);
		cgRef.emit.emitStore(labelAddress(cgRef.vtable(clssym)), 0, reg);
		return reg;
	}

	@Override
	public Register nullConst(NullConst ast, Void arg) {
		Register reg = cgRef.rm.getRegister();
		cgRef.emit.emit("movl", "$0", reg);
		return reg;
	}

	@Override
	public Register thisRef(ThisRef ast, Void arg) {
		Register reg = cgRef.rm.getRegister();
		cgRef.emit.emitLoad(cgRef.THIS_OFFSET, BASE_REG, reg);
		return reg;
	}

	@Override
	public Register methodCall(MethodCallExpr ast, Void arg) {
		return cgRef.sg.methodCall(ast.sym, ast.allArguments());
	}

	@Override
	public Register var(Var ast, Void arg) {
		Register reg = cgRef.rm.getRegister();
		switch (ast.sym.kind) {
		case LOCAL:
		case PARAM:
			assert ast.sym.offset != -1;
			cgRef.emit.emitLoad(ast.sym.offset, BASE_REG, reg);
			break;
		case FIELD:
			// These are removed by the ExprRewriter added to the
			// end of semantic analysis.
			throw new RuntimeException("Should not happen");
		}
		return reg;
	}

	@Override
	public Register unaryOp(UnaryOp ast, Void arg) {
		if (ast.operator == UOp.U_MINUS) {
			Register argReg = gen(ast.arg());
			cgRef.emit.emit("negl", argReg);
			return argReg;
		} else {
			return super.unaryOp(ast, arg);
		}
	}
}

/* Dispatches BinaryOp based on the types of the operands
 */
abstract class OperandsDispatcher {

	public abstract void integerOp(Register leftReg, BOp op,
			Register rightReg);

	public abstract void booleanOp(Register leftReg, BOp op,
			Register rightReg);

	public void binaryOp(BinaryOp ast, Register leftReg, Register rightReg) {

		assert ast.type != null;

		if (ast.type == PrimitiveTypeSymbol.intType) {
			integerOp(leftReg, ast.operator, rightReg);
		} else if (ast.type == PrimitiveTypeSymbol.booleanType) {

			final TypeSymbol opType = ast.left().type;

			if (opType == PrimitiveTypeSymbol.intType) {
				integerOp(leftReg, ast.operator, rightReg);
			} else if (opType == PrimitiveTypeSymbol.booleanType) {
				booleanOp(leftReg, ast.operator, rightReg);
			} else {
				integerOp(leftReg, ast.operator, rightReg);
			}

		}

	}

}
