package com.englishcenter.importdata;

import static org.assertj.core.api.Assertions.assertThat;

import com.englishcenter.classroom.ClassDayOfWeek;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

class LegacyExcelWorkbookParserTest {
    private final LegacyExcelWorkbookParser parser = new LegacyExcelWorkbookParser();

    @Test
    void parsesMetadataHeaderAndStudentRows() throws Exception {
        byte[] bytes;
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("GRADE 4-A");
            Row meta1 = sheet.createRow(0);
            meta1.createCell(0).setCellValue("Ngày bắt đầu lớp");
            meta1.createCell(1).setCellValue("05/01/2026");
            Row meta2 = sheet.createRow(1);
            meta2.createCell(0).setCellValue("Lịch học");
            meta2.createCell(1).setCellValue("T2, T4");
            Row header = sheet.createRow(3);
            header.createCell(0).setCellValue("STT");
            header.createCell(1).setCellValue("Họ tên");
            header.createCell(2).setCellValue("Ngày bắt đầu học");
            header.createCell(3).setCellValue("SĐT");
            header.createCell(5).setCellValue("  NGHỈ   KHÔNG PHÉP ");
            header.createCell(6).setCellValue("Xin phép");
            Row student = sheet.createRow(4);
            student.createCell(0).setCellValue(1);
            student.createCell(1).setCellValue("Nguyễn Văn A");
            student.createCell(2).setCellValue("05/01/2026");
            student.createCell(3).setCellValue("0901 234-567");
            student.createCell(5).setCellValue("12/01/2026; 14/01/2026");
            student.createCell(6).setCellValue("19/01/2026\n21/01/2026\n23/01/2026");
            workbook.write(output);
            bytes = output.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "legacy.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new ByteArrayInputStream(bytes)
        );

        var sheets = parser.parse(file);
        assertThat(sheets).hasSize(1);
        var parsed = sheets.getFirst();
        assertThat(parsed.sheetName()).isEqualTo("GRADE 4-A");
        assertThat(parsed.classroomStartDate()).isEqualTo(LocalDate.of(2026, 1, 5));
        assertThat(parsed.daysOfWeek()).containsExactlyInAnyOrder(ClassDayOfWeek.MONDAY, ClassDayOfWeek.WEDNESDAY);
        assertThat(parsed.rows()).hasSize(1);
        assertThat(parsed.rows().getFirst().studentName()).isEqualTo("Nguyễn Văn A");
        assertThat(parsed.rows().getFirst().phone()).isEqualTo("0901234567");
        assertThat(parsed.rows().getFirst().learningStartDate()).isEqualTo(LocalDate.of(2026, 1, 5));
        assertThat(parsed.rows().getFirst().absentDates()).containsExactly(
                LocalDate.of(2026, 1, 12),
                LocalDate.of(2026, 1, 14)
        );
        assertThat(parsed.rows().getFirst().excusedDates()).containsExactly(
                LocalDate.of(2026, 1, 19),
                LocalDate.of(2026, 1, 21),
                LocalDate.of(2026, 1, 23)
        );
    }

    @Test
    void deduplicatesAttendanceDatesAndReportsMalformedTokens() throws Exception {
        byte[] bytes;
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("GRADE 4-B");
            Row meta1 = sheet.createRow(0);
            meta1.createCell(0).setCellValue("Ngày bắt đầu lớp");
            meta1.createCell(1).setCellValue("05/06/2026");
            Row meta2 = sheet.createRow(1);
            meta2.createCell(0).setCellValue("Lịch học");
            meta2.createCell(1).setCellValue("T3, T6");
            Row header = sheet.createRow(3);
            header.createCell(1).setCellValue("Họ tên");
            header.createCell(2).setCellValue("Ngày bắt đầu học2");
            header.createCell(5).setCellValue("Nghỉ không phép");
            header.createCell(6).setCellValue("Xin phép");
            Row student = sheet.createRow(4);
            student.createCell(1).setCellValue("Nguyễn Văn Test");
            student.createCell(2).setCellValue("05/06/2026");
            student.createCell(5).setCellValue("12/06/2026,12/06/2026,16/06/2026");
            student.createCell(6).setCellValue("19/06/2026,31/06/2026");
            workbook.write(output);
            bytes = output.toByteArray();
        }

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "legacy.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new ByteArrayInputStream(bytes)
        );

        var row = parser.parse(file).getFirst().rows().getFirst();
        assertThat(row.absentDates()).containsExactly(
                LocalDate.of(2026, 6, 12),
                LocalDate.of(2026, 6, 16)
        );
        assertThat(row.excusedDates()).containsExactly(LocalDate.of(2026, 6, 19));
        assertThat(row.attendanceWarnings()).singleElement().asString().contains("bị nhập trùng");
        assertThat(row.attendanceErrors()).singleElement().asString().contains("31/06/2026");
    }
}
