package cd;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Scanner;

import cd.tree.Node;

public class TreeLoader {
	public static Node load(Path path) throws IOException {
		Scanner in = new Scanner(Files.newBufferedReader(path));

		int n = in.nextInt();
		Node[] nodes = new Node[n];
		for (int i = 0; i < n; i++) {
			int value = in.nextInt();
			if (i == 0)
				nodes[0] = new Node(i, value, null);
			else {
				int parent = in.nextInt();
				int side = in.nextInt();

				nodes[i] = new Node(i, value, nodes[parent]);

				if (side == 0)
					nodes[parent].setLeftChild(nodes[i]);
				else
					nodes[parent].setRightChild(nodes[i]);
			}
		}

		return nodes[0];
	}
}
