package com.github.ruediste.framework.classReload;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;


public class GateTest {

	private boolean passed;
	
	@Before
	public void setup(){
		passed=false;
	}
	
	@Test
	public void simple(){
		Gate gate=new Gate();
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				sleep();
				passed=true;
				gate.open();
			}
		}).start();
		assertFalse(passed);
		gate.pass();
		assertTrue(passed);
	}
	
	@Test(timeout=200)
	public void goThroughFirst(){
		Gate gate=new Gate();
		
		new Thread(new Runnable() {
			
			@Override
			public void run() {
				gate.open();
				passed=true;
			}
		}).start();
		sleep();
		assertTrue(passed);
		gate.pass();
	}
	
	protected void sleep() {
		try {
			Thread.sleep(100);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}
}
