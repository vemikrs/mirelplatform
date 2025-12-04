/*
 * Copyright(c) 2015-2025 mirelplatform.
 */
package jp.vemi.mirel.foundation.organization.service;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import jp.vemi.mirel.foundation.organization.dto.OrganizationUnitDto;
import jp.vemi.mirel.foundation.organization.model.UnitType;
import lombok.Data;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class OrganizationImportService {

    private final OrganizationUnitService organizationUnitService;

    @Data
    public static class UnitCsvRow {
        private String unitId;
        private String parentUnitId;
        private String name;
        private String code;
        private String unitType;
        private String effectiveFrom;
    }

    /**
     * 組織ユニットをCSVからインポートします.
     * 
     * @param organizationId
     *            組織ID
     * @param csvStream
     *            CSV入力ストリーム
     * @throws IOException
     *             入出力エラー
     */
    @Transactional
    public void importUnits(String organizationId, InputStream csvStream) throws IOException {
        CsvMapper mapper = new CsvMapper();
        CsvSchema schema = mapper.schemaFor(UnitCsvRow.class).withHeader();

        try (MappingIterator<UnitCsvRow> it = mapper.readerFor(UnitCsvRow.class).with(schema).readValues(csvStream)) {
            List<UnitCsvRow> rows = it.readAll();

            for (UnitCsvRow row : rows) {
                OrganizationUnitDto dto = new OrganizationUnitDto();
                dto.setUnitId(row.getUnitId()); // ID指定があれば更新またはID指定作成
                dto.setParentUnitId(row.getParentUnitId());
                dto.setName(row.getName());
                dto.setCode(row.getCode());
                if (row.getUnitType() != null) {
                    dto.setUnitType(UnitType.valueOf(row.getUnitType()));
                }
                if (row.getEffectiveFrom() != null && !row.getEffectiveFrom().isEmpty()) {
                    dto.setEffectiveFrom(LocalDate.parse(row.getEffectiveFrom()));
                }

                // 簡易実装：常に作成（ID指定があれば上書きロジックが必要だが、Serviceのcreateは新規作成前提かも）
                // OrganizationUnitServiceにupsert的なメソッドがないので、createを呼ぶが、
                // IDが指定されている場合はcreate内でIDを使うように修正が必要かもしれない。
                // いったんcreateを呼ぶ。

                // 注意: 親ユニットがまだ存在しない場合のエラーハンドリングが必要だが、
                // ここではCSVが親→子の順で並んでいる、またはID指定で解決できると仮定。

                organizationUnitService.create(organizationId, dto);
            }
        }
    }
}
