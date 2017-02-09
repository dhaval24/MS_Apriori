import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FrequencyCalculator {

	public List<List<Item>> ItemsCannotBeTogether = new ArrayList<>();
	public List<Item> ItemsMustHave = new ArrayList<>();
	public List<Transaction> Transactions = new ArrayList<>();
	public double SDC; // Support difference constant, prevent same very
						// frequent and rare items to occur together

    //Perform input parsing and populate data structures
	public FrequencyCalculator(String inputFile, String parameterFile) throws IOException {

		Pattern r = Pattern.compile("MIS\\((\\d+)\\) = ([0|1]\\.\\d{1,2})");

		try (BufferedReader br = new BufferedReader(new FileReader(parameterFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				// process the line.
				Matcher m = r.matcher(line);

				if (m.find()) {
					Item.AddItem(Integer.parseInt(m.group(1)), Double.parseDouble(m.group(2)));
				}

				else if (line.contains("SDC")) {
					SDC = Double.parseDouble(line.split("=")[1].trim());
				}

				else if (line.contains("cannot_be_together")) {
					String x = line.split(":")[1].trim();
					for (String s1 : x.split("and")) {
						List<Item> y = new ArrayList<>();
						String s2 = s1.replace('{', ' ').replace('}', ' ');
						for (String s : s2.split(",")) {
							y.add(Item.FindItem(Integer.parseInt(s.trim())));
						}
						ItemsCannotBeTogether.add(y);
					}
				}

				else if (line.contains("must-have")) {
					String x = line.split(":")[1].trim();
					for (String s : x.split("or")) {
						ItemsMustHave.add(Item.FindItem(Integer.parseInt(s.trim())));
					}
				}
			}
			br.close();
		}
		Item.sortItems(); // sort step according to Minsupport
//		System.out.println("M list:" + Item.Items);

		try (BufferedReader br = new BufferedReader(new FileReader(inputFile))) {
			String line;
			while ((line = br.readLine()) != null) {
				Transaction t = new Transaction();
				for (String s : line.substring(1, line.length() - 1).split(",")) {
					t.Items.add(Item.FindItem(Integer.parseInt(s.trim())));
				}
				Transactions.add(t);
			}
			System.out.println("Transactions List :" + Transactions);
			br.close();
		}

	}

    //Main Algorithm function
	public List<List<ItemSet>> MSAprioriAlgo() {

		List<Item> L = initalPass(Item.Items, Transactions); // Generate list
																// after initial
																// pass
		List<ItemSet> f1 = new ArrayList<>();
		List<List<ItemSet>> Fkfinal = new ArrayList<>(); // Final set

		int n = Transactions.size();

		// loop for F1 generation
		for (Item item : L) {
			if ((double) item.suppCount / n >= item.mis && ItemsMustHave.contains(item)) {
				ItemSet is = new ItemSet();
				is.Items.add(item);
				is.count = item.suppCount;
				f1.add(is);
			}
		}

//		System.out.println("L list: " + L);
//		System.out.println("F list: " + f1);

		Fkfinal.add(f1);
		List<ItemSet> prevCandidateFkSet = null; // Maintaining this to prevent some of the itemsets which might be removed
		for (int k = 1;; k++) {

			List<ItemSet> candidateK; // List for candidate set of size k
			if (k == 1) { // special case for generating candidate set when K==1
				candidateK = level2CandidateGen(L, SDC, n);
			} else {
				candidateK = MSCandidateGeneration(prevCandidateFkSet, SDC, n);
			}

			for (Transaction T : Transactions) {
				for (ItemSet c : candidateK) {
					if (c.IsPartOf(T.Items)) {
						c.count++;
					}
					ArrayList<Item> items = new ArrayList<>(c.Items);
					items.remove(0);
					boolean isFound = true;
					for (Item x : items) {
						if (!T.Items.contains(x)) {
							isFound = false;
							break;
						}
					}
					if (isFound)
						c.tailCount++; //Increasing tail count
				}
			}
			List<ItemSet> Fk = new ArrayList<>();
			// for creating final Fk
			// set
			prevCandidateFkSet = new ArrayList<>();
			for (ItemSet c : candidateK) {
				// System.out.println("Checking " + c);
				if ((double) c.count / n >= c.Items.get(0).mis && c.count > 0) {

					prevCandidateFkSet.add(c);
					// must have constraint
					for (Item ic : ItemsMustHave) {
						if (c.Items.contains(ic)) {
							Fk.add(c);
							break;
						}
					}
				}

			}
			System.out.println("Candidates, Level " + (k + 1) + ": " + candidateK);
			System.out.println("Level " + (k + 1) + ": " + Fk);
			if (Fk.size() > 0)
				Fkfinal.add(Fk); // adding into final set
			else
				break;
		}
		return Fkfinal;
	}

	private boolean CanItemSetExist(ItemSet c) {
		int foundCount = 0;
		for (List<Item> x : ItemsCannotBeTogether) {
			foundCount = 0;
			for (Item ic : c.Items) {
				if (x.contains(ic)) {
					foundCount++;
					if (foundCount == 2)
						break;
				}
			}
			if (foundCount == 2)
				break;
		}
		return foundCount < 2;
	}

	// Method for initial pass
	private List<Item> initalPass(List<Item> allItems, List<Transaction> transactions) {

		int n = transactions.size();
		List<Item> L = new ArrayList<>();
		for (Transaction t : transactions) {
			for (Item item : t.Items) {
				item.suppCount++;
			}
		}

        //Finding first satisfying element
		double misI = 0;
		int index = 0;
		for (Item item : allItems) {
			if ((double) item.suppCount / n >= item.mis) {
				misI = item.mis;
				L.add(item);
				break;
			}
			index++;
		}

        //Finding rest of the element that satisfy the MIS condition of first
		for (int i = index + 1; i < allItems.size(); i++) {
			Item currItem = allItems.get(i);
			if ((double) currItem.suppCount / n >= misI) {
				L.add(currItem);
			}
		}
		return L;
	}

	// Level 2 candidate generation function, should be similar to algorithm
	// mentioned in the book, different parameters
	// according to requirement in the code
	private List<ItemSet> level2CandidateGen(List<Item> L, double SDC, int totalTrans) {

		List<ItemSet> candidateList = new ArrayList<>();
		int len = L.size();
		for (int i = 0; i < len; i++) {
			Item current = L.get(i);
			if ((double) current.suppCount / totalTrans >= current.mis) {
				for (int j = i + 1; j < len; j++) {
					Item currentInside = L.get(j);

					if ((double) currentInside.suppCount / totalTrans >= current.mis
							&& Math.abs(((double) currentInside.suppCount / totalTrans)
									- ((double) current.suppCount / totalTrans)) <= SDC) {

						List<Item> interCand = new ArrayList<>();
						interCand.add(current);
						interCand.add(currentInside);
						ItemSet is = new ItemSet();
						is.Items.addAll(interCand);
						if (CanItemSetExist(is))
							candidateList.add(is);
					}
				}
			}
		}
		return candidateList;
	}

	// MS candidate generation algorithm
	private List<ItemSet> MSCandidateGeneration(List<ItemSet> Fkprev, double SDC, int totalTrans) {

		List<ItemSet> candidateList = new ArrayList<>();
		List<Item> f1;
		List<Item> f2;
		for (int i = 0; i < Fkprev.size(); i++) {
			for (int j = i + 1; j < Fkprev.size(); j++) {
				f1 = Fkprev.get(i).Items;
				f2 = Fkprev.get(j).Items;

				boolean canContinue = true;
				for (int k = 0; k < f1.size() - 1; k++) {
					if (f1.get(k) != f2.get(k)) {
						canContinue = false;
						break;
					}
				}
				if (!canContinue)
					continue;

				Item i1 = f1.get(f1.size() - 1);
				Item i2 = f2.get(f2.size() - 1);
				List<Item> tempC = new ArrayList<>();
				// pair wise merge step
				if (i1.mis <= i2.mis
						&& Math.abs(((double) i1.suppCount / totalTrans) - (double) i2.suppCount / totalTrans) <= SDC) {
					tempC = new ArrayList<>(f1);
					tempC.add(i2);
					ItemSet is = new ItemSet(tempC);

					boolean isAdd = true;
					List<List<Item>> subsetTempC = getAllSubSets(tempC);
					for (List<Item> itemList : subsetTempC) {
						if (itemList.contains(tempC.get(0)) || (tempC.get(1)).mis == tempC.get(0).mis) {
							if (!contains(Fkprev, itemList)) {
								// perform pruning
								isAdd = false;
							}
						}
					}
					if (isAdd && CanItemSetExist(is))
						candidateList.add(is);
				}
			}
		}
		return candidateList;
	}

    //Step to check if the sublist is the part of super list
	private boolean contains(List<ItemSet> superList, List<Item> subList) {

		for (ItemSet items : superList) {
			if (items.IsPartOf(subList))
				return true;
		}
		return false;
	}

	// Complete this part. Generate all possible k-1 subsets according to
	// minsupport not id

	private List<List<Item>> getAllSubSets(List<Item> tempC) {
		List<List<Item>> result = new ArrayList<>();
		for (int i = 0; i < tempC.size(); i++) {
			List<Item> items = new ArrayList<>(tempC);
			items.remove(i);
			result.add(items);
		}
		return result;
	}

    //Function to print output
	private void printOutput(List<List<ItemSet>> FkFinal) {

		try (BufferedWriter writer = new BufferedWriter(new FileWriter("data\\output1-2.txt"))) {
			for (int i = 0; i < FkFinal.size(); i++) {
				List<ItemSet> temp = FkFinal.get(i);
				writer.write("Frequent " + (i + 1) + "-itemsets");
				writer.newLine();
				writer.newLine();
				for (int j = 0; j < temp.size(); j++) {

					if (i == 0) {
						writer.write("\t" + temp.get(j).count + " : " + "{" + temp.get(j).Items + "}");
						writer.newLine();
					} else {
						writer.write("\t" + temp.get(j).count + " : " + "{" + temp.get(j).Items + "}");
						writer.newLine();
						writer.write("Tail Count = " + temp.get(j).tailCount);
						writer.newLine();
					}

				}
				writer.newLine();
				writer.write("\tTotal number of frequent " + (i + 1) + "-itemsets = " + temp.size());
				writer.newLine();
				writer.newLine();
			}
			writer.close();
		} catch (IOException ex) {
			ex.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {

        FrequencyCalculator fc = new FrequencyCalculator("data\\data-1.txt", "data\\para1-2.txt");
		fc.printOutput(fc.MSAprioriAlgo());
	}
}
