package com.boot.listener4;

import java.util.Observable;
import java.util.Observer;

//观察者
public class Lgxhg4JDK implements Observer {

	public void update(Observable o, Object arg) {
		System.out.println("lgxhg--4--jdk observer");
	}

}
