/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.mste.domain.service;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.ConstructorException;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import jp.vemi.framework.exeption.MirelApplicationException;
import jp.vemi.framework.exeption.MirelSystemException;
import jp.vemi.framework.util.FileUtil;
import jp.vemi.framework.util.StorageUtil;
import jp.vemi.mirel.apps.mste.domain.dao.entity.MsteStencil;
import jp.vemi.mirel.apps.mste.domain.dao.repository.MsteStencilRepository;
import jp.vemi.mirel.apps.mste.domain.dto.ReloadStencilMasterParameter;
import jp.vemi.mirel.apps.mste.domain.dto.ReloadStencilMasterResult;
import jp.vemi.mirel.foundation.abst.dao.entity.FileManagement;
import jp.vemi.mirel.foundation.abst.dao.repository.FileManagementRepository;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;
import jp.vemi.ste.domain.dto.yml.StencilSettingsYml;
import jp.vemi.ste.domain.dto.yml.StencilSettingsYml.Stencil.Config;
import jp.vemi.ste.domain.engine.TemplateEngineProcessor;

/**
 * {@link ReloadStencilMasterService ステンシルマスタの更新} の具象です。
 */
@Service
@Transactional
public class ReloadStencilMasterServiceImp implements ReloadStencilMasterService {

    /** {@link MsteStencilRepository ステンシルマスタ} */
    @Autowired
    protected MsteStencilRepository stencilRepository;

    @Autowired
    protected FileManagementRepository fileManagementRepository;

    /**
     * {@inheritDoc}
     */
    @Override
    public ApiResponse<ReloadStencilMasterResult> invoke(ApiRequest<ReloadStencilMasterParameter> parameter) {

        ApiResponse<ReloadStencilMasterResult> resp = ApiResponse.<ReloadStencilMasterResult>builder().build();

        this.read();
        resp.setData(ReloadStencilMasterResult.builder().build());

        return resp;

    }

    /**
     * read from stencil directory.
     */
    protected void read() {
        // clear.
        stencilRepository.deleteAll();

        // road stencil settings.
        String dir = StorageUtil.getBaseDir() + TemplateEngineProcessor.getStencilMasterStorageDir();
        List<String> files = FileUtil.findByFileName(dir, "stencil-settings.yml");
        Map<String, String> categories = Maps.newLinkedHashMap();

        // save stencil record.
        for (String fileName : files) {
            StencilSettingsYml settings = readYaml(new File(fileName));
            Config config = settings.getStencil().getConfig();

            MsteStencil entry = new MsteStencil();
            entry.setStencilCd(config.getId());
            entry.setStencilName(config.getName());
            entry.setItemKind("1");
            entry.setSort(0);
            stencilRepository.save(entry);
            System.out.println(config.getId() + "/" + config.getSerial() + ":" + config.getName());

            if (false == StringUtils.isEmpty(config.getCategoryId())) {
                if (false == categories.containsKey(config.getCategoryId())) {
                    if (StringUtils.isEmpty(categories.get(config.getCategoryId()))) {
                        categories.put(config.getCategoryId(), config.getCategoryName());
                    }
                }
            }
        }

        // save stencil record.
        for (Entry<String, String> catentry : categories.entrySet()) {
            MsteStencil entry = new MsteStencil();
            entry.setStencilCd(catentry.getKey());
            entry.setStencilName(catentry.getValue());
            entry.setItemKind("0");
            entry.setSort(0);
            stencilRepository.save(entry);
        }

        String fileDir = dir + "/_filemanagement";
        File[] filemanagementFiles = new File(fileDir).listFiles();
        for (File file : filemanagementFiles) {
            File[] filesInUuid = file.listFiles();

            if (0 == filesInUuid.length) {
                continue;
            }

            // create entity.
            FileManagement fileManagement = new FileManagement();
            fileManagement.fileId = file.getName();
            fileManagement.fileName = filesInUuid[0].getName();
            fileManagement.filePath = filesInUuid[0].getAbsolutePath();
            fileManagement.expireDate = DateUtils.addDays(new Date(), 3650);
            fileManagementRepository.save(fileManagement);

        }

    }

    /**
     * read Stencil settings file.
     * 
     * @param file
     *            Setting file (Yaml)
     * @return {@link StencilSettingsYml ステンシル定義YAML}
     */
    protected StencilSettingsYml readYaml(File file) {
        StencilSettingsYml settings = null;
        try (InputStream stream = new FileSystemResource(file).getInputStream()) {
            LoaderOptions options = new LoaderOptions();
            settings = new Yaml(new SafeConstructor(options))
                    .loadAs(stream, StencilSettingsYml.class);
        } catch (final ConstructorException e) {
            e.printStackTrace();
            String msg = "yamlの読込でエラーが発生しました。";
            throw new MirelApplicationException(msg, e);
        } catch (final IOException e) {
            e.printStackTrace();
            throw new MirelSystemException("yamlの読込で入出力エラーが発生しました。", e);
        }
        return settings;
    }
}
