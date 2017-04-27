package cd.test;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

import cd.Config;
import cd.Main;
import cd.backend.codegen.AssemblyFailedException;
import cd.frontend.parser.ParseFailure;
import cd.frontend.semantic.SemanticFailure;
import cd.ir.Ast.ClassDecl;
import cd.util.FileUtil;
import cd.util.debug.AstDump;

abstract public class AbstractTestAgainstFrozenReference {
    
	public static final String SEMANTIC_OK = "OK";
    public static final String PARSE_FAILURE = "ParseFailure";
	
    public File file, sfile, binfile, infile;
	public File parserreffile, semanticreffile, execreffile, cfgreffile, optreffile;
	public File errfile;
	public Main main;

	public static int counter = 0;

	@Test(timeout=10000)
	public void test() throws Throwable {
		System.err.println("[" + counter++ + " = " + file + "]");
		
		try {
			// Delete intermediate files from previous runs:
			if (sfile.exists())
				sfile.delete();
			if (binfile.exists())
				binfile.delete();

			runReference();
			
			List<ClassDecl> astRoots = testParser();
			if (astRoots != null) {
				{
					boolean passedSemanticAnalysis = testSemanticAnalyzer(astRoots);
					
					{
						if (passedSemanticAnalysis) {
							testCodeGenerator(astRoots);
						}
					}
				}
			}
		} catch (org.junit.ComparisonFailure cf) {
			throw cf;
		} catch (Throwable e) {
			PrintStream err = new PrintStream(errfile);
			err.println("Debug information for file: " + this.file);
			err.println(this.main.debug.toString());
			err.println("Test failed because an exception was thrown:");
			err.println("    " + e.getLocalizedMessage());
			err.println("Stack trace:");
			e.printStackTrace(err);
			System.err.println(FileUtil.read(errfile));
			throw e;
		}

		// if we get here, then the test passed, so delete the errfile:
		// (which has been accumulating debug output etc)
		if (errfile.exists())
			errfile.delete();
	}

	private void runReference() throws IOException, InterruptedException {
        String slash = File.separator;
        String colon = File.pathSeparator;
        String javaExe = System.getProperty("java.home") + slash + "bin" + slash + Config.JAVA_EXE;
        
        ProcessBuilder pb = new ProcessBuilder(
                javaExe, "-Dcd.meta_hidden.Version=" + referenceVersion(),
                "-cp", "lib/frozenReferenceObf.jar" + colon + " lib/junit-4.12.jar" + colon + "lib/antlr-4.4-complete.jar",
                "cd.FrozenReferenceMain", file.getAbsolutePath());
	        
        Process proc = pb.start();
        proc.waitFor();
        try (InputStream err = proc.getErrorStream()) {
            if (err.available() > 0) {
                byte b[] = new byte[err.available()];
                err.read(b, 0, b.length);
                System.err.println(new String(b));
            }
        }
	}
	
	private static String referenceVersion() {
        {
            return "CD_HW_CODEGEN_FULL_SOL";
        }
	}

	/** Run the parser and compare the output against the reference results */
	private List<ClassDecl> testParser() throws Exception {
		String parserRef = findParserRef();
		List<ClassDecl> astRoots = null;
		String parserOut;

		try {
			astRoots = main.parse(new FileReader(this.file));

			parserOut = AstDump.toString(astRoots);
		} catch (ParseFailure pf) {
			{
				// Parse errors are ok too.
				main.debug("");
				main.debug("");
				main.debug("%s", pf.toString());
				parserOut = PARSE_FAILURE;
			}
		}

		{
			// Now that the 2nd assignment is over, we don't
			// do a detailed comparison of the AST, just check
			// whether the parse succeeded or failed.
			if (parserOut.equals(PARSE_FAILURE)	|| parserRef.equals(PARSE_FAILURE))
				assertEquals("parser", parserRef, parserOut);
		}
		return astRoots;
	}

	private String findParserRef() throws IOException {
		// Check for a .ref file
		if (parserreffile.exists() && parserreffile.lastModified() >= file.lastModified()) {
			return FileUtil.read(parserreffile);
		}
		throw new RuntimeException("ERROR: could not find parser .ref");
	}

	private boolean testSemanticAnalyzer(List<ClassDecl> astRoots)
			throws IOException {
		String semanticRef = findSemanticRef();

		boolean passed;
		String result;
		try {
			main.semanticCheck(astRoots);
			result = SEMANTIC_OK;
			passed = true;
		} catch (SemanticFailure sf) {
			result = sf.cause.name();
			main.debug("Error message: %s", sf.getLocalizedMessage());
			passed = false;
		}

		assertEquals("semantic", semanticRef, result);
		return passed;
	}
	
	private String findSemanticRef() throws IOException {
		// Semantic ref file is a little different. It consists
		// of 2 lines, but only the first line is significant.
		// The second one contains additional information that we log
		// to the debug file.
	
		// Read in the result
		String res;
		if (semanticreffile.exists() && semanticreffile.lastModified() > file.lastModified())
			res = FileUtil.read(semanticreffile);
		else
			throw new RuntimeException("ERROR: could not find semantic .ref");
	
		// Extract the first line: there should always be multiple lines,
		// but someone may have tinkered with the file or something
		if (res.contains("\n")) {
			int newline = res.indexOf("\n");
			String info = res.substring(newline + 1);
			if (!info.equals("") && !info.equals("\n"))
				main.debug("Error message from reference is: %s", info);
			return res.substring(0, newline); // 1st line
		} else {
			return res;
		}
	}

	/**
	 * Run the code generator, assemble the resulting .s file, and (if the output
	 * is well-defined) compare against the expected output.
	 */
	private void testCodeGenerator(List<ClassDecl> astRoots)
			throws IOException {
		// Determine the input and expected output.
		String inFile = (infile.exists() ? FileUtil.read(infile) : "");
		String execRef = findExecRef();

		// Run the code generator:
		try (FileWriter fw = new FileWriter(this.sfile)) {
			main.generateCode(astRoots, fw);
		}

		// At this point, we have generated a .s file and we have to compile
		// it to a binary file. We need to call out to GCC or something
		// to do this.
		String asmOutput = FileUtil.runCommand(
				Config.ASM_DIR,
				Config.ASM,
				new String[] { binfile.getAbsolutePath(),
						sfile.getAbsolutePath() }, null, false);

		// To check if gcc succeeded, check if the binary file exists.
		// We could use the return code instead, but this seems more
		// portable to other compilers / make systems.
		if (!binfile.exists())
			throw new AssemblyFailedException(asmOutput);

		// Execute the binary file, providing input if relevant, and
		// capturing the output. Check the error code so see if the
		// code signaled dynamic errors.
		String execOut = FileUtil.runCommand(new File("."),
				new String[] { binfile.getAbsolutePath() }, new String[] {},
				inFile, true);

		// Compute the output to what we expected to see.
		if (!execRef.equals(execOut))
		    assertEqualOutput("exec", execRef, execOut);
	}

	private String findExecRef() throws IOException {
		// Check for a .ref file
		if (execreffile.exists() && execreffile.lastModified() > file.lastModified()) {
			return FileUtil.read(execreffile);
		}

		throw new RuntimeException("ERROR: could not find execution .ref");
	}

	private void assertEquals(String phase, String exp, String act_) {
		String act = act_.replace("\r\n", "\n"); // for windows machines
		if (!exp.equals(act)) {
			warnAboutDiff(phase, exp, act);
		}
	}
	
	/**
	 * Compare the output of two executions
	 */
	private void assertEqualOutput(String phase, String exp, String act_) {
		String act = act_.replace("\r\n", "\n"); // for windows machines
		if (!exp.equals(act)) {
			warnAboutDiff(phase, exp, act);
		}
	}

	private void warnAboutDiff(String phase, String exp, String act) {
		try {
			try (PrintStream err = new PrintStream(errfile)) {
				err.println("Debug information for file: " + this.file);
				err.println(this.main.debug.toString());
				err.println(String.format(
						"Phase %s failed because we expected to see:", phase));
				err.println(exp);
				err.println("But we actually saw:");
				err.println(act);
				err.println("The difference is:");
				err.println(Diff.computeDiff(exp, act));
			}
		} catch (FileNotFoundException exc) {
			System.err.println("Unable to write debug output to " + errfile
					+ ":");
			exc.printStackTrace();
		}
		Assert.assertEquals(
				String.format("Phase %s for %s failed!", phase,
						file.getPath()), exp, act);
	}
	
}
