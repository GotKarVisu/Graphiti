package wiki_parser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class Parser {
	private String url;
	private String title;
	private ArrayList<Article> list;
	private Document doc;
	private String text;
	
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
		removeHttps();
		parseDocument();
		makeList();
		setTitle();
		cleanList();
		sort(list);
	}
	
	public void setUrl(String inUrl) {
		this.url = inUrl;
	}
	public String getURL() {
		return this.url;
	}
	public String getTitle() {
		return this.title;
	}
	public ArrayList<Article> getList() {
		return this.list;
	}

	private void parseDocument() {
		try {
			this.doc = Jsoup.connect(this.url).get();
		} catch (IOException e) {
			e.printStackTrace();
		}
		this.text = getHTMLText();
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
		String tmp = this.doc.title();
		if(tmp.endsWith("Wikipedia")) {
			
			this.title = tmp.substring(0, tmp.length()-12);
		}
	}

	private void cleanList() {
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
					|| a.url.startsWith(this.url)
					|| a.titel.length() < 3) {
				tmp.add(a);
			}
			else {
				for(Article a2 : this.list) {
					if(a != a2 && a.url.equals(a2.url)) {
						tmp.add(a2);
					}
				}
				a.count = countTitle(a.titel, this.text);
			}
		}
		this.list.removeAll(tmp);
	}
	
	private int countTitle(String substr, String str) {
		return str.split(Pattern.quote(substr), -1).length - 1;
	}
	
	private void sort(ArrayList<Article> x) {
		quicksort(x, 0, x.size()-1);
	}
	private void quicksort(ArrayList<Article> x, int l, int r) {
		if(l < r) {
			int i = partitionQS(x, l, r);
			quicksort(x,l,i-1);
			quicksort(x,i+1,r);
		}
	}
	private static int partitionQS(ArrayList<Article> x, int l, int r) {
		Article pivot, tmp;
		int i, j;
		pivot = x.get(r);
		i = l;
		j = r - 1;
		while(i <= j) {
			if(x.get(i).count < pivot.count) {
				tmp = x.get(i);
				x.set(i, x.get(j));
				x.set(j, tmp);
				j--;
			}
			else {
				i++;
			}
		}
		tmp = x.get(i);
		x.set(i, x.get(r));
		x.set(r, tmp);
		return i;
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
	
	private void removeHttps() {
		String tmp = this.url.toLowerCase();
		if(tmp.startsWith("https")) {
			this.url = "http" + this.url.substring(5,tmp.length());
		}
	}
}
