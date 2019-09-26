package com.boot.listener2;

//被观察者
public class Movie implements Runnable {

	/**
	 * 版本3：事件的多样性
	 */
	Llsydn llsydn;

	public Movie(Llsydn llsydn) {
		this.llsydn = llsydn;
	}

	public void play() {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//事件的多样性
		MovieEvent movieEvent = new MovieEvent();
		movieEvent.setContext("only");
		movieEvent.setType("1");

		//耦合性比较高，不能实现多个人看电影
		llsydn.update(movieEvent);
	}


	public void run() {
		play();
	}
}
