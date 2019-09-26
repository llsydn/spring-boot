package com.boot.listener1;

public class TestEvent {

	/**
	 * 版本2：被观察者持有观察者引用
	 */
	public static void main(String[] args) {
		Movie movie = new Movie(new Llsydn());
		Thread threadm = new Thread(movie);
		threadm.start();
	}
}
