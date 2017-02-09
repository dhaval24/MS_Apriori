import java.util.ArrayList;
import java.util.List;

public class ItemSet {

	ArrayList<Item> Items = new ArrayList<>();
	int count = 0;
	int tailCount = 0;

	public ItemSet() {
	}

	public ItemSet(List<Item> tempC) {
		Items.addAll(tempC);
	}

	public boolean IsPartOf(ItemSet superSet) {
		return IsPartOf(superSet.Items);
	}

	public boolean IsPartOf(List<Item> items2) {
		for (Item i : Items) {
			if (!items2.contains(i)) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		return String.valueOf(" {" + Items.toString() + ":" + this.count + "} ");
	}
}
