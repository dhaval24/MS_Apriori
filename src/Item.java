import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class Item {

	public int id;
	public double mis;
	public int suppCount;

    //set MIS and id
	private Item(int i, double m) {

		id = i;
		mis = m;

	}

    //Maintaing list of items
	public static List<Item> Items = new ArrayList<>();

    //Add item into data structure
	public static Item AddItem(int i, double m) {

		Item it = new Item(i, m);
		Items.add(it);
		return it;

	}

    //Find item from the collection
	public static Item FindItem(int x) {

		Item it = null;
		for (Item i : Items) {
			if (i.id == x) {
				it = i;
				break;
			}
		}
		return it;

	}

	public static void sortItems() {
		Items.sort(new ItemComparator());
	}

	@Override
	public String toString() {
		return String.valueOf(this.id);
	}
}

//Custom comparator to sort primarily on MIS and secondary on ID
class ItemComparator implements Comparator<Item> {

	@Override
	public int compare(Item o1, Item o2) {
		if (o1.mis < o2.mis) return -1;
		else if (o1.mis > o2.mis) return 1;
		else if (o1.id < o2.id) return -1;
		else return 1;
	}

}
