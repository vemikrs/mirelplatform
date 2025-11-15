/*
 * Copyright(c) 2019 mirelplatform All right reserved.
 */
package jp.vemi.mirel.apps.mste.domain.service;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import jp.vemi.framework.exeption.MessagingException;
import jp.vemi.framework.util.InstanceUtil;
import jp.vemi.mirel.apps.mste.domain.dto.GenerateParameter;
import jp.vemi.mirel.apps.mste.domain.dto.GenerateResult;
import jp.vemi.mirel.foundation.abst.dao.entity.FileManagement;
import jp.vemi.mirel.foundation.abst.dao.repository.FileManagementRepository;
import jp.vemi.mirel.foundation.feature.files.service.FileRegisterService;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;
import jp.vemi.ste.domain.context.SteContext;
import jp.vemi.ste.domain.engine.TemplateEngineProcessor;
import jp.vemi.ste.domain.engine.StructureReader;

/**
 * {@link GenerateService} の具象です。
 */
@Service
@Transactional
public class GenerateServiceImp implements GenerateService {

    @Autowired
    protected FileRegisterService fileRegisterService;
    /** {@link FileManagementRepository} */
    @Autowired
    protected FileManagementRepository fileManagementRepository;

    /** Spring標準のリソース検索機能 */
    @Autowired
    protected ResourcePatternResolver resourcePatternResolver;

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiResponse<GenerateResult> invoke(ApiRequest<GenerateParameter> parameter) {

        ApiResponse<GenerateResult> resp = ApiResponse.<GenerateResult>builder().build();

        List<Pair<String, String>> retItems = Lists.newArrayList();
        parameter.getModel().params.stream().forEach(item -> {

            Map<String, Object> once = InstanceUtil.forceCast(item.get("content"));
            List<String> errs = validate(once);
            if (false == CollectionUtils.isEmpty(errs)) {
                // has err
                resp.addErrors(errs);
                return;
            }

            // prepare.
            TemplateEngineProcessor engine = TemplateEngineProcessor.create(
                    SteContext.newSteContext(once), resourcePatternResolver);
            List<Map<String, Object>> delements = engine.getStencilSettings().getStencilDeAndDd();
            for (Map<String, Object> delement : delements) {
                Object typeObject = delement.get("type");
                if (null == typeObject || false == typeObject instanceof String) {
                    // タイプの宣言が不正なので処理しない
                    continue;
                }
                String type = InstanceUtil.forceCast(typeObject);
                if (false == "file".equals(type)) {
                    // ファイルでないので処理しない
                    continue;
                }
                Object valueObject = delement.get("value");
                if (null == valueObject || false == valueObject instanceof String) {
                    // バリューの宣言が不正なので処理しない
                    continue;
                }
                String value = InstanceUtil.forceCast(valueObject);

                try {
                    engine.appendContext(file(value, resp));
                } catch (Exception e) {
                    e.printStackTrace();
                    resp.addError(e.getLocalizedMessage());
                    return;
                }
            }

            // create.
            String filePath;
            try {
                filePath = engine.execute();
                
            } catch (MessagingException e) {
                e.printStackTrace();
                resp.addErrors(e.messages);
                return;
            } catch (Exception e) {
                e.printStackTrace();
                resp.addError(e.getLocalizedMessage());
                return;
            }

            // register fileitem.
            Pair<String, String> file = fileRegisterService.register(new File(filePath), true);

            retItems.add(file);

        });

        resp.setData(GenerateResult.builder().files(retItems).build());

        return resp;

    }

    protected Map<String, Object> file(final String fileId, ApiResponse<GenerateResult> resp) {

        Map<String, Object> once = Maps.newHashMap();
        FileManagement item;
        try {
            item = fileManagementRepository.findById(fileId).get();
        } catch (NoSuchElementException e) {
            e.printStackTrace();
            resp.addError("ファイルが見つかりません。ファイル管理ID：" + fileId);
            return once;
        }

        // file record is null.
        if (null == item) {
            resp.addError("ファイルが見つかりません。ファイル管理ID：" + fileId);
            return once;
        }

        StructureReader sreader = new StructureReader();
        Map<String, List<Map<String, Object>>> tac = sreader.read(item.getFilePath());
        for (Map.Entry<String, List<Map<String, Object>>> entry : tac.entrySet()) {
            once.put(entry.getKey(), entry.getValue());
        }

        return once;
    }

    /**
     * validate. <br/>
     * 
     * @param param
     *            パラメータ
     * @return メッセージ一覧
     */
    public List<String> validate(Map<String, Object> param) {

        List<String> valids = Lists.newArrayList();

        final Object stencilCanonicalNameObject = param.get("stencilCanonicalName");
        final String stencilCanonicalName = stencilCanonicalNameObject == null ? StringUtils.EMPTY
                : stencilCanonicalNameObject.toString();
        if (StringUtils.isEmpty(stencilCanonicalName) || "*".equals(stencilCanonicalName)) {
            valids.add("ステンシルが指定されていません。");
        }

        final Object serialNoObject = param.get("serialNo");
        final String serialNo = serialNoObject == null ? StringUtils.EMPTY : serialNoObject.toString();
        if (StringUtils.isEmpty(serialNo) || "*".equals(serialNo)) {
            valids.add("シリアルが指定されていません。");
        }

        return valids;
    }
}
