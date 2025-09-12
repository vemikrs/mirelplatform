/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.framework.exeption;

import java.util.List;

import com.google.common.collect.Lists;

public class MessagingException extends RuntimeException {

  /**
   * serialVersionUID
   */
  private static final long serialVersionUID = 1L;

  /**
   * messages.
   */
  public List<String> messages = Lists.newArrayList();

  /**
   * default constructor.<br/>
   */
  public MessagingException() {
    super();
  }

  /**
   * default constructor.<br/>
   * 
   * @param message
   */
  public MessagingException(String message) {
    super(message);
    this.messages.add(message);
  }

  /**
   * default constructor.<br/>
   * 
   * @param msgs
   */
  public MessagingException(List<String> msgs) {
    super(msgs.toString());
    this.messages.addAll(msgs);
  }

  /**
   * default constructor.<br/>
   * 
   * @param message
   * @param throwable
   */
  public MessagingException(String message, Throwable throwable) {
    super(message, throwable);
    this.messages.add(message);
  }

  /**
   * default constructor.<br/>
   * 
   * @param throwable
   */
  public MessagingException(Throwable throwable) {
    super(throwable);
    this.messages.add(throwable.getLocalizedMessage());
  }

}
