/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.foundation.abst.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

public abstract class ServiceParameter {

  /**
   * {@inheritDoc}
   */
  @Override
  public String toString() {
    return ToStringBuilder.reflectionToString(this);
  }

}