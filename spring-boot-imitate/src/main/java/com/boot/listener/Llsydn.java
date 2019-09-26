package com.boot.listener;

//观察者
public class Llsydn implements Runnable {
	/**
	 * 版本1：观察者拥有被观察者的实例
	 */
	Movie movice;
	public Llsydn(Movie movice) {
		this.movice = movice;
	}
	public void toilet() {
		System.out.println("去上厕所了...");
	}
	public void run() {
		System.out.println("开始看电影了...");
		//一直在看电影，观察Movice的状态
		//当时这样的话，llsydn需要一直在看电影，性能耗时
		while (!movice.isMove()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		//要去上厕所
		toilet();
	}
}
