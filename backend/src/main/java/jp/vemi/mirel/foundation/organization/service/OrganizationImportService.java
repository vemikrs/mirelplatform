/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import jp.vemi.mirel.foundation.feature.TenantContext;
import jp.vemi.mirel.foundation.organization.dto.OrganizationDto;
import jp.vemi.mirel.foundation.organization.model.OrganizationType;
import lombok.Data;
import lombok.RequiredArgsConstructor;

/**
 * 組織インポートサービス.
 */
@Service
@RequiredArgsConstructor
public class OrganizationImportService {

    private final OrganizationService organizationService;

    @Data
    public static class OrganizationCsvRow {
        private String id;
        private String parentId;
        private String name;
        private String displayName;
        private String code;
        private String type;
        private String startDate;
    }

    /**
     * 組織をCSVからインポートします.
     * 
     * @param tenantId テナントID
     * @param csvStream CSV入力ストリーム
     * @throws IOException 入出力エラー
     */
    @Transactional
    public void importOrganizations(String tenantId, InputStream csvStream) throws IOException {
        // tenantId を TenantContext に設定
        TenantContext.setTenantId(tenantId);

        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(OrganizationCsvRow.class).withHeader();

        try (MappingIterator<OrganizationCsvRow> it = mapper.readerFor(OrganizationCsvRow.class).with(schema).readValues(csvStream)) {
            List<OrganizationCsvRow> rows = it.readAll();

            // CSV id → 生成UUID のマッピング（親子参照解決用）
            Map<String, String> idMapping = new HashMap<>();

            for (OrganizationCsvRow row : rows) {
                OrganizationDto dto = new OrganizationDto();
                
                // parentId を idMapping で変換（既にインポート済みの親を参照）
                String resolvedParentId = null;
                if (row.getParentId() != null && !row.getParentId().isEmpty()) {
                    resolvedParentId = idMapping.get(row.getParentId());
                    if (resolvedParentId == null) {
                        // マッピングにない場合は元のIDをそのまま使用（既存データ参照の可能性）
                        resolvedParentId = row.getParentId();
                    }
                }
                dto.setParentId(resolvedParentId);
                
                dto.setName(row.getName());
                dto.setDisplayName(row.getDisplayName());
                dto.setCode(row.getCode());
                if (row.getType() != null && !row.getType().isEmpty()) {
                    dto.setType(OrganizationType.valueOf(row.getType()));
                }
                if (row.getStartDate() != null && !row.getStartDate().isEmpty()) {
                    dto.setStartDate(LocalDate.parse(row.getStartDate()));
                }

                OrganizationDto created = organizationService.create(dto);
                
                // CSV id → 生成UUID をマッピングに追加
                if (row.getId() != null && !row.getId().isEmpty()) {
                    idMapping.put(row.getId(), created.getId());
                }
            }
        }
    }
}
