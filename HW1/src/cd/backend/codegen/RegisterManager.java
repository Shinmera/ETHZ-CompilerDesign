package cd.backend.codegen;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Simple class that manages the set of currently used
 * and unused registers
 */
public class RegisterManager {
	private List<Register> registers = new ArrayList<Register>();

	// lists of register to save by the callee and the caller
	public static final Register CALLEE_SAVE[] = new Register[]{Register.ESI,
			Register.EDI, Register.EBX};
	public static final Register CALLER_SAVE[] = new Register[]{Register.EAX,
			Register.ECX, Register.EDX};
	
	// list of general purpose registers
	public static final Register GPR[] = new Register[]{Register.EAX, Register.EBX,
		Register.ECX, Register.EDX, Register.ESI, Register.EDI};

	// special purpose registers
	public static final Register BASE_REG = Register.EBP;
	public static final Register STACK_REG = Register.ESP;

	public static final int SIZEOF_REG = 4;

	
	public enum Register {
		EAX("%eax", ByteRegister.EAX), EBX("%ebx", ByteRegister.EBX), ECX(
				"%ecx", ByteRegister.ECX), EDX("%edx", ByteRegister.EDX), ESI(
				"%esi", null), EDI("%edi", null), EBP("%ebp", null), ESP(
				"%esp", null);

		public final String repr;
		private final ByteRegister lowByteVersion;

		private Register(String repr, ByteRegister bv) {
			this.repr = repr;
			this.lowByteVersion = bv;
		}

		@Override
		public String toString() {
			return repr;
		}

		/**
		 * determines if this register has an 8bit version
		 */
		public boolean hasLowByteVersion() {
			return lowByteVersion != null;
		}

		/**
		 * Given a register like {@code %eax} returns {@code %al}, but doesn't
		 * work for {@code %esi} and {@code %edi}!
		 */
		public ByteRegister lowByteVersion() {
			assert hasLowByteVersion();
			return lowByteVersion;
		}
	}

	public enum ByteRegister {
		EAX("%al"), EBX("%bl"), ECX("%cl"), EDX("%dl");

		public final String repr;

		private ByteRegister(String repr) {
			this.repr = repr;
		}

		@Override
		public String toString() {
			return repr;
		}
	}

	/**
	 * Reset all general purpose registers to free
	 */
	public void initRegisters() {
		registers.clear();
		registers.addAll(Arrays.asList(GPR));
	}

	/**
	 * returns a free register and marks it as used
	 */
	public Register getRegister() {
		int last = registers.size() - 1;
		if (last < 0)
			throw new AssemblyFailedException(
					"Program requires too many registers");

		return registers.remove(last);
	}

	/**
	 * marks a currently used register as free
	 */
	public void releaseRegister(Register reg) {
		assert !registers.contains(reg);
		registers.add(reg);
	}

	/**
	 * Returns whether the register is currently non-free
	 */
	public boolean isInUse(Register reg) {
		return !registers.contains(reg);
	}

	/**
	 * returns the number of free registers
	 */
	public int availableRegisters() {
		return registers.size();
	}
}