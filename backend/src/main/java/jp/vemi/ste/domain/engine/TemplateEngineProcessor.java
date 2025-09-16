/*
 * Copyright(c) 2018 mirelplatform.
 */
package jp.vemi.ste.domain.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
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
import java.util.logging.Level;
import java.util.logging.Logger;

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

    protected static Logger logger = Logger.getLogger(TemplateEngineProcessor.class.getName());

    /**
     * default constructor.
     * @param context {@link SteContext}
     * @return instance
     */
    public static TemplateEngineProcessor create(final SteContext context) {
        return create(context, null);
    }

    /**
     * constructor with ResourcePatternResolver.
     * @param context {@link SteContext}
     * @param resourcePatternResolver {@link ResourcePatternResolver}
     * @return instance
     */
    public static TemplateEngineProcessor create(final SteContext context, final ResourcePatternResolver resourcePatternResolver) {
        final TemplateEngineProcessor instance = new TemplateEngineProcessor();

        // context.
        instance.context = context;
        
        // resourcePatternResolver with null check
        instance.resourcePatternResolver = resourcePatternResolver;
        
        if (resourcePatternResolver == null) {
            System.out.println("WARNING: ResourcePatternResolver is null in TemplateEngineProcessor.create()");
            System.out.println("This may cause classpath resource search to fail");
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
     * @return 生成結果Path
     */
    public String execute(final String generateId) {

        // validate stencil-settings.yml
        final Tuple3<List<String>, List<String>, List<String>> validRets = validate();
        if (false == validRets.getV3().isEmpty()) {
            throw new MirelApplicationException(validRets.getV3());
        }

        validRets.getV2().forEach(logger::warning);
        validRets.getV2().forEach(logger::info);

        // output dir
        final String outputDir = createOutputFileDir(StringUtils.isEmpty(generateId) ? createGenerateId() : generateId);

        // parse content.
        if (isLegacy) {
            parseTypes();
        }

        // binding.
        prepareBind();

        // get file items.
        final List<String> stencilFileNames = getStencilTemplateFiles();

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

            if (cname.startsWith("\\.")) {
                // 
                logger.log(Level.INFO, "folder starts with '.': " + cname);
                continue;
            }
            final freemarker.template.Template template = newTemplateFileSpec3(cname);

            if (null == template) {
                // テンプレートのインスタンスがNullの場合、生成対象外と判断されたもの。
                logger.log(Level.INFO, "template is null.");
                continue;
            }

            final File outputFile = bindFileName(cname, new File(outputDir));

            File parentDir = outputFile.getParentFile();
            try {
              Files.createDirectories(parentDir.toPath());
            } catch (IOException e) {
              throw new MirelSystemException(e);
            }
        
            try {
                template.process(commonBinds ,new FileWriter(outputFile));
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

    private void createConfiguration() {
        // configuration.
        cfg = new Configuration(Configuration.VERSION_2_3_29);
        
        try {
            // FreeMarkerのMultiTemplateLoaderを使用してファイルシステムとクラスパスの両方をサポート
            List<TemplateLoader> loaders = new ArrayList<>();
            
            // Layer 1: ファイルシステムローダー（既存の機能）
            File stencilDir = new File(StorageUtil.getBaseDir() + getStencilAndSerialStorageDir());
            if (stencilDir.exists()) {
                loaders.add(new FileTemplateLoader(stencilDir));
                System.out.println("Added filesystem template loader: " + stencilDir.getAbsolutePath());
            } else {
                System.out.println("Filesystem stencil directory not found: " + stencilDir.getAbsolutePath());
            }
            
            // Layer 2: クラスパスローダー（新機能）
            if (resourcePatternResolver != null) {
                // SpringClasspathローダーは複数のクラスパスパスをサポートできる
                String stencilPath = "promarker/stencil/samples" + context.getStencilCanonicalName() + "/" + context.getSerialNo();
                ClassTemplateLoader classpathLoader = new ClassTemplateLoader(getClass().getClassLoader(), stencilPath);
                loaders.add(classpathLoader);
                System.out.println("Added classpath template loader: " + stencilPath);
            }
            
            // Layer 3: 一時ファイル用テンプレートローダー
            if (!tempFileToOriginalMap.isEmpty()) {
                File tempDir = new File(System.getProperty("java.io.tmpdir"));
                FileTemplateLoader tempFileLoader = new FileTemplateLoader(tempDir);
                loaders.add(tempFileLoader);
                System.out.println("Added temp file template loader: " + tempDir.getAbsolutePath());
            }
            
            if (!loaders.isEmpty()) {
                TemplateLoader multiLoader = new MultiTemplateLoader(loaders.toArray(new TemplateLoader[0]));
                cfg.setTemplateLoader(multiLoader);
                System.out.println("FreeMarker configured with " + loaders.size() + " template loaders");
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
                refs.put((String)map.get("id"), (String)map.get("reference"));
            }
        }

        return refs;
    }
    /**
     * validate. <br/>
     * @param storagePath ストレージ上のパス
     * @param stencilName ステンシル名
     */
    protected Tuple3<List<String>, List<String>, List<String>> validate() {

        final StencilSettingsYml settings = getStencilSettings();
        System.out.println("validate with settings: ");
        System.out.println(settings);

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
        if(item.containsKey("reference")) {
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

            if(false == commonBinds.containsKey(entry.getValue())){
                // reference value not found.
                return;
            }

            // copy reference value.
            commonBinds.put(entry.getKey(), commonBinds.get(entry.getValue()));
        });

    }

    /**
     * ファイル名にバインドをアタッチします。<br/>
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
     * @param binding
     */
    private void appendSubInvoker(final EngineBinds binding) {
        // keys のうち コンテンツ対象の展開
        for (final Entry<String, Object> entry : commonBinds.entrySet()) {
            if (entry.getValue() instanceof StringContent) {
                // StringContent
                ((StringContent)entry.getValue()).appendSubInvoker(entry.getKey(), binding);
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
        System.out.println("=== newTemplateFileSpec3: " + stencilName + " ===");

        // Validate.
        Assert.notNull(stencilName, "stencil name must not be null");

        // 一時ファイル名の場合は、元のファイル名を取得
        String actualTemplateName = stencilName;
        if (tempFileToOriginalMap.containsKey(stencilName)) {
            actualTemplateName = tempFileToOriginalMap.get(stencilName);
            System.out.println("Mapping temp file '" + stencilName + "' to original name '" + actualTemplateName + "'");
        } else if (stencilName.endsWith(".tmp")) {
            // 一時ファイルだが、マッピングが見つからない場合は一時ファイル名をそのまま使用
            actualTemplateName = stencilName;
            System.out.println("Using temp file name directly: " + stencilName);
        }

        // FreeMarkerのConfigurationからテンプレートを取得
        // MultiTemplateLoaderが自動的にファイルシステムとクラスパスを検索する
        try {
            freemarker.template.Template template = cfg.getTemplate(actualTemplateName);
            System.out.println("Successfully loaded template via MultiTemplateLoader: " + actualTemplateName);
            return template;
        } catch (TemplateNotFoundException e) {
            System.out.println("Template not found: " + actualTemplateName);
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
        return getStencilStorageDir() + "/" + context.getSerialNo();
    }

    public String getStencilStorageDir() {

        // validate.
        Assert.notNull(context, "context");
        Assert.hasText(context.getStencilCanonicalName(), "stencilCanonicalName must not be empty");

        Assert.isTrue(context.getStencilCanonicalName().startsWith("/"),
            "stencilCanonicalName must be start with '/'");

        final String dir = StringUtils.join(getStencilMasterStorageDir(), context.getStencilCanonicalName());
        return dir;
    }

    /** 
     * 
     */
    protected void parseTypes() {

        for(final Map.Entry<String, Object> entry: context.entrySet()) {
            if(null == entry) {
                continue;
            }
            if(StringUtils.isEmpty(entry.getKey())) {
                continue;
            }
            if(null == entry.getValue()) {
                continue;
            }

            if(entry.getValue() instanceof String) {
                final StringContent content = StringContent.string(entry.getValue().toString());
                context.put(entry.getKey(), content);
                continue;
            }
        }
    }

    protected StencilSettingsYml getSsYmlRecurive(final File file) {

        if(false == file.exists()) {
            throw new MirelSystemException("ステンシル定義が見つかりません。ファイル：" + context.getStencilCanonicalName() + "/" + file.getName() , null);
        }

        // load as stencil-settings.
        StencilSettingsYml settings = null;
        try(InputStream stream = new FileSystemResource(file).getInputStream()) {
            LoaderOptions options = new LoaderOptions();
            Yaml yaml = new Yaml(options);
            settings = yaml.loadAs(stream, StencilSettingsYml.class);
        } catch (final ConstructorException e) {
            e.printStackTrace();
            String msg = "yamlの読込でエラーが発生しました。";
            if(isDebugMode()) {
                msg += "debug log:" + e.getLocalizedMessage();
            }
            throw new MirelApplicationException(msg, e);
        } catch (final IOException e) {
            e.printStackTrace();
            throw new MirelSystemException("yamlの読込で入出力エラーが発生しました。", e);
        }

        return settings;

    }

    protected boolean isDebugMode() {
        return true;
    }

    public StencilSettingsYml getStencilSettings() {
        // レイヤード検索: ユーザー → 標準 → サンプル の順で検索
        StencilSettingsYml settings = findStencilSettingsInLayers();
        if (settings == null) {
            throw new MirelSystemException(
                "ステンシル定義が見つかりません。ステンシル：" + context.getStencilCanonicalName(), null);
        }
        return settings;
    }

    /**
     * レイヤード検索でstencil-settings.ymlを検索する
     * @return 見つかったStencilSettingsYml、または null
     */
    private StencilSettingsYml findStencilSettingsInLayers() {
        System.out.println("=== DEBUG findStencilSettingsInLayers ===");
        System.out.println("context.getStencilCanonicalName(): " + context.getStencilCanonicalName());
        System.out.println("resourcePatternResolver: " + resourcePatternResolver);
        
        // 優先度順にレイヤーを検索: ユーザー → 標準 → サンプル
        String[] searchLayers = {
            StorageConfig.getUserStencilDir(),
            StorageConfig.getStandardStencilDir(),
            StorageConfig.getSamplesStencilDir()
        };
        
        for (String layerDir : searchLayers) {
            StencilSettingsYml settings = findStencilSettingsInLayer(layerDir);
            if (settings != null) {
                System.out.println("Found stencil settings in layer: " + layerDir);
                return settings;
            }
        }
        
        System.out.println("No stencil settings found in any layer");
        return null;
    }

    /**
     * 指定されたレイヤーディレクトリでstencil-settings.ymlを検索する
     * @param layerDir レイヤーディレクトリ
     * @return 見つかったStencilSettingsYml、または null
     */
    private StencilSettingsYml findStencilSettingsInLayer(String layerDir) {
        if (StringUtils.isEmpty(layerDir)) {
            return null;
        }
        
        // classpath: プレフィックスの場合はclasspath検索
        if (layerDir.startsWith("classpath:")) {
            return findStencilSettingsInClasspath(layerDir);
        }
        
        // ファイルシステム検索
        return findStencilSettingsInFileSystem(layerDir);
    }

    /**
     * classpathからstencil-settings.ymlを検索する
     * ResourcePatternResolverを使用して動的検索を行う
     * @param classpathLocation classpath:で始まるパス
     * @return 見つかったStencilSettingsYml、または null  
     */
    private StencilSettingsYml findStencilSettingsInClasspath(String classpathLocation) {
        try {
            System.out.println("=== DEBUG findStencilSettingsInClasspath ===");
            System.out.println("classpathLocation: " + classpathLocation);
            System.out.println("resourcePatternResolver is null: " + (resourcePatternResolver == null));
            
            // ResourcePatternResolverが利用可能な場合は動的検索を使用
            if (resourcePatternResolver != null) {
                System.out.println("Using ResourcePatternResolver for dynamic search");
                return findStencilSettingsWithResourcePatternResolver(classpathLocation);
            }
            
            // フォールバック: 従来の固定パス検索
            System.out.println("Fallback to fixed path search");
            return findStencilSettingsWithFixedPath(classpathLocation);
            
        } catch (Exception e) {
            System.out.println("Error finding stencil settings in classpath: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * ResourcePatternResolverを使用してstencil-settings.ymlを動的検索
     * @param classpathLocation classpath:で始まるパス
     * @return 見つかったStencilSettingsYml、または null
     */
    private StencilSettingsYml findStencilSettingsWithResourcePatternResolver(String classpathLocation) {
        try {
            // 指定されたstencilCanonicalNameに対応するstencil-settings.ymlを検索
            String searchPattern = classpathLocation + context.getStencilCanonicalName() + "/**/stencil-settings.yml";
            System.out.println("Searching for stencil settings with pattern: " + searchPattern);
            
            Resource[] resources = resourcePatternResolver.getResources(searchPattern);
            System.out.println("Found " + resources.length + " stencil-settings.yml resources for " + context.getStencilCanonicalName());
            
            // 見つかったリソースから最初の有効なものを使用
            for (Resource resource : resources) {
                try {
                    StencilSettingsYml settings = loadStencilSettingsFromResource(resource);
                    if (settings != null && settings.getStencil() != null && settings.getStencil().getConfig() != null) {
                        // serialNoが指定されている場合はマッチするかチェック
                        if (!StringUtils.isEmpty(context.getSerialNo()) && !"*".equals(context.getSerialNo())) {
                            String configSerial = settings.getStencil().getConfig().getSerial();
                            if (!context.getSerialNo().equals(configSerial)) {
                                continue; // serialNoが一致しない場合はスキップ
                            }
                        }
                        System.out.println("Found matching stencil settings: " + resource.getURI());
                        return settings;
                    }
                } catch (Exception resourceException) {
                    System.out.println("Could not load stencil settings from resource " + resource.getURI() + ": " + resourceException.getMessage());
                }
            }
            
            return null;
        } catch (Exception e) {
            System.out.println("Error in ResourcePatternResolver search: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 従来の固定パス検索（フォールバック）
     * @param classpathLocation classpath:で始まるパス
     * @return 見つかったStencilSettingsYml、または null
     */
    private StencilSettingsYml findStencilSettingsWithFixedPath(String classpathLocation) {
        try {
            // シリアル番号が指定されている場合は直接検索
            if (!StringUtils.isEmpty(context.getSerialNo()) && !"*".equals(context.getSerialNo())) {
                String resourcePath = classpathLocation.substring("classpath:".length()) 
                    + context.getStencilCanonicalName() + "/" + context.getSerialNo() + "/stencil-settings.yml";
                
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
            List<String> availableSerials = findAvailableSerialsInClasspath(classpathLocation, context.getStencilCanonicalName());
            for (String serial : availableSerials) {
                String resourcePath = classpathLocation.substring("classpath:".length()) 
                    + context.getStencilCanonicalName() + "/" + serial + "/stencil-settings.yml";
                
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
            logger.log(Level.WARNING, "classpath検索でエラーが発生: " + classpathLocation, e);
        }
        
        return null;
    }

    /**
     * classpathリソースからStencilSettingsYmlを読み込む
     * @param resourcePath リソースパス
     * @return 読み込まれたStencilSettingsYml、または null
     */
    private StencilSettingsYml loadStencilSettingsFromClasspath(String resourcePath) {
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
            logger.log(Level.FINE, "classpathリソース読み込み失敗: " + resourcePath, e);
        }
        return null;
    }

    /**
     * classpathから利用可能なシリアル番号一覧を検索する
     * @param classpathLocation classpath:で始まるパス
     * @param stencilCanonicalName ステンシル正規名
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
            String[] commonSerials = {"250913A", "191207A", "201019A"}; // よく使われるシリアル番号パターン
            
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
            logger.log(Level.FINE, "シリアル番号検索でエラー: " + classpathLocation, e);
        }
        
        return serials;
    }

    /**
     * ファイルシステムからstencil-settings.ymlを検索する
     * @param layerDir レイヤーディレクトリ
     * @return 見つかったStencilSettingsYml、または null
     */
    private StencilSettingsYml findStencilSettingsInFileSystem(String layerDir) {
        try {
            System.out.println("=== DEBUG findStencilSettingsInFileSystem ===");
            System.out.println("layerDir: " + layerDir);
            System.out.println("stencilCanonicalName: " + context.getStencilCanonicalName());
            
            // パス構築の改善
            String stencilPath;
            if (layerDir.endsWith("/")) {
                stencilPath = layerDir + context.getStencilCanonicalName().substring(1); // 先頭の"/"を除去
            } else {
                stencilPath = layerDir + context.getStencilCanonicalName(); 
            }
            
            // serialNoが指定されている場合はそれも含める
            if (!StringUtils.isEmpty(context.getSerialNo()) && !"*".equals(context.getSerialNo())) {
                stencilPath = stencilPath + "/" + context.getSerialNo();
            }
            
            File settingsFile = new File(stencilPath + "/stencil-settings.yml");
            System.out.println("Searching settings file: " + settingsFile.getAbsolutePath());
            
            if (settingsFile.exists() && settingsFile.isFile()) {
                System.out.println("Found stencil-settings.yml: " + settingsFile.getAbsolutePath());
                return getSsYmlRecurive(settingsFile);
            } else {
                System.out.println("Settings file not found: " + settingsFile.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("Error in filesystem search: " + e.getMessage());
            logger.log(Level.WARNING, "ファイルシステム検索でエラーが発生: " + layerDir, e);
        }
        
        return null;
    }

    protected static String createGenerateId() {
        return DateUtil.toString(new Date(), "yyMMddHHmmssSSS");
    }

    protected static String createOutputFileDir(final String generateId) {
        return convertStorageToFileDir(getOutputBaseStorageDir())
                + "/" + generateId;
    }

    public static String getStencilMasterStorageDir(){
        final String stencilMasterDir ="/stencil";
        return StringUtils.join(getAppStorageDir(), stencilMasterDir);
    }

    // TODO: ProMarkerStorageConfig -> ProMarkerStorageUtil
    public static String getAppStorageDir(){
        final String appDir = "/apps/promarker";
        return appDir;
    }

    public static String getOutputBaseStorageDir() {
        return getAppStorageDir() + "/output";
    }

    public static String convertStorageToFileDir(final String storageDir) {
        return StorageUtil.getBaseDir() + "/" + storageDir;
    }

    public List<String> getSerialNos() {

        final File sdir = StorageUtil.getFile(getStencilStorageDir());
        final File[] sdirFiles = sdir.listFiles();

        final List<String> serialNos = Lists.newArrayList();

        // for npe
        if (null == sdirFiles) {
            return serialNos;
        }

        for (File sdirFile : sdirFiles) {
            if(isSerialNoDir(sdirFile)) {
                serialNos.add(sdirFile.getName());
            }
        }

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
     * @param resource Spring Resource
     * @return StencilSettingsYml、または null
     */
    private StencilSettingsYml loadStencilSettingsFromResource(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            LoaderOptions loaderOptions = new LoaderOptions();
            Yaml yaml = new Yaml(loaderOptions);
            return yaml.loadAs(inputStream, StencilSettingsYml.class);
        } catch (Exception e) {
            System.out.println("Error loading stencil settings from resource " + resource.getDescription() + ": " + e.getMessage());
            return null;
        }
    }

    /**
     * Resourceを一時ファイルに展開
     * @param resource Spring Resource
     * @param templateFileName テンプレートファイル名
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
        
        System.out.println("Extracted template to temp file: " + tempFile.getAbsolutePath());
        return tempFile;
    }

    /**
     * ResourcePatternResolverを使用してステンシルテンプレートファイルリストを取得
     * @return テンプレートファイル名のリスト（stencil-settings.ymlは除外）
     */
    private List<String> getStencilTemplateFiles() {
        List<String> templateFiles = new ArrayList<>();
        Set<String> foundFileNames = new HashSet<>(); // 重複排除のため
        
        try {
            String stencilCanonicalName = context.getStencilCanonicalName();
            String serialNo = context.getSerialNo();
            
            System.out.println("=== Layered template search for stencil: " + stencilCanonicalName + " serial: " + serialNo + " ===");
            
            // Layer 1: Filesystem stencils (既存の/apps/mste/stencil)
            searchFilesystemTemplates(templateFiles, foundFileNames, stencilCanonicalName, serialNo);
            
            // Layer 2: Classpath stencils (旧stencil-samples)
            searchClasspathTemplates(templateFiles, foundFileNames, stencilCanonicalName, serialNo);
            
            System.out.println("=== Total template files found: " + templateFiles.size() + " ===");
            for (String file : templateFiles) {
                System.out.println("Template file: " + file);
            }
            
        } catch (Exception e) {
            System.out.println("Error in getStencilTemplateFiles: " + e.getMessage());
            e.printStackTrace();
        }
        
        return templateFiles;
    }
    
    private void searchFilesystemTemplates(List<String> templateFiles, Set<String> foundFileNames,
                                          String stencilCanonicalName, String serialNo) {
        try {
            String fullPath = getStencilAndSerialStorageDir();
            System.out.println("Searching filesystem layer: " + StorageUtil.getBaseDir() + fullPath);
            
            if (new File(StorageUtil.getBaseDir() + fullPath).exists()) {
                List<String> files = StorageUtil.getFiles(fullPath);
                
                for (String filePath : files) {
                    File file = new File(filePath);
                    String fileName = file.getName();
                    
                    if (!fileName.equals("stencil-settings.yml") && 
                        !foundFileNames.contains(fileName)) {
                        templateFiles.add(filePath);
                        foundFileNames.add(fileName);
                        System.out.println("FILESYSTEM found: " + fileName);
                    }
                }
            } else {
                System.out.println("FILESYSTEM directory not found: " + StorageUtil.getBaseDir() + fullPath);
            }
        } catch (Exception e) {
            System.out.println("Error searching filesystem layer: " + e.getMessage());
        }
    }
    
    private void searchClasspathTemplates(List<String> templateFiles, Set<String> foundFileNames,
                                        String stencilCanonicalName, String serialNo) {
        try {
            if (resourcePatternResolver == null) {
                System.out.println("ResourcePatternResolver not available for classpath search");
                return;
            }
            
            String searchPattern = "classpath*:promarker/stencil/samples/" + stencilCanonicalName + "/" + serialNo + "/**";
            System.out.println("Searching CLASSPATH layer: " + searchPattern);
            
            Resource[] resources = resourcePatternResolver.getResources(searchPattern);
            System.out.println("Found " + resources.length + " classpath resources");
            
            for (Resource resource : resources) {
                try {
                    String filename = resource.getFilename();
                    if (filename != null && 
                        !filename.equals("stencil-settings.yml") && 
                        !resource.getURI().toString().endsWith("/") &&
                        !foundFileNames.contains(filename)) {
                        
                        // classpath resourceを一時ファイルに展開
                        File tempFile = extractResourceToTempFile(resource, filename);
                        if (tempFile != null) {
                            templateFiles.add(tempFile.getAbsolutePath());
                            foundFileNames.add(filename);
                            System.out.println("CLASSPATH found: " + filename + " -> " + tempFile.getAbsolutePath());
                        }
                    }
                } catch (Exception e) {
                    System.out.println("Error processing classpath resource: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            System.out.println("Error searching classpath layer: " + e.getMessage());
        }
    }
    
    /**
     * ファイルパスから相対ファイル名を抽出（name用）
     * @param fullPath 完全なファイルパス
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
     * @param fullPath 完全なファイルパス
     * @return テンプレートファイル名
     */
    private String extractTemplateFileName(String fullPath) {
        if (StringUtils.isEmpty(fullPath)) {
            return "";
        }
        
        // 一時ファイル名から元のテンプレートファイル名を取得
        String fileName = new File(fullPath).getName();
        if (tempFileToOriginalMap.containsKey(fileName)) {
            String originalFileName = tempFileToOriginalMap.get(fileName);
            System.out.println("Mapped temp file: " + fileName + " -> " + originalFileName);
            return originalFileName;
        }
        
        String baseDirPath = StorageUtil.getBaseDir();
        String stencilAndSerialDir = getStencilAndSerialStorageDir();
        
        // ファイルシステムパスの場合
        if (fullPath.startsWith(baseDirPath)) {
            try {
                return fullPath.substring(baseDirPath.length() + stencilAndSerialDir.length());
            } catch (StringIndexOutOfBoundsException e) {
                // fallback: ファイル名のみ
                return fileName;
            }
        }
        
        // クラスパスから展開された一時ファイルの場合（マッピングが見つからない場合）
        return fileName;
    }
}
