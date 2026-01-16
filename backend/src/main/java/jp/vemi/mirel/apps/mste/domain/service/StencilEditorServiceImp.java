/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.mste.domain.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import jp.vemi.framework.util.SanitizeUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;

import jp.vemi.framework.config.StorageConfig;
import jp.vemi.framework.storage.StorageService;
import jp.vemi.framework.util.FileUtil;
import jp.vemi.mirel.apps.mste.domain.dao.entity.MsteStencil;
import jp.vemi.mirel.apps.mste.domain.dao.repository.MsteStencilRepository;
import jp.vemi.mirel.apps.mste.domain.dto.LoadStencilParameter;
import jp.vemi.mirel.apps.mste.domain.dto.LoadStencilResult;
import jp.vemi.mirel.apps.mste.domain.dto.SaveStencilParameter;
import jp.vemi.mirel.apps.mste.domain.dto.SaveStencilResult;
import jp.vemi.mirel.apps.mste.domain.dto.StencilConfigDto;
import jp.vemi.mirel.apps.mste.domain.dto.StencilFileDto;
import jp.vemi.mirel.apps.mste.domain.dto.StencilVersionDto;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;
import jp.vemi.ste.domain.dto.yml.StencilSettingsYml;
import jp.vemi.ste.domain.dto.yml.StencilSettingsYml.Stencil.Config;

/**
 * {@link StencilEditorService} の実装
 */
@Service
@Transactional
public class StencilEditorServiceImp implements StencilEditorService {

    private static final Logger logger = LoggerFactory.getLogger(StencilEditorServiceImp.class);

    @Autowired
    private MsteStencilRepository stencilRepository;

    @Autowired
    private StorageService storageService;

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiResponse<LoadStencilResult> loadStencil(ApiRequest<LoadStencilParameter> parameter) {
        ApiResponse<LoadStencilResult> response = ApiResponse.<LoadStencilResult>builder().build();

        try {
            LoadStencilParameter param = parameter.getModel();
            String stencilId = param.getStencilId();
            String serial = param.getSerial();

            // レイヤー検索: user → standard → samples
            // フォルダ名を正とする（ディレクトリ名完全一致のみ）
            // レイヤー検索: user → standard → samples
            // フォルダ名を正とする（ディレクトリ名完全一致のみ）
            String[] layers = {
                    StorageConfig.getUserStencilDir(),
                    StorageConfig.getStandardStencilDir()
            };

            Path stencilPath = null;
            for (String layer : layers) {
                try {
                    Path baseLayerPath = Paths.get(layer).toAbsolutePath().normalize();
                    Path candidatePath = baseLayerPath.resolve(stencilId).resolve(serial).toAbsolutePath().normalize();

                    // Path injection対策: ベースパス配下にあるかチェック
                    if (!candidatePath.startsWith(baseLayerPath)) {
                        logger.warn("Invalid path traversal attempt skipped: layer={}, stencilId={}, serial={}", layer,
                                SanitizeUtil.forLog(stencilId), SanitizeUtil.forLog(serial));
                        continue;
                    }

                    if (Files.exists(candidatePath)) {
                        stencilPath = candidatePath;
                        break;
                    }
                } catch (Exception e) {
                    logger.warn("Path resolution failed", e);
                }
            }

            if (stencilPath == null) {
                response.addError("ステンシルが見つかりません: " + stencilId + "/" + serial);
                return response;
            }

            // stencil-settings.yml読み込み
            Path settingsPath = stencilPath.resolve("stencil-settings.yml").normalize();
            if (!settingsPath.startsWith(stencilPath)) {
                response.addError("不正なファイルパス");
                return response;
            }

            // StorageService経由またはローカルでファイル存在確認
            boolean fileExists = storageService.exists(settingsPath.toString()) || Files.exists(settingsPath);
            if (!fileExists) {
                response.addError("stencil-settings.ymlが見つかりません");
                return response;
            }

            String yamlContent;
            if (storageService.exists(settingsPath.toString())) {
                yamlContent = storageService.readString(settingsPath.toString());
            } else {
                yamlContent = Files.readString(settingsPath);
            }
            // フォルダ名を正として、YAML内serialを上書き
            StencilConfigDto config = parseStencilConfig(yamlContent, stencilId, serial);
            config.setSerial(serial); // ディレクトリ名で上書き

            // ファイル一覧読み込み
            List<StencilFileDto> files = loadFiles(stencilPath);

            // バージョン履歴取得
            List<StencilVersionDto> versions = loadVersionHistory(stencilId);

            LoadStencilResult result = LoadStencilResult.builder()
                    .config(config)
                    .files(files)
                    .versions(versions)
                    .build();

            response.setData(result);

        } catch (Exception e) {
            logger.error("ステンシル読込エラー", e);
            response.addError("ステンシル読込エラー: " + e.getMessage());
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiResponse<SaveStencilResult> saveStencil(ApiRequest<SaveStencilParameter> parameter) {
        ApiResponse<SaveStencilResult> response = ApiResponse.<SaveStencilResult>builder().build();

        try {
            SaveStencilParameter param = parameter.getModel();

            // 新しいシリアル番号を生成 (YYMMDDx形式)
            String newSerial = generateNewSerial(param.getStencilId());

            // userレイヤーに保存
            String userDir = StorageConfig.getUserStencilDir();
            Path baseUserPath = Paths.get(userDir).toAbsolutePath().normalize();
            Path stencilPath = baseUserPath.resolve(param.getStencilId()).resolve(newSerial).toAbsolutePath()
                    .normalize();

            // Path injection対策
            if (!stencilPath.startsWith(baseUserPath)) {
                throw new SecurityException("Invalid path traversal detected: " + stencilPath);
            }

            Files.createDirectories(stencilPath);

            // ファイル保存
            for (StencilFileDto file : param.getFiles()) {
                Path filePath = stencilPath.resolve(file.getPath()).toAbsolutePath().normalize();

                // Path injection対策: stencilPath配下にあるかチェック
                if (!filePath.startsWith(stencilPath)) {
                    throw new SecurityException("Invalid file path detected: " + file.getPath());
                }

                Files.createDirectories(filePath.getParent());
                // StorageService経由でもローカルでも保存
                try {
                    storageService.writeString(filePath.toString(), file.getContent());
                } catch (Exception e) {
                    logger.warn(
                            "Failed to write stencil file via StorageService. Fallback to local file write. path={}",
                            SanitizeUtil.forLog(filePath.toString()), e);
                    // フォールバック: ローカルファイル書き込み
                    try {
                        Files.writeString(filePath, file.getContent());
                    } catch (IOException ioException) {
                        logger.error(
                                "Failed to write stencil file via both StorageService and local file system. path={}",
                                SanitizeUtil.forLog(filePath.toString()), ioException);
                        throw ioException;
                    }
                }
            }

            // DB更新
            updateDatabase(param.getStencilId(), newSerial, param.getConfig());

            SaveStencilResult result = SaveStencilResult.builder()
                    .newSerial(newSerial)
                    .success(true)
                    .build();

            response.setData(result);
            response.addMessage("保存しました: " + newSerial);

        } catch (Exception e) {
            logger.error("ステンシル保存エラー", e);
            response.addError("保存エラー: " + e.getMessage());
        }

        return response;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiResponse<List<StencilVersionDto>> getVersionHistory(String stencilId) {
        ApiResponse<List<StencilVersionDto>> response = ApiResponse.<List<StencilVersionDto>>builder().build();

        try {
            List<StencilVersionDto> versions = loadVersionHistory(stencilId);
            response.setData(versions);

        } catch (Exception e) {
            logger.error("バージョン履歴取得エラー", e);
            response.addError("バージョン履歴取得エラー: " + e.getMessage());
        }

        return response;
    }

    /**
     * YAMLからStencilConfigDtoをパース
     */
    private StencilConfigDto parseStencilConfig(String yamlContent, String stencilId, String serial) {
        Yaml yaml = new Yaml();
        @SuppressWarnings("unchecked")
        var data = (java.util.Map<String, Object>) yaml.load(yamlContent);
        @SuppressWarnings("unchecked")
        var stencilData = (java.util.Map<String, Object>) data.get("stencil");
        @SuppressWarnings("unchecked")
        var configData = (java.util.Map<String, Object>) stencilData.get("config");

        return StencilConfigDto.builder()
                .id(stencilId)
                .name((String) configData.get("name"))
                .categoryId((String) configData.get("categoryId"))
                .categoryName((String) configData.get("categoryName"))
                .serial(serial)
                .lastUpdate((String) configData.get("lastUpdate"))
                .lastUpdateUser((String) configData.get("lastUpdateUser"))
                .description((String) configData.get("description"))
                .build();
    }

    /**
     * ディレクトリ内のファイル一覧を読み込む
     */
    private List<StencilFileDto> loadFiles(Path stencilPath) throws IOException {
        List<StencilFileDto> files = new ArrayList<>();

        Files.walk(stencilPath)
                .filter(Files::isRegularFile)
                .forEach(path -> {
                    try {
                        String relativePath = stencilPath.relativize(path).toString();
                        String content;
                        // StorageService経由 or ローカル
                        if (storageService.exists(path.toString())) {
                            content = storageService.readString(path.toString());
                        } else {
                            content = Files.readString(path);
                        }
                        String fileName = path.getFileName().toString();

                        StencilFileDto file = StencilFileDto.builder()
                                .path(relativePath)
                                .name(fileName)
                                .content(content)
                                .type(classifyFileType(fileName))
                                .language(getLanguageMode(fileName))
                                .isEditable(isEditable(fileName))
                                .build();

                        files.add(file);
                    } catch (IOException e) {
                        logger.error("ファイル読込エラー: " + path, e);
                    }
                });

        return files;
    }

    /**
     * バージョン履歴を読み込む
     * 
     * ディレクトリ名とYAML内serialの両方をチェックし、
     * YAML内serialが存在する場合はそれを優先して使用
     */
    private List<StencilVersionDto> loadVersionHistory(String stencilId) {
        List<StencilVersionDto> versions = new ArrayList<>();
        Yaml yaml = new Yaml();

        // userレイヤーから履歴を取得
        // userレイヤーから履歴を取得
        String userDir = StorageConfig.getUserStencilDir();
        Path baseUserPath = Paths.get(userDir).toAbsolutePath().normalize();
        Path categoryPath = baseUserPath.resolve(stencilId).toAbsolutePath().normalize();

        // Path injection対策
        if (!categoryPath.startsWith(baseUserPath)) {
            logger.warn("Invalid path traversal attempt in loadVersionHistory: stencilId={}",
                    SanitizeUtil.forLog(stencilId));
            return new ArrayList<>();
        }

        if (Files.exists(categoryPath)) {
            try {
                Files.list(categoryPath)
                        .filter(Files::isDirectory)
                        .forEach(path -> {
                            String dirName = path.getFileName().toString();
                            String displaySerial = dirName; // デフォルトはディレクトリ名

                            // YAML内serialを読み込んで優先使用
                            Path settingsPath = path.resolve("stencil-settings.yml");
                            if (Files.exists(settingsPath)) {
                                try {
                                    String yamlContent;
                                    if (storageService.exists(settingsPath.toString())) {
                                        yamlContent = storageService.readString(settingsPath.toString());
                                    } else {
                                        yamlContent = Files.readString(settingsPath);
                                    }
                                    @SuppressWarnings("unchecked")
                                    var data = (java.util.Map<String, Object>) yaml.load(yamlContent);
                                    @SuppressWarnings("unchecked")
                                    var stencilData = (java.util.Map<String, Object>) data.get("stencil");
                                    @SuppressWarnings("unchecked")
                                    var configData = (java.util.Map<String, Object>) stencilData.get("config");

                                    String yamlSerial = (String) configData.get("serial");
                                    if (yamlSerial != null && !yamlSerial.isEmpty()) {
                                        displaySerial = yamlSerial;
                                        logger.debug("Version history: dir={}, yaml serial={}", dirName, yamlSerial);
                                    }
                                } catch (Exception e) {
                                    logger.warn("Failed to read serial from YAML: {}", settingsPath, e);
                                }
                            }

                            versions.add(StencilVersionDto.builder()
                                    .serial(displaySerial)
                                    .createdAt(getCreatedDate(path))
                                    .createdBy("system")
                                    .isActive(true)
                                    .build());
                        });
            } catch (IOException e) {
                logger.error("バージョン履歴読込エラー", e);
            }
        }

        return versions.stream()
                .sorted((a, b) -> b.getSerial().compareTo(a.getSerial()))
                .collect(Collectors.toList());
    }

    /**
     * 新しいシリアル番号を生成 (YYMMDDx形式)
     */
    private String generateNewSerial(String stencilId) {
        String datePrefix = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMdd"));

        List<StencilVersionDto> versions = loadVersionHistory(stencilId);

        // 同日のバージョンをカウント
        long count = versions.stream()
                .filter(v -> v.getSerial().startsWith(datePrefix))
                .count();

        char suffix = (char) ('A' + count);
        return datePrefix + suffix;
    }

    /**
     * データベースを更新
     */
    private void updateDatabase(String stencilId, String serial, StencilConfigDto config) {
        MsteStencil entity = new MsteStencil();
        entity.setStencilCd(stencilId + "/" + serial);
        entity.setStencilName(config.getName());
        entity.setItemKind("1"); // 1: ステンシル
        entity.setSort(0);

        stencilRepository.save(entity);
    }

    /**
     * ファイル種別を判定
     */
    private String classifyFileType(String fileName) {
        if ("stencil-settings.yml".equals(fileName)) {
            return "stencil-settings";
        } else if (fileName.endsWith("_stencil-settings.yml")) {
            return "category-settings";
        } else if (".gitkeep".equals(fileName)) {
            return "gitkeep";
        } else if (fileName.endsWith(".ftl")) {
            return "template";
        } else {
            return "other";
        }
    }

    /**
     * 言語モードを取得
     */
    private String getLanguageMode(String fileName) {
        String ext = fileName.substring(fileName.lastIndexOf('.') + 1).toLowerCase();

        return switch (ext) {
            case "yml", "yaml" -> "yaml";
            case "ftl" -> "freemarker";
            case "java" -> "java";
            case "js" -> "javascript";
            case "ts" -> "typescript";
            case "json" -> "json";
            default -> "text";
        };
    }

    /**
     * 編集可能か判定
     */
    private boolean isEditable(String fileName) {
        return !".gitkeep".equals(fileName);
    }

    /**
     * 作成日時を取得
     */
    private String getCreatedDate(Path path) {
        try {
            return Files.getLastModifiedTime(path).toString();
        } catch (IOException e) {
            return LocalDateTime.now().toString();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiResponse<Map<String, Object>> listStencils(String categoryId) {
        ApiResponse<Map<String, Object>> response = ApiResponse.<Map<String, Object>>builder().build();

        try {
            // ストレージレイヤーから全ステンシル設定ファイルを収集
            List<String> stencilFiles = collectStencilSettingsFromLayers();

            // カテゴリマップ（categoryId -> categoryName）
            Map<String, String> categoryMap = new LinkedHashMap<>();
            // ステンシルマップ（stencilId -> List<Config>）
            Map<String, List<Config>> stencilMap = new LinkedHashMap<>();

            // YAMLファイルを解析
            Yaml yaml = new Yaml();
            for (String filePath : stencilFiles) {
                try {
                    StencilSettingsYml settings = loadStencilSettings(filePath, yaml);
                    if (settings == null || settings.getStencil() == null
                            || settings.getStencil().getConfig() == null) {
                        continue;
                    }

                    Config config = settings.getStencil().getConfig();

                    // フォルダ名を正として、ディレクトリ構造から抽出して上書き
                    String serialFromPath = extractSerialFromPath(filePath);
                    String stencilIdFromPath = extractStencilIdFromPath(filePath);

                    if (serialFromPath != null) {
                        logger.debug("Extracted serial from path: {} -> {}", SanitizeUtil.forLog(filePath),
                                SanitizeUtil.forLog(serialFromPath));
                        config.setSerial(serialFromPath);
                    } else {
                        logger.warn("Failed to extract serial from path: {}", SanitizeUtil.forLog(filePath));
                    }

                    if (stencilIdFromPath != null) {
                        logger.debug("Extracted stencilId from path: {} -> {}", SanitizeUtil.forLog(filePath),
                                SanitizeUtil.forLog(stencilIdFromPath));
                        config.setId(stencilIdFromPath);
                    } else {
                        logger.warn("Failed to extract stencilId from path: {}", SanitizeUtil.forLog(filePath));
                    }

                    // カテゴリ情報を収集
                    if (!StringUtils.isEmpty(config.getCategoryId())) {
                        categoryMap.putIfAbsent(config.getCategoryId(), config.getCategoryName());
                    }

                    // ステンシル情報を収集（バージョンごとにグループ化）
                    String stencilId = config.getId();
                    stencilMap.computeIfAbsent(stencilId, k -> new ArrayList<>()).add(config);

                } catch (Exception e) {
                    logger.warn("Failed to parse stencil settings: {}", SanitizeUtil.forLog(filePath), e);
                }
            }

            // カテゴリフィルタリング適用
            final String filterCategoryId = categoryId;
            if (filterCategoryId != null) {
                stencilMap.entrySet().removeIf(entry -> {
                    if (entry.getValue().isEmpty())
                        return true;
                    Config firstConfig = entry.getValue().get(0);
                    return !filterCategoryId.equals(firstConfig.getCategoryId());
                });
            }

            // カテゴリ一覧を作成
            List<Map<String, Object>> categories = categoryMap.entrySet().stream()
                    .map(entry -> {
                        String catId = entry.getKey();
                        long count = stencilMap.values().stream()
                                .filter(configs -> !configs.isEmpty() && catId.equals(configs.get(0).getCategoryId()))
                                .count();

                        Map<String, Object> category = new LinkedHashMap<>();
                        category.put("id", catId);
                        category.put("name", entry.getValue() != null ? entry.getValue() : catId);
                        category.put("stencilCount", count);
                        return category;
                    })
                    .collect(Collectors.toList());

            // ステンシル一覧を作成（各ステンシルの最新バージョン情報）
            List<Map<String, Object>> stencils = stencilMap.entrySet().stream()
                    .map(entry -> {
                        String stencilId = entry.getKey();
                        List<Config> versions = entry.getValue();

                        // 最新バージョンを取得（serialNoでソート）
                        Config latest = versions.stream()
                                .max((a, b) -> {
                                    String serialA = a.getSerial() != null ? a.getSerial() : "";
                                    String serialB = b.getSerial() != null ? b.getSerial() : "";
                                    return serialA.compareTo(serialB);
                                })
                                .orElse(versions.get(0));

                        Map<String, Object> stencil = new LinkedHashMap<>();
                        stencil.put("id", stencilId);
                        stencil.put("name", latest.getName() != null ? latest.getName() : stencilId);
                        stencil.put("categoryId", latest.getCategoryId() != null ? latest.getCategoryId() : "");
                        stencil.put("categoryName", latest.getCategoryName() != null ? latest.getCategoryName() : "");
                        stencil.put("serial", latest.getSerial() != null ? latest.getSerial() : "");
                        stencil.put("latestSerial", latest.getSerial() != null ? latest.getSerial() : "");
                        stencil.put("lastUpdate", latest.getLastUpdate() != null ? latest.getLastUpdate() : "");
                        stencil.put("lastUpdateUser",
                                latest.getLastUpdateUser() != null ? latest.getLastUpdateUser() : "");
                        stencil.put("description", latest.getDescription() != null ? latest.getDescription() : "");
                        stencil.put("versionCount", versions.size());
                        return stencil;
                    })
                    .collect(Collectors.toList());

            response.setData(Map.of(
                    "categories", categories,
                    "stencils", stencils));

        } catch (Exception e) {
            logger.error("Failed to list stencils", e);
            response.addError("ステンシル一覧の取得に失敗しました: " + e.getMessage());
        }

        return response;
    }

    /**
     * ファイルパスからstencilId（カテゴリ + ステンシル名）を抽出
     * 
     * 例: /path/to/user/ebuilder/usermodule-biz300/230317A/stencil-settings.yml
     * → /ebuilder/usermodule-biz300
     */
    private String extractStencilIdFromPath(String filePath) {
        try {
            String cleanPath = filePath;

            // classpathリソースの前処理
            if (filePath.startsWith("classpath:")) {
                cleanPath = filePath.substring("classpath:".length());
                if (cleanPath.startsWith("file:")) {
                    cleanPath = cleanPath.substring("file:".length());
                }
            }

            Path path = Paths.get(cleanPath);

            // stencil-settings.ymlの親ディレクトリ = serial
            // さらにその親 = stencil name
            // さらにその親 = category
            Path serialDir = path.getParent(); // .../ serial/
            if (serialDir == null)
                return null;

            Path stencilDir = serialDir.getParent(); // .../ stencilName/
            if (stencilDir == null)
                return null;

            Path categoryDir = stencilDir.getParent(); // .../ category/
            if (categoryDir == null)
                return null;

            // categoryとstencilNameを結合してstencilIdを作成
            String category = categoryDir.getFileName().toString();
            String stencilName = stencilDir.getFileName().toString();

            return "/" + category + "/" + stencilName;

        } catch (Exception e) {
            logger.warn("Failed to extract stencilId from path: {}", SanitizeUtil.forLog(filePath), e);
        }
        return null;
    }

    /**
     * ファイルパスからserial（親ディレクトリ名）を抽出
     * 
     * 例:
     * /path/to/stencil/user/ebuilder/usermodule-biz200/191207A/stencil-settings.yml
     * → 191207A
     */
    private String extractSerialFromPath(String filePath) {
        try {
            // classpathリソースの場合
            if (filePath.startsWith("classpath:")) {
                String resourcePath = filePath.substring("classpath:".length());
                if (resourcePath.startsWith("file:")) {
                    resourcePath = resourcePath.substring("file:".length());
                }
                Path path = Paths.get(resourcePath);
                // 親ディレクトリの名前を取得
                Path parentDir = path.getParent();
                if (parentDir != null) {
                    return parentDir.getFileName().toString();
                }
            } else {
                // ファイルシステムの場合
                Path path = Paths.get(filePath);
                // stencil-settings.ymlの親ディレクトリ名 = serial
                Path parentDir = path.getParent();
                if (parentDir != null) {
                    return parentDir.getFileName().toString();
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to extract serial from path: {}", SanitizeUtil.forLog(filePath), e);
        }
        return null;
    }

    /**
     * ストレージレイヤーから全ステンシル設定ファイルを収集
     */
    private List<String> collectStencilSettingsFromLayers() {
        List<String> allFiles = new ArrayList<>();

        // user/standardレイヤーからファイルシステムで検索
        String[] layerDirs = {
                StorageConfig.getUserStencilDir(),
                StorageConfig.getStandardStencilDir()
        };

        for (String layerDir : layerDirs) {
            File dir = new File(layerDir);
            if (dir.exists() && dir.isDirectory()) {
                List<String> layerFiles = FileUtil.findByFileName(layerDir, "stencil-settings.yml");
                allFiles.addAll(layerFiles);
                logger.debug("Found {} stencil files in {}", layerFiles.size(), layerDir);
            }
        }

        // samplesレイヤー（classpath）から検索
        collectClasspathStencils(allFiles);

        logger.info("Total stencil files collected: {}", allFiles.size());
        return allFiles;
    }

    /**
     * classpathからサンプルステンシルを収集
     */
    private void collectClasspathStencils(List<String> allFiles) {
        try {
            String samplesDir = StorageConfig.getSamplesStencilDir();
            if (samplesDir != null && samplesDir.startsWith("classpath:")) {
                String pattern = samplesDir.replace("classpath:", "classpath*:") + "/**/stencil-settings.yml";

                ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                Resource[] resources = resolver.getResources(pattern);

                for (Resource resource : resources) {
                    try {
                        // classpathリソースは特殊な識別子で保存
                        allFiles.add("classpath:" + resource.getURI().toString());
                        logger.debug("Found classpath stencil: {}", resource.getURI());
                    } catch (IOException e) {
                        logger.warn("Failed to get URI for resource: {}", resource, e);
                    }
                }

                logger.debug("Found {} stencil files in classpath", resources.length);
            }
        } catch (Exception e) {
            logger.error("Failed to collect classpath stencils", e);
        }
    }

    /**
     * ステンシル設定ファイルを読み込む
     */
    private StencilSettingsYml loadStencilSettings(String filePath, Yaml yaml) {
        try {
            if (filePath.startsWith("classpath:")) {
                // classpathリソースの処理
                String resourcePath = filePath.substring("classpath:".length());
                if (resourcePath.startsWith("file:")) {
                    resourcePath = resourcePath.substring("file:".length());
                }

                ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
                Resource resource = resolver.getResource("classpath:" + resourcePath);

                if (resource.exists()) {
                    try (InputStream is = resource.getInputStream()) {
                        return yaml.loadAs(is, StencilSettingsYml.class);
                    }
                }
            } else {
                // ファイルシステムの処理 - StorageService経由 or ローカル
                if (storageService.exists(filePath)) {
                    try (InputStream is = storageService.getInputStream(filePath)) {
                        return yaml.loadAs(is, StencilSettingsYml.class);
                    }
                }
                File file = new File(filePath);
                if (file.exists()) {
                    try (InputStream is = Files.newInputStream(file.toPath())) {
                        return yaml.loadAs(is, StencilSettingsYml.class);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Failed to load stencil settings from: {}", SanitizeUtil.forLog(filePath), e);
        }
        return null;
    }
}
