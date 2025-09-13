/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.mste.domain.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.ArrayList;

import com.google.common.collect.Maps;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.ConstructorException;

import jp.vemi.framework.exeption.MirelApplicationException;
import jp.vemi.framework.exeption.MirelSystemException;
import jp.vemi.framework.util.FileUtil;
import jp.vemi.framework.util.StorageUtil;
import jp.vemi.framework.config.StorageConfig;
import jp.vemi.mirel.apps.mste.domain.dao.entity.MsteStencil;
import jp.vemi.mirel.apps.mste.domain.dao.repository.MsteStencilRepository;
import jp.vemi.mirel.apps.mste.domain.dto.ReloadStencilMasterParameter;
import jp.vemi.mirel.apps.mste.domain.dto.ReloadStencilMasterResult;
import jp.vemi.mirel.foundation.abst.dao.entity.FileManagement;
import jp.vemi.mirel.foundation.abst.dao.repository.FileManagementRepository;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;
import jp.vemi.ste.domain.dto.yml.StencilSettingsYml;
import jp.vemi.ste.domain.dto.yml.StencilSettingsYml.Stencil.Config;
import jp.vemi.ste.domain.engine.TemplateEngineProcessor;

/**
 * {@link ReloadStencilMasterService ステンシルマスタの更新} の具象です。
 */
@Service
@Transactional
public class ReloadStencilMasterServiceImp implements ReloadStencilMasterService {

    /** {@link MsteStencilRepository ステンシルマスタ} */
    @Autowired
    protected MsteStencilRepository stencilRepository;

    @Autowired
    protected FileManagementRepository fileManagementRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiResponse<ReloadStencilMasterResult> invoke(ApiRequest<ReloadStencilMasterParameter> parameter) {

        ApiResponse<ReloadStencilMasterResult> resp = ApiResponse.<ReloadStencilMasterResult>builder().build();

        this.read();
        resp.setData(ReloadStencilMasterResult.builder().build());

        return resp;

    }

    /**
     * read from stencil directory.
     */
    protected void read() {
        try {
            // clear.
            stencilRepository.deleteAll();

            // レイヤード検索でstencil-settings.ymlを収集
            Map<String, String> categories = Maps.newLinkedHashMap();
            List<String> allFiles = collectStencilSettingsFromLayers();
            
            System.out.println("Found " + allFiles.size() + " stencil-settings.yml files");

            // save stencil record.
            for (String fileName : allFiles) {
                try {
                    StencilSettingsYml settings = readYaml(new File(fileName));
                    if (settings == null || settings.getStencil() == null || settings.getStencil().getConfig() == null) {
                        System.out.println("Warning: Invalid stencil settings in " + fileName);
                        continue;
                    }
                    
                    Config config = settings.getStencil().getConfig();

                    MsteStencil entry = new MsteStencil();
                    entry.setStencilCd(config.getId());
                    entry.setStencilName(config.getName());
                    entry.setItemKind("1");
                    entry.setSort(0);
                    stencilRepository.save(entry);
                    System.out.println(config.getId() + "/" + config.getSerial() + ":" + config.getName());

                    if (false == StringUtils.isEmpty(config.getCategoryId())) {
                        if (false == categories.containsKey(config.getCategoryId())) {
                            if (StringUtils.isEmpty(categories.get(config.getCategoryId()))) {
                                categories.put(config.getCategoryId(), config.getCategoryName());
                            }
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error processing stencil settings file: " + fileName + " - " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // save stencil record.
            for (Entry<String, String> catentry : categories.entrySet()) {
                MsteStencil entry = new MsteStencil();
                entry.setStencilCd(catentry.getKey());
                entry.setStencilName(catentry.getValue());
                entry.setItemKind("0");
                entry.setSort(0);
                stencilRepository.save(entry);
            }

            // 従来のファイル管理処理（後方互換性）
            readFileManagementLegacy();
            
        } catch (Exception e) {
            System.out.println("Error in read() method: " + e.getMessage());
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * レイヤード検索でstencil-settings.ymlファイルを収集する
     * TemplateEngineProcessorの既存機能を活用してステンシル設定を収集
     */
    private List<String> collectStencilSettingsFromLayers() {
        List<String> allFiles = new ArrayList<>();
        
        try {
            // 既存のファイルシステム検索（ユーザー・標準レイヤー）
            String baseDir = StorageUtil.getBaseDir();
            String stencilMasterStorageDir = TemplateEngineProcessor.getStencilMasterStorageDir();
            String dir = baseDir + stencilMasterStorageDir;
            
            System.out.println("=== Layered Stencil Collection Using TemplateEngineProcessor ===");
            System.out.println("Base directory: " + baseDir);
            System.out.println("Stencil master storage dir: " + stencilMasterStorageDir);
            
            // 既存ディレクトリ検索（ユーザー・標準レイヤー）
            List<String> legacyFiles = FileUtil.findByFileName(dir, "stencil-settings.yml");
            allFiles.addAll(legacyFiles);
            System.out.println("Found " + legacyFiles.size() + " legacy/user stencil files");
            
            // TemplateEngineProcessorを使って既知のサンプルステンシルを検索
            collectKnownSampleStencilsUsingTemplateEngine(allFiles);
            
        } catch (Exception e) {
            System.out.println("Error in collectStencilSettingsFromLayers: " + e.getMessage());
            e.printStackTrace();
            
            // フォールバック: 最低限の既存ディレクトリ検索
            try {
                String dir = StorageUtil.getBaseDir() + TemplateEngineProcessor.getStencilMasterStorageDir();
                List<String> legacyFiles = FileUtil.findByFileName(dir, "stencil-settings.yml");
                if (!allFiles.containsAll(legacyFiles)) {
                    allFiles.addAll(legacyFiles);
                }
            } catch (Exception fallbackException) {
                System.out.println("Fallback search also failed: " + fallbackException.getMessage());
            }
        }
        
        System.out.println("Total files collected: " + allFiles.size());
        return allFiles;
    }

    /**
     * TemplateEngineProcessorを使って既知のサンプルステンシルを検索し、一時ファイルとして収集
     */
    private void collectKnownSampleStencilsUsingTemplateEngine(List<String> allFiles) {
        try {
            // auto-deploy-samples が有効な場合のみ実行
            if (!StorageConfig.isAutoDeploySamples()) {
                System.out.println("Auto-deploy samples is disabled, skipping sample collection");
                return;
            }
            
            System.out.println("Collecting sample stencils using direct classpath resource loading...");
            
            // 既知のサンプルステンシルリスト
            String[] knownSampleStencils = {"/samples/hello-world"};
            
            for (String stencilCanonicalName : knownSampleStencils) {
                try {
                    // classpathから直接stencil-settings.ymlを読み込み
                    String resourcePath = "/stencil-samples" + stencilCanonicalName + "/stencil-settings.yml";
                    
                    try (InputStream inputStream = getClass().getResourceAsStream(resourcePath)) {
                        if (inputStream != null) {
                            // YAMLファイルを読み込んでStencilSettingsYmlオブジェクトに変換
                            StencilSettingsYml settings = loadStencilSettingsFromStream(inputStream);
                            
                            if (settings != null && settings.getStencil() != null && settings.getStencil().getConfig() != null) {
                                // 設定が見つかった場合、一時ファイルとして保存
                                File tempFile = createTempStencilSettingsFile(stencilCanonicalName, settings);
                                allFiles.add(tempFile.getAbsolutePath());
                                System.out.println("Sample stencil collected: " + stencilCanonicalName + " -> " + tempFile.getAbsolutePath());
                            }
                        } else {
                            System.out.println("Sample stencil resource not found: " + resourcePath);
                        }
                    }
                } catch (Exception stencilException) {
                    System.out.println("Could not collect sample stencil " + stencilCanonicalName + ": " + stencilException.getMessage());
                    // 個別のステンシルが見つからない場合は継続
                }
            }
        } catch (Exception e) {
            System.out.println("Error collecting sample stencils: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * StencilSettingsYmlオブジェクトを一時YAMLファイルとして作成
     */
    private File createTempStencilSettingsFile(String stencilCanonicalName, StencilSettingsYml settings) throws Exception {
        File tempFile = File.createTempFile(
            "stencil-settings-" + stencilCanonicalName.replace("/", "-"), 
            ".yml"
        );
        tempFile.deleteOnExit();
        
        // 簡易的なYAML形式で書き出し（既存のStencilSettingsYml構造を参考）
        try (java.io.FileWriter writer = new java.io.FileWriter(tempFile)) {
            var config = settings.getStencil().getConfig();
            writer.write("stencil:\n");
            writer.write("  config:\n");
            writer.write("    categoryId: " + (config.getCategoryId() != null ? config.getCategoryId() : "") + "\n");
            writer.write("    categoryName: " + (config.getCategoryName() != null ? config.getCategoryName() : "") + "\n");
            writer.write("    id: " + (config.getId() != null ? config.getId() : "") + "\n");
            writer.write("    name: " + (config.getName() != null ? config.getName() : "") + "\n");
            writer.write("    serial: " + (config.getSerial() != null ? config.getSerial() : "") + "\n");
            writer.write("    lastUpdate: " + (config.getLastUpdate() != null ? config.getLastUpdate() : "") + "\n");
            writer.write("    lastUpdateUser: " + (config.getLastUpdateUser() != null ? config.getLastUpdateUser() : "") + "\n");
            writer.write("    description: |\n");
            writer.write("      " + (config.getDescription() != null ? config.getDescription().replace("\n", "\n      ") : "") + "\n");
        }
        
        return tempFile;
    }

    /**
     * 従来のファイル管理処理（後方互換性）
     */
    private void readFileManagementLegacy() {
        String dir = StorageUtil.getBaseDir() + TemplateEngineProcessor.getStencilMasterStorageDir();
        String fileDir = dir + "/_filemanagement";
        File fileDirFile = new File(fileDir);
        
        if (!fileDirFile.exists()) {
            return;
        }
        
        File[] filemanagementFiles = fileDirFile.listFiles();
        if (filemanagementFiles == null) {
            return;
        }
        
        for (File file : filemanagementFiles) {
            File[] filesInUuid = file.listFiles();

            if (filesInUuid == null || filesInUuid.length == 0) {
                continue;
            }

            // create entity.
            FileManagement fileManagement = new FileManagement();
            fileManagement.fileId = file.getName();
            fileManagement.fileName = filesInUuid[0].getName();
            fileManagement.filePath = filesInUuid[0].getAbsolutePath();
            fileManagement.expireDate = DateUtils.addDays(new Date(), 3650);
            fileManagementRepository.save(fileManagement);
        }
    }

    /**
     * read Stencil settings from InputStream.
     * 
     * @param stream
     *            InputStream of Stencil settings (Yaml)
     * @return {@link StencilSettingsYml ステンシル定義YAML}
     */
    protected StencilSettingsYml loadStencilSettingsFromStream(InputStream stream) {
        StencilSettingsYml settings = null;
        try {
            // InputStreamを一時ファイルに書き出し、readYamlメソッドを使用（既存の動作実績パターン）
            File tempYamlFile = File.createTempFile("temp-stencil-settings-", ".yml");
            tempYamlFile.deleteOnExit();
            
            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempYamlFile)) {
                stream.transferTo(fos);
            }
            
            // 既存の動作しているreadYamlメソッドを使用
            settings = readYaml(tempYamlFile);
            
            System.out.println("Successfully loaded stencil settings from stream using readYaml method");
        } catch (final ConstructorException e) {
            System.out.println("ConstructorException details: " + e.getMessage());
            e.printStackTrace();
            String msg = "classpathリソースのyaml読込でエラーが発生しました: " + e.getMessage();
            throw new MirelApplicationException(msg, e);
        } catch (final Exception e) {
            System.out.println("General exception during YAML loading: " + e.getMessage());
            e.printStackTrace();
            throw new MirelSystemException("yamlの読込で予期しないエラーが発生しました: " + e.getMessage(), e);
        }
        return settings;
    }

    /**
     * read Stencil settings file.
     * 
     * @param file
     *            Setting file (Yaml)
     * @return {@link StencilSettingsYml ステンシル定義YAML}
     */
    protected StencilSettingsYml readYaml(File file) {
        StencilSettingsYml settings = null;
        try (InputStream stream = new FileSystemResource(file).getInputStream()) {
            settings = new Yaml().loadAs(stream, StencilSettingsYml.class);
        } catch (final ConstructorException e) {
            e.printStackTrace();
            String msg = "yamlの読込でエラーが発生しました。";
            throw new MirelApplicationException(msg, e);
        } catch (final IOException e) {
            e.printStackTrace();
            throw new MirelSystemException("yamlの読込で入出力エラーが発生しました。", e);
        }
        return settings;
    }
}
