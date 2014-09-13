package gui;

import java.util.ArrayList;

import wiki_parser.Article;

public class TreeElement {
	public Article node;
	public String parentNode;
	public ArrayList<String> childNodes;
	public String titel;
	
	public TreeElement() {
		this.node = new Article();
		this.parentNode = "";
		this.childNodes = new ArrayList<String>();
		this.titel = "";
	}
	
	public void setTitel(String value) {
		this.titel = value;
	}
	public String getTitel() {
		return this.titel;
	}
	public void setNode(Article value) {
		this.node = value;
	}
	public Article getNode() {
		return this.node;
	}
	public void setParentNode(String value) {
		this.parentNode = value;
	}
	public String getParentNode() {
		return this.parentNode;
	}
	public void setChildren(String value) {
		this.childNodes.add(value);
	}
	public ArrayList<String> getChildren() {
		return this.childNodes;
	}

}
