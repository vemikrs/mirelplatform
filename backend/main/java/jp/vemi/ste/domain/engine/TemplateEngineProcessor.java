/*
 * Copyright(c) 2018 mirelplatform.
 */
package jp.vemi.ste.domain.engine;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.ConstructorException;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.TemplateException;
import freemarker.template.TemplateExceptionHandler;
import groovy.lang.Tuple3;
import jp.vemi.framework.exeption.MirelApplicationException;
import jp.vemi.framework.exeption.MirelSystemException;
import jp.vemi.framework.util.CloseableUtil;
import jp.vemi.framework.util.DateUtil;
import jp.vemi.framework.util.ModelUtil;
import jp.vemi.framework.util.ResourceUtil;
import jp.vemi.framework.util.StorageUtil;
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

    protected Configuration cfg = null;
    protected static final String STENCIL_EXTENSION = ".ftl";
    protected static final String REGEX = "[0-9]{6}[A-Z]+";
    protected boolean isLegacy = true;

    protected static Logger logger = Logger.getLogger(TemplateEngineProcessor.class.getName());

    /**
     * default constructor.
     * @param context {@link SteContext}
     * @return instance
     */
    public static TemplateEngineProcessor create(final SteContext context) {
        final TemplateEngineProcessor instance = new TemplateEngineProcessor();

        // context.
        instance.context = context;

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
        final List<String> stencilFileNames = StorageUtil.getFiles(getStencilAndSerialStorageDir());

        // ignore settings file.
        if (false == stencilFileNames.remove(new File(
                StorageUtil.getBaseDir() + getStencilAndSerialStorageDir() + "/stencil-settings.yml")
                        .getAbsolutePath())) {
            throw new MirelSystemException("ステンシル定義が行方不明です。。", null);
        }

        // initialize configuration object.
        createConfiguration();

        // generate.
        for (final String stencilFileName : stencilFileNames) {

            final String name = stencilFileName.substring(
                StorageUtil.getBaseDir().length() + getStencilMasterStorageDir().length());
            final String cname = stencilFileName.substring(
                StorageUtil.getBaseDir().length() + getStencilAndSerialStorageDir().length());

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
            cfg.setDirectoryForTemplateLoading(new File(StorageUtil.getBaseDir() + getStencilAndSerialStorageDir()));
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

        // Validate.
        Assert.notNull(stencilName, "stencil name must not be null");

        // Get template resource.
        String stencilResource = getResourceWithParseLine(
                StorageUtil.parseToCanonicalPath(StringUtils.join(getStencilAndSerialStorageDir(), "/", stencilName)));

        // Stop if skip target.
        if (StringUtils.isEmpty(stencilResource) || "skip:file".equals(stencilResource)) {
            return null;
        }

        // Create template.
        try {
            return cfg.getTemplate(stencilName);
        } catch (ParseException e) {
            String message = e.getLocalizedMessage();
            throw new MirelSystemException("テンプレートの解析に失敗しました。" + stencilName + " Couase: " + message, e);
        } catch (IOException e) {
            throw new MirelSystemException("IOException in" + stencilName, e);
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

        // validate
        File file = new File(
                StorageUtil.getBaseDir() + getStencilAndSerialStorageDir() + "/stencil-settings.yml");
        if(false == file.exists()) {
            throw new MirelSystemException(
                    "ステンシル定義が見つかりません。ファイル：" + context.getStencilCanonicalName() + "/" + file.getName(), null);
        };

        final File dir = StorageUtil.getFile(getStencilAndSerialStorageDir());

        StencilSettingsYml settings = null;
        File current = dir;
        int stencilDirLength = StorageUtil.getFile(getStencilMasterStorageDir()).getAbsolutePath().length();
        while (true) {

            File[] currentFiles = current.listFiles(new FilenameFilter(){
                @Override
                public boolean accept(File dir, String name) {
                    if(name.endsWith("stencil-settings.yml")) {
                        return true;
                    } else {
                        return false;
                    }
                }
            });

            if (currentFiles.length == 0) {
                if (current.getAbsolutePath().length() <=stencilDirLength) {
                    break;
                } else {
                    current = current.getParentFile();
                    continue;
                }
            }

            for(File currentFile : currentFiles) {
                StencilSettingsYml currSettings = getSsYmlRecurive(currentFile);
                if (null == settings) {
                    settings = currSettings;
                } else {
                    // append only dataelement
                    settings.appendDataElementSublist(currSettings.getStencil().getDataDomain());
                }
            }

            if (current.getParentFile().getAbsolutePath().length() <= stencilDirLength) {
                break;
            } else {
                current = current.getParentFile();
            }

        }

        return settings;
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

    public static String getAppStorageDir(){
        final String appDir = "/apps/mste";
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
}
