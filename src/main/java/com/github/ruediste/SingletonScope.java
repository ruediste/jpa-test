package com.github.ruediste;

import java.io.ObjectStreamException;
import java.io.Serializable;
import java.lang.reflect.Method;

import net.sf.cglib.proxy.Callback;
import net.sf.cglib.proxy.CallbackFilter;
import net.sf.cglib.proxy.Enhancer;
import net.sf.cglib.proxy.InvocationHandler;
import net.sf.cglib.proxy.LazyLoader;

import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;
import com.google.inject.Scopes;

final class SingletonScope implements Scope {
	private interface IWriteObject extends Serializable{
		Object writeReplace() throws ObjectStreamException;
	}
	
	private static class ReplacementObject implements Serializable{
		private static final long serialVersionUID = 1L;
		private Key<?> key;

		public ReplacementObject(Key<?> key) {
			this.key = key;
		}

		public Object readResolve() throws ObjectStreamException{
			return ContextInjector.getInjector().getInstance(key);
		}
	}
	
	private static CallbackFilter callbackFilter=new CallbackFilter() {
		
		@Override
		public int accept(Method method) {
			if (method.getName().equals("writeReplace") && Object.class.equals(method.getReturnType())){
				return 1;
			}
			return 0;
		}
	};
	
	@Override
	public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
		Provider<T> singletonScopedProvider = Scopes.SINGLETON.scope(key, unscoped);
		if (false)
			return singletonScopedProvider;
		else
		return new Provider<T>() {

			@SuppressWarnings("unchecked")
			@Override
			public T get() {
				T singletonScoped = singletonScopedProvider.get();
				Class<? super T> rawType = key.getTypeLiteral().getRawType();
				Enhancer e = new Enhancer();
				if (rawType.isInterface()){
					e.setSuperclass(Object.class);
					e.setInterfaces(new Class<?>[]{rawType, IWriteObject.class});
				}
				else {
					e.setSuperclass(rawType);
					e.setInterfaces(new Class<?>[]{IWriteObject.class});
				}
				
				e.setCallback(null);
				e.setCallbackFilter(callbackFilter);
				e.setCallbacks(new Callback[]{
						new LazyLoader() {
							
							@Override
							public Object loadObject() throws Exception {
								return singletonScoped;
							}
						},
						new InvocationHandler() {
							
							@Override
							public Object invoke(Object proxy, Method method, Object[] args)
									throws Throwable {
								// called for writeReplace
								return new ReplacementObject(key);
							}
						}
				});
				return (T) e.create();
			}
		};
	}
}