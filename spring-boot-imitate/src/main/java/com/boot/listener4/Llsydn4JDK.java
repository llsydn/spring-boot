package com.boot.listener4;

import java.util.Observable;
import java.util.Observer;

//观察者
public class Llsydn4JDK implements Observer {

	public void update(Observable o, Object arg) {
		System.out.println("llsydn--4--jdk  observer");
	}

}
