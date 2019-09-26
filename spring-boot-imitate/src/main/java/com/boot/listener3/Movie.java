package com.boot.listener3;

import java.util.ArrayList;
import java.util.List;

//被观察者
public class Movie implements Runnable {

	/**
	 * 版本3：观察者多样性，事件多样性
	 */
	List<MovieListener> listeners = new ArrayList<MovieListener>();

	public void addListeners(MovieListener listener) {
		listeners.add(listener);
	}


	public void play() {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		//事件的多样性
		MovieEvent movieEvent = new MovieEvent(this);
		movieEvent.setContext("only");
		movieEvent.setType("1");

		for (MovieListener listener : listeners) {
			listener.update(movieEvent);
		}

	}


	public void run() {
		play();
	}
}
