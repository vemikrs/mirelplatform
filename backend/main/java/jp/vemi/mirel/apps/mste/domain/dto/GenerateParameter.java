/*
 * Copyright(c) 2019 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.mste.domain.dto;

import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 生成パラメータ
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GenerateParameter {
  /**
   * params
   */
  public List<Map<String, Object>> params;
}
