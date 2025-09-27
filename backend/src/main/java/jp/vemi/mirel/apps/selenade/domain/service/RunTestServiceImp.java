/*
 * Copyright(c) 2015-2021 vemi/mirelplatform.
 */
package jp.vemi.mirel.apps.selenade.domain.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import javax.annotation.Generated;

import com.codeborne.selenide.SelenideConfig;
import com.codeborne.selenide.SelenideDriver;
import com.codeborne.selenide.SelenideElement;
import com.google.common.collect.Maps;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.openqa.selenium.OutputType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.ConstructorException;
import jp.vemi.extension.function_resolver.function.Function;
import jp.vemi.extension.function_resolver.main.FunctionResolver;
import jp.vemi.framework.exeption.MirelApplicationException;
import jp.vemi.framework.exeption.MirelSystemException;
import jp.vemi.framework.util.DateUtil;
import jp.vemi.framework.util.FileUtil;
import jp.vemi.framework.util.StorageUtil;
import jp.vemi.mirel.apps.selenade.domain.dto.RunTestParameter;
import jp.vemi.mirel.apps.selenade.domain.dto.RunTestResult;
import jp.vemi.mirel.apps.selenade.dto.ArTestRun;
import jp.vemi.mirel.apps.selenade.dto.evidence.Evidence;
import jp.vemi.mirel.apps.selenade.dto.yml.ArUsecase;
import jp.vemi.mirel.apps.selenade.dto.yml.ArSelenadePage.Page;
import jp.vemi.mirel.apps.selenade.dto.yml.ArUsecase.Action;
import jp.vemi.mirel.apps.selenade.evidencve.EvidenceManager;
import jp.vemi.mirel.apps.selenade.evidencve.EvidenceManager.ImageFile;
import jp.vemi.mirel.apps.selenade.dto.yml.ArConfig;
import jp.vemi.mirel.apps.selenade.dto.yml.ArData;
import jp.vemi.mirel.apps.selenade.dto.yml.ArScenario;
import jp.vemi.mirel.apps.selenade.dto.yml.ArSelenadePage;
import jp.vemi.mirel.foundation.abst.dao.repository.FileManagementRepository;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;

/**
 * {@link RunTestService テスト実行} の具象です。
 */
@Service
@Transactional
@Generated(value = {
        "jp.vemi.ste.domain.engine.TemplateEngineProcessor" }, comments = "Generated from /mirel/service:191207A")
public class RunTestServiceImp implements RunTestService {

    /** {@link FileManagementRepository} */
    @Autowired
    protected FileManagementRepository fileManagementRepository;

    /** 無視ファイルズ */
    private static final String[] IGNORE_FILES = { ".git", "README.md" };

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiResponse<RunTestResult> invoke(ApiRequest<RunTestParameter> parameter) {

        ApiResponse<RunTestResult> resp = ApiResponse.<RunTestResult>builder().build();
        resp.setData(RunTestResult.builder().build());
        exec(parameter, resp);
        return resp;

    }

    protected void exec(ApiRequest<RunTestParameter> parameter, ApiResponse<RunTestResult> resp) {
        final String testId = (String) parameter.getModel().params.get(0).get("testId");
        if (StringUtils.isEmpty(testId)) {
            resp.addError("testId が指定されていません。");
            return;
        }

        List<File> files = FileUtil.getFiles(StorageUtil.getFile("apps/apprunner/defs/" + testId), IGNORE_FILES);
        if (CollectionUtils.isEmpty(files)) {
            resp.addError("テスト定義ファイルが見つかりません。");
            return;
        }

        ArTestRun testRun = new ArTestRun();
        for (File file : files) {
            if (false != file.isDirectory()) {
                continue;
            }

            String fileName = file.getName();

            // Config.
            if (fileName.startsWith("config") && isYaml(fileName)) {
                ArConfig config;
                try {
                    config = getYaml(file, ArConfig.class);
                    validConfig(config, resp, fileName);
                    testRun.addConfig(config);
                    continue;
                } catch (Throwable e) {
                    resp.addError(e.getLocalizedMessage());
                }
            }

            // Data.
            if (fileName.startsWith("data-") && isYaml(fileName)) {
                ArData data;
                try {
                    data = getYaml(file, ArData.class);
                    validData(data, resp, fileName);
                    testRun.addData(data);
                    continue;
                } catch (Throwable e) {
                    resp.addError(e.getLocalizedMessage());
                }
            }

            // Usecase.
            if (fileName.startsWith("usecase-") && isYaml(fileName)) {
                ArUsecase usecase;
                try {
                    usecase = getYaml(file, ArUsecase.class);
                    validUsecase(usecase, resp, fileName);
                    testRun.addUsecase(usecase);
                    continue;
                } catch (Throwable e) {
                    resp.addError(e.getLocalizedMessage());
                }
            }

            // Page.
            if (fileName.startsWith("page-") && isYaml(fileName)) {
                ArSelenadePage page;
                try {
                    page = getYaml(file, ArSelenadePage.class);
                    validPage(page, resp, fileName);
                    testRun.addPage(page);
                    continue;
                } catch (Throwable e) {
                    resp.addError(e.getLocalizedMessage());
                }
            }

            // Scenario.
            if (fileName.startsWith("scenario-") && isYaml(fileName)) {
                ArScenario scenario;
                try {
                    scenario = getYaml(file, ArScenario.class);
                    validScenario(scenario, resp, fileName);
                    testRun.addScenario(scenario);
                } catch (Throwable e) {
                    resp.addError(e.getLocalizedMessage());
                }
            }
        }

        if (false == CollectionUtils.isEmpty(resp.getErrors())) {
            return;
        }

        // Environment settings.
        Map<String, ArConfig.Environment> environmentTable = Maps.newLinkedHashMap();
        Map<String, ArConfig.Server> appTable = Maps.newLinkedHashMap();
        for (ArConfig.Config arconfig : testRun.getConfig().values()) {
            for (ArConfig.Environment env : arconfig.getEnvironment()) {
                environmentTable.put(env.getId(), env);
                for (ArConfig.Server server : env.getServer()) {
                    appTable.put(server.getId(), server);
                }
            }
        }

        String run = createId();
        // Execute.
        for (Map.Entry<String, ArScenario.Scenario> entry : testRun.getScenarios().entrySet()) {
            ArScenario.Scenario scenario = entry.getValue();
            EvidenceManager eManager = EvidenceManager
                    .create("Run#" + run + " " + scenario.getId() + ":" + scenario.getName());
            Map<String, SelenideDriver> selDriverTable = Maps.newLinkedHashMap();
            for (ArScenario.Usecase usecase : scenario.getUsecase()) {
                Evidence evidence = Evidence.$evidenceHeader(usecase.getId());
                ArUsecase.Usecase defaultUsecase = testRun.getUsecases().get(usecase.getUsecaseId());
                ArUsecase.Usecase mergedUsecase = mergeUsecase(usecase, defaultUsecase);

                List<ArUsecase.Step> steps = mergedUsecase.getStep();
                for (ArUsecase.Step step : steps) {
                    log("Step: " + step.getId());
                    log("  IsSelenade?: " + step.isSelenade());
                    if (step.isSelenade()) {

                        ArUsecase.Selenade selenidePlugin = step.getPlugin().getSelenade();
                        ArSelenadePage.Page page = testRun.getPages().get(selenidePlugin.getPageId());
                        if (null == page) {
                            resp.addError(selenidePlugin.getPageId() + " の Page plugin が見つかりません。");
                            continue;
                        }

                        SelenideDriver selDriver;
                        if (selDriverTable.containsKey(page.getContextPath())) {
                            selDriver = selDriverTable.get(page.getContextPath());
                        } else {
                            ArConfig.Server server = appTable.get(page.getAppId());
                            SelenideConfig selConfig = new SelenideConfig();
                            selConfig.baseUrl(server.getUrl());
                            selDriver = new SelenideDriver(selConfig);
                            selDriver.open(page.getContextPath());
                            selDriverTable.put(page.getContextPath(), selDriver);
                        }

                        log("  Page: " + page.getName());
                        for (ArUsecase.Action ucaction : selenidePlugin.getAction()) {
                            ArUsecase.Action action = completeAction(ucaction, page);
                            log("    Action: " + action.getName());
                            String locator = action.getLocator();
                            Function function = FunctionResolver.resolveSingleFunction(locator);
                            SelenideElement sement = null;
                            switch (function.getFunctionName()) {
                                case "url":
                                    selDriver.open(function.getArgs().get("0").toString());
                                    break;
                                case "xpath":
                                    sement = selDriver.$x((String) function.getArgs().get("0"));
                                    break;
                                default:
                                    break;
                            }

                            if (isAction(action.getType())) { // TODO fix type condition
                                if (null == sement) {
                                    resp.addError("Selenide イベントの生成がされていません。アクション：" + action.getName());
                                } else {
                                    // input or click.
                                    switch (action.getType()) {
                                        case "input":
                                            String value = action.getValue();
                                            resolve(value);
                                            if (sement.exists()) {
                                                sement.setValue(value);
                                            } else {
                                                Boolean isIgnore = action.getIgnoreIfNotFound();
                                                if (null == isIgnore) {
                                                    isIgnore = false;
                                                }
                                                if (isIgnore) {
                                                    // TODO ok.
                                                    log("ignore target.");
                                                } else {
                                                    // TODO error.
                                                    log("element not found.");
                                                }
                                            }
                                            break;
                                        case "click":
                                            if (sement.exists()) {
                                                sement.click();
                                            } else {
                                                log("click function is not available.");
                                            }
                                            break;
                                        case "select":
                                            break;
                                        default:
                                            break;
                                    }
                                }
                            }
                            if (Boolean.TRUE == action.getSaveScreen() && sement != null && sement.exists()) {
                                File file = sement.screenshot();
                                eManager.append(evidence, ImageFile.as(file, StorageUtil.getBaseDir()
                                        + "/apps/apprunner/testrun/" + run + "/evidence/component/"));
                            }
                        }
                        File stepScreenshot = selDriver.screenshot(OutputType.FILE);
                        eManager.append(evidence, ImageFile.as(stepScreenshot,
                                StorageUtil.getBaseDir() + "/apps/apprunner/testrun/" + run + "/evidence/"));
                    }
                }
            }

            // Close driver.
            selDriverTable.values().stream().forEach(driver -> {
                if (driver.hasWebDriverStarted()) {
                    driver.close();
                }
            });

            eManager.buildAndPrint();
        }
    }

    private Action completeAction(Action action, Page page) {

        if (StringUtils.isEmpty(action.getActionTemplate())) {
            return action;
        }

        for (ArSelenadePage.Action pageAction : page.getAction()) {
            if (false == action.getActionTemplate().equals(pageAction.getId())) {
                continue;
            }

            if (StringUtils.isEmpty(action.getName())) {
                action.setName(pageAction.getName());
            }

            if (StringUtils.isEmpty(action.getType())) {
                action.setType(pageAction.getType());
            }

            if (StringUtils.isEmpty(action.getLocator())) {
                action.setLocator(pageAction.getLocator());
            }

            if (null == action.getIgnoreIfNotFound()) {
                action.setIgnoreIfNotFound(pageAction.getIgnoreIfNotFound());
            }
        }
        return action;
    }

    private void validConfig(ArConfig config, ApiResponse<RunTestResult> resp, String fileName) {
        if (null == config.getConfig()) {
            resp.addError(errInFile("configブロックを作成してください。", fileName));
            return;
        }

        if (StringUtils.isEmpty(config.getConfig().getId())) {
            resp.addError(errInFile("configのIDを設定してください。", fileName));
        }

        List<ArConfig.Environment> environments = config.getConfig().getEnvironment();
        if (false == CollectionUtils.isEmpty(environments)) {
            Integer environmentIdx = 0;
            for (ArConfig.Environment environment : environments) {
                if (StringUtils.isEmpty(environment.getId())) {
                    resp.addError(errInFile((environmentIdx + 1) + "件目のenvironmentにIDがありません。", fileName));
                }
            }
        }
    }

    private void validScenario(ArScenario scenario, ApiResponse<RunTestResult> resp, String fileName) {
        if (null == scenario.getScenario()) {
            resp.addError(errInFile("scenarioブロックを作成してください。", fileName));
            return;
        }

        if (StringUtils.isEmpty(scenario.getScenario().getId())) {
            resp.addError(errInFile("scenarioのIDを設定してください。", fileName));
        }

        List<ArScenario.Usecase> usecases = scenario.getScenario().getUsecase();
        if (false == CollectionUtils.isEmpty(usecases)) {
            Integer usecaseIdx = 0;
            for (ArScenario.Usecase usecase : usecases) {
                if (StringUtils.isEmpty(usecase.getId())) {
                    resp.addError(errInFile((usecaseIdx + 1) + "件目のusecaseにIDがありません。", fileName));
                }
            }
        }
    }

    private void validPage(ArSelenadePage page, ApiResponse<RunTestResult> resp, String fileName) {
        if (null == page.getPage()) {
            resp.addError(errInFile("pageブロックを作成してください。", fileName));
            return;
        }

        if (StringUtils.isEmpty(page.getPage().getId())) {
            resp.addError(errInFile("page の ID を設定してください。", fileName));
        }

        List<ArSelenadePage.Action> actions = page.getPage().getAction();
        if (false == CollectionUtils.isEmpty(actions)) {
            Integer actionIdx = 0;
            for (ArSelenadePage.Action action : actions) {
                if (StringUtils.isEmpty(action.getId())) {
                    resp.addError(errInFile((actionIdx + 1) + "件目の action にIDがありません。", fileName));
                }
            }
        }
    }

    private void validUsecase(ArUsecase usecase, ApiResponse<RunTestResult> resp, String fileName) {
        if (null == usecase.getUsecase() && null == usecase.getUsecaseGroup()) {
            resp.addError(errInFile("usecase または usecaseGroup ブロックを作成してください。", fileName));
            return;
        }

        if (null != usecase.getUsecase()) {
            if (StringUtils.isEmpty(usecase.getUsecase().getId())) {
                resp.addError(errInFile("usecase の ID を設定してください。", fileName));
            } else {
                List<ArUsecase.Step> steps = usecase.getUsecase().getStep();
                if (false == CollectionUtils.isEmpty(steps)) {
                    Integer stepIdx = 0;
                    for (ArUsecase.Step step : steps) {
                        if (StringUtils.isEmpty(step.getId())) {
                            resp.addError(errInFile((stepIdx + 1) + "件目の step に ID がありません。", fileName));
                        }
                    }
                }
            }
        }

        if (false == CollectionUtils.isEmpty(usecase.getUsecaseGroup())) {
            Integer usecaseGroupIdx = 0;
            for (ArUsecase.UsecaseGroup usecaseGroup : usecase.getUsecaseGroup()) {
                boolean isBadUsecaseGroup = false;
                if (StringUtils.isEmpty(usecaseGroup.getId())) {
                    isBadUsecaseGroup = true;
                    resp.addError(errInFile((usecaseGroupIdx + 1) + "件目の usecaseGroup に ID がありません。", fileName));
                }

                if (false == isBadUsecaseGroup) {
                    for (ArUsecase.Usecase usecase2 : usecaseGroup.getUsecase()) {
                        List<ArUsecase.Step> steps = usecase2.getStep();
                        if (false == CollectionUtils.isEmpty(steps)) {
                            Integer stepIdx = 0;
                            for (ArUsecase.Step step : steps) {
                                if (StringUtils.isEmpty(step.getId())) {
                                    resp.addError(errInFile((stepIdx + 1) + "件目の step に ID がありません。UsecaseGroup："
                                            + usecaseGroup.getId() + "、Usecase：" + usecase2.getId(), fileName));
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void validData(ArData data, ApiResponse<RunTestResult> resp, String fileName) {
        if (null == data.getDataTemplate()) {
            resp.addError(errInFile("dataTemplate ブロックを作成してください。", fileName));
            return;
        }
        if (StringUtils.isEmpty(data.getDataTemplate().getId())) {
            resp.addError(errInFile("dataTemplate のIDを設定してください。", fileName));
        }
        // TODO impl.
    }

    private static String errInFile(String message, String fileName) {
        StringBuilder sBuilder = new StringBuilder();
        if (StringUtils.isEmpty(message)) {
            sBuilder.append("何か想定外のエラーです。。゜(゜´Д｀゜)゜。");
        } else {
            sBuilder.append(message);
        }

        if (StringUtils.isEmpty(fileName)) {
            sBuilder.append("：ファイル名がわかりません。゜(゜´Д｀゜)゜。");
        } else {
            sBuilder.append(" ファイル名：");
            sBuilder.append(fileName);
        }
        return sBuilder.toString();
    }

    /**
     * 
     * @param type
     * @return
     */
    private Boolean isAction(String type) {
        switch (type) {
            case "input":
            case "click":
            case "select":
                return true;
            default:
                return false;
        }
    }

    protected ArUsecase.Usecase mergeUsecase(ArScenario.Usecase usecase, ArUsecase.Usecase defaultUsecase) {
        if (null == defaultUsecase) {
            return new ArUsecase.Usecase();
        }
        // TODO マージ実装
        return defaultUsecase;
    }

    /**
     * Get object from yaml file.<br/>
     * 
     * @param <T>
     * @param file
     * @param clazz
     * @return
     */
    protected <T> T getYaml(File file, Class<T> clazz) {
        T object;
        try (InputStream stream = new FileSystemResource(file).getInputStream()) {
            LoaderOptions options = new LoaderOptions();
            Yaml yaml = new Yaml(options);
            object = yaml.loadAs(stream, clazz);
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
        return object;
    }

    protected boolean isDebugMode() {
        return true;
    }

    private static Boolean isYaml(String fileName) {
        if (StringUtils.isEmpty(fileName)) {
            return false;
        }

        if (fileName.endsWith(".yml")) {
            return true;
        }

        if (fileName.endsWith(".yaml")) {
            return true;
        }

        return false;
    }

    private String resolve(String value) {
        return value;
    }

    private static void log(String... messages) {
        if (0 == messages.length) {
            return;
        }
        Arrays.asList(messages).stream().forEach(System.out::println);
    }

    protected static String createId() {
        return DateUtil.toString(new Date(), "yyMMddHHmmssSSS");
    }

}
