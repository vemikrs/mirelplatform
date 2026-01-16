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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.ConstructorException;

import jp.vemi.framework.exeption.MirelApplicationException;
import jp.vemi.framework.exeption.MirelSystemException;
import jp.vemi.framework.storage.StorageService;
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

    private static final Logger logger = LoggerFactory.getLogger(ReloadStencilMasterServiceImp.class);

    /** {@link MsteStencilRepository ステンシルマスタ} */
    @Autowired
    protected MsteStencilRepository stencilRepository;

    @Autowired
    protected FileManagementRepository fileManagementRepository;

    /** Spring標準のリソース検索機能 */
    @Autowired
    protected ResourcePatternResolver resourcePatternResolver;

    @Autowired
    protected StorageService storageService;

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
            // stencilRepository.deleteAll();
            // FileManagementテーブルもクリア（重複エラー回避）
            // fileManagementRepository.deleteAll();

            // レイヤード検索でstencil-settings.ymlを収集
            Map<String, String> categories = Maps.newLinkedHashMap();
            List<String> allFiles = collectStencilSettingsFromLayers();

            logger.info("Found {} stencil-settings.yml files", allFiles.size());

            // save stencil record.
            for (String fileName : allFiles) {
                try {
                    StencilSettingsYml settings = readYamlFromPath(fileName);
                    if (settings == null || settings.getStencil() == null
                            || settings.getStencil().getConfig() == null) {
                        logger.warn("Invalid stencil settings in {}", fileName);
                        continue;
                    }

                    Config config = settings.getStencil().getConfig();

                    MsteStencil entry = new MsteStencil();
                    entry.setStencilCd(config.getId());
                    entry.setStencilName(config.getName());
                    entry.setItemKind("1");
                    entry.setSort(0);
                    saveStencilSafely(entry);
                    logger.info("{}/{}: {}", config.getId(), config.getSerial(), config.getName());

                    if (false == StringUtils.isEmpty(config.getCategoryId())) {
                        if (false == categories.containsKey(config.getCategoryId())) {
                            if (StringUtils.isEmpty(categories.get(config.getCategoryId()))) {
                                categories.put(config.getCategoryId(), config.getCategoryName());
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.error("Error processing stencil settings file: {} - {}", fileName, e.getMessage(), e);

                }
            }

            // save stencil record.
            for (Entry<String, String> catentry : categories.entrySet()) {
                MsteStencil entry = new MsteStencil();
                entry.setStencilCd(catentry.getKey());
                entry.setStencilName(catentry.getValue());
                entry.setItemKind("0");
                entry.setSort(0);
                saveStencilSafely(entry);
            }

            // 従来のファイル管理処理（後方互換性）
            readFileManagementLegacy();

        } catch (Exception e) {
            logger.error("Error in read() method: {}", e.getMessage(), e);

            throw e;
        }
    }

    /**
     * レイヤード検索でstencil-settings.ymlファイルを収集する
     * StorageConfigの新しいレイヤー構造を使用してステンシル設定を収集
     */
    private List<String> collectStencilSettingsFromLayers() {
        List<String> allFiles = new ArrayList<>();

        try {
            logger.info("=== Layered Stencil Collection Using StorageConfig ===");

            // StorageConfigから新しいレイヤーディレクトリを取得
            String[] layerDirs = {
                    StorageConfig.getUserStencilDir(),
                    StorageConfig.getStandardStencilDir()
            };

            for (String layerDir : layerDirs) {
                logger.debug("Searching layer directory: {}", layerDir);
                if (storageService.exists(layerDir) || new File(layerDir).exists()) {
                    List<String> layerFiles = FileUtil.findByFileName(layerDir, "stencil-settings.yml");
                    allFiles.addAll(layerFiles);
                    logger.info("Found {} stencil files in layer: {}", layerFiles.size(), layerDir);
                } else {
                    logger.debug("Layer directory does not exist: {}", layerDir);
                }
            }

            // TemplateEngineProcessorを使って既知のサンプルステンシルを検索
            collectKnownSampleStencilsUsingTemplateEngine(allFiles);

        } catch (Exception e) {
            logger.error("Error in collectStencilSettingsFromLayers: {}", e.getMessage(), e);

            // フォールバック: 最低限の既存ディレクトリ検索
            try {
                String dir = StorageUtil.getBaseDir() + TemplateEngineProcessor.getStencilMasterStorageDir();
                List<String> legacyFiles = FileUtil.findByFileName(dir, "stencil-settings.yml");
                if (!allFiles.containsAll(legacyFiles)) {
                    allFiles.addAll(legacyFiles);
                }
            } catch (Exception fallbackException) {
                logger.debug("Fallback search also failed: {}", fallbackException.getMessage());
            }
        }

        logger.info("Total files collected: {}", allFiles.size());
        return allFiles;
    }

    /**
     * サンプルステンシルをclasspathから動的に収集
     * Spring標準のResourcePatternResolverを使用してclasspathリソースを検索
     */
    private void collectKnownSampleStencilsUsingTemplateEngine(List<String> allFiles) {
        try {
            // auto-deploy-samples が有効な場合のみ実行
            if (!StorageConfig.isAutoDeploySamples()) {
                logger.info("Auto-deploy samples is disabled, skipping sample collection");
                return;
            }

            logger.info("Collecting sample stencils using ResourcePatternResolver...");

            // サンプルステンシルディレクトリを取得
            String samplesDir = StorageConfig.getSamplesStencilDir();

            if (samplesDir.startsWith("classpath:")) {
                // classpathリソース検索パターンを構築（再帰的に全てのstencil-settings.ymlを検索）
                String searchPattern = samplesDir + "/**/stencil-settings.yml";
                logger.debug("Searching for resources with pattern: {}", searchPattern);

                // ResourcePatternResolverでclasspathリソースを検索
                Resource[] resources = resourcePatternResolver.getResources(searchPattern);
                logger.info("Found {} sample stencil-settings.yml resources", resources.length);

                // 見つかったリソースを処理
                for (Resource resource : resources) {
                    try {
                        // Resourceから直接StencilSettingsYmlを読み込み
                        StencilSettingsYml settings = loadStencilSettingsFromResource(resource);

                        if (settings != null && settings.getStencil() != null
                                && settings.getStencil().getConfig() != null) {
                            // classpathリソースを物理ファイルとしてsamplesレイヤーに展開
                            String stencilId = settings.getStencil().getConfig().getId();
                            String serialNo = settings.getStencil().getConfig().getSerial();

                            // samplesレイヤーディレクトリに物理ファイルとして保存
                            File samplesLayerFile = createPhysicalSampleStencilFile(stencilId, serialNo, settings);
                            if (samplesLayerFile != null) {
                                allFiles.add(samplesLayerFile.getAbsolutePath());
                                logger.debug("Sample stencil expanded to physical file: {} -> {}", resource.getURI(),
                                        samplesLayerFile.getAbsolutePath());
                            }
                        }
                    } catch (Exception resourceException) {
                        logger.debug("Could not process sample stencil resource {}: {}", resource.getURI(),
                                resourceException.getMessage());
                    }
                }
            } else {
                // ファイルシステムの場合は従来通りFileUtil.findByFileNameを使用
                logger.debug("Using filesystem search for samples directory: {}", samplesDir);
                List<String> foundFiles = FileUtil.findByFileName(samplesDir, "stencil-settings.yml");
                allFiles.addAll(foundFiles);
                logger.info("Found {} filesystem sample stencil files", foundFiles.size());
            }

        } catch (Exception e) {
            logger.error("Error collecting sample stencils: {}", e.getMessage(), e);

        }
    }

    /**
     * ResourceからStencilSettingsYmlを読み込む
     * 
     * @param resource
     *            Spring Resource
     * @return StencilSettingsYml、または null
     */
    private StencilSettingsYml loadStencilSettingsFromResource(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            StencilSettingsYml settings = new Yaml().loadAs(inputStream, StencilSettingsYml.class);

            // NOTE: 親設定のマージはDB保存時には不要。
            // 実行時にTemplateEngineProcessor.getStencilSettings()が統一的にマージします。

            return settings;
        } catch (Exception e) {
            logger.debug("Error loading stencil settings from resource {}: {}", resource.getDescription(),
                    e.getMessage());
            return null;
        }
    }

    /**
     * StencilSettingsYmlオブジェクトを物理YAMLファイルとしてsamplesレイヤーに作成
     * TemplateEngineProcessorが見つけられるようにファイルシステムに展開
     */
    private File createPhysicalSampleStencilFile(String stencilId, String serialNo, StencilSettingsYml settings) {
        try {
            // samplesレイヤーディレクトリのパスを構築
            String samplesDir = StorageConfig.getSamplesStencilDir();
            if (samplesDir.startsWith("classpath:")) {
                // classpathの場合はファイルシステムのsamplesディレクトリを使用
                samplesDir = StorageConfig.getStandardStencilDir().replace("/standard", "/samples");
            }

            // ステンシル固有のディレクトリ作成: /samples/hello-world/250913A/
            File stencilDir = new File(samplesDir + stencilId + "/" + serialNo);
            if (!stencilDir.exists()) {
                stencilDir.mkdirs();
            }

            File stencilFile = new File(stencilDir, "stencil-settings.yml");

            // StencilSettingsYmlオブジェクトをYAMLファイルとして書き込み
            try (java.io.FileWriter writer = new java.io.FileWriter(stencilFile)) {
                var config = settings.getStencil().getConfig();
                writer.write("stencil:\n");
                writer.write("  config:\n");
                writer.write(
                        "    categoryId: \"" + (config.getCategoryId() != null ? config.getCategoryId() : "") + "\"\n");
                writer.write("    categoryName: \"" + (config.getCategoryName() != null ? config.getCategoryName() : "")
                        + "\"\n");
                writer.write("    id: \"" + (config.getId() != null ? config.getId() : "") + "\"\n");
                writer.write("    name: \"" + (config.getName() != null ? config.getName() : "") + "\"\n");
                writer.write("    serial: \"" + (config.getSerial() != null ? config.getSerial() : "") + "\"\n");
                writer.write(
                        "    lastUpdate: \"" + (config.getLastUpdate() != null ? config.getLastUpdate() : "") + "\"\n");
                writer.write("    lastUpdateUser: \""
                        + (config.getLastUpdateUser() != null ? config.getLastUpdateUser() : "") + "\"\n");
                writer.write("    description: |\n");
                writer.write("      "
                        + (config.getDescription() != null ? config.getDescription().replace("\n", "\n      ") : "")
                        + "\n");

                // dataElementとdataDomainも含める
                if (settings.getStencil().getDataElement() != null
                        && !settings.getStencil().getDataElement().isEmpty()) {
                    writer.write("  dataElement:\n");
                    for (var element : settings.getStencil().getDataElement()) {
                        writer.write("    - id: \"" + element.get("id") + "\"\n");
                    }
                }

                if (settings.getStencil().getDataDomain() != null && !settings.getStencil().getDataDomain().isEmpty()) {
                    writer.write("  dataDomain:\n");
                    for (var domain : settings.getStencil().getDataDomain()) {
                        writer.write("    - id: \"" + domain.get("id") + "\"\n");
                        if (domain.get("name") != null)
                            writer.write("      name: \"" + domain.get("name") + "\"\n");
                        if (domain.get("value") != null)
                            writer.write("      value: \"" + domain.get("value") + "\"\n");
                        if (domain.get("type") != null)
                            writer.write("      type: \"" + domain.get("type") + "\"\n");
                        if (domain.get("placeholder") != null)
                            writer.write("      placeholder: \"" + domain.get("placeholder") + "\"\n");
                        if (domain.get("note") != null)
                            writer.write("      note: \"" + domain.get("note") + "\"\n");
                    }
                }

                // codeInfoも含める
                if (settings.getStencil().getCodeInfo() != null) {
                    var codeInfo = settings.getStencil().getCodeInfo();
                    writer.write("  codeInfo:\n");
                    writer.write("    copyright: \"" + (codeInfo.getCopyright() != null ? codeInfo.getCopyright() : "")
                            + "\"\n");
                    writer.write("    versionNo: \"" + (codeInfo.getVersionNo() != null ? codeInfo.getVersionNo() : "")
                            + "\"\n");
                    writer.write(
                            "    author: \"" + (codeInfo.getAuthor() != null ? codeInfo.getAuthor() : "") + "\"\n");
                    writer.write(
                            "    vendor: \"" + (codeInfo.getVendor() != null ? codeInfo.getVendor() : "") + "\"\n");
                }
            }

            return stencilFile;

        } catch (Exception e) {
            logger.error("Error creating physical sample stencil file: {}", e.getMessage(), e);

            return null;
        }
    }

    /**
     * 楽観ロック競合を回避するため新しいトランザクションでステンシル保存
     * 並列実行時の重複save操作を安全に処理
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void saveStencilSafely(MsteStencil entry) {
        try {
            // 重複チェック: 既存エンティティの確認 -> Update (Upsert)
            // if (stencilRepository.existsById(entry.getStencilCd())) {
            // logger.debug("Stencil already exists, updating: {}", entry.getStencilCd());
            // }
            stencilRepository.save(entry);
        } catch (Exception e) {
            logger.warn("Error saving stencil safely: {} - {}", entry.getStencilCd(), e.getMessage());
            // 並列実行での重複保存は無視
        }
    }

    /**
     * 従来のファイル管理処理（後方互換性）
     * 新しいトランザクションで実行して楽観ロック競合を回避
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void readFileManagementLegacy() {
        String dir = StorageUtil.getBaseDir() + TemplateEngineProcessor.getStencilMasterStorageDir();
        String fileDir = dir + "/_filemanagement";

        // StorageService経由 or ローカルで存在確認
        if (!storageService.exists(fileDir) && !new File(fileDir).exists()) {
            return;
        }

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

            String fileId = file.getName();

            // 重複チェック: 既存のFileManagementレコードがあるかチェック
            // if (fileManagementRepository.existsById(fileId)) {
            // logger.debug("FileManagement already exists, updating: {}", fileId);
            // }

            // create entity.
            FileManagement fileManagement = new FileManagement();
            fileManagement.fileId = fileId;
            fileManagement.fileName = filesInUuid[0].getName();
            fileManagement.filePath = filesInUuid[0].getAbsolutePath();
            // 既存データがある場合はexpireDateを維持したいが、ここでは簡易的に上書きまたは新規設定
            // 厳密には findById -> update の方が良いが、レガシー互換機能のため save で上書きする
            fileManagement.expireDate = DateUtils.addDays(new Date(), 3650);

            // @Versionフィールドは自動管理に委ねる（nullのまま保存）

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
            // セキュリティ強化: Files.createTempFile を使用して適切なパーミッション設定
            java.nio.file.Path tempPath;
            try {
                // POSIX対応環境では厳格なパーミッションを設定
                tempPath = java.nio.file.Files.createTempFile(
                        "temp-stencil-settings-", ".yml",
                        java.nio.file.attribute.PosixFilePermissions.asFileAttribute(
                                java.nio.file.attribute.PosixFilePermissions.fromString("rw-------")));
            } catch (UnsupportedOperationException ex) {
                // Windows等のPOSIX非対応環境ではパーミッション指定なしで作成
                logger.debug("POSIX file permissions not supported, falling back to default permissions", ex);
                tempPath = java.nio.file.Files.createTempFile("temp-stencil-settings-", ".yml");
            }
            File tempYamlFile = tempPath.toFile();
            tempYamlFile.deleteOnExit();

            try (java.io.FileOutputStream fos = new java.io.FileOutputStream(tempYamlFile)) {
                stream.transferTo(fos);
            }

            // 既存の動作しているreadYamlメソッドを使用
            settings = readYaml(tempYamlFile);

            logger.info("Successfully loaded stencil settings from stream using readYaml method");
        } catch (final ConstructorException e) {
            logger.error("ConstructorException details: {}", e.getMessage(), e);

            String msg = "classpathリソースのyaml読込でエラーが発生しました: " + e.getMessage();
            throw new MirelApplicationException(msg, e);
        } catch (final Exception e) {
            logger.error("General exception during YAML loading: {}", e.getMessage(), e);

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
        return readYamlFromPath(file.getAbsolutePath());
    }

    /**
     * read Stencil settings file from path (StorageService compatible).
     * 
     * @param filePath
     *            Setting file path
     * @return {@link StencilSettingsYml ステンシル定義YAML}
     */
    protected StencilSettingsYml readYamlFromPath(String filePath) {
        StencilSettingsYml settings = null;
        try {
            InputStream stream;
            // StorageService経由 or ローカルファイル
            if (storageService.exists(filePath)) {
                stream = storageService.getInputStream(filePath);
            } else {
                stream = new FileSystemResource(filePath).getInputStream();
            }
            try (stream) {
                settings = new Yaml().loadAs(stream, StencilSettingsYml.class);
            }

            // NOTE: 親設定のマージはDB保存時には不要。
            // 実行時にTemplateEngineProcessor.getStencilSettings()が統一的にマージします。

        } catch (final ConstructorException e) {
            String msg = "yamlの読込でエラーが発生しました。";
            throw new MirelApplicationException(msg, e);
        } catch (final IOException e) {
            throw new MirelSystemException("yamlの読込で入出力エラーが発生しました。", e);
        }
        return settings;
    }
}
