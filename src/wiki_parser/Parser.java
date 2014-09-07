package wiki_parser;
import java.io.IOException;
import java.util.ArrayList;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Parser {
	private String url;
	private String title;
	private ArrayList<Article> list;
	private Document doc;
	
	public Parser() {
		this.url = "";
		this.title = "";
		this.list = new ArrayList<Article>();
		this.doc = null;
	}
	public Parser(String inUrl) {
		this.url = inUrl;
		this.title = "";
		this.list = new ArrayList<Article>();
		this.doc = null;
		parse();
	}
	
	public Parser(Parser cpy) {
		this.url = cpy.url;
		this.title = cpy.title;
		this.list = cpy.list;
		this.doc = cpy.doc;
	}
	
	public void parse() {
		parseDocument();
		makeList();
		setTitle();
		clearDoubled();
		clearNoArticle();
		clearThisArticle();
		String text = getHTMLText();
		for(Article a : list) {
			a.count = countTitle(a.titel, text);
		}
		sortList();
		System.out.println(list.size());
	}
	
	public void setUrl(String inUrl) {
		this.url = inUrl;
	}
	public String getURL() {
		return url;
	}
	public String getTitle() {
		return title;
	}
	public ArrayList<Article> getList() {
		return list;
	}

	private void parseDocument() {
		try {
			this.doc = Jsoup.connect(this.url).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void makeList() {
		Elements el = getElements();
		for(Element e : el) {
			Article a = new Article();
			a.url = e.attr("abs:href");
			a.titel = trim(e.text(), 100);
			this.list.add(a);
		}
	}
	
	private void setTitle() {
		this.title = this.doc.title().replaceAll(" – Wikipedia", "");
	}
	
	// Entfernt alle Links, die doppelt vorkommen
	private void clearDoubled() {
		ArrayList<Article> tmp = new ArrayList<Article>();
		for(Article a1 : this.list) {
			for(Article a2 : this.list) {
				if(a1 != a2 && a1.url.equals(a2.url)) {
					tmp.add(a2);
				}
			}
		}
		this.list.removeAll(tmp);
	}

	private void clearNoArticle() {
		ArrayList<Article> tmp = new ArrayList<Article>();
		for(Article a : this.list) {
			if(!a.url.startsWith("http://de.wikipedia.org/wiki/")
					|| a.url.startsWith("http://de.wikipedia.org/wiki/Datei")
					|| a.url.startsWith("http://de.wikipedia.org/wiki/Spezial")
					|| a.url.startsWith("http://de.wikipedia.org/wiki/Hilfe")
					|| a.url.startsWith("http://de.wikipedia.org/wiki/Wikipedia")
					|| a.url.startsWith("http://de.wikipedia.org/wiki/Portal")
					|| a.url.startsWith("http://de.wikipedia.org/wiki/Kategorie")
					|| a.url.startsWith("http://de.wikipedia.org/wiki/Diskussion")
					|| a.titel.length() < 3) {
				tmp.add(a);
			}
		}
		// FIXME - Leere Liste, nach Saeuberung
		this.list.removeAll(tmp);
	}

	// Entfernt alle Links, die auf den Aktuellen Artikel verweisen
	private void clearThisArticle() {
		ArrayList<Article> tmp = new ArrayList<Article>();
		for(Article a : this.list) {
			if(a.url.startsWith(this.url)) {
				tmp.add(a);
			}
		}
		this.list.removeAll(tmp);
	}
	
	private int countTitle(String title, String site) {
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
	
	private void sortList() {
		ArrayList<Article> tmp = new ArrayList<Article>();
		while(!this.list.isEmpty()) {
			int i = 0;
			Article pNew = new Article();
			for(Article a : this.list) {
				if(a.count > i) {
					i = a.count;
					pNew = a;
				}
			}
			tmp.add(pNew);
			this.list.remove(pNew);
		}
		this.list = tmp;
	}
	
	private Elements getElements() {
        return this.doc.select("a[href]");
	}
	
	private String trim(String s, int width) {
        if (s.length() > width)
            return s.substring(0, width-1) + ".";
        else
            return s;
    }

	public void printList() {
		for(Article a : this.list) {
			System.out.println(a.titel + " -- " + a.url + "  --  " + a.count);
		}
	}

	private String getHTMLText() {
		return this.doc.body().text();
	}
}
