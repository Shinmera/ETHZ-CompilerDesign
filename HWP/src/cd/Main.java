package cd;

import cd.tree.*;

import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Paths;

public class Main {

	public static void main(String[] args) throws IOException {
		Node root = TreeLoader.load(Paths.get("input.txt"));
		run(root, System.out);
	}

	public static void run(Node r, PrintStream out) {
		out.println("Your output here");
	}
}
