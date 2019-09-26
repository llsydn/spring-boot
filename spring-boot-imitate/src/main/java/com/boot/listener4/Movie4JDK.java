package com.boot.listener4;

import java.util.Observable;

//被观察者
public class Movie4JDK extends Observable {

	public void play(){
		setChanged();       //状态改变
		notifyObservers();  //通知所有观察者
	}

}
