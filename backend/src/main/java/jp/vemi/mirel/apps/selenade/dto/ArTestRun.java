/*
 * Copyright(c) 2015-2021 vemi/mirelplatform.
 */
package jp.vemi.mirel.apps.selenade.dto;

import java.util.Map;

import com.google.common.collect.Maps;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import jp.vemi.mirel.apps.selenade.dto.yml.ArUsecase;
import jp.vemi.mirel.apps.selenade.dto.yml.ArConfig;
import jp.vemi.mirel.apps.selenade.dto.yml.ArData;
import jp.vemi.mirel.apps.selenade.dto.yml.ArScenario;
import jp.vemi.mirel.apps.selenade.dto.yml.ArSelenadePage;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Scenario.<br/>
 */
@Getter
@Setter
@NoArgsConstructor
public class ArTestRun {


    /** config. */
    Map<String, ArConfig.Config> config = Maps.newLinkedHashMap();

    /** data. */
    Map<String, ArData> data = Maps.newLinkedHashMap();

    /** scenario. */
    Map<String, ArScenario.Scenario> scenarios = Maps.newLinkedHashMap();

    /** activity. */
    Map<String, ArUsecase.Usecase> usecases = Maps.newLinkedHashMap();

    /** page. */
    Map<String, ArSelenadePage.Page> pages = Maps.newLinkedHashMap();

    /**
     * Add scenario.<br/>
     * @param scenario
     */
    public void addConfig(ArConfig config) {
        if (null == config
            || null == config.getConfig()
            || StringUtils.isEmpty(config.getConfig().getId())) {
            return;
        }
        this.config.put(config.getConfig().getId(), config.getConfig());
    }

    public void addData(ArData data) {
        if (null == data
            || null == data.getDataTemplate()
            || StringUtils.isEmpty(data.getDataTemplate().getId())) {
            return;
        }
        this.data.put(data.getDataTemplate().getId(), data);
    }

    /**
     * Add usecase.<br/>
     * @param usecase
     */
    public void addUsecase(ArUsecase usecase) {
        if (null == usecase) {
            return;
        }

        if (null != usecase.getUsecase()) {
            this.usecases.put(usecase.getUsecase().getId(), usecase.getUsecase());
            return;
        }

        if (false == CollectionUtils.isEmpty(usecase.getUsecaseGroup())) {
            for (ArUsecase.UsecaseGroup usecaseGroup : usecase.getUsecaseGroup()) {
                for (ArUsecase.Usecase usecase2 : usecaseGroup.getUsecase()) {
                    this.usecases.put(usecase2.getId(), usecase2);
                }
            }
        }

    }

    /**
     * Add page.<br/>
     * @param page
     */
    public void addPage(ArSelenadePage page) {
        if (null == page
            || null == page.getPage()
            || StringUtils.isEmpty(page.getPage().getId())) {
            return;
        }
        this.pages.put(page.getPage().getId(), page.getPage());
    }

    /**
     * Add scenario.<br/>
     * @param scenario
     */
    public void addScenario(ArScenario scenario) {
        if (null == scenario
            || null == scenario.getScenario()
            || StringUtils.isEmpty(scenario.getScenario().getId())) {
            return;
        }
        this.scenarios.put(scenario.getScenario().getId(), scenario.getScenario());
    }

}
