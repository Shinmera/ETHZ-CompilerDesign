package cd.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

public class FileUtil {

	public static String readAll(Reader ubReader) throws IOException {
		try (BufferedReader bReader = new BufferedReader(ubReader)) {
			StringBuilder sb = new StringBuilder();
	
			while (true) {
				int ch = bReader.read();
				if (ch == -1)
					break;
				
				sb.append((char) ch);
			}
			return sb.toString();
		}
	}

	public static String read(File file) throws IOException {
		return readAll(new FileReader(file));
	}

	public static void write(File file, String text) throws IOException {
		try (FileWriter writer = new FileWriter(file)) {
			writer.write(text);
		}
	}

	public static String runCommand(File dir, String[] command,
			String[] substs, String input, boolean detectError)
			throws IOException {
		// Substitute the substitution strings $0, $1, etc
		String newCommand[] = new String[command.length];
		for (int i = 0; i < command.length; i++) {
			String newItem = command[i];
			for (int j = 0; j < substs.length; j++)
				newItem = newItem.replace("$" + j, substs[j]);
			newCommand[i] = newItem;
		}

		// Run the command in the specified directory
		ProcessBuilder pb = new ProcessBuilder(newCommand);
		pb.directory(dir);
		pb.redirectErrorStream(true);
		final Process p = pb.start();
		if (input != null && !input.equals("")) {
			try (OutputStreamWriter osw = new OutputStreamWriter(p.getOutputStream())) {
				osw.write(input);
			}
		}
		
		try {
			final StringBuffer result = new StringBuffer();
			// thread to read stdout from child so that p.waitFor() is interruptible
			// by JUnit's timeout mechanism. Otherwise it would block in readAll()
			Thread t = new Thread () {
				public void run() {
					try {
						result.append(readAll(new InputStreamReader(p.getInputStream())));
					} catch (IOException e) {
					}
				}
			};
			t.start();
	
			if (detectError) {
				int err = p.waitFor();

				// hack: same as ReferenceServer returns when
				// a dynamic error occurs running the interpreter
				if (err != 0)
					return "Error: " + err + "\n";
			}
			
			t.join();
			return result.toString();
		} catch (InterruptedException e) {
			return "Error: execution of " + command[0] + " got interrupted (probably timed out)\n";
		} finally {
			// in case child is still running, destroy
			p.destroy();
		}
	}

	/**
	 * Finds all .javali under directory {@code testDir}, adding File objects
	 * into {@code result} for each one.
	 */
	public static void findJavaliFiles(File testDir, List<Object[]> result) {
		for (File testFile : testDir.listFiles()) {
			if (testFile.getName().endsWith(".javali"))
				result.add(new Object[] { testFile });
			else if (testFile.isDirectory())
				findJavaliFiles(testFile, result);
		}
	}

	/** Finds all .javali under directory {@code testDir} and returns them. */
	public static List<File> findJavaliFiles(File testDir) {
		List<File> result = new ArrayList<File>();
		for (File testFile : testDir.listFiles()) {
			if (testFile.getName().endsWith(".javali"))
				result.add(testFile);
			else if (testFile.isDirectory())
				result.addAll(findJavaliFiles(testFile));
		}
		return result;
	}
}
