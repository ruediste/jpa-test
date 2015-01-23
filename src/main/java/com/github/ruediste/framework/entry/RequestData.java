package com.github.ruediste.framework.entry;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import com.google.inject.Key;

public class RequestData {

	private final static ThreadLocal<RequestData> current=new ThreadLocal<RequestData>(){
		@Override
		protected RequestData initialValue() {
			return new RequestData();
		}
	};
	
	public static void setCurrent(RequestData data){
		current.set(data);
	}
	
	public static RequestData getCurrent(){
		return current.get();
	}

	public static void remove() {
		current.remove();
	}
	
	public final Map<Key<?>, Object> requestScopedInstances=new HashMap<>();
	public final HashSet<Key<?>> lockedRequestScopeKeys=new HashSet<>();
}
