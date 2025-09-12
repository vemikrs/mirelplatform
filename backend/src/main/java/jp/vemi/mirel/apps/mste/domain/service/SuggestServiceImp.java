/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.mste.domain.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;

import jp.vemi.mirel.apps.mste.domain.dao.entity.MsteStencil;
import jp.vemi.mirel.apps.mste.domain.dao.repository.MsteStencilRepository;
import jp.vemi.mirel.apps.mste.domain.dto.SuggestParameter;
import jp.vemi.mirel.apps.mste.domain.dto.SuggestResult;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;
import jp.vemi.mirel.foundation.web.api.dto.node.Node;
import jp.vemi.mirel.foundation.web.api.dto.node.RootNode;
import jp.vemi.mirel.foundation.web.api.dto.node.StencilParameterPrototypeNode;
import jp.vemi.mirel.foundation.web.model.ValueText;
import jp.vemi.mirel.foundation.web.model.ValueTextItems;
import jp.vemi.ste.domain.context.SteContext;
import jp.vemi.ste.domain.dto.yml.StencilSettingsYml;
import jp.vemi.ste.domain.engine.TemplateEngineProcessor;

/**
 * {@link SuggestService} の具象です。
 */
@Service
@Transactional
public class SuggestServiceImp implements SuggestService {

    /** {@link MsteStencilRepository} */
    @Autowired
    protected MsteStencilRepository stencilRepository;

    /**
     * Const
     */
    protected static class Const {
        final static String STENCIL_ITEM_KIND_CATEGORY = "0";
        final static String STENCIL_ITEM_KIND_ITEM = "1";
    }

    /** {@inheritDoc} */
    @Override
    public ApiResponse<SuggestResult> invoke(ApiRequest<SuggestParameter> parameter) {

        SuggestResult resultModel = new SuggestResult();

        resultModel.fltStrStencilCategory = getStencils(Const.STENCIL_ITEM_KIND_CATEGORY, "");
        resultModel.fltStrStencilCategory.selected = parameter.getModel().stencilCategory;
        setFirstItemIfNoSelected(resultModel.fltStrStencilCategory);

        List<MsteStencil> stencils = stencilRepository.findByStencilCd(resultModel.fltStrStencilCategory.selected,
                Const.STENCIL_ITEM_KIND_ITEM);
        resultModel.fltStrStencilCd = new ValueTextItems(convertStencilToValueTexts(stencils), parameter.getModel().stencilCd);
        setFirstItemIfNoSelected(resultModel.fltStrStencilCd);

        String stencilCd = resultModel.fltStrStencilCd.selected;

        if ("*".equals(stencilCd)) {
            return new ApiResponse<>(resultModel);
        }

        TemplateEngineProcessor engine;
        try {
            engine = TemplateEngineProcessor.create(SteContext.standard(stencilCd, parameter.getModel().serialNo));
        } catch (Throwable e) {
            e.printStackTrace();
            throw e;
        }

        String stencilNo = engine.getSerialNo();
        List<String> serials = engine.getSerialNos();
        resultModel.fltStrSerialNo = new ValueTextItems(convertSerialNosToValueTexts(serials), stencilNo);

        StencilSettingsYml settingsYaml = null;
        try {
            settingsYaml = engine.getStencilSettings();
        } catch(Throwable e) {
            e.printStackTrace();
            throw e;
        }

        System.out.println(settingsYaml);

        // stencil-settings
        resultModel.stencil = settingsYaml.getStencil();
        resultModel.params = itemsToNode(settingsYaml); // convert.

        return new ApiResponse<>(resultModel);
    }

    private List<ValueText> convertSerialNosToValueTexts(List<String> serials) {
        return Lists.transform(serials, new Function<String,ValueText>() {
            @Override
            public ValueText apply(String input) {
                return new ValueText(input, input);
            }
        });
    }

    protected Collection<?> map() {
        return null;
    };


    protected Map<String, Object> propToTree(Hashtable<String, Object> from) {

        Map<String, Object> top = null;

        for(Entry<String, Object> entry : from.entrySet()) {
            
            if(CollectionUtils.isEmpty(top)) {
                if(entry.getKey().contains("[") &&
                    entry.getKey().endsWith("]")) {
                        top = Maps.newLinkedHashMap();
                } else {
                    
                }
            }

            String key = entry.getKey();
            List<String> keys = Arrays.asList(key.split("."));

            keys.forEach(keyItem -> {
                
            });
        }

        return null;
    }

    /**
     * 初期値設定.<br/>
     * @param store
     */
    protected static void setFirstItemIfNoSelected(ValueTextItems store) {

        if(null == store) {
            return;
        }
        if(CollectionUtils.isEmpty(store.items)) {
            return;
        }

        if(false == StringUtils.isEmpty(store.selected)) {
            return;
        }

        store.selected = store.items.stream().findFirst().get().value;

    }

    protected static Node itemsToNode(StencilSettingsYml settings){

        Node root = new RootNode();

        if(null == settings ||
            null == settings.getStencil()){
            return root;
        }

        List<Map<String, Object>> elems = mergeStencilDeAndDd(
            settings.getStencil().getDataElement(),
            settings.getStencil().getDataDomain());

        elems.forEach(entry -> {
            root.addChild(convertItemToNodeItem(entry));
        });

        return root;
    }

    /**
     * mergeMapList.<br/>
     * 
     * @param list1
     * @param list2
     * @return
     */
    protected static List<Map<String, Object>> mergeMapList(List<Map<String, Object>> list1,
        List<Map<String, Object>> list2) {

        final List<Map<String, Object>> elems = Lists.newArrayList();

        if (CollectionUtils.isEmpty(list1)) {
            if (false == CollectionUtils.isEmpty(list2)) {
                elems.addAll(list2);
            }
            return elems;
        }

        if (CollectionUtils.isEmpty(list2)) {
            if (false == CollectionUtils.isEmpty(list1)) {
                elems.addAll(list1);
            }
            return elems;
        }

        list1.forEach(dataElement -> {

            Map<String, Object> target = Maps.newLinkedHashMap(dataElement);
            final String id = (String) target.get("id");

            list2.forEach(list2item -> {

                if(false == id.equals(list2item.get("id"))) {
                    return;
                }

                // list1-id matched in list2.
                list2item.entrySet().forEach(entry -> {

                    if (target.containsKey(entry.getKey())) {
                        return;
                    }

                    // contains in dataelement.
                    target.put(entry.getKey(), entry.getValue());
                });
            });

            elems.add(target);

        });



        return elems;
    }

    /**
     * mergeStencilDeAndDd.<br/>
     * 
     * @param dataElements
     * @param dataDomains
     * @return
     */
    protected static List<Map<String, Object>> mergeStencilDeAndDd(List<Map<String, Object>> dataElements,
        List<Map<String, Object>> dataDomains) {

        return mergeMapList(dataElements, dataDomains);

    }
    /**
     * 
     */
    protected static StencilParameterPrototypeNode convertItemToNodeItem(Map<String, Object> att) {

        if(null == att){
            return StencilParameterPrototypeNode.builder().build(); 
        }

        return StencilParameterPrototypeNode.builder()
            .id(getAsString(att, "id"))
            .name(getAsString(att, "name"))
            .valueType(getAsString(att, "type"))
            .value(getAsString(att, "value"))
            .placeholder(StringUtils.defaultIfEmpty(
                getAsString(att, "placeholder"), "please input " + getAsString(att, "id")))
            .note(getAsString(att, "note"))
            .sort(getAsInteger(att, "sort"))
            .noSend(getAsBoolean(att, "noSend"))
            .build();

    }

    /**
     * 
     */
    protected static Boolean getAsBoolean(Map<String, Object> map, String key) {
        return (Boolean) map.get(key);
    }

    /**
     * 
     */
    protected static Integer getAsInteger(Map<String, Object> map, String key) {
        return (Integer) map.get(key);
    }

    /**
     * 
     */
    protected static String getAsString(Map<String, Object> map, String key) {
        return (String) map.get(key);
    }

    /**
     * 
     */
    protected ValueTextItems getStencils(String kind, String selected) {

        Assert.notNull(kind, "kind must not be null");

        List<MsteStencil> stencils = stencilRepository.findAll();
        /*
         * 0：ステンシル分類, 1：ステンシル
         */
        stencils.removeIf(item -> {
            return false == kind.equals(item.itemKind);
        });

        return new ValueTextItems(convertStencilToValueTexts(stencils), selected);
    }

    /**
     * convertStencilToValueTexts.<br/>
     * 
     * @param stencils
     * @return
     */
    protected static List<ValueText> convertStencilToValueTexts(List<MsteStencil> stencils) {

        List<ValueText> valueTexts = new ArrayList<>();

        if (null == stencils) {
            return valueTexts;
        }

        valueTexts = Lists.transform(stencils, new Function<MsteStencil, ValueText>() {
            /**
             * {@inheritDoc}
             */
            @Override
            public ValueText apply(MsteStencil input) {
                return new ValueText(input.stencilCd, input.stencilName);
            }
        });

        return valueTexts;
    }
}
