package com.boot.listener;

//被观察者
public class Movie implements Runnable {

	private boolean isMove = false;

	public void play() {
		try {
			Thread.sleep(5000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		isMove = true;
	}
	public boolean isMove(){
		return isMove;
	}

	public void run() {
		play();
	}
}
