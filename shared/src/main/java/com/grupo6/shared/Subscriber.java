package com.grupo6.shared;

public interface Subscriber<C> {
  void update(C context);
}
