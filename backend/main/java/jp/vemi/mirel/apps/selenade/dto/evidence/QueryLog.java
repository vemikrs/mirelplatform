/*
 * Copyright(c) 2015-2025 vemi/mirelplatform.
 */
package jp.vemi.mirel.apps.selenade.dto.evidence;

import java.util.List;
import java.util.Map;

import com.google.common.collect.Lists;

@lombok.Data
@lombok.NoArgsConstructor(access = lombok.AccessLevel.PRIVATE)
public class QueryLog {

    private String sql;
    private List<Map<String, Object>> resultList = Lists.newArrayList();

    public QueryLog(String sql) {
        this.sql = sql;
    }

    /**
     * countを取得します。
     *
     * @return count
     */
    public int getCount() {
        return this.resultList.size();
    }

}
