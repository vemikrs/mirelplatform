/*
 * Copyright(c) 2019 mirelplatform All Right Reserved.
 */
package jp.vemi.mirel.apps.mste.domain.dto;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

/**
 * 生成結果
 */
@lombok.Data
@lombok.Builder
@lombok.NoArgsConstructor(force = true, staticName = "of")
@lombok.AllArgsConstructor(staticName = "of")
public class GenerateResult {

  /** files */
  // ファイイルID・ファイル名
  @lombok.Builder.Default
  private List<Pair<String, String>> files = new java.util.ArrayList<>();

  /** errors */
  @lombok.Builder.Default
  private List<String> errors = new java.util.ArrayList<>();

}
