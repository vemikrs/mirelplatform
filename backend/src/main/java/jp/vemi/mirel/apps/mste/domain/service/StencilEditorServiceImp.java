/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.mste.domain.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.Yaml;

import jp.vemi.framework.config.StorageConfig;
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

/**
 * {@link StencilEditorService} の実装
 */
@Service
@Transactional
public class StencilEditorServiceImp implements StencilEditorService {

    private static final Logger logger = LoggerFactory.getLogger(StencilEditorServiceImp.class);

    @Autowired
    private MsteStencilRepository stencilRepository;

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
            String[] layers = {
                StorageConfig.getUserStencilDir(),
                StorageConfig.getStandardStencilDir()
            };
            
            Path stencilPath = null;
            for (String layer : layers) {
                Path candidatePath = Paths.get(layer, stencilId, serial);
                if (Files.exists(candidatePath)) {
                    stencilPath = candidatePath;
                    break;
                }
            }
            
            if (stencilPath == null) {
                response.addError("ステンシルが見つかりません: " + stencilId + "/" + serial);
                return response;
            }
            
            // stencil-settings.yml読み込み
            Path settingsPath = stencilPath.resolve("stencil-settings.yml");
            if (!Files.exists(settingsPath)) {
                response.addError("stencil-settings.ymlが見つかりません");
                return response;
            }
            
            String yamlContent = Files.readString(settingsPath);
            StencilConfigDto config = parseStencilConfig(yamlContent, stencilId, serial);
            
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
            Path stencilPath = Paths.get(userDir, param.getStencilId(), newSerial);
            Files.createDirectories(stencilPath);
            
            // ファイル保存
            for (StencilFileDto file : param.getFiles()) {
                Path filePath = stencilPath.resolve(file.getPath());
                Files.createDirectories(filePath.getParent());
                Files.writeString(filePath, file.getContent());
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
                    String content = Files.readString(path);
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
     */
    private List<StencilVersionDto> loadVersionHistory(String stencilId) {
        List<StencilVersionDto> versions = new ArrayList<>();
        
        // userレイヤーから履歴を取得
        String userDir = StorageConfig.getUserStencilDir();
        Path categoryPath = Paths.get(userDir, stencilId);
        
        if (Files.exists(categoryPath)) {
            try {
                Files.list(categoryPath)
                    .filter(Files::isDirectory)
                    .forEach(path -> {
                        String serial = path.getFileName().toString();
                        versions.add(StencilVersionDto.builder()
                            .serial(serial)
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
}
