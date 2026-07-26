package com.englishcenter.importdata;

import com.englishcenter.classroom.ClassDayOfWeek;
import com.englishcenter.common.exception.BusinessException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

@Component
public class LegacyExcelWorkbookParser {
    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("d/M/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("d-M-yyyy"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
    };

    private final DataFormatter dataFormatter = new DataFormatter(Locale.ROOT);

    public List<ParsedSheet> parse(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Excel file is required");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.toLowerCase(Locale.ROOT).endsWith(".xlsx")
                && !filename.toLowerCase(Locale.ROOT).endsWith(".xls"))) {
            throw new BusinessException("Only .xlsx or .xls Excel files are supported");
        }

        try (InputStream inputStream = file.getInputStream();
             Workbook workbook = WorkbookFactory.create(inputStream)) {
            List<ParsedSheet> sheets = new ArrayList<>();
            for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                Sheet sheet = workbook.getSheetAt(i);
                if (sheet == null || workbook.isSheetHidden(i) || workbook.isSheetVeryHidden(i)) {
                    continue;
                }
                String sheetName = LegacyImportNormalizer.normalizeClassroomName(sheet.getSheetName());
                if (sheetName == null) {
                    continue;
                }
                sheets.add(parseSheet(i, sheetName, sheet));
            }
            if (sheets.isEmpty()) {
                throw new BusinessException("Workbook has no importable sheets");
            }
            return sheets;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("Unable to read Excel workbook: " + ex.getMessage());
        }
    }

    private ParsedSheet parseSheet(int sheetIndex, String sheetName, Sheet sheet) {
        LocalDate classroomStartDate = null;
        Set<ClassDayOfWeek> daysOfWeek = new HashSet<>();
        List<String> sheetErrors = new ArrayList<>();
        List<String> sheetWarnings = new ArrayList<>();

        int headerRowIndex = detectHeaderRow(sheet);
        for (int r = 0; r < (headerRowIndex >= 0 ? headerRowIndex : Math.min(8, sheet.getLastRowNum() + 1)); r++) {
            Row row = sheet.getRow(r);
            if (row == null) {
                continue;
            }
            String label = cellAsString(row.getCell(0));
            String value = cellAsString(row.getCell(1));
            if (label == null) {
                continue;
            }
            String normalizedLabel = label.toLowerCase(Locale.ROOT);
            if (isStartDateLabel(normalizedLabel)) {
                classroomStartDate = parseDateCell(row.getCell(1));
                if (classroomStartDate == null && value != null) {
                    sheetErrors.add("Invalid classroomStartDate in metadata: " + value);
                }
            } else if (isDaysOfWeekLabel(normalizedLabel)) {
                daysOfWeek = LegacyImportNormalizer.parseDaysOfWeek(value);
                if (daysOfWeek.isEmpty() && value != null && !value.isBlank()) {
                    sheetErrors.add("Unable to parse daysOfWeek metadata: " + value);
                }
            }
        }

        if (classroomStartDate == null) {
            sheetErrors.add("classroomStartDate metadata is required (label in column A, date in column B)");
        }
        if (daysOfWeek.isEmpty()) {
            sheetErrors.add("daysOfWeek metadata is required (label in column A, value in column B)");
        }

        ColumnMap columnMap = resolveColumns(sheet, headerRowIndex);
        int dataStartRow = headerRowIndex >= 0 ? headerRowIndex + 1 : 0;
        if (headerRowIndex < 0) {
            sheetWarnings.add("Header row not found; using default columns B=name, C=learningStartDate, D=phone");
            dataStartRow = findFirstDataRow(sheet);
        }

        List<ParsedRow> rows = new ArrayList<>();
        int lastRow = sheet.getLastRowNum();
        for (int r = dataStartRow; r <= lastRow; r++) {
            Row row = sheet.getRow(r);
            if (row == null || isEmptyRow(row, columnMap)) {
                continue;
            }
            if (looksLikeHeaderRow(row)) {
                continue;
            }

            String studentName = LegacyImportNormalizer.normalizePersonName(cellAsString(row.getCell(columnMap.nameCol())));
            String phoneRaw = cellAsPhoneString(row.getCell(columnMap.phoneCol()));
            String phone = LegacyImportNormalizer.normalizePhone(phoneRaw);
            LocalDate learningStartDate = parseDateCell(row.getCell(columnMap.dateCol()));

            if (studentName == null && phone == null && learningStartDate == null) {
                continue;
            }

            rows.add(new ParsedRow(
                    r + 1,
                    studentName,
                    phone,
                    learningStartDate,
                    phoneRaw
            ));
        }

        if (rows.isEmpty()) {
            sheetErrors.add("No student rows found in sheet");
        }

        return new ParsedSheet(
                sheetIndex,
                sheetName,
                classroomStartDate,
                daysOfWeek,
                sheetErrors,
                sheetWarnings,
                rows
        );
    }

    private int detectHeaderRow(Sheet sheet) {
        int maxScan = Math.min(sheet.getLastRowNum(), 30);
        for (int r = 0; r <= maxScan; r++) {
            Row row = sheet.getRow(r);
            if (row != null && looksLikeHeaderRow(row)) {
                return r;
            }
        }
        return -1;
    }

    private boolean looksLikeHeaderRow(Row row) {
        for (Cell cell : row) {
            String value = cellAsString(cell);
            if (value == null) {
                continue;
            }
            String normalized = value.toLowerCase(Locale.ROOT);
            if (normalized.contains("họ tên")
                    || normalized.contains("ho ten")
                    || normalized.contains("họ và tên")
                    || normalized.contains("student name")
                    || normalized.equals("tên")
                    || normalized.contains("học viên")) {
                return true;
            }
        }
        return false;
    }

    private ColumnMap resolveColumns(Sheet sheet, int headerRowIndex) {
        if (headerRowIndex < 0) {
            return new ColumnMap(1, 2, 3);
        }
        Row header = sheet.getRow(headerRowIndex);
        Integer nameCol = null;
        Integer dateCol = null;
        Integer phoneCol = null;
        short lastCell = header.getLastCellNum();
        for (int c = 0; c < lastCell; c++) {
            String value = cellAsString(header.getCell(c));
            if (value == null) {
                continue;
            }
            String normalized = value.toLowerCase(Locale.ROOT);
            if (nameCol == null && (normalized.contains("họ tên")
                    || normalized.contains("ho ten")
                    || normalized.contains("họ và tên")
                    || normalized.contains("student name")
                    || normalized.equals("tên")
                    || normalized.contains("học viên"))) {
                nameCol = c;
            } else if (dateCol == null && (normalized.contains("ngày bắt đầu")
                    || normalized.contains("ngay bat dau")
                    || normalized.contains("learning start")
                    || normalized.contains("start date")
                    || normalized.equals("ngày học"))) {
                dateCol = c;
            } else if (phoneCol == null && (normalized.contains("sđt")
                    || normalized.contains("sdt")
                    || normalized.contains("phone")
                    || normalized.contains("điện thoại")
                    || normalized.contains("dien thoai"))) {
                phoneCol = c;
            }
        }
        return new ColumnMap(
                nameCol != null ? nameCol : 1,
                dateCol != null ? dateCol : 2,
                phoneCol != null ? phoneCol : 3
        );
    }

    private int findFirstDataRow(Sheet sheet) {
        int maxScan = Math.min(sheet.getLastRowNum(), 20);
        for (int r = 0; r <= maxScan; r++) {
            Row row = sheet.getRow(r);
            if (row == null || looksLikeHeaderRow(row)) {
                continue;
            }
            String maybeName = cellAsString(row.getCell(1));
            LocalDate maybeDate = parseDateCell(row.getCell(2));
            if (LegacyImportNormalizer.normalizePersonName(maybeName) != null && maybeDate != null) {
                return r;
            }
        }
        return 0;
    }

    private boolean isEmptyRow(Row row, ColumnMap columnMap) {
        return cellAsString(row.getCell(columnMap.nameCol())) == null
                && cellAsPhoneString(row.getCell(columnMap.phoneCol())) == null
                && parseDateCell(row.getCell(columnMap.dateCol())) == null;
    }

    private boolean isStartDateLabel(String label) {
        return label.contains("ngày bắt đầu lớp")
                || label.contains("ngay bat dau lop")
                || label.contains("classroomstartdate")
                || label.contains("start date")
                || label.equals("ngày bắt đầu")
                || label.equals("startdate");
    }

    private boolean isDaysOfWeekLabel(String label) {
        return label.contains("lịch học")
                || label.contains("lich hoc")
                || label.contains("daysofweek")
                || label.contains("days of week")
                || label.equals("ngày học trong tuần")
                || label.equals("days");
    }

    private String cellAsString(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        String formatted = dataFormatter.formatCellValue(cell).trim();
        return formatted.isEmpty() ? null : formatted;
    }

    /**
     * Phone must stay a string so leading zeros are preserved.
     * Numeric Excel cells are formatted without scientific notation.
     */
    private String cellAsPhoneString(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && !DateUtil.isCellDateFormatted(cell)) {
            BigDecimal number = BigDecimal.valueOf(cell.getNumericCellValue()).stripTrailingZeros();
            return number.toPlainString();
        }
        return cellAsString(cell);
    }

    private LocalDate parseDateCell(Cell cell) {
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return null;
        }
        if (cell.getCellType() == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
            Date date = cell.getDateCellValue();
            return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            double value = cell.getNumericCellValue();
            if (DateUtil.isValidExcelDate(value)) {
                Date date = DateUtil.getJavaDate(value);
                return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
            }
        }
        String text = cellAsString(cell);
        if (text == null) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                return LocalDate.parse(text, formatter);
            } catch (DateTimeParseException ignored) {
                // try next
            }
        }
        return null;
    }

    public record ParsedSheet(
            int sheetIndex,
            String sheetName,
            LocalDate classroomStartDate,
            Set<ClassDayOfWeek> daysOfWeek,
            List<String> errors,
            List<String> warnings,
            List<ParsedRow> rows
    ) {
    }

    public record ParsedRow(
            int excelRowNumber,
            String studentName,
            String phone,
            LocalDate learningStartDate,
            String rawPhone
    ) {
    }

    private record ColumnMap(int nameCol, int dateCol, int phoneCol) {
    }
}
