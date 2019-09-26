package com.boot.listener2;

//观察者
public class Llsydn {

	public void update(MovieEvent movieEvent) {
		if (movieEvent.getType() == "1") {
			System.out.println("cry");
		}

	}

}
