package com.github.ruediste.laf.core.guice;

import java.util.HashSet;
import java.util.Set;
import java.util.function.Function;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import net.sf.cglib.proxy.Dispatcher;
import net.sf.cglib.proxy.Enhancer;

import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.inject.Key;
import com.google.inject.Provider;
import com.google.inject.Scope;

/**
 * {@link Guice} module to be used by application instances
 */
public class ApplicationInstanceModule extends AbstractModule {

	private static final class SessionScope implements Scope {

		@Inject
		Injector injector;

		ThreadLocal<Set<Key<?>>> lockedKeys = new ThreadLocal<Set<Key<?>>>() {
			@Override
			protected java.util.Set<Key<?>> initialValue() {
				return new HashSet<>();
			}
		};

		@Override
		public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
			return new Provider<T>() {

				// create the proxy right away, such that it can be reused
				// afterwards
				Object proxy = Enhancer.create(key.getTypeLiteral()
						.getRawType(), new Dispatcher() {

					@Override
					public Object loadObject() throws Exception {
						return SessionData.getCurrent().sessionScopedInstanceContainer
								.getInstance(key, () -> {
									boolean doRemove = false;
									if (!lockedKeys.get().contains(key)) {
										lockedKeys.get().add(key);
										doRemove = true;
									}
									try {
										return injector.getInstance(key);
									} finally {
										if (doRemove)
											lockedKeys.get().remove(key);
									}
								});
					}
				});

				@SuppressWarnings("unchecked")
				@Override
				public T get() {
					if (lockedKeys.get().contains(key)) {
						return unscoped.get();
					}
					return (T) proxy;
				}
			};

		}
	}

	public static class RequestScope implements Scope {
		@Inject
		Injector injector;

		@Override
		public <T> Provider<T> scope(Key<T> key, Provider<T> unscoped) {
			return new Provider<T>() {

				// create the proxy right away, such that it can be reused
				// afterwards
				Object proxy = Enhancer.create(key.getTypeLiteral()
						.getRawType(), new Dispatcher() {

					@Override
					public Object loadObject() throws Exception {
						return RequestData.getCurrent().requestScopedInstances.computeIfAbsent(
								key,
								(x) -> {
									Set<Key<?>> lockedKeys = RequestData.getCurrent().lockedRequestScopeKeys;
									boolean doRemove = false;
									if (!lockedKeys.contains(key)) {
										lockedKeys.add(key);
										doRemove = true;
									}
									try {
										return injector.getInstance(key);
									} finally {
										if (doRemove)
											lockedKeys.remove(key);
									}
								});
					}
				});

				@SuppressWarnings("unchecked")
				@Override
				public T get() {
					if (RequestData.getCurrent().lockedRequestScopeKeys.contains(key)) {
						return unscoped.get();
					}
					return (T) proxy;
				}
			};
		}

	}

	@Override
	protected void configure() {
		install(new LoggerBindingModule());
		install(new PostConstructModule());

		// bind scopes
		{
			SessionScope scope = new SessionScope();
			bindScope(SessionScoped.class, scope);
			bind(SessionScope.class).toInstance(scope);
		}
		{
			RequestScope scope = new RequestScope();
			bindScope(RequestScoped.class, scope);
			bind(RequestScope.class).toInstance(scope);
		}
		
		bindToConnection(HttpServletResponse.class, x->x.getResponse());
		bindToConnection(HttpServletRequest.class, x->x.getRequest());
		
	}

	private <T> void bindToConnection(Class<T> clazz, Function<RequestData, T> func){
		bind(clazz).toProvider(new Provider<T>() {

			@SuppressWarnings("unchecked")
			T proxy = (T) Enhancer.create(clazz, new Dispatcher(){
				
				@Override
				public Object loadObject() throws Exception {
					return func.apply(RequestData.getCurrent());
				}});

			@Override
			public T get() {
				return proxy;
			}
		});
	}
}