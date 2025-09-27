/*
 * Copyright(c) 2019 mirelplatform All right reserved.
 */
package jp.vemi.mirel.foundation.feature.files.service;

import java.io.File;

import org.apache.commons.lang3.tuple.Pair;
import org.springframework.web.multipart.MultipartFile;

/**
 * ファイル登録サービス .<br/>
 */
public interface FileRegisterService {

  public Pair<String, String> register(File file, boolean isZip);

  public Pair<String, String> register(MultipartFile multipartFile);

  public Pair<String, String> register(File srcFile, boolean isZip, String fileName);

}