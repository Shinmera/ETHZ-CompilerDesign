package cd.backend.codegen;

public class AssemblyFailedException extends RuntimeException {
	private static final long serialVersionUID = -5658502514441032016L;
	
	public final String assemblerOutput;
	public AssemblyFailedException(
			String assemblerOutput) {
		super("Executing assembler failed.\n"
				+ "Output:\n"
				+ assemblerOutput);
		this.assemblerOutput = assemblerOutput;
	}	
}
