package cd.backend;

public enum ExitCode {
	OK(0),
	INVALID_DOWNCAST(1),
	INVALID_ARRAY_STORE(2),
	INVALID_ARRAY_BOUNDS(3),
	NULL_POINTER(4),
	INVALID_ARRAY_SIZE(5),
	INFINITE_LOOP(6),
	DIVISION_BY_ZERO(7),
	INTERNAL_ERROR(22);
	
	public final int value;

    private ExitCode(int value) {
	    this.value = value;
	}
}