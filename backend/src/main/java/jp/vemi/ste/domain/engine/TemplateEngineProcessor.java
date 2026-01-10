/*
 * Copyright(c) 2018 mirelplatform.
 */
package jp.vemi.ste.domain.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.ConstructorException;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import jp.vemi.framework.util.FileUtil;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import freemarker.template.TemplateNotFoundException;
import groovy.lang.Tuple3;
import jp.vemi.framework.exeption.MirelApplicationException;
import jp.vemi.framework.exeption.MirelSystemException;
import jp.vemi.framework.util.CloseableUtil;
import jp.vemi.framework.util.DateUtil;
import jp.vemi.framework.util.ModelUtil;
import jp.vemi.framework.util.ResourceUtil;
import jp.vemi.framework.util.StorageUtil;
import jp.vemi.framework.util.SanitizeUtil;
import jp.vemi.framework.config.StorageConfig;
import jp.vemi.ste.domain.DictionaryMetaData;
import jp.vemi.ste.domain.EngineBinds;
import jp.vemi.ste.domain.context.SteContext;
import jp.vemi.ste.domain.context.SteContext.StringContent;
import jp.vemi.ste.domain.dto.yml.StencilSettingsYml;

/**
 * ロジックのテンプレートエンジンです。<br/>
 *
 * @author mirelplatform
 *
 */
public class TemplateEngineProcessor {

    /**
     * 共通バインド
     */
    public EngineBinds commonBinds = new EngineBinds();

    protected SteContext context;

    /** Spring標準のリソース検索機能 */
    protected ResourcePatternResolver resourcePatternResolver;

    /**
     * ResourcePatternResolverを設定（テスト用）
     */
    public void setResourcePatternResolver(ResourcePatternResolver resourcePatternResolver) {
        this.resourcePatternResolver = resourcePatternResolver;
    }

    protected Configuration cfg = null;
    protected static final String STENCIL_EXTENSION = ".ftl";
    protected static final String REGEX = "[0-9]{6}[A-Z]+";
    protected boolean isLegacy = true;

    /** 一時ファイルと元のファイル名のマッピング */
    protected Map<String, String> tempFileToOriginalMap = new HashMap<>();

    private static final Logger logger = LoggerFactory.getLogger(TemplateEngineProcessor.class);

    /**
     * default constructor.
     * 
     * @param context
     *            {@link SteContext}
     * @return instance
     */
    public static TemplateEngineProcessor create(final SteContext context) {
        return create(context, null);
    }

    /**
     * constructor with ResourcePatternResolver.
     * 
     * @param context
     *            {@link SteContext}
     * @param resourcePatternResolver
     *            {@link ResourcePatternResolver}
     * @return instance
     */
    public static TemplateEngineProcessor create(final SteContext context,
            final ResourcePatternResolver resourcePatternResolver) {
        final TemplateEngineProcessor instance = new TemplateEngineProcessor();

        // context.
        instance.context = context;

        // resourcePatternResolver with null check
        instance.resourcePatternResolver = resourcePatternResolver;

        if (resourcePatternResolver == null) {
            logger.warn("WARNING: ResourcePatternResolver is null in TemplateEngineProcessor.create()");
            logger.warn("This may cause classpath resource search to fail");
            // nullの場合でもインスタンス生成は継続（フォールバック処理で対応）
        }

        // decide serialNo (only stencil selected)
        if (false == StringUtils.isEmpty(instance.context.getStencilCanonicalName())
                && StringUtils.isEmpty(instance.context.getSerialNo())) {
            List<String> nya = instance.getSerialNos();
            Assert.notEmpty(nya, "使用可能なシリアルがありません。ステンシルを登録してください。");
            String serialNo = nya.get(nya.size() - 1);
            context.put("serialNo", serialNo);
        }

        return instance;
    }

    /**
     * default constructor.
     * 
     * @return instance
     */
    public static TemplateEngineProcessor create() {
        return create(SteContext.standard());
    }

    public String execute() {
        final String generateId = createGenerateId();
        return execute(generateId);
    }

    /**
     * execute.
     * 
     * @return 生成結果Path
     */
    public String execute(final String generateId) {
        // デバッグログ

        // validate stencil-settings.yml
        final Tuple3<List<String>, List<String>, List<String>> validRets = validate();
        if (false == validRets.getV3().isEmpty()) {
            throw new MirelApplicationException(validRets.getV3());
        }

        validRets.getV2().forEach(logger::warn);
        validRets.getV2().forEach(logger::info);

        // output dir
        final String outputDir = createOutputFileDir(StringUtils.isEmpty(generateId) ? createGenerateId() : generateId);

        // ディレクトリ作成
        try {
            Files.createDirectories(Paths.get(outputDir));
        } catch (IOException e) {
            throw new MirelSystemException("出力ディレクトリの作成に失敗しました: " + outputDir, e);
        }

        // デバッグログ

        // parse content.
        if (isLegacy) {
            parseTypes();
        }

        // binding.
        prepareBind();

        // get file items.
        final List<String> stencilFileNames = getStencilTemplateFiles();

        // デバッグログ

        // ignore settings file.
        if (stencilFileNames.isEmpty()) {
            throw new MirelSystemException("ステンシル定義が行方不明です。。", null);
        }

        // initialize configuration object.
        createConfiguration();

        // generate.
        for (final String stencilFileName : stencilFileNames) {

            // ファイルシステムとクラスパスの両方に対応したファイル名抽出
            final String name = extractRelativeFileName(stencilFileName);
            final String cname = extractTemplateFileName(stencilFileName);

            // デバッグログ

            if (cname.startsWith("\\.")) {
                //
                logger.info("folder starts with '.': {}", SanitizeUtil.forLog(cname));
                continue;
            }
            final freemarker.template.Template template = newTemplateFileSpec3(cname);

            if (null == template) {
                // テンプレートのインスタンスがNullの場合、生成対象外と判断されたもの。
                logger.info("template is null.");
                continue;
            }

            final File outputFile = bindFileName(cname, new File(outputDir));

            // デバッグログ

            File parentDir = outputFile.getParentFile();
            try {
                Files.createDirectories(parentDir.toPath());
            } catch (IOException e) {
                throw new MirelSystemException(e);
            }

            try {
                template.process(commonBinds, new FileWriter(outputFile));
                // デバッグログ
            } catch (final TemplateException e) {
                final String secondCouse = " 原因：" + e.getLocalizedMessage();
                throw new MirelSystemException(
                        "ステンシルに埋め込まれたプロパティのバインドに失敗しました。ステンシルファイル：" + name + secondCouse, e);
            } catch (final IOException e) {
                throw new MirelSystemException("文書生成に失敗しました。ステンシルファイル：" + name, e);
            }

        }

        return outputDir;
    }

    @SuppressWarnings("lgtm[java/path-injection]")
    private void createConfiguration() {
        // configuration.
        cfg = new Configuration(Configuration.VERSION_2_3_29);

        try {
            // FreeMarkerのMultiTemplateLoaderを使用してファイルシステムとクラスパスの両方をサポート
            List<TemplateLoader> loaders = new ArrayList<>();

            // Layer 1: ファイルシステムローダー（serialNoディレクトリ全体を基準）
            // NOTE: Path is validated through constructSecurePath to prevent path traversal
            String stencilStorageBase = getStencilMasterStorageDir();
            String stencilPath = context.getStencilCanonicalName();
            if (!StringUtils.isEmpty(context.getSerialNo()) && !"*".equals(context.getSerialNo())) {
                stencilPath = stencilPath + "/" + context.getSerialNo();
            }
            File serialDir = constructSecurePath(stencilStorageBase, stencilPath);
            if (serialDir.exists() && serialDir.isDirectory()) {
                FileTemplateLoader fileLoader = new FileTemplateLoader(serialDir);
                loaders.add(fileLoader);
                logger.debug("Added filesystem template loader: {}", SanitizeUtil.forLog(serialDir.getAbsolutePath()));
            } else {
                logger.debug("Filesystem serial directory not found: {}",
                        SanitizeUtil.forLog(serialDir.getAbsolutePath()));
            }

            // Layer 2: クラスパスローダー（serialNoディレクトリ全体を基準）
            if (resourcePatternResolver != null) {
                // serialNoディレクトリを基準パスとする
                String classpathStencilPath = "promarker/stencil/samples" + context.getStencilCanonicalName() + "/"
                        + context.getSerialNo();
                ClassTemplateLoader classpathLoader = new ClassTemplateLoader(getClass().getClassLoader(),
                        classpathStencilPath);
                loaders.add(classpathLoader);
                logger.debug("Added classpath template loader: {}", SanitizeUtil.forLog(classpathStencilPath));
            }

            // Layer 3: 一時ファイル用テンプレートローダー
            if (!tempFileToOriginalMap.isEmpty()) {
                File tempDir = new File(System.getProperty("java.io.tmpdir"));
                FileTemplateLoader tempFileLoader = new FileTemplateLoader(tempDir);
                loaders.add(tempFileLoader);
                logger.debug("Added temp file template loader: {}", tempDir.getAbsolutePath());
            }

            if (!loaders.isEmpty()) {
                TemplateLoader multiLoader = new MultiTemplateLoader(loaders.toArray(new TemplateLoader[0]));
                cfg.setTemplateLoader(multiLoader);
                logger.info("FreeMarker configured with {} template loaders", loaders.size());
            } else {
                throw new IOException("No template loaders available");
            }

        } catch (IOException e1) {
            e1.printStackTrace();
            throw new MirelSystemException("システムエラー: ", e1);
        }

        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(true);
        cfg.setWrapUncheckedExceptions(true);
        cfg.setFallbackOnNullLoopVariable(false);
    }

    protected static Map<String, String> getReferences(final List<Map<String, Object>> deadbs) {
        final Map<String, String> refs = Maps.newLinkedHashMap();

        for (final Map<String, Object> map : deadbs) {
            if (false == ObjectUtils.isEmpty(map.get("reference")) &&
                    false == ObjectUtils.isEmpty(map.get("id"))) {
                refs.put((String) map.get("id"), (String) map.get("reference"));
            }
        }

        return refs;
    }

    /**
     * validate. <br/>
     * 
     * @param storagePath
     *            ストレージ上のパス
     * @param stencilName
     *            ステンシル名
     */
    protected Tuple3<List<String>, List<String>, List<String>> validate() {

        final StencilSettingsYml settings = getStencilSettings();
        logger.info("validate with settings: {}", settings);

        final List<String> infos = Lists.newArrayList();
        final List<String> warns = Lists.newArrayList();
        final List<String> errs = Lists.newArrayList();

        final List<Map<String, Object>> deadbs = settings.getStencilDeAndDd();
        for (final Map<String, Object> deadb : deadbs) {
            // empty element and domain :ignore
            // maybe... miss in settings
            if (CollectionUtils.isEmpty(deadb)) {
                warns.add("empty block in stencil settings.");
                continue;
            }

            // key:id not found :ignore
            // maybe... miss in settings
            if (false == deadb.containsKey("id")) {
                warns.add("id not found item :" + deadb.toString());
                continue;
            }

            if (false == valueEmptyValidate(deadb)) {
                errs.add("Required value: " + deadb.get("name"));
            }

        }

        return new Tuple3<>(infos, warns, errs);
    }

    /**
     * valueEmptyValidate.<br/>
     * 
     * @param item
     * @return
     */
    protected boolean valueEmptyValidate(final Map<String, Object> item) {

        // validate.
        Assert.notEmpty(item, "map must not be null or empty");

        // when nullable ? ok or pass
        if (item.containsKey("nullable")) {
            final Object value = item.get("nullable");
            if (value instanceof Boolean) {
                if (true == (Boolean) value) {
                    // allright ok.
                    return true;
                }
            }
        }

        // when reference item ? allok or pass
        if (item.containsKey("reference")) {
            // allright ok.
            return true;
        }

        // no contains -> ERR.
        if (ObjectUtils.isEmpty(context.get(item.get(("id"))))) {
            return false;
        }

        // ok.
        return true;
    }

    /**
     * Append context.
     * 
     * @param bind
     */
    public void appendContext(Map<String, Object> bind) {
        this.context.putAll(bind);
    }

    /**
     * 共通バインドを構築します。<br/>
     */
    protected void prepareBind() {
        commonBinds.put("dictionaryMetaData", new DictionaryMetaData(context));
        commonBinds.putAll(ModelUtil.createMap(context));
        commonBinds.putAll(context);

        // get stencil-settings
        final StencilSettingsYml settings = getStencilSettings();
        final List<Map<String, Object>> deadbs = settings.getStencilDeAndDd(); // 何回も取得すな･･･。

        final Map<String, String> refItems = getReferences(deadbs);
        refItems.entrySet().forEach(entry -> {

            if (commonBinds.containsKey(entry.getKey())) {
                // allready registed.
                return;
            }

            if (false == commonBinds.containsKey(entry.getValue())) {
                // reference value not found.
                return;
            }

            // copy reference value.
            commonBinds.put(entry.getKey(), commonBinds.get(entry.getValue()));
        });

    }

    /**
     * ファイル名にバインドをアタッチします。<br/>
     * 
     * @param templatePath
     * @param outputDir
     * @return
     */
    protected File bindFileName(final String stencilName, final File outputDir) {

        String fileName = stencilName;

        final EngineBinds binds = new EngineBinds(commonBinds);
        if (isLegacy) {
            appendSubInvoker(binds);
        }

        final Iterator<Entry<String, Object>> keys = binds.entrySet().iterator();
        while (keys.hasNext()) {
            final Entry<String, Object> entry = keys.next();
            while (true) {
                final String after = replaceFileName(entry, fileName);
                if (fileName.equals(after)) {
                    break;
                } else {
                    fileName = after;
                }
            }
        }

        // remove converted .stencil file
        if (fileName.endsWith(STENCIL_EXTENSION)) {
            fileName = fileName.substring(0, fileName.length() - STENCIL_EXTENSION.length());
        }

        final File file = new File(outputDir, fileName);
        return file;
    }

    /**
     * append sub invoker.
     * 
     * @param binding
     */
    private void appendSubInvoker(final EngineBinds binding) {
        // keys のうち コンテンツ対象の展開
        for (final Entry<String, Object> entry : commonBinds.entrySet()) {
            if (entry.getValue() instanceof StringContent) {
                // StringContent
                ((StringContent) entry.getValue()).appendSubInvoker(entry.getKey(), binding);
            }
            // add more...
            // もうちょっとキレイに書きたい。
        }
    }

    /**
     * ファイル、ディレクトリの名前から、最初に出現するkeyにマッチする文字列にバインド変数をアタッチします。ファイル名を全て変換するには呼出元で再帰的に実行してください。
     * 
     * @param entry
     * @param fileName
     * @return 変換後のファイル名
     */
    private String replaceFileName(final Entry<String, Object> entry, final String fileName) {
        final Object value = entry.getValue();
        if (null == value) {
            return fileName;
        }

        final String replaceKey = StringUtils.join("_", entry.getKey(), "_");
        final String after = fileName.replace(replaceKey, value.toString());

        return after;
    }

    public freemarker.template.Template newTemplateFileSpec3(final String stencilName) {
        logger.debug("=== newTemplateFileSpec3: {} ===", SanitizeUtil.forLog(stencilName));

        // Validate.
        Assert.notNull(stencilName, "stencil name must not be null");

        // 元のファイル名(hello.ftl)が渡されている場合、対応する一時ファイル名を逆引き
        String actualTemplateName = stencilName;
        for (Map.Entry<String, String> entry : tempFileToOriginalMap.entrySet()) {
            if (entry.getValue().equals(stencilName)) {
                // hello.ftl → template-hello-ftl*.tmp の逆引き成功
                actualTemplateName = entry.getKey();
                logger.info("Reverse mapping original '" + SanitizeUtil.forLog(stencilName) + "' to temp file '"
                        + SanitizeUtil.forLog(actualTemplateName) + "'");
                break;
            }
        }

        // FreeMarkerのConfigurationからテンプレートを取得
        // MultiTemplateLoaderが自動的にファイルシステムとクラスパスを検索する
        try {
            freemarker.template.Template template = cfg.getTemplate(actualTemplateName);
            logger.debug("Successfully loaded template via MultiTemplateLoader: {}",
                    SanitizeUtil.forLog(actualTemplateName));
            return template;
        } catch (TemplateNotFoundException e) {
            logger.debug("Template not found: {}", SanitizeUtil.forLog(actualTemplateName));
            // デバッグログ
            return null; // テンプレートが見つからない場合はnullを返す（スキップ対象）
        } catch (ParseException e) {
            String message = e.getLocalizedMessage();
            throw new MirelSystemException("テンプレートの解析に失敗しました。" + actualTemplateName + " Cause: " + message, e);
        } catch (IOException e) {
            throw new MirelSystemException("IOException in " + actualTemplateName, e);
        }
    }

    /**
     *
     * @param stencilFilePath
     * @return
     */
    public String getResourceWithParseLine(final String stencilFilePath) {
        final BufferedReader reader = new BufferedReader(ResourceUtil.newFileReader(stencilFilePath));
        String resource = reader.toString();
        CloseableUtil.close(reader);
        return resource;
    }

    public String getStencilAndSerialStorageDir() {
        // serialNo は "*" を許容する仕様のため、パス結合前に検証
        final String serial = context.getSerialNo();
        if (!StringUtils.isEmpty(serial) && !"*".equals(serial)) {
            SanitizeUtil.sanitizeIdentifierAllowWildcard(serial); // 実質 IDENTIFIER 検証
        }
        return getStencilStorageDir() + "/" + serial;
    }

    /**
     * Securely constructs a file path by validating that the resolved path
     * is within the expected base directory, preventing path traversal attacks.
     * 
     * @param baseDir
     *            Base directory path
     * @param userProvidedPath
     *            User-provided path component (already sanitized)
     * @return Secure File object
     * @throws IllegalArgumentException
     *             if path escapes base directory
     */
    private File constructSecurePath(String baseDir, String userProvidedPath) {
        try {
            Path basePath = Paths.get(baseDir).toRealPath();
            // Resolve and normalize the combined path
            Path resolvedPath = basePath.resolve(userProvidedPath.replaceFirst("^/", "")).normalize();

            // Verify the resolved path is within base directory
            if (!resolvedPath.startsWith(basePath)) {
                throw new IllegalArgumentException(
                        "Path traversal detected: resolved path escapes base directory");
            }

            return resolvedPath.toFile();
        } catch (IOException e) {
            // If base path doesn't exist or can't be resolved, fall back to regular File
            // construction
            // but still validate that the path doesn't contain suspicious patterns
            Path basePath = Paths.get(baseDir).normalize();
            Path resolvedPath = basePath.resolve(userProvidedPath.replaceFirst("^/", "")).normalize();

            if (!resolvedPath.startsWith(basePath)) {
                throw new IllegalArgumentException(
                        "Path traversal detected: resolved path escapes base directory");
            }

            return resolvedPath.toFile();
        }
    }

    public String getStencilStorageDir() {
        // validate.
        Assert.notNull(context, "context");
        Assert.hasText(context.getStencilCanonicalName(), "stencilCanonicalName must not be empty");
        // 正規名パス検証（先頭"/"、セグメントは識別子、.. や \\ 禁止）
        String sanitizedCanonicalName = SanitizeUtil.sanitizeCanonicalPath(context.getStencilCanonicalName());

        // レイヤー検索: user → standard の順で実際に存在するディレクトリを返す
        String[] layerDirs = {
                StorageConfig.getUserStencilDir(),
                StorageConfig.getStandardStencilDir()
        };

        for (String layerDir : layerDirs) {
            // Use secure path construction to prevent path traversal
            File candidateDir = constructSecurePath(layerDir, sanitizedCanonicalName);

            if (candidateDir.exists() && candidateDir.isDirectory()) {
                logger.debug("Found stencil directory in layer: {}",
                        SanitizeUtil.forLog(candidateDir.getAbsolutePath()));
                return candidateDir.getAbsolutePath();
            }
        }

        // 見つからない場合はデフォルトのパス（下位互換性）
        File defaultDir = constructSecurePath(getStencilMasterStorageDir(), sanitizedCanonicalName);
        return defaultDir.getAbsolutePath();
    }

    /** 
     * 
     */
    protected void parseTypes() {

        for (final Map.Entry<String, Object> entry : context.entrySet()) {
            if (null == entry) {
                continue;
            }
            if (StringUtils.isEmpty(entry.getKey())) {
                continue;
            }
            if (null == entry.getValue()) {
                continue;
            }

            if (entry.getValue() instanceof String) {
                final StringContent content = StringContent.string(entry.getValue().toString());
                context.put(entry.getKey(), content);
                continue;
            }
        }
    }

    protected StencilSettingsYml getSsYmlRecurive(final File file) {

        if (false == file.exists()) {
            throw new MirelSystemException(
                    "ステンシル定義が見つかりません。ファイル：" + context.getStencilCanonicalName() + "/" + file.getName(), null);
        }

        // load as stencil-settings.
        // NOTE: File path validation is performed by caller (constructSecurePath or
        // sanitization)
        // The file parameter comes from validated sources within the stencil storage
        // directory
        StencilSettingsYml settings = null;
        FileSystemResource fileResource = new FileSystemResource(file);
        try (InputStream stream = fileResource.getInputStream()) {
            LoaderOptions options = new LoaderOptions();
            Yaml yaml = new Yaml(options);
            settings = yaml.loadAs(stream, StencilSettingsYml.class);
        } catch (final ConstructorException e) {
            e.printStackTrace();
            String msg = "yamlの読込でエラーが発生しました。";
            if (isDebugMode()) {
                msg += "debug log:" + e.getLocalizedMessage();
            }
            throw new MirelApplicationException(msg, e);
        } catch (final IOException e) {
            e.printStackTrace();
            throw new MirelSystemException("yamlの読込で入出力エラーが発生しました。", e);
        }

        // NOTE:
        // 親設定のマージはgetStencilSettings()のmergeParentStencilSettingsUnified()で統一的に処理されます

        return settings;

    }

    protected boolean isDebugMode() {
        return true;
    }

    public StencilSettingsYml getStencilSettings() {
        logger.debug("[GET_SETTINGS] Called with stencilCanonicalName={}, serialNo={}",
                SanitizeUtil.forLog(context.getStencilCanonicalName()), SanitizeUtil.forLog(context.getSerialNo()));

        // レイヤード検索: ユーザー → 標準 → サンプル の順で検索
        StencilSettingsYml settings = findStencilSettingsInLayers();
        if (settings == null) {
            throw new MirelSystemException(
                    "ステンシル定義が見つかりません。ステンシル：" + context.getStencilCanonicalName(), null);
        }

        logger.debug("[GET_SETTINGS] Found settings, dataDomain size (before merge): {}",
                settings.getStencil() != null && settings.getStencil().getDataDomain() != null
                        ? settings.getStencil().getDataDomain().size()
                        : 0);

        // ✅ Phase 2-1: 親設定を統一的にマージ（filesystem/classpath両対応）
        mergeParentStencilSettingsUnified(settings);

        logger.debug("[GET_SETTINGS] dataDomain size (after merge): {}",
                settings.getStencil() != null && settings.getStencil().getDataDomain() != null
                        ? settings.getStencil().getDataDomain().size()
                        : 0);

        return settings;
    }

    /**
     * レイヤード検索でstencil-settings.ymlを検索する
     * 
     * @return 見つかったStencilSettingsYml、または null
     */
    private StencilSettingsYml findStencilSettingsInLayers() {
        logger.debug("=== DEBUG findStencilSettingsInLayers ===");
        logger.debug("context.getStencilCanonicalName(): {}", SanitizeUtil.forLog(context.getStencilCanonicalName()));
        logger.debug("resourcePatternResolver: {}", resourcePatternResolver);

        // 優先度順にレイヤーを検索: ユーザー → 標準 → サンプル
        String[] searchLayers = {
                StorageConfig.getUserStencilDir(),
                StorageConfig.getStandardStencilDir(),
                StorageConfig.getSamplesStencilDir()
        };

        for (String layerDir : searchLayers) {
            StencilSettingsYml settings = findStencilSettingsInLayer(layerDir);
            if (settings != null) {
                logger.debug("Found stencil settings in layer: {}", layerDir);
                return settings;
            }
        }

        logger.warn("No stencil settings found in any layer");
        return null;
    }

    /**
     * 指定されたレイヤーディレクトリでstencil-settings.ymlを検索する
     * 
     * @param layerDir
     *            レイヤーディレクトリ
     * @return 見つかったStencilSettingsYml、または null
     */
    private StencilSettingsYml findStencilSettingsInLayer(String layerDir) {
        if (StringUtils.isEmpty(layerDir)) {
            return null;
        }

        logger.debug("[FIND_LAYER] Searching in layerDir: {}", layerDir);
        logger.debug("[FIND_LAYER] Is classpath resource: {}", layerDir.startsWith("classpath:"));

        // classpath: プレフィックスの場合はclasspath検索
        if (layerDir.startsWith("classpath:")) {
            logger.debug("[FIND_LAYER] Using classpath search");
            return findStencilSettingsInClasspath(layerDir);
        }

        // ファイルシステム検索
        logger.debug("[FIND_LAYER] Using filesystem search");
        return findStencilSettingsInFileSystem(layerDir);
    }

    /**
     * classpathからstencil-settings.ymlを検索する
     * ResourcePatternResolverを使用して動的検索を行う
     * 
     * @param classpathLocation
     *            classpath:で始まるパス
     * @return 見つかったStencilSettingsYml、または null
     */
    private StencilSettingsYml findStencilSettingsInClasspath(String classpathLocation) {
        try {
            logger.debug("=== DEBUG findStencilSettingsInClasspath ===");
            logger.debug("classpathLocation: {}", classpathLocation);
            logger.debug("resourcePatternResolver is null: {}", (resourcePatternResolver == null));

            // ResourcePatternResolverが利用可能な場合は動的検索を使用
            if (resourcePatternResolver != null) {
                logger.debug("Using ResourcePatternResolver for dynamic search");
                return findStencilSettingsWithResourcePatternResolver(classpathLocation);
            }

            // フォールバック: 従来の固定パス検索
            logger.debug("Fallback to fixed path search");
            return findStencilSettingsWithFixedPath(classpathLocation);

        } catch (Exception e) {
            logger.debug("Error finding stencil settings in classpath: {}", e.getMessage());
            logger.trace("Stack trace:", e);
            return null;
        }
    }

    /**
     * ResourcePatternResolverを使用してstencil-settings.ymlを動的検索
     * 
     * @param classpathLocation
     *            classpath:で始まるパス
     * @return 見つかったStencilSettingsYml、または null
     */
    private StencilSettingsYml findStencilSettingsWithResourcePatternResolver(String classpathLocation) {
        try {
            // 指定されたstencilCanonicalNameに対応するstencil-settings.ymlを検索
            String searchPattern = classpathLocation + context.getStencilCanonicalName() + "/**/stencil-settings.yml";
            logger.debug("Searching for stencil settings with pattern: {}", SanitizeUtil.forLog(searchPattern));

            Resource[] resources = resourcePatternResolver.getResources(searchPattern);
            logger.debug("Found {} stencil-settings.yml resources for {}", resources.length,
                    SanitizeUtil.forLog(context.getStencilCanonicalName()));

            // 見つかったリソースから最初の有効なものを使用
            for (Resource resource : resources) {
                try {
                    StencilSettingsYml settings = loadStencilSettingsFromResource(resource);
                    if (settings != null && settings.getStencil() != null
                            && settings.getStencil().getConfig() != null) {
                        // serialNoが指定されている場合はマッチするかチェック
                        if (!StringUtils.isEmpty(context.getSerialNo()) && !"*".equals(context.getSerialNo())) {
                            // serial の識別子検証（"*" 以外）
                            SanitizeUtil.sanitizeIdentifierAllowWildcard(context.getSerialNo());
                            String configSerial = settings.getStencil().getConfig().getSerial();
                            if (!context.getSerialNo().equals(configSerial)) {
                                continue; // serialNoが一致しない場合はスキップ
                            }
                        }
                        logger.info("Found matching stencil settings: " + resource.getURI());
                        return settings;
                    }
                } catch (Exception resourceException) {
                    logger.info("Could not load stencil settings from resource " + resource.getURI() + ": "
                            + resourceException.getMessage());
                }
            }

            return null;
        } catch (Exception e) {
            logger.debug("Error in ResourcePatternResolver search: {}", e.getMessage());
            logger.trace("Stack trace:", e);
            return null;
        }
    }

    /**
     * 従来の固定パス検索（フォールバック）
     * 
     * @param classpathLocation
     *            classpath:で始まるパス
     * @return 見つかったStencilSettingsYml、または null
     */
    private StencilSettingsYml findStencilSettingsWithFixedPath(String classpathLocation) {
        try {
            // ステンシル名のサニタイズと正規化
            String rawStencilName = context.getStencilCanonicalName();
            String safeStencilName = sanitizeStencilCanonicalName(rawStencilName);
            if (StringUtils.isEmpty(safeStencilName)) {
                logger.warn("Invalid or empty stencilCanonicalName: {}", SanitizeUtil.forLog(rawStencilName));
                return null;
            }

            // シリアル番号が指定されている場合は直接検索
            if (!StringUtils.isEmpty(context.getSerialNo()) && !"*".equals(context.getSerialNo())) {
                SanitizeUtil.sanitizeIdentifierAllowWildcard(context.getSerialNo());
                String resourcePath = classpathLocation.substring("classpath:".length())
                        + safeStencilName + "/" + context.getSerialNo() + "/stencil-settings.yml";

                // classpathリソースは先頭スラッシュを含まないため除去
                if (resourcePath.startsWith("/")) {
                    resourcePath = resourcePath.substring(1);
                }

                StencilSettingsYml result = loadStencilSettingsFromClasspath(resourcePath);
                if (result != null) {
                    return result;
                }
            }

            // シリアル番号が未指定の場合は利用可能なシリアル番号を動的検索
            List<String> availableSerials = findAvailableSerialsInClasspath(classpathLocation, safeStencilName);
            for (String serial : availableSerials) {
                String resourcePath = classpathLocation.substring("classpath:".length())
                        + safeStencilName + "/" + serial + "/stencil-settings.yml";

                // classpathリソースは先頭スラッシュを含まないため除去
                if (resourcePath.startsWith("/")) {
                    resourcePath = resourcePath.substring(1);
                }

                StencilSettingsYml result = loadStencilSettingsFromClasspath(resourcePath);
                if (result != null) {
                    return result;
                }
            }

        } catch (Exception e) {
            logger.warn("classpath検索でエラーが発生: {}", SanitizeUtil.forLog(classpathLocation), e);
        }

        return null;
    }

    /**
     * ユーザー入力から渡される可能性があるステンシル名をサニタイズします。
     * 
     * - 先頭のスラッシュは削除
     * - 「..」やバックスラッシュを含む場合は例外
     * 
     * @param rawStencilName
     *            生のステンシル名
     * @return サニタイズ済みのステンシル名
     */
    private String sanitizeStencilCanonicalName(String rawStencilName) {
        if (StringUtils.isEmpty(rawStencilName)) {
            return "";
        }

        String trimmed = rawStencilName.trim();
        // 先頭のスラッシュを除去（classpathパスは先頭にスラッシュを含まない）
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }

        // ディレクトリトラバーサルや不正な区切り文字を禁止
        if (trimmed.contains("..") || trimmed.contains("\\")) {
            logger.warn("Detected invalid stencilCanonicalName: {}", SanitizeUtil.forLog(rawStencilName));
            throw new MirelApplicationException(Collections.singletonList("Invalid stencilCanonicalName"));
        }

        return trimmed;
    }

    /**
     * classpathリソースからStencilSettingsYmlを読み込む
     * 
     * @param resourcePath
     *            リソースパス
     * @return 読み込まれたStencilSettingsYml、または null
     */
    private StencilSettingsYml loadStencilSettingsFromClasspath(String resourcePath) {
        // パストラバーサル攻撃を防ぐための検証
        if (resourcePath == null || resourcePath.contains("..") || resourcePath.contains("\\")) {
            logger.warn("Invalid resourcePath detected: {}", SanitizeUtil.forLog(resourcePath));
            return null;
        }

        try {
            InputStream inputStream = this.getClass().getClassLoader()
                    .getResourceAsStream(resourcePath);

            if (inputStream != null) {
                LoaderOptions options = new LoaderOptions();
                Yaml yaml = new Yaml(options);
                try {
                    return yaml.loadAs(inputStream, StencilSettingsYml.class);
                } finally {
                    CloseableUtil.close(inputStream);
                }
            }
        } catch (Exception e) {
            logger.debug("classpathリソース読み込み失敗: {}", SanitizeUtil.forLog(resourcePath), e);
        }
        return null;
    }

    /**
     * classpathから利用可能なシリアル番号一覧を検索する
     * 
     * @param classpathLocation
     *            classpath:で始まるパス
     * @param stencilCanonicalName
     *            ステンシル正規名
     * @return 利用可能なシリアル番号一覧（降順ソート）
     */
    private List<String> findAvailableSerialsInClasspath(String classpathLocation, String stencilCanonicalName) {
        List<String> serials = Lists.newArrayList();

        try {
            String baseResourcePath = classpathLocation.substring("classpath:".length()) + stencilCanonicalName;
            if (baseResourcePath.startsWith("/")) {
                baseResourcePath = baseResourcePath.substring(1);
            }

            // classpathからディレクトリ一覧を取得（簡易実装）
            // 実際のclasspath検索は制限があるため、既知のシリアル番号パターンで試行
            String[] commonSerials = { "250913A", "191207A", "201019A" }; // よく使われるシリアル番号パターン

            for (String serial : commonSerials) {
                String testPath = baseResourcePath + "/" + serial + "/stencil-settings.yml";
                InputStream testStream = this.getClass().getClassLoader().getResourceAsStream(testPath);
                if (testStream != null) {
                    serials.add(serial);
                    CloseableUtil.close(testStream);
                }
            }

            // 降順ソート（新しいシリアル番号が先頭に）
            serials.sort(Collections.reverseOrder());

        } catch (Exception e) {
            logger.debug("シリアル番号検索でエラー: {}", SanitizeUtil.forLog(classpathLocation), e);
        }

        return serials;
    }

    /**
     * ファイルシステムからstencil-settings.ymlを検索する
     * 
     * @param layerDir
     *            レイヤーディレクトリ
     * @return 見つかったStencilSettingsYml、または null
     */
    private StencilSettingsYml findStencilSettingsInFileSystem(String layerDir) {
        try {
            logger.debug("=== DEBUG findStencilSettingsInFileSystem ===");
            logger.debug("layerDir: {}", SanitizeUtil.forLog(layerDir));
            logger.debug("stencilCanonicalName: {}", SanitizeUtil.forLog(context.getStencilCanonicalName()));

            // パス構築の改善 - constructSecurePath()でパストラバーサル攻撃を防ぐ
            String relativePath = context.getStencilCanonicalName().replaceFirst("^/", "");

            // serialNoが指定されている場合はそれも含める
            if (!StringUtils.isEmpty(context.getSerialNo()) && !"*".equals(context.getSerialNo())) {
                SanitizeUtil.sanitizeIdentifierAllowWildcard(context.getSerialNo());
                relativePath = relativePath + "/" + context.getSerialNo();
            }

            relativePath = relativePath + "/stencil-settings.yml";

            // constructSecurePath()でパス検証を実施
            File settingsFile = constructSecurePath(layerDir, relativePath);
            logger.debug("Searching settings file: {}", SanitizeUtil.forLog(settingsFile.getAbsolutePath()));

            if (settingsFile.exists() && settingsFile.isFile()) {
                logger.debug("Found stencil-settings.yml: {}", SanitizeUtil.forLog(settingsFile.getAbsolutePath()));

                return getSsYmlRecurive(settingsFile);
            } else {
                logger.debug("Settings file not found: {}", SanitizeUtil.forLog(settingsFile.getAbsolutePath()));
            }
        } catch (Exception e) {
            logger.debug("Error in filesystem search: {}", e.getMessage());
            logger.warn("ファイルシステム検索でエラーが発生: {}", layerDir, e);
        }

        return null;
    }

    /**
     * 統一された親ステンシル設定マージロジック（Phase 2-1）
     * filesystem/classpath両対応
     * 
     * @param childSettings
     *            マージ対象の子ステンシル設定
     */
    private void mergeParentStencilSettingsUnified(StencilSettingsYml childSettings) {
        if (childSettings == null || childSettings.getStencil() == null) {
            logger.debug("[MERGE_UNIFIED] childSettings is null or has no stencil config");
            return;
        }

        String stencilCanonicalName = context.getStencilCanonicalName();
        if (StringUtils.isEmpty(stencilCanonicalName)) {
            logger.debug("[MERGE_UNIFIED] stencilCanonicalName is empty");
            return;
        }

        logger.debug("[MERGE_UNIFIED] Starting parent merge for: {}", SanitizeUtil.forLog(stencilCanonicalName));

        // パス分解: /user/project/module_service → ["user", "project", "module_service"]
        String[] pathSegments = stencilCanonicalName.split("/");
        List<String> segments = new ArrayList<>();
        for (String segment : pathSegments) {
            if (!StringUtils.isEmpty(segment)) {
                segments.add(segment);
            }
        }

        logger.debug("[MERGE_UNIFIED] Path segments: {}", SanitizeUtil.forLog(segments));

        // 親階層を下から上へ検索（module_service → project → user）
        for (int i = segments.size() - 1; i >= 1; i--) {
            List<String> parentSegments = segments.subList(0, i);
            String parentPath = "/" + String.join("/", parentSegments);

            logger.debug("[MERGE_UNIFIED] Searching parent settings at: {}", parentPath);

            // 親設定を検索（レイヤード検索）
            StencilSettingsYml parentSettings = findParentStencilSettings(parentPath);

            if (parentSettings != null &&
                    parentSettings.getStencil() != null &&
                    parentSettings.getStencil().getDataDomain() != null) {

                int parentDataDomainSize = parentSettings.getStencil().getDataDomain().size();
                logger.info("[MERGE_UNIFIED] Merging {} dataDomain entries from parent: {}",
                        parentDataDomainSize, parentPath);

                // 親のdataDomainを子にマージ（子の定義が優先される）
                childSettings.appendDataElementSublist(parentSettings.getStencil().getDataDomain());

                logger.debug("[MERGE_UNIFIED] Successfully merged parent dataDomain from: {}", parentPath);
            } else {
                logger.debug("[MERGE_UNIFIED] No parent settings found at: {}", parentPath);
            }
        }

        logger.debug("[MERGE_UNIFIED] Parent merge completed");
    }

    /**
     * 親ステンシル設定を検索（Phase 2-1）
     * レイヤード検索: user → standard の順（samplesは親検索スキップ）
     * 
     * @param parentPath
     *            親パス（例: "/user/project"）
     * @return 見つかった親設定、またはnull
     */
    private StencilSettingsYml findParentStencilSettings(String parentPath) {
        logger.debug("[FIND_PARENT] Searching for parent: {}", parentPath);

        // レイヤー検索: user → standard の順（samplesはclasspathなのでスキップ）
        String[] searchLayers = {
                StorageConfig.getUserStencilDir(),
                StorageConfig.getStandardStencilDir()
        };

        for (String layerDir : searchLayers) {
            if (layerDir.startsWith("classpath:")) {
                // classpathレイヤーは親検索スキップ（ディレクトリ構造が異なる）
                logger.debug("[FIND_PARENT] Skipping classpath layer: {}", layerDir);
                continue;
            }

            logger.debug("[FIND_PARENT] Searching in layer: {}", layerDir);

            // 親ディレクトリのパス構築
            String parentDirPath;
            if (layerDir.endsWith("/")) {
                parentDirPath = layerDir + parentPath.substring(1); // 先頭の"/"を除去
            } else {
                parentDirPath = layerDir + parentPath;
            }

            File parentDir = new File(parentDirPath);
            logger.debug("[FIND_PARENT] Checking directory: {}, exists: {}",
                    parentDirPath, parentDir.exists());

            if (!parentDir.exists() || !parentDir.isDirectory()) {
                logger.debug("[FIND_PARENT] Directory not found: {}", parentDirPath);
                continue;
            }

            // *_stencil-settings.yml を検索
            File[] parentSettingsFiles = parentDir.listFiles((dir, name) -> name.endsWith("_stencil-settings.yml"));

            if (parentSettingsFiles != null && parentSettingsFiles.length > 0) {
                // 最初に見つかった親設定を使用
                File parentSettingsFile = parentSettingsFiles[0];
                logger.debug("[FIND_PARENT] Found parent settings file: {}",
                        parentSettingsFile.getName());

                try (InputStream stream = new FileInputStream(parentSettingsFile)) {
                    LoaderOptions options = new LoaderOptions();
                    Yaml yaml = new Yaml(options);
                    StencilSettingsYml parentSettings = yaml.loadAs(stream, StencilSettingsYml.class);

                    logger.info("[FIND_PARENT] Loaded parent settings from: {}",
                            parentSettingsFile.getName());
                    return parentSettings;

                } catch (Exception e) {
                    logger.warn("[FIND_PARENT] Failed to load parent settings from {}: {}",
                            parentSettingsFile.getName(), e.getMessage());
                }
            } else {
                logger.debug("[FIND_PARENT] No *_stencil-settings.yml found in: {}", parentDirPath);
            }
        }

        logger.debug("[FIND_PARENT] No parent settings found for: {}", parentPath);
        return null;
    }

    protected static String createGenerateId() {
        return DateUtil.toString(new Date(), "yyMMddHHmmssSSS");
    }

    protected static String createOutputFileDir(final String generateId) {
        // getAppStorageDir()は既に完全なパスを返すので、convertStorageToFileDirは不要
        return getAppStorageDir() + "/output/" + generateId;
    }

    public static String getStencilMasterStorageDir() {
        final String stencilMasterDir = "/stencil";
        return StringUtils.join(getAppStorageDir(), stencilMasterDir);
    }

    // TODO: ProMarkerStorageConfig -> ProMarkerStorageUtil
    public static String getAppStorageDir() {
        // StorageConfigから正しいパスを取得
        return StorageConfig.getStorageDir() + "/apps/promarker";
    }

    public static String getOutputBaseStorageDir() {
        return getAppStorageDir() + "/output";
    }

    public static String convertStorageToFileDir(final String storageDir) {
        return StorageUtil.getBaseDir() + "/" + storageDir;
    }

    public List<String> getSerialNos() {
        final List<String> serialNos = Lists.newArrayList();
        final Set<String> foundSerials = new HashSet<>(); // 重複排除

        // レイヤー検索: user → standard → samples の順
        String[] layerDirs = {
                StorageConfig.getUserStencilDir(),
                StorageConfig.getStandardStencilDir(),
                StorageConfig.getSamplesStencilDir()
        };

        // Sanitize canonical name first
        String sanitizedCanonicalName = SanitizeUtil.sanitizeCanonicalPath(context.getStencilCanonicalName());

        for (String layerDir : layerDirs) {
            // classpath: プレフィックスの場合はclasspath検索
            if (layerDir != null && layerDir.startsWith("classpath:")) {
                List<String> classpathSerials = findSerialNosInClasspath(layerDir, sanitizedCanonicalName);
                for (String serial : classpathSerials) {
                    if (!foundSerials.contains(serial)) {
                        serialNos.add(serial);
                        foundSerials.add(serial);
                    }
                }
            } else {
                // ファイルシステム検索 - Use secure path construction
                File stencilDir = constructSecurePath(layerDir, sanitizedCanonicalName);

                if (!stencilDir.exists() || !stencilDir.isDirectory()) {
                    continue;
                }

                File[] serialDirs = stencilDir.listFiles();
                if (serialDirs == null) {
                    continue;
                }

                for (File serialDir : serialDirs) {
                    if (isSerialNoDir(serialDir) && !foundSerials.contains(serialDir.getName())) {
                        serialNos.add(serialDir.getName());
                        foundSerials.add(serialDir.getName());
                    }
                }
            }

            // 見つかったら早期終了（上位レイヤー優先）
            if (!serialNos.isEmpty()) {
                break;
            }
        }

        return serialNos;
    }

    /**
     * classpathから指定されたstencilのシリアル番号を検索
     * 
     * @param classpathLocation
     *            classpath:で始まるパス
     * @param stencilCanonicalName
     *            ステンシルの正規名
     * @return 見つかったシリアル番号のリスト
     */
    private List<String> findSerialNosInClasspath(String classpathLocation, String stencilCanonicalName) {
        List<String> serialNos = new ArrayList<>();
        Set<String> foundSerials = new HashSet<>();

        try {
            if (resourcePatternResolver == null) {
                logger.debug("findSerialNosInClasspath: resourcePatternResolver is null");
                return serialNos;
            }

            // stencil-settings.ymlファイルを検索してシリアル番号を抽出
            // パターン:
            // classpath:/promarker/stencil/samples/samples/hello-world/*/stencil-settings.yml
            String searchPattern = classpathLocation + stencilCanonicalName + "/*/stencil-settings.yml";
            logger.debug("findSerialNosInClasspath: searching with pattern: {}", SanitizeUtil.forLog(searchPattern));

            Resource[] resources = resourcePatternResolver.getResources(searchPattern);
            logger.debug("findSerialNosInClasspath: found {} resources", resources.length);

            for (Resource resource : resources) {
                try {
                    // URLからパスを取得
                    String urlPath = resource.getURL().getPath();
                    logger.info("findSerialNosInClasspath: processing resource: " + urlPath);

                    // パスからシリアル番号を抽出
                    // 例: /path/to/samples/hello-world/250913A/stencil-settings.yml -> 250913A
                    String[] pathSegments = urlPath.split("/");
                    for (int i = pathSegments.length - 1; i >= 0; i--) {
                        String segment = pathSegments[i];
                        if (isSerialNoVal(segment) && !foundSerials.contains(segment)) {
                            serialNos.add(segment);
                            foundSerials.add(segment);
                            logger.info("findSerialNosInClasspath: found serial: " + segment);
                            break; // 見つかったら次のリソースへ
                        }
                    }
                } catch (IOException e) {
                    logger.debug("findSerialNosInClasspath: IOException for resource: {}", e.getMessage());
                    // リソース処理エラーは無視して続行
                }
            }

        } catch (Exception e) {
            logger.debug("Error finding serial numbers in classpath: {}", e.getMessage());
            logger.trace("Stack trace:", e);
        }

        logger.debug("findSerialNosInClasspath: returning serials: {}", serialNos);
        return serialNos;
    }

    protected static boolean isSerialNoDir(File file) {
        if (null == file) {
            return false;
        }

        if (false == file.exists()) {
            return false;
        }

        if (false == file.isDirectory()) {
            return false;
        }

        if (false == isSerialNoVal(file.getName())) {
            return false;
        }

        return true;
    }

    protected static boolean isSerialNoVal(String serialNo) {
        if (StringUtils.isEmpty(serialNo)) {
            return false;
        }
        return serialNo.matches(REGEX);
    }

    public String getSerialNo() {
        return context.getSerialNo();
    }

    /**
     * Resource からStencilSettingsYmlを読み込む
     * 
     * @param resource
     *            Spring Resource
     * @return StencilSettingsYml、または null
     */
    private StencilSettingsYml loadStencilSettingsFromResource(Resource resource) {

        try (InputStream inputStream = resource.getInputStream()) {
            LoaderOptions loaderOptions = new LoaderOptions();
            Yaml yaml = new Yaml(loaderOptions);
            StencilSettingsYml settings = yaml.loadAs(inputStream, StencilSettingsYml.class);

            // NOTE:
            // 親設定のマージはgetStencilSettings()のmergeParentStencilSettingsUnified()で統一的に処理されます

            return settings;
        } catch (Exception e) {
            logger.info("Error loading stencil settings from resource " + resource.getDescription() + ": "
                    + e.getMessage());
            return null;
        }
    }

    /**
     * Resourceを一時ファイルに展開
     * 
     * @param resource
     *            Spring Resource
     * @param templateFileName
     *            テンプレートファイル名
     * @return 一時ファイル
     */
    private File extractResourceToTempFile(Resource resource, String templateFileName) throws Exception {
        File tempFile = File.createTempFile("template-" + templateFileName.replace(".", "-"), ".tmp");
        tempFile.deleteOnExit();

        try (InputStream inputStream = resource.getInputStream();
                FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            inputStream.transferTo(outputStream);
        }

        // 一時ファイル名と元のテンプレートファイル名のマッピングを保存
        tempFileToOriginalMap.put(tempFile.getName(), templateFileName);

        logger.info("Extracted template to temp file: " + tempFile.getAbsolutePath());
        return tempFile;
    }

    /**
     * ResourcePatternResolverを使用してステンシルテンプレートファイルリストを取得
     * 
     * @return テンプレートファイル名のリスト（stencil-settings.ymlは除外）
     */
    private List<String> getStencilTemplateFiles() {
        List<String> templateFiles = new ArrayList<>();
        Set<String> foundFileNames = new HashSet<>(); // 重複排除のため

        try {
            String stencilCanonicalName = context.getStencilCanonicalName();
            // 正規名パス検証
            SanitizeUtil.sanitizeCanonicalPath(stencilCanonicalName);
            String serialNo = context.getSerialNo();
            if (!StringUtils.isEmpty(serialNo) && !"*".equals(serialNo)) {
                SanitizeUtil.sanitizeIdentifierAllowWildcard(serialNo);
            }

            // デバッグ用ファイル出力
            try {
                java.io.FileWriter fw = new java.io.FileWriter("logs/template-debug.log", true);
                fw.write("\n=== Layered template search for stencil: " + stencilCanonicalName + " serial: " + serialNo
                        + " ===\n");
                fw.close();
            } catch (Exception e) {
                /* ignore */ }

            logger.debug("=== Layered template search for stencil: {} serial: {} ===",
                    SanitizeUtil.forLog(stencilCanonicalName), SanitizeUtil.forLog(serialNo));

            // Layer 1: Filesystem stencils (既存の/apps/mste/stencil)
            searchFilesystemTemplates(templateFiles, foundFileNames, stencilCanonicalName, serialNo);

            // Layer 2: Classpath stencils (旧stencil-samples)
            searchClasspathTemplates(templateFiles, foundFileNames, stencilCanonicalName, serialNo);

            logger.debug("=== Total template files found (after duplicate check): {} ===", templateFiles.size());
            for (String file : templateFiles) {
                logger.info("Template file: {}", SanitizeUtil.forLog(file));
            }

        } catch (Exception e) {
            logger.error("Error in getStencilTemplateFiles: {}", e.getMessage(), e);
        }

        return templateFiles;
    }

    @SuppressWarnings("lgtm[java/path-injection]")
    private void searchFilesystemTemplates(List<String> templateFiles, Set<String> foundFileNames,
            String stencilCanonicalName, String serialNo) {
        // NOTE: Paths are validated through constructSecurePath to prevent path
        // traversal
        // レイヤード検索: user → standard の順（findStencilSettingsInLayer()と同じロジック）
        String[] layerDirs = {
                StorageConfig.getUserStencilDir(),
                StorageConfig.getStandardStencilDir()
        };

        for (String layerDir : layerDirs) {
            if (StringUtils.isEmpty(layerDir) || layerDir.startsWith("classpath:")) {
                continue;
            }

            try {
                String stencilPath = stencilCanonicalName;
                if (!StringUtils.isEmpty(serialNo) && !"*".equals(serialNo)) {
                    stencilPath = stencilPath + "/" + serialNo;
                }
                File serialDir = constructSecurePath(layerDir, stencilPath);
                logger.debug("Searching filesystem layer: {}", SanitizeUtil.forLog(serialDir.getAbsolutePath()));

                if (serialDir.exists() && serialDir.isDirectory()) {
                    // serialNoディレクトリ配下の全ファイルを再帰的に取得（絶対パスで）
                    List<File> files = FileUtil.getFiles(serialDir);
                    logger.debug("Found {} total files in filesystem layer: {}", files.size(), layerDir);

                    for (File file : files) {
                        String fileName = file.getName();

                        // stencil-settings.ymlは除外
                        if (fileName.equals("stencil-settings.yml")) {
                            continue;
                        }

                        try {
                            // 相対パスを計算（serialNoディレクトリからの相対パス）
                            String relativePath = serialDir.toPath().relativize(file.toPath()).toString();
                            // Windows パス区切りをUnix形式に統一
                            relativePath = relativePath.replace('\\', '/');

                            // 重複チェックは相対パスで行う（同名ファイルが異なるディレクトリにある場合を考慮）
                            if (!foundFileNames.contains(relativePath)) {
                                templateFiles.add(file.getCanonicalPath());
                                foundFileNames.add(relativePath);
                                logger.debug("FILESYSTEM found: {}", SanitizeUtil.forLog(relativePath));
                            }
                        } catch (IOException e) {
                            logger.warn("Error processing file {}: {}", SanitizeUtil.forLog(file.getAbsolutePath()),
                                    SanitizeUtil.forLog(e.getMessage()));
                        }
                    }

                    // ファイルが見つかったらこのレイヤーで終了（上位レイヤーが優先）
                    if (!templateFiles.isEmpty()) {
                        logger.debug("Found templates in layer: {}, stopping search", layerDir);
                        return;
                    }
                } else {
                    logger.debug("FILESYSTEM serial directory not found: {}",
                            SanitizeUtil.forLog(serialDir.getAbsolutePath()));
                }
            } catch (Exception e) {
                logger.error("Error searching filesystem layer {}: {}", layerDir, e.getMessage(), e);
            }
        }
    }

    private void searchClasspathTemplates(List<String> templateFiles, Set<String> foundFileNames,
            String stencilCanonicalName, String serialNo) {
        try {
            if (resourcePatternResolver == null) {
                logger.info("ResourcePatternResolver not available for classpath search");
                return;
            }

            // serialNoディレクトリ配下の全ファイルを検索（**で再帰検索）
            String searchPattern = "classpath*:promarker/stencil/samples/" + stencilCanonicalName + "/" + serialNo
                    + "/**";
            logger.info("Searching CLASSPATH layer: {}", SanitizeUtil.forLog(searchPattern));

            Resource[] resources = resourcePatternResolver.getResources(searchPattern);
            logger.info("Found " + resources.length + " classpath resources");

            for (Resource resource : resources) {
                try {
                    String filename = resource.getFilename();
                    if (filename == null ||
                            filename.equals("stencil-settings.yml") ||
                            resource.getURI().toString().endsWith("/")) {
                        continue;
                    }

                    // リソースURIから相対パスを抽出
                    String uri = resource.getURI().toString();
                    String basePath = "promarker/stencil/samples/" + stencilCanonicalName + "/" + serialNo + "/";
                    int baseIndex = uri.indexOf(basePath);
                    String relativePath = filename; // デフォルトはファイル名のみ

                    if (baseIndex >= 0) {
                        relativePath = uri.substring(baseIndex + basePath.length());
                        // URLエンコードされている場合はデコード
                        relativePath = java.net.URLDecoder.decode(relativePath, "UTF-8");
                    }

                    // 重複チェックは相対パスで行う
                    if (!foundFileNames.contains(relativePath)) {
                        // classpath resourceを一時ファイルに展開
                        File tempFile = extractResourceToTempFile(resource, relativePath);
                        if (tempFile != null) {
                            templateFiles.add(tempFile.getAbsolutePath());
                            foundFileNames.add(relativePath);
                            logger.info("CLASSPATH found: " + relativePath + " -> " + tempFile.getAbsolutePath());
                        }
                    }
                } catch (Exception e) {
                    logger.info("Error processing classpath resource: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            logger.info("Error searching classpath layer: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * ファイルパスから相対ファイル名を抽出（name用）
     * 
     * @param fullPath
     *            完全なファイルパス
     * @return 相対ファイル名
     */
    private String extractRelativeFileName(String fullPath) {
        if (StringUtils.isEmpty(fullPath)) {
            return "";
        }

        String baseDirPath = StorageUtil.getBaseDir();
        String stencilMasterDir = getStencilMasterStorageDir();

        // ファイルシステムパスの場合
        if (fullPath.startsWith(baseDirPath)) {
            try {
                return fullPath.substring(baseDirPath.length() + stencilMasterDir.length());
            } catch (StringIndexOutOfBoundsException e) {
                // fallback: ファイル名のみ
                return new File(fullPath).getName();
            }
        }

        // クラスパスから展開された一時ファイルの場合
        return new File(fullPath).getName();
    }

    /**
     * ファイルパスからテンプレートファイル名を抽出（cname用）
     * 
     * @param fullPath
     *            完全なファイルパス
     * @return テンプレートファイル名（serialNoディレクトリからの相対パス）
     */
    private String extractTemplateFileName(String fullPath) {
        if (StringUtils.isEmpty(fullPath)) {
            return "";
        }

        File file = new File(fullPath);
        String fileName = file.getName();

        // 一時ファイル名から元のテンプレートファイル名を取得
        if (tempFileToOriginalMap.containsKey(fileName)) {
            String originalFileName = tempFileToOriginalMap.get(fileName);
            logger.debug("Mapped temp file: {} -> {}", SanitizeUtil.forLog(fileName),
                    SanitizeUtil.forLog(originalFileName));
            return originalFileName;
        }

        // 一時ファイルの完全パスがマップにある場合（相対パス付き）
        if (tempFileToOriginalMap.containsKey(fullPath)) {
            String originalPath = tempFileToOriginalMap.get(fullPath);
            logger.debug("Mapped temp file path: {} -> {}", SanitizeUtil.forLog(fullPath),
                    SanitizeUtil.forLog(originalPath));
            return originalPath;
        }

        String serialDirPath = getStencilAndSerialStorageDir();
        File serialDir = new File(serialDirPath);

        // ファイルシステムパスの場合：serialNoディレクトリからの相対パスを計算
        if (fullPath.startsWith(serialDirPath)) {
            try {
                String relativePath = serialDir.toPath().relativize(file.toPath()).toString();
                // Windowsパス区切りをUnix形式に統一
                relativePath = relativePath.replace('\\', '/');
                logger.info("Extracted relative path: {} from {}", SanitizeUtil.forLog(relativePath),
                        SanitizeUtil.forLog(fullPath));
                return relativePath;
            } catch (Exception e) {
                logger.info("Error extracting relative path: " + e.getMessage());
                return fileName;
            }
        }

        // クラスパスから展開された一時ファイルの場合（マッピングが見つからない場合）
        logger.debug("Using filename only for: {}", SanitizeUtil.forLog(fullPath));
        return fileName;
    }
}
