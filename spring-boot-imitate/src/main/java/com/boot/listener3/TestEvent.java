package com.boot.listener3;

public class TestEvent {

	/**
	 * 版本3：被观察者持有观察者引用
	 * 观察者多样性，事件多样性
	 */
	public static void main(String[] args) {

		Movie movie = new Movie();
		movie.addListeners(new Llsydn());
		movie.addListeners(new Lgxhg());

		Thread threadm = new Thread(movie);
		threadm.start();

	}

}
