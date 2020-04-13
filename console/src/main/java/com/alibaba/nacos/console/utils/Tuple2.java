package com.alibaba.nacos.console.utils;

public class Tuple2<K, V> {
	private K left;
	private V right;
	
	public Tuple2(K left, V right) {
		super();
		this.left = left;
		this.right = right;
	}

	public K getLeft() {
		return left;
	}

	public V getRight() {
		return right;
	}
	
}
