package gui;

import wiki_parser.Article;

public class TreeElement {
	private String title;
	private Article article; 
	public String url;
	
	public TreeElement() {
		this.title = "";
		this.url = "";
	}
	
	public TreeElement(String title, String url) {
		this.title = title;
		this.url = url;
	}
	public void setUrl(String value) {
		this.url = value;
	}
	public String getTitle() {
		return this.title;
	}
	public void setTitle(String value) {
		this.title = value;
	}
	public String getUrl() {
		return this.url;
	}

}
