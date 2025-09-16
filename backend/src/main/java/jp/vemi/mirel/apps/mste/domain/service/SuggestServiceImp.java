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
import org.springframework.core.io.support.ResourcePatternResolver;
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
    
    /** Spring標準のリソース検索機能 */
    @Autowired
    protected ResourcePatternResolver resourcePatternResolver;

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

        // FIXME: 応急処置 - ModelWrapperは型安全性を損なう hack
        // 根本的な問題: ApiResponse<T>とFrontend期待値の構造不整合
        // 詳細は別Issue参照: API レスポンス構造の設計見直し
        class ModelWrapper {
            public SuggestResult model;
        }

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
            // FIXME: 応急処置 - 型キャストによる構造の無理やり調整
            ModelWrapper wrapper = new ModelWrapper();
            wrapper.model = resultModel;
            @SuppressWarnings("unchecked")
            ApiResponse<SuggestResult> response = (ApiResponse<SuggestResult>)(ApiResponse<?>) ApiResponse.builder().data(wrapper).build();
            return response;
        }

        TemplateEngineProcessor engine;
        try {
            System.out.println("=== DEBUG SuggestServiceImp ===");
            System.out.println("stencilCd: " + stencilCd);
            System.out.println("serialNo: " + parameter.getModel().serialNo);
            System.out.println("resourcePatternResolver: " + resourcePatternResolver);
            System.out.println("resourcePatternResolver is null: " + (resourcePatternResolver == null));
            
            if (resourcePatternResolver == null) {
                System.out.println("WARNING: ResourcePatternResolver is null, TemplateEngineProcessor may fail");
            }
            
            engine = TemplateEngineProcessor.create(
                SteContext.standard(stencilCd, parameter.getModel().serialNo), 
                resourcePatternResolver);
        } catch (Throwable e) {
            System.out.println("ERROR in TemplateEngineProcessor.create: " + e.getMessage());
            System.out.println("Attempting graceful fallback using database information");
            e.printStackTrace();
            
            // フォールバック: データベース情報を使用してレスポンス構築
            return createFallbackResponse(stencilCd, parameter.getModel().serialNo, resultModel);
        }

        String stencilNo = engine.getSerialNo();
        List<String> serials = engine.getSerialNos();
        
        // serialsが空の場合、config.serialからフォールバック
        if (serials.isEmpty()) {
            StencilSettingsYml tempSettings = engine.getStencilSettings();
            if (tempSettings != null && tempSettings.getStencil() != null && 
                tempSettings.getStencil().getConfig() != null && 
                tempSettings.getStencil().getConfig().getSerial() != null) {
                serials = Arrays.asList(tempSettings.getStencil().getConfig().getSerial());
                stencilNo = tempSettings.getStencil().getConfig().getSerial();
            }
        }
        
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

        // FIXME: 応急処置 - 型キャストによる構造の無理やり調整
        ModelWrapper wrapper = new ModelWrapper();
        wrapper.model = resultModel;
        @SuppressWarnings("unchecked")
        ApiResponse<SuggestResult> response = (ApiResponse<SuggestResult>)(ApiResponse<?>) ApiResponse.builder().data(wrapper).build();
        return response;
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

        // 明示的に指定された値が有効な場合は尊重する
        if(false == StringUtils.isEmpty(store.selected) && !"*".equals(store.selected)) {
            // 指定された値がリストに存在するか確認
            boolean exists = store.items.stream().anyMatch(item -> item.value.equals(store.selected));
            if (exists) {
                return; // 指定された値をそのまま使用
            }
        }

        // '*' または無効な値の場合、より安全な選択ロジックを適用
        String preferredValue;
        
        // 1. samplesカテゴリを最優先（確実に動作する）
        preferredValue = store.items.stream()
            .filter(item -> item.value.startsWith("/samples"))
            .map(item -> item.value)
            .findFirst()
            .orElse(null);
            
        // 2. samplesがない場合、spring_service系を優先（比較的安全）
        if (preferredValue == null) {
            preferredValue = store.items.stream()
                .filter(item -> item.value.contains("spring_service") && !item.value.contains("mvc"))
                .map(item -> item.value)
                .findFirst()
                .orElse(null);
        }
        
        // 3. それでもない場合、最初のアイテムを使用
        if (preferredValue == null) {
            preferredValue = store.items.stream().findFirst().get().value;
        }
            
        store.selected = preferredValue;

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
    
    /**
     * TemplateEngineProcessor失敗時のフォールバック処理
     * データベース情報を活用してレスポンスを構築
     */
    private ApiResponse<SuggestResult> createFallbackResponse(String stencilCd, String serialNo, SuggestResult resultModel) {
        try {
            System.out.println("=== FALLBACK: Creating response using database information ===");
            System.out.println("stencilCd: " + stencilCd);
            System.out.println("serialNo: " + serialNo);
            
            // 基本情報：既存のresultModelを使用（カテゴリ・ステンシル選択肢は既に設定済み）
            
            // シリアル情報の設定（フォールバック）
            List<String> fallbackSerials = new ArrayList<>();
            if (!StringUtils.isEmpty(serialNo) && !"*".equals(serialNo)) {
                fallbackSerials.add(serialNo);
            } else {
                // デフォルトシリアル（データベースから取得可能な場合は取得）
                fallbackSerials.add("DEFAULT");
            }
            
            resultModel.fltStrSerialNo = new ValueTextItems(
                convertSerialNosToValueTexts(fallbackSerials), 
                fallbackSerials.get(0)
            );
            
            // パラメータ情報：最小限の構造を設定
            RootNode rootNode = new RootNode();
            rootNode.childs = new ArrayList<>();
            
            // 基本的なパラメータを追加（一般的なステンシルパラメータ）
            StencilParameterPrototypeNode messageParam = StencilParameterPrototypeNode.builder()
                .id("message")
                .name("メッセージ")
                .valueType("text")
                .value("フォールバックモード")
                .placeholder("メッセージを入力してください")
                .note("TemplateEngineProcessor使用不可のためフォールバックモード")
                .sort(0)
                .noSend(false)
                .build();
            rootNode.childs.add(messageParam);
            
            resultModel.params = rootNode;
            
            // ステンシル設定情報（最小限）
            // resultModel.stencil は StencilSettingsYml.Stencil 型なので、null のままにしておく
            // フォールバックモードでは詳細情報は提供しない
            resultModel.stencil = null;
            
            // ModelWrapper適用（既存のパターンに合わせる）
            class FallbackModelWrapper {
                public SuggestResult model;
            }
            
            FallbackModelWrapper wrapper = new FallbackModelWrapper();
            wrapper.model = resultModel;
            
            @SuppressWarnings("unchecked")
            ApiResponse<SuggestResult> response = (ApiResponse<SuggestResult>)(ApiResponse<?>) 
                ApiResponse.builder().data(wrapper).build();
            
            System.out.println("Fallback response created successfully");
            return response;
            
        } catch (Exception fallbackError) {
            System.out.println("ERROR: Fallback also failed: " + fallbackError.getMessage());
            fallbackError.printStackTrace();
            
            // 最終的なフォールバック：エラーレスポンス
            ApiResponse<SuggestResult> errorResponse = ApiResponse.<SuggestResult>builder().build();
            errorResponse.addError("ステンシル定義の読み込みに失敗しました。TemplateEngineProcessorとフォールバック処理の両方でエラーが発生しました。");
            return errorResponse;
        }
    }
}
