import java.util.ArrayList;
import java.util.List;


public class Util {
	public static <T> List<T> union(List<T> list1, List<T> list2) {
		if (list1 == null)
			return list2;
		else if (list2 == null)
			return list2;
        List<T> list = new ArrayList<T>(list1);

        for (T t : list2) {
            if(!list1.contains(t)) {
                list.add(t);
            }
        }

        return list;
    }
	
	public static String arrayString(int a[]) {
		if (a.length == 0)
			return "{}";
		String s = "{";
		for (int i = 0; i < a.length; ) {
			s += a[i];
			i++;
			if (i < a.length)
				s += ", ";
		}
		s += "}";
		return s;
	}
}

