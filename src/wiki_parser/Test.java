package wiki_parser;

public class Test {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		String url = "http://de.wikipedia.org/wiki/Friede_von_Utrecht";
		Parser parser = new Parser(url);
		String title = parser.getTitle();
		System.out.println(title);
		parser.printList();
	}
}