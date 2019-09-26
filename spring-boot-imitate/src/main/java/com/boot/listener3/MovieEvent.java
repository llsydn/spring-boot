package com.boot.listener3;

//事件
public class MovieEvent {

	Object source = null;

	public MovieEvent(Object source){
		this.source=source;
	}

	private String context;
	private String type;

	public String getContext() {
		return context;
	}

	public void setContext(String context) {
		this.context = context;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}
}
