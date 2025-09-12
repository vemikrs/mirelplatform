/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.mste.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import org.apache.commons.lang3.builder.ToStringBuilder;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * ステンシルマスタの更新 結果クラス.<br/>
 */
@Setter
@Getter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class ReloadStencilMasterResult {

  /**
  * {@inheritDoc}
  */
  @Override
  public String toString() {
      return ToStringBuilder.reflectionToString(this);
  }

}
