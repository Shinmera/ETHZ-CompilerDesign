package cd.tree;

public class Node {
	private int id;
	private int value;
	private Node left, right, parent;

	public Node(int id, int v, Node parent) {
		this.id = id;
		this.value = v;
		this.parent = parent;
	}

	public int getId() {
		return id;
	}

	public Node getParent() {
		return parent;
	}

	public int getValue() {
		return value;
	}

	public void setValue(int value) {
		this.value = value;
	}

	public Node getLeftChild() {
		return left;
	}

	public void setLeftChild(Node left) {
		assert left.getParent() == this;
		this.left = left;
	}

	public Node getRightChild() {
		return right;
	}

	public void setRightChild(Node right) {
		assert right.getParent() == this;
		this.right = right;
	}
}