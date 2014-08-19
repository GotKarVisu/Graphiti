package wiki_parser;
import java.io.IOException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Parser {
	public static void main(String[] args) {
		Elements test = parse("http://de.wikipedia.org/wiki/Bauhaus-Universit%C3%A4t_Weimar");
		printElements(test);
	}
	public static Elements parse(String inURL) {
		String url = inURL;
		//System.out.printf("Fetching %s...", url);
		Document doc = null;
		try {
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Elements links = doc.select("a[href]");
        //print("\nLinks: (%d)", links.size());
        /*for (Element link : links) {
        	System.out.println(link.attr("abs:href") + " ---- " + trim(link.text(), 35));
        }*/
        return links;
	}
	private static String trim(String s, int width) {
        if (s.length() > width)
            return s.substring(0, width-1) + ".";
        else
            return s;
    }
	private static void printElements(Elements el) {
		
		for (Element link : el) {
        	System.out.println(link.attr("abs:href") + " ---- " + trim(link.text(), 35));
        }
	}
}
