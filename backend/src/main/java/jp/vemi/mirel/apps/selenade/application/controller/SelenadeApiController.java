/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.selenade.application.controller;

import java.util.Map;

import com.google.common.collect.Lists;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jp.vemi.mirel.apps.selenade.domain.api.SelenadeApi;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;

@RestController
@RequestMapping("apps/arr/api")
public class SelenadeApiController {

    @Autowired(required = false)
    private Map<String, SelenadeApi> apis;

    @RequestMapping("/{path}")
    public ResponseEntity<ApiResponse<?>> index(@RequestBody Map<String, Object> request,
            @PathVariable String path) {

        String apiName = path + "Api";

        if (false == apis.containsKey(apiName)) {
            return new ResponseEntity<>(ApiResponse.builder().errors(
                    Lists.newArrayList(apiName + " api not found.")).build(),
                    HttpStatus.OK);
        }

        SelenadeApi api = apis.get(apiName);

        ApiResponse<?> body;
        try {
            body = api.service(request);

        } catch (RuntimeException e) {
            e.printStackTrace();
            return new ResponseEntity<>(ApiResponse.builder().errors(
                    Lists.newArrayList(e.getLocalizedMessage())).build(),
                    HttpStatus.OK);
        }

        // init state
        HttpStatus status = HttpStatus.OK;
        ResponseEntity<ApiResponse<?>> rentity = new ResponseEntity<>(body, status);

        return rentity;
    }

}
