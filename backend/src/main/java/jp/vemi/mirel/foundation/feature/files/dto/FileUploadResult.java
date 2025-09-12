/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.foundation.feature.files.dto;

import java.nio.file.Path;
import java.util.List;

import com.google.common.collect.Lists;

import groovy.lang.Tuple3;
import jp.vemi.mirel.foundation.abst.dto.ServiceResult;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.Builder.Default;

/**
 * {@link FileDownloadResult}
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileUploadResult extends ServiceResult {

  @Default
  public List<Tuple3<String, String, Path>> paths = Lists.newArrayList();
  public String uuid;
  public String fileName;
}
