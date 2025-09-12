/*
 * Copyright(c) 2018 mirelplatform.
 */
package jp.vemi.ste.domain.engine;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import jp.vemi.framework.util.InstanceUtil;
import jp.vemi.framework.util.JsonUtil;
import jp.vemi.ste.domain.context.SteContext;

public class SteExecutor {

    public static List<String> execute(String parameter) {

        Map<String, Object> defaultValues = Maps.newHashMap();
        List<Map<String, Object>> list;
        if (JsonUtil.isArray(parameter)) {
            list = InstanceUtil.forceCast(JsonUtil.parse(parameter, List.class));
        } else {
            list = Lists.newArrayList();
            Map<String, Object> map = InstanceUtil.forceCast(JsonUtil.parse(parameter, Map.class));
            if (map.containsKey("items")) {
                if (map.containsKey("defaultValues")) {
                    Map<String, Object> defaultValuesMap = InstanceUtil.forceCast(map.get("defaultValues"));
                    defaultValues.putAll(defaultValuesMap);
                }
                List<Map<String, Object>> items = InstanceUtil.forceCast(map.get("items"));
                list.addAll(items);
            } else {
                list.add(map);
            }
        }

        return SteExecutor.execute(list, defaultValues);
    }

    public static List<String> execute(List<Map<String, Object>> list) {
        return SteExecutor.execute(list, Maps.newHashMap());
    }

    public static List<String> execute(List<Map<String, Object>> list, Map<String, Object> defaultValues) {

        List<String> results = Lists.newArrayList();

        for (Map<String, Object> map : list) {

            for (Map.Entry<String, Object> entry : defaultValues.entrySet()) {
                if (map.containsKey(entry.getKey())) {
                    continue;
                }
                map.put(entry.getKey(), entry.getValue());
            }

            SteContext context = SteContext.newSteContext(map);
            String result = TemplateEngineProcessor.create(context).execute();
            results.add(result);
        }

        return results;
    }

}
