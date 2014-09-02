package wiki_parser;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Parser {
	public static void main(String[] args) {
		String testURL = "http://de.wikipedia.org/wiki/Bauhaus-Universit%C3%A4t_Weimar";
		//String testURL = "http://de.wikipedia.org/wiki/Deutschland";
		/*Elements elements = parse(testURL);
		ArrayList<Pair> links = makeList(elements);
		//printList(links);
		System.out.println(links.size());
		links = clearDoubled(links);
		System.out.println(links.size());
		links = clearNoArticle(links);
		System.out.println(links.size());
		links = clearThisArticle(links, testURL);
		System.out.println(links.size());
		links = clearDoubled(links);
		System.out.println(links.size());
		printList(links);*/
		ArrayList<Pair> l = getList(testURL);
		printList(l);
		getHTMLText(testURL);
	}
	private static Elements parse(String inURL) {
		String url = inURL;
		Document doc = null;
		try {
			doc = Jsoup.connect(url).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		Elements links = doc.select("a[href]");
        return links;
	}
	private static String trim(String s, int width) {
        if (s.length() > width)
            return s.substring(0, width-1) + ".";
        else
            return s;
    }
	private static ArrayList<Pair> makeList(Elements el) {
		ArrayList<Pair> liste = new ArrayList<Pair>();
		for(Element e : el) {
			Pair tmp = new Pair();
			tmp.url = e.attr("abs:href");
			tmp.titel = trim(e.text(), 100);
			liste.add(tmp);
		}
		return liste;
	}
	private static void printList(ArrayList<Pair> l) {
		for(Pair p : l) {
			System.out.println(p.titel + " -- " + p.url + "  --  " + p.count);
		}
	}
	// Entfernt alle Links, die doppelt vorkommen
	private static ArrayList<Pair> clearDoubled(ArrayList<Pair> l) {
		ArrayList<Pair> tmp = new ArrayList<Pair>();
		for(Pair p1 : l) {
			for(Pair p2 : l) {
				if(p1 != p2 && p1.url.equals(p2.url)) {
					tmp.add(p2);
				}
			}
		}
		l.removeAll(tmp);
		return l;
	}
	// Entfernt alle Links, die nicht auf einen Wikipedia Artikel verweisen
	private static ArrayList<Pair> clearNoArticle(ArrayList<Pair> l) {
		ArrayList<Pair> tmp = new ArrayList<Pair>();
		for(Pair p : l) {
			if(!p.url.startsWith("http://de.wikipedia.org/wiki/")
					|| p.url.startsWith("http://de.wikipedia.org/wiki/Datei")
					|| p.url.startsWith("http://de.wikipedia.org/wiki/Spezial")
					|| p.url.startsWith("http://de.wikipedia.org/wiki/Hilfe")
					|| p.url.startsWith("http://de.wikipedia.org/wiki/Wikipedia")
					|| p.url.startsWith("http://de.wikipedia.org/wiki/Portal")
					|| p.url.startsWith("http://de.wikipedia.org/wiki/Kategorie")
					|| p.url.startsWith("http://de.wikipedia.org/wiki/Diskussion")
					|| p.titel.length() < 3) {
				tmp.add(p);
			}
		}
		l.removeAll(tmp);
		return l;
	}
	// Entfernt alle Links, die auf den Aktuellen Artikel verweisen
	private static ArrayList<Pair> clearThisArticle(ArrayList<Pair> l, String url) {
		ArrayList<Pair> tmp = new ArrayList<Pair>();
		for(Pair p : l) {
			if(p.url.startsWith(url)) {
				tmp.add(p);
			}
		}
		l.removeAll(tmp);
		return l;
	}
	public static ArrayList<Pair> getList(String url) {
		Elements elements = parse(url);
		ArrayList<Pair> links = makeList(elements);
		links = clearDoubled(links);
		links = clearNoArticle(links);
		links = clearThisArticle(links, url);
		links = clearDoubled(links);
		String text = getHTMLText(url);
		for(Pair p : links) {
			p.count = countTitle(p.titel, text);
		}
		return links;
	}
	private static String getHTMLText(String inURL) {

		Document doc = null;
		try {
			doc = Jsoup.connect(inURL).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		String text = doc.body().text();
		return text;
	}
	private static int countTitle(String title, String site) {
		int lastIndex = 0;
		int count = 0;
		while(lastIndex != -1) {
			lastIndex = site.indexOf(title,lastIndex);
			if(lastIndex != -1) {
				count ++;
				lastIndex += title.length();
			}
		}
		return count;
	}
}
