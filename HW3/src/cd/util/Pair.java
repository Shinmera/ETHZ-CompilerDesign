package cd.util;

import java.util.ArrayList;
import java.util.List;

/** Simple class for joining two objects of the same type */
public class Pair<T> {
	public T a;
	public T b;
	
	public Pair(T a, T b) {
		this.a = a;
		this.b = b;
	}
	
	public static <T> List<Pair<T>> zip(List<T> listA, List<T> listB) {
		List<Pair<T>> res = new ArrayList<Pair<T>>();
		for (int i = 0; i < Math.min(listA.size(), listB.size()); i++) {
			res.add(new Pair<T>(listA.get(i), listB.get(i)));
		}
		return res;
	}
	
	public static <T> List<T> unzipA(List<Pair<T>> list) {
		List<T> res = new ArrayList<T>();
		for (Pair<T> p : list)
			res.add(p.a);
		return res;
	}
	
	public static <T> List<T> unzipB(List<Pair<T>> list) {
		List<T> res = new ArrayList<T>();
		for (Pair<T> p : list)
			res.add(p.b);
		return res;
	}
	
	public static String join(
			List<Pair<?>> pairs, 
			String itemSep, 
			String pairSep) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (Pair<?> pair : pairs) {
			if (!first) sb.append(pairSep);
			sb.append(pair.a);
			sb.append(itemSep);
			sb.append(pair.b);
			first = false;
		}
		return sb.toString();
	}
}
