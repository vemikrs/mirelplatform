/*
 * Copyright(c) 2019 mirelplatform.
 */
package jp.vemi.ste.domain.engine;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.EncryptedDocumentException;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;

import groovy.lang.Tuple2;
import jp.vemi.framework.exeption.MirelApplicationException;
import jp.vemi.framework.exeption.MirelSystemException;
import jp.vemi.framework.util.CloseableUtil;

public class StructureReader {

    protected static class Const {
        final static String SHEET_NAME_MASTER = "_@generate";
        final static Integer MASTER_SHEET_HEADER_ROWNUM = 5;
    }

    public Map<String, List<Map<String, Object>>> read() {
        final String filePath = "C:\\data\\m2\\storage\\foundation\\filemanagement\\20\\02\\fe53d0cb-cbd2-47f4-a1f5-31f8de704243\\a_.xlsx";
        return read(filePath);
    }

    public Map<String, List<Map<String, Object>>> read(String filePath) {

        Workbook workbook = this.getWorkbook(filePath);

        Sheet masterSheet = workbook.getSheet(Const.SHEET_NAME_MASTER);

        if (null == masterSheet) {
            throw new MirelApplicationException("シートが見つかりません。シート名：" + Const.SHEET_NAME_MASTER, null);
        }

        boolean isHeaderReaded = false;
        Map<Integer, Tuple2<String, CellType>> headerItem = Maps.newLinkedHashMap();
        List<Map<String, Object>> details = Lists.newArrayList();
        int lastRowNum = masterSheet.getLastRowNum();
        for (int idx = Const.MASTER_SHEET_HEADER_ROWNUM - 1; idx <= lastRowNum; idx++) {
            Row item = masterSheet.getRow(idx);
            if (null == item) {
                continue;
            }
    
            if (false == isHeaderReaded) {
                item.forEach(cell -> {
                    headerItem.put(cell.getColumnIndex(),
                            new Tuple2<>(getCellValueWithNullSafe(cell).toString(), cell.getCellType()));
                });
                isHeaderReaded = true;
                continue;
            }

            Map<String, Object> detailKeyValue = Maps.newLinkedHashMap();

            if (CellType.BLANK == item.getCell(0).getCellType()) {
                continue;
            }

            item.forEach(cell -> {
                Tuple2<String, CellType> cellitem = headerItem.get(cell.getColumnIndex());
                if(null == cellitem) {
                    return;
                }
                detailKeyValue.put(cellitem.getV1(), getCellValue(cell));
            });
            if (ObjectUtils.isEmpty(detailKeyValue.get("nouse"))) {
                details.add(detailKeyValue);
            }
        }

        Set<String> demodels = Sets.newLinkedHashSet();
        details.forEach(map -> {
            String sheetName = (String) map.get("model");
            if(false == StringUtils.isEmpty(sheetName)) {
                demodels.add(sheetName);
            }
        });;
        
        Map<String, List<Map<String, Object>>> dataElementsItems = Maps.newLinkedHashMap();
        for (String demodel : demodels) {
            List<Map<String, Object>> indicate = details.stream().filter(detail -> {
                return demodel.equals(detail.get("model"));
            }).collect(Collectors.toList());
            dataElementsItems.put(demodel, getSheetData(workbook, indicate));
        }

        System.out.println(dataElementsItems);

        return dataElementsItems;
    }

    protected static String getMapValueAsString(Map<String, Object> map, String key) {
        Object value = map.get(key);
        if(null == value) {
            return "";
        } else {
            return value.toString();
        }
    }

    protected List<Map<String, Object>> getSheetData(Workbook workbook, List<Map<String, Object>> inputColumnDefs) {

        Map<String, Object> firstInputColumnDef = inputColumnDefs.get(0);

        String sheetName = getMapValueAsString(firstInputColumnDef, "sheetName");
        double startLineDouble = (double) firstInputColumnDef.get("startLine");
        int startIdx = ((int) startLineDouble) -1;

        List<Map<String, Object>> sheetData = Lists.newArrayList();

        Sheet sheet = workbook.getSheet(sheetName);
        if(null == sheet) {
            log("シートがありません。シート名：" + sheetName);
            return sheetData;
        }

        List<Map<String, Object>> columnDefs = Lists.newArrayList();
        for (Map<String, Object> columnDef : inputColumnDefs) {
            String refa = (String)columnDef.get("column");
            CellReference ref = new CellReference(refa + "1");
            short columnIdx = ref.getCol();
            columnDef.put("columnIdx", columnIdx);
            columnDefs.add(columnDef);
        }

        int lastRowNum = sheet.getLastRowNum();
        for (int idx = startIdx; idx <= lastRowNum; idx++) {
            Row item = sheet.getRow(idx);

            boolean isSkipRecord = false;
            // data reference
            Map<String, Object> sheetDataRecord = Maps.newLinkedHashMap();
            for (Map<String, Object> columnDef : columnDefs) {
                short cellnum = (short) columnDef.get("columnIdx");
                Cell cell = item.getCell(cellnum);
                Object value = getCellValue(cell);
                if (null == value) {
                    boolean isSkipColumnIfEmpty = getMapValueAsString(columnDef, "skipEmpty").length() > 0;
                    if (isSkipColumnIfEmpty) {
                        isSkipRecord = true;
                        break;
                    } else {
                        value = "";
                    }
                }
                sheetDataRecord.put(getMapValueAsString(columnDef, "dataElement"), value);
            }

            if (isSkipRecord) {
                // skip target record
                continue;
            }

            sheetData.add(sheetDataRecord);
        }

        return sheetData;
    }

    protected static void log(String message) {
        System.out.println(message);
    }

    protected Object getCellValueWithNullSafe(Cell cell) {
        Object val = getCellValue(cell);
        if (null == val) {
            return new Object();
        } else {
            return val;
        }
    }

    protected Object getCellValue(Cell cell) {

        if (null == cell) {
            return null;
        }

        CellType cellType = cell.getCellType();
        CellType valueType;
        if (CellType.FORMULA.equals(cell.getCellType())) {
            valueType = cell.getCachedFormulaResultType();
        } else {
            valueType = cellType;
        }
        switch(valueType) {
            case NUMERIC: // including datetime
                if (org.apache.poi.ss.usermodel.DateUtil.isCellDateFormatted(cell)) {
                    // datetime format.
                    return cell.getDateCellValue();

                } else {
                    // numeric format.
                    return cell.getNumericCellValue();
                }
            case STRING:
                return cell.getStringCellValue();
            case BOOLEAN:
                return cell.getBooleanCellValue();
            case BLANK:
                return null;
            case ERROR:
                return cell.getErrorCellValue();
            default:
                return null;
        }
    }

    protected String getCellValueAsString(Cell cell){
        Object cellValue = getCellValue(cell);

        if (cellValue == null) {
            return "";
        }

        if (cellValue instanceof String) {
            return String.class.cast(cellValue);
        }

        if (cellValue instanceof Date) {
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss.SSS");
            LocalDateTime localDateTime = LocalDateTime.ofInstant(Date.class.cast(cellValue).toInstant(), ZoneId.systemDefault());
            return dateTimeFormatter.format(localDateTime);
        }

        if (cellValue instanceof Double) {
            return Double.class.cast(cellValue).toString();
        }

        return cellValue.toString();
    }

    protected Workbook getWorkbook(String filePath) {

        InputStream isp;
        try {
            isp = new FileInputStream(filePath);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            throw new MirelSystemException(e);
        }

        try {
            return WorkbookFactory.create(isp);
        } catch (EncryptedDocumentException | IOException e) {
            e.printStackTrace();
            throw new MirelSystemException(e);
        } finally {
            CloseableUtil.close(isp);
        }

    }
}