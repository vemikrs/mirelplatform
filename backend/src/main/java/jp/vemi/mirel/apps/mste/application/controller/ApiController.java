/*
 * Copyright(c) 2015-2020 mirelplatform.
 */
package jp.vemi.mirel.apps.mste.application.controller;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jp.vemi.mirel.apps.mste.domain.api.MsteApi;
import jp.vemi.mirel.apps.mste.domain.service.GenerateService;
import jp.vemi.mirel.foundation.web.api.dto.ApiResponse;

@RestController
@RequestMapping("apps/mste/api")
public class ApiController {

    /** {@link GenerateService} */
    @Autowired
    protected GenerateService generateService;

    @Autowired()
    private Map<String, MsteApi> apis;

    @RequestMapping("/{path}")
    public ResponseEntity<ApiResponse<?>> index(@RequestBody Map<String, Object> request,
            @PathVariable String path) {

        String apiName = path + "Api";

        if (false == apis.containsKey(apiName)) {
            return new ResponseEntity<>(ApiResponse.builder().errors(
                    Lists.newArrayList(apiName + " api not found.")).build(),
                    HttpStatus.OK);
        }

        MsteApi api = apis.get(apiName);
        System.out.println(request);

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

        System.out.println(rentity);
        return rentity;
    }

    @RequestMapping("/gen")
    public Map<String, Object> gen() {

        Object sresult = generateService.invoke(null);
        System.out.println(sresult);

        Map<String, Object> map = newHashMap();

        Map<String, Object> child = newHashMap();
        child.put("key", "child1");

        List<Map<String, Object>> params = new ArrayList<>();

        {
            Map<String, Object> param = newHashMap();
            param.put("key", "param1");
            params.add(param);
        }

        {
            Map<String, Object> param = newHashMap();
            param.put("key", "param2");
            params.add(param);
        }

        map.put("child", child);
        map.put("params", params);

        return map;
    }

    protected static Map<String, Object> newHashMap() {
        return new LinkedHashMap<>();
    }

}
