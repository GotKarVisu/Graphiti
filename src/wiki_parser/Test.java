package wiki_parser;

public class Test {

	public static void main(String[] args) {
//		String url = "http://de.wikipedia.org/wiki/Weimar";
		String url = "http://de.wikipedia.org/wiki/Deutschland";
		Parser parser = new Parser(url);
		String title = parser.getTitle();
		System.out.println(title);
		System.out.println(parser.getTeaser().length() + " --- " + parser.getTeaser());
		//parser.printList();
	}
}