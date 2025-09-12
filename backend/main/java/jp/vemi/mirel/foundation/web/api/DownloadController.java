/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.foundation.web.api;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.constraints.NotEmpty;

import com.google.common.base.Function;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import groovy.lang.Tuple3;
import jp.vemi.framework.util.InstanceUtil;
import jp.vemi.mirel.foundation.feature.files.service.FileDownloadService;
import jp.vemi.mirel.foundation.feature.files.dto.FileDownloadParameter;
import jp.vemi.mirel.foundation.feature.files.dto.FileDownloadResult;
import jp.vemi.mirel.foundation.web.api.dto.ApiRequest;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;

/**
 * .<br/>
 */
@RestController
public class DownloadController {

    /**
     * {@link FileDownloadService}
     */
    @Autowired
    FileDownloadService service;

    /**
     * get.<br/>
     * 
     * @param path
     * @param response
     * @return {@link ResponseEntity}
     */
    @RequestMapping(path = "commons/dlsite/{path}", method = RequestMethod.GET)
    public ResponseEntity<ApiResponse<FileDownloadResult>> index4Get(@NotEmpty @PathVariable String path,
            final HttpServletResponse response) {
        String[] spliteds = path.split(",");

        List<Map<String, Object>> files = Lists.newArrayList();
        for (String splited : spliteds) {
            Map<String, Object> fileitem = Maps.newLinkedHashMap();
            fileitem.put("fileId", splited);
            files.add(fileitem);
        }

        Map<String, Object> request = Maps.newLinkedHashMap();
        request.put("content", files);
        return miho(request, response);
    }

    /**
     * post. <br/>
     * 
     * @param request
     * @param response
     * @return
     */
    @RequestMapping(path = "commons/download")
    public ResponseEntity<ApiResponse<FileDownloadResult>> index(@RequestBody final Map<String, Object> request,
            final HttpServletResponse response) {
        return miho(request, response);
    }

    /**
     * download controller (core).<br/>
     * 
     * @param request
     * @param response
     * @return {@link ResponseEntity}
     */
    protected ResponseEntity<ApiResponse<FileDownloadResult>> miho(Map<String, Object> request,
            final HttpServletResponse response) {

        final FileDownloadParameter parameter = createParameter(request);
        final ApiResponse<FileDownloadResult> apiResp = service
                .invoke(new ApiRequest<FileDownloadParameter>(parameter));

        if (false == apiResp.getErrors().isEmpty()) {
            return new ResponseEntity<>(apiResp, HttpStatus.OK);
        }

        // decide file name
        String fileName = "";
        final List<Tuple3<String, String, Path>> paths = apiResp.getData().paths;
        if (paths.size() > 1) {
            fileName = "download.zip";
        } else if (paths.size() == 1) {
            fileName = paths.stream().findFirst().get().getV2();
        } else {
            // error
            return new ResponseEntity<>(apiResp, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        String encodedFileName;
        try {
            encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            // error
            e.printStackTrace();
            return new ResponseEntity<>(apiResp, HttpStatus.INTERNAL_SERVER_ERROR);
        }

        response.setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_OCTET_STREAM_VALUE);
        response.addHeader("Content-Disposition", "attachment; filename=" + encodedFileName);

        if (paths.size() > 1) {
            try (ZipOutputStream zostream = new ZipOutputStream(response.getOutputStream())) {
                for (final Tuple3<String, String, Path> item : apiResp.getData().paths) {
                    File entryFile = item.getV3().toFile();
                    final ZipEntry entry = new ZipEntry(
                            new File(entryFile.getParent()).getName() + "-" + item.getV2());
                    zostream.putNextEntry(entry);
                    zostream.write(Files.readAllBytes(item.getV3()));
                }
            } catch (final IOException e) {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else if (paths.size() == 1) {
            try {
                Files.copy(paths.stream().findFirst().get().getV3(), response.getOutputStream());
            } catch (IOException e) {
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }
        } else {
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

        return new ResponseEntity<>(HttpStatus.OK);

    }

    /**
     * create parameter.<br/>
     * 
     * @param requestMap
     * @return {@link FileDownloadParameter}
     */
    protected FileDownloadParameter createParameter(final Map<String, Object> requestMap) {
        List<Map<String, Object>> files = InstanceUtil.forceCast(requestMap.get("content"));
        List<String> fileIds = Lists.transform(files, new Function<Map<String, Object>, String>() {
            /**
             * {@inheritDoc}
             */
            @Override
            public String apply(final Map<String, Object> input) {
                if (CollectionUtils.isEmpty(input)) {
                    return "";
                }
                String fileId = (String) input.get("fileId");
                return StringUtils.defaultIfEmpty(fileId, "");
            }
        });

        final FileDownloadParameter parameter = FileDownloadParameter.builder().fileIds(fileIds).build();
        return parameter;
    }

}
