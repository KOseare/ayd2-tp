package com.grupo6.shared;

public interface Publisher<C> {
  void subscribe(Subscriber<C> s);

  void unsubscribe(Subscriber<C> s);

  void notifySubscribers(C context);
}
