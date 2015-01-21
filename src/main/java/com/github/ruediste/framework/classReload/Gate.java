package com.github.ruediste.framework.classReload;

/**
 * A gate which starts closed and can be opened at some point
 */
public class Gate {

	private boolean isOpen;
	private final Object lock=new Object();
	
	/**
	 * Pass the gate. If the gate is closed, wait until it is opened
	 */
	public void pass(){
		if (!isOpen)
			synchronized (lock) {
				if (!isOpen)
					try {
						lock.wait();
					} catch (InterruptedException e) {
						throw new RuntimeException(e);
					}
			}
	}
	
	/**
	 * Open the gate
	 */
	public void open(){
		synchronized (lock) {
			isOpen=true;
			lock.notifyAll();
		}
	}
}
