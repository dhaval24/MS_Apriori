import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Transaction {

	List<Item> Items = new ArrayList<>();

	public void Sort() {
		Items.sort(new CustomComparator());
	}

	// Custom comparator to sort Transaction on the basis of MIS
	private class CustomComparator implements Comparator<Item> {
		@Override
		public int compare(Item o1, Item o2) {
			return Double.compare(o1.mis, o2.mis);
		}
	}

	public String toString() {
		return Items.toString();
	}
}
