package com.github.ruediste;

import java.io.Serializable;

import net.sf.cglib.core.ClassGenerator;
import net.sf.cglib.core.DefaultGeneratorStrategy;
import net.sf.cglib.proxy.Dispatcher;
import net.sf.cglib.proxy.Enhancer;

import org.junit.Test;


public class TestApplicationInstanceTest implements Serializable{

	public static class TestClass{
		
	}
	@Test
	public void test(){
		Enhancer e=new Enhancer();
		
		e.setSuperclass(TestClass.class);
	
		e.setCallback(new Dispatcher() {
			
			@Override
			public Object loadObject() throws Exception {
				// TODO Auto-generated method stub
				return null;
			}
		});
		e.setStrategy(new DefaultGeneratorStrategy(){
			@Override
			protected ClassGenerator transform(ClassGenerator cg)
					throws Exception {
				return new ClassGenerator() {
					
					@Override
					public void generateClass(ClassVisitor v) throws Exception {
						cg.generateClass(new ClassVisitor(Opcodes.ASM4,v) {
							@Override
							public void visitEnd() {
								cv.visitEnd();
							}
						});
					}
				}; 
			}
		});
		Object instance = e.create();
	}
	
	
}
