package com.boot.listener3;

//观察者
public class Llsydn implements MovieListener{

	public void update(MovieEvent movieEvent) {
		if (movieEvent.getType() == "1") {
			System.out.println("llsydn cry");
		}
	}
}
