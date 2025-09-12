/*
 * Copyright(c) 2015-2021 vemi/mirelplatform.
 */
package jp.vemi.mirel.apps.selenade.domain.dto;

import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.apache.commons.lang3.builder.ToStringBuilder;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

/**
 * テスト実行 結果クラス.<br/>
 */
@Setter
@Getter
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@Generated(value = {"jp.vemi.ste.domain.engine.TemplateEngineProcessor"}, comments = "Generated from /mirel/service:191207A")
public class RunTestResult {


  /**
  * {@inheritDoc}
  */
  @Override
  public String toString() {
      return ToStringBuilder.reflectionToString(this);
  }

}
