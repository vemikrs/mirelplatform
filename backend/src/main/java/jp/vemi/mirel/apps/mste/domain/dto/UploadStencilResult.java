/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.mste.domain.dto;

import org.apache.commons.lang3.builder.ToStringBuilder;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * ステンシルアップロード 結果クラス
 */
@Setter
@Getter
@Builder
public class UploadStencilResult {


  /**
  * {@inheritDoc}
  */
  @Override
  public String toString() {
      return ToStringBuilder.reflectionToString(this);
  }

}

