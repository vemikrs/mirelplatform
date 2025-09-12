/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.foundation.feature.files.dto;

import java.util.List;

import jp.vemi.mirel.foundation.abst.dto.ServiceParameter;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * {@link FileDownloadParameter}
 */
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FileDownloadParameter extends ServiceParameter {

  public List<String> fileIds;

  public String user;

}
