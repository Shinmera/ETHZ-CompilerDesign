package cd.util.debug;

import static java.util.Collections.sort;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import cd.ir.Ast.ClassDecl;
import cd.ir.Ast.MethodDecl;

class DumpUtils {
	
	static final Comparator<ClassDecl> classComparator = new Comparator<ClassDecl>() {
		public int compare(ClassDecl left, ClassDecl right) {
			return left.name.compareTo(right.name);
		}
	};
	
	static final Comparator<MethodDecl> methodComparator = new Comparator<MethodDecl>() {
		public int compare(MethodDecl left, MethodDecl right) {
			return left.name.compareTo(right.name);
		}
	};

	static List<String> sortedStrings(Set<?> set) {
		List<String> strings = new ArrayList<String>();
		for(Object element : set)
			strings.add(element.toString());
		sort(strings);
		return strings;
	}
}
