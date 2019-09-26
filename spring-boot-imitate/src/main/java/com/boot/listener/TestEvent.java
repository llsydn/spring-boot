package com.boot.listener;

public class TestEvent {
	/**
	 * 版本1：观察者持有被观察者的实例
	 */
	public static void main(String[] args) {
		Movie movie = new Movie();
		Thread threadm = new Thread(movie);
		threadm.start();

		Llsydn llsydn = new Llsydn(movie);
		Thread threadf = new Thread(llsydn);
		threadf.start();
	}
}
