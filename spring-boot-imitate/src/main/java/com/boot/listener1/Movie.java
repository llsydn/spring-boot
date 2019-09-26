package com.boot.listener1;

//被观察者
public class Movie implements Runnable {

	/**
	 * 版本2：被观察者持有观察者引用
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
		//这个耦合度太高
		//事件也太单一，不一定会上厕所
		llsydn.toilet();
	}

	public void run() {
		play();
	}
}
