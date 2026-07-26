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
            Row student = sheet.createRow(4);
            student.createCell(0).setCellValue(1);
            student.createCell(1).setCellValue("Nguyễn Văn A");
            student.createCell(2).setCellValue("05/01/2026");
            student.createCell(3).setCellValue("0901 234-567");
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
    }
}
