package com.boot.listener4;

public class Test4JDK {

	/**
	 * 版本5：JDK观察者模式
	 */
	public static void main(String[] args) {

		Movie4JDK movie4JDK = new Movie4JDK();
		movie4JDK.addObserver(new Llsydn4JDK());
		movie4JDK.addObserver(new Lgxhg4JDK());
		movie4JDK.deleteObserver(new Llsydn4JDK());
		movie4JDK.play();

	}

}
