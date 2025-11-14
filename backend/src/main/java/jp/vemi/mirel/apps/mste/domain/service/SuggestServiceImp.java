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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

    private static final Logger logger = LoggerFactory.getLogger(SuggestServiceImp.class);

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
        SuggestResult resultModel = new SuggestResult();

        SuggestParameter p = parameter.getModel();

        // 1) Category list
        resultModel.fltStrStencilCategory = getStencils(Const.STENCIL_ITEM_KIND_CATEGORY, p.stencilCategory);
        if (isWildcard(resultModel.fltStrStencilCategory.selected) && p.selectFirstIfWildcard) {
            autoSelectFirst(resultModel.fltStrStencilCategory);
        } else {
            validateSelectedExists(resultModel.fltStrStencilCategory);
        }

        // If category not fixed yet → return early (no stencil/serial)
        if (StringUtils.isEmpty(resultModel.fltStrStencilCategory.selected)) {
            return wrap(resultModel);
        }

        // 2) Stencil list (depends on category)
        List<MsteStencil> stencils = stencilRepository.findByStencilCd(resultModel.fltStrStencilCategory.selected, Const.STENCIL_ITEM_KIND_ITEM);
        resultModel.fltStrStencilCd = new ValueTextItems(convertStencilToValueTexts(stencils), p.stencilCd);
        if (isWildcard(resultModel.fltStrStencilCd.selected) && p.selectFirstIfWildcard) {
            autoSelectFirst(resultModel.fltStrStencilCd);
        } else {
            validateSelectedExists(resultModel.fltStrStencilCd);
        }

        if (StringUtils.isEmpty(resultModel.fltStrStencilCd.selected)) {
            return wrap(resultModel);
        }

        // 3) Serial & parameters (depends on stencil)
        String requestedSerial = p.serialNo;
        boolean needAutoSelectSerial = isWildcard(requestedSerial) && p.selectFirstIfWildcard;
        boolean serialSpecified = !isWildcard(requestedSerial);
        String effectiveSerial = null;
        TemplateEngineProcessor engine = null;
        if (needAutoSelectSerial || serialSpecified) {
            try {
                engine = TemplateEngineProcessor.create(
                    SteContext.standard(resultModel.fltStrStencilCd.selected, isWildcard(requestedSerial)?"":requestedSerial),
                    resourcePatternResolver);
            } catch (Throwable e) {
                logger.error("ERROR in TemplateEngineProcessor.create: {}", e.getMessage(), e);
                return createFallbackResponse(resultModel.fltStrStencilCd.selected, requestedSerial, resultModel);
            }

            List<String> serials = engine.getSerialNos();
            if (serials.isEmpty()) {
                StencilSettingsYml tempSettings = engine.getStencilSettings();
                if (tempSettings != null && tempSettings.getStencil()!=null && tempSettings.getStencil().getConfig()!=null && tempSettings.getStencil().getConfig().getSerial()!=null) {
                    serials = Arrays.asList(tempSettings.getStencil().getConfig().getSerial());
                }
            }
            if (needAutoSelectSerial) {
                effectiveSerial = serials.isEmpty()?"":serials.get(0);
            } else if (serialSpecified) {
                effectiveSerial = requestedSerial;
            }
            resultModel.fltStrSerialNo = new ValueTextItems(convertSerialNosToValueTexts(serials), effectiveSerial==null?"":effectiveSerial);
            if (StringUtils.isEmpty(resultModel.fltStrSerialNo.selected)) {
                return wrap(resultModel); // serial 未確定 → params なし
            }
            // 完全確定 → settings & params
            StencilSettingsYml settingsYaml = engine.getStencilSettings();
            resultModel.stencil = settingsYaml.getStencil();
            resultModel.params = itemsToNode(settingsYaml);
            return wrap(resultModel);
        } else {
            // Serial まだ不要 → 空の serial list
            resultModel.fltStrSerialNo = new ValueTextItems(Lists.newArrayList(), "");
            return wrap(resultModel);
        }
    }

    private boolean isWildcard(String v){
        return StringUtils.isEmpty(v) || "*".equals(v);
    }
    private void autoSelectFirst(ValueTextItems items){
        if(items==null||CollectionUtils.isEmpty(items.items)){return;}
        // 既存優先ルール再利用
        String preferred = items.items.stream().filter(i->i.value.startsWith("/samples"))
            .map(i->i.value).findFirst().orElse(null);
        if(preferred==null){
            preferred = items.items.stream().filter(i->i.value.contains("spring_service") && !i.value.contains("mvc"))
                .map(i->i.value).findFirst().orElse(null);
        }
        if(preferred==null){
            preferred = items.items.get(0).value;
        }
        items.selected = preferred;
    }
    private void validateSelectedExists(ValueTextItems items){
        if(items==null){return;}
        if(StringUtils.isEmpty(items.selected)){return;}
        boolean exists = items.items.stream().anyMatch(i->i.value.equals(items.selected));
        if(!exists){
            // 不正指定はクリアして上位へ早期リターン可能にする
            items.selected = "";
        }
    }
    private ApiResponse<SuggestResult> wrap(SuggestResult model){
        class ModelWrapper { @SuppressWarnings("unused") public SuggestResult model; }
        ModelWrapper w = new ModelWrapper();
        w.model = model;
        @SuppressWarnings("unchecked")
        ApiResponse<SuggestResult> response = (ApiResponse<SuggestResult>)(ApiResponse<?>) ApiResponse.builder().data(w).build();
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
    protected static void setFirstItemIfNoSelected(ValueTextItems store, boolean isInitialLoad) {
        // 初回ロード時は自動選択しない
        if (isInitialLoad) {
            store.selected = "";
            return;
        }

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

        List<MsteStencil> stencils = new ArrayList<>(stencilRepository.findAll());
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
            logger.info("=== FALLBACK: Creating response using database information ===");
            logger.debug("stencilCd: {}", stencilCd);
            logger.debug("serialNo: {}", serialNo);
            
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
            
            // ステンシル設定情報（最小限でも構造体を返す - フロントのStencilInfo表示用）
            StencilSettingsYml.Stencil stencilInfo = new StencilSettingsYml.Stencil();
            StencilSettingsYml.Stencil.Config minimalConfig = new StencilSettingsYml.Stencil.Config();
            // resultModelに既にセット済みのカテゴリ・ステンシル情報を使用
            minimalConfig.setCategoryId(resultModel.fltStrStencilCategory != null ? resultModel.fltStrStencilCategory.selected : "");
            minimalConfig.setCategoryName("フォールバックカテゴリ");
            minimalConfig.setId(stencilCd);
            minimalConfig.setName("フォールバックステンシル");
            minimalConfig.setSerial(fallbackSerials.get(0));
            minimalConfig.setLastUpdate("N/A");
            minimalConfig.setLastUpdateUser("system");
            minimalConfig.setDescription("TemplateEngineProcessor使用不可のためフォールバックモード");
            stencilInfo.setConfig(minimalConfig);
            resultModel.stencil = stencilInfo;
            
            // ModelWrapper適用（既存のパターンに合わせる）
            class FallbackModelWrapper {
                @SuppressWarnings("unused")
                public SuggestResult model;
            }
            
            FallbackModelWrapper wrapper = new FallbackModelWrapper();
            wrapper.model = resultModel;
            
            @SuppressWarnings("unchecked")
            ApiResponse<SuggestResult> response = (ApiResponse<SuggestResult>)(ApiResponse<?>) 
                ApiResponse.builder().data(wrapper).build();
            
            logger.info("Fallback response created successfully");
            return response;
            
        } catch (Exception fallbackError) {
            logger.error("ERROR: Fallback also failed: {}", fallbackError.getMessage(), fallbackError);
            fallbackError.printStackTrace();
            
            // 最終的なフォールバック：エラーレスポンス
            ApiResponse<SuggestResult> errorResponse = ApiResponse.<SuggestResult>builder().build();
            errorResponse.addError("ステンシル定義の読み込みに失敗しました。TemplateEngineProcessorとフォールバック処理の両方でエラーが発生しました。");
            return errorResponse;
        }
    }
}
