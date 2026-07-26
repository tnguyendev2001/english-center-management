package com.englishcenter.finance;

import com.englishcenter.common.exception.BusinessException;
import com.englishcenter.finance.dto.AccountBalanceResponse;
import com.englishcenter.finance.dto.CashTransactionResponse;
import com.englishcenter.finance.dto.CategoryBreakdownItemResponse;
import com.englishcenter.finance.dto.ComparePeriodsResponse;
import com.englishcenter.finance.dto.FinanceOverviewResponse;
import com.englishcenter.finance.dto.PaymentReconciliationResponse;
import com.englishcenter.finance.dto.PeriodSummaryResponse;
import com.englishcenter.finance.dto.StudentDebtItemResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.CreationHelper;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FinanceExportService {
    private final FinanceCalculationService financeCalculationService;

    public FinanceExportService(FinanceCalculationService financeCalculationService) {
        this.financeCalculationService = financeCalculationService;
    }

    @Transactional(readOnly = true)
    public byte[] exportWorkbook(FinanceScope scope, Integer year, Integer month) {
        FinanceScope effectiveScope = scope != null ? scope : FinanceScope.MONTH;
        FinanceOverviewResponse overview = financeCalculationService.getOverview(effectiveScope, year, month);

        LocalDate fromDate = overview.periodStart();
        LocalDate toDate = overview.periodEnd();
        PeriodSummaryResponse period = null;
        if (effectiveScope == FinanceScope.MONTH && overview.year() != null && overview.month() != null) {
            period = financeCalculationService.calculatePeriodSummary(overview.year(), overview.month(), true);
        }

        List<CashTransactionResponse> ledger = financeCalculationService
                .searchTransactions(fromDate, toDate, null, null, null, null, null, null, 0, 100)
                .getContent();
        List<CategoryBreakdownItemResponse> incomeCats = financeCalculationService.calculateCategoryBreakdown(
                fromDate, toDate, CategoryDirection.INCOME
        );
        List<CategoryBreakdownItemResponse> expenseCats = financeCalculationService.calculateCategoryBreakdown(
                fromDate, toDate, CategoryDirection.EXPENSE
        );
        List<AccountBalanceResponse> balances = financeCalculationService.calculateAccountBalances(toDate);
        List<StudentDebtItemResponse> debts = financeCalculationService.calculateStudentDebtDetailsAsOf(toDate);
        PaymentReconciliationResponse reconciliation = financeCalculationService.reconcilePayments(fromDate, toDate);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle moneyStyle = moneyStyle(workbook);
            CellStyle dateStyle = dateStyle(workbook);

            writeOverviewSheet(workbook, overview, period, headerStyle, moneyStyle);
            writeLedgerSheet(workbook, ledger, headerStyle, moneyStyle, dateStyle);
            writeCategorySheet(workbook, "THU THEO DANH MỤC", incomeCats, headerStyle, moneyStyle);
            writeCategorySheet(workbook, "CHI THEO DANH MỤC", expenseCats, headerStyle, moneyStyle);
            writeBalancesSheet(workbook, balances, headerStyle, moneyStyle, dateStyle);
            writeDebtSheet(workbook, debts, headerStyle, moneyStyle);
            writeReconciliationSheet(workbook, reconciliation, headerStyle, moneyStyle);

            if (effectiveScope == FinanceScope.MONTH && overview.year() != null && overview.month() != null) {
                int y = overview.year();
                int m = overview.month();
                YearMonth current = YearMonth.of(y, m);
                YearMonth previousMonth = current.minusMonths(1);
                try {
                    ComparePeriodsResponse monthCompare = financeCalculationService.comparePeriods(
                            y, m, previousMonth.getYear(), previousMonth.getMonthValue()
                    );
                    writeCompareSheet(workbook, "SO SÁNH THÁNG", monthCompare, headerStyle, moneyStyle);
                } catch (BusinessException ignored) {
                    // Previous month may be before financeStartDate.
                }
                YearMonth previousYear = current.minusYears(1);
                try {
                    ComparePeriodsResponse yearCompare = financeCalculationService.comparePeriods(
                            y, m, previousYear.getYear(), previousYear.getMonthValue()
                    );
                    writeCompareSheet(workbook, "SO SÁNH CÙNG KỲ NĂM TRƯỚC", yearCompare, headerStyle, moneyStyle);
                } catch (BusinessException ignored) {
                    // Previous year may be before financeStartDate; skip year compare sheet.
                }
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to export finance workbook", ex);
        }
    }

    private void writeOverviewSheet(
            Workbook workbook,
            FinanceOverviewResponse overview,
            PeriodSummaryResponse period,
            CellStyle headerStyle,
            CellStyle moneyStyle
    ) {
        Sheet sheet = workbook.createSheet("TỔNG QUAN");
        Row header = sheet.createRow(0);
        createTextCell(header, 0, "Chỉ tiêu", headerStyle);
        createTextCell(header, 1, "Giá trị", headerStyle);

        boolean allScope = overview.scope() == FinanceScope.ALL;
        Object[][] rows = {
                {"Phạm vi", null},
                {"Tổng số dư", overview.totalBalance()},
                {"Tiền mặt", overview.cashBalance()},
                {"Tiền ngân hàng", overview.bankBalance()},
                {allScope ? "Tổng thu" : "Thu tháng", overview.totalIncome()},
                {allScope ? "Tổng chi" : "Chi tháng", overview.totalExpense()},
                {allScope ? "Dòng tiền ròng toàn kỳ" : "Dòng tiền ròng tháng", overview.netCashFlow()},
                {"Công nợ học viên", overview.outstandingDebt()}
        };
        int rowIdx = 1;
        Row scopeRow = sheet.createRow(rowIdx++);
        createTextCell(scopeRow, 0, "Phạm vi", null);
        createTextCell(scopeRow, 1, overview.scope().name()
                + " (" + overview.periodStart() + " → " + overview.periodEnd() + ")", null);
        for (int i = 1; i < rows.length; i++) {
            Row row = sheet.createRow(rowIdx++);
            createTextCell(row, 0, (String) rows[i][0], null);
            createMoneyCell(row, 1, (BigDecimal) rows[i][1], moneyStyle);
        }
        if (period != null) {
            Object[][] periodRows = {
                    {"Số dư đầu kỳ", period.openingBalance()},
                    {"Thu kỳ", period.totalIncome()},
                    {"Chi kỳ", period.totalExpense()},
                    {"Số dư cuối kỳ", period.closingBalance()}
            };
            for (Object[] periodRow : periodRows) {
                Row row = sheet.createRow(rowIdx++);
                createTextCell(row, 0, (String) periodRow[0], null);
                createMoneyCell(row, 1, (BigDecimal) periodRow[1], moneyStyle);
            }
        }
        freezeAndFilter(sheet, 2);
        autosize(sheet, 2);
    }

    private void writeLedgerSheet(
            Workbook workbook,
            List<CashTransactionResponse> ledger,
            CellStyle headerStyle,
            CellStyle moneyStyle,
            CellStyle dateStyle
    ) {
        Sheet sheet = workbook.createSheet("SỔ THU CHI");
        String[] headers = {
                "Ngày", "Mã giao dịch", "Nội dung", "Danh mục", "Tài khoản", "Nguồn", "Thu", "Chi", "Trạng thái"
        };
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            createTextCell(header, i, headers[i], headerStyle);
        }
        int r = 1;
        for (CashTransactionResponse tx : ledger) {
            Row row = sheet.createRow(r++);
            createDateCell(row, 0, tx.transactionDate(), dateStyle);
            createTextCell(row, 1, tx.transactionCode(), null);
            createTextCell(row, 2, tx.description(), null);
            createTextCell(row, 3, tx.categoryName(), null);
            createTextCell(row, 4, tx.accountName(), null);
            createTextCell(row, 5, tx.sourceType() != null ? tx.sourceType().name() : null, null);
            createMoneyCell(row, 6, tx.incomeAmount(), moneyStyle);
            createMoneyCell(row, 7, tx.expenseAmount(), moneyStyle);
            createTextCell(row, 8, tx.status() != null ? tx.status().name() : null, null);
        }
        freezeAndFilter(sheet, headers.length);
        autosize(sheet, headers.length);
    }

    private void writeCategorySheet(
            Workbook workbook,
            String sheetName,
            List<CategoryBreakdownItemResponse> items,
            CellStyle headerStyle,
            CellStyle moneyStyle
    ) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row header = sheet.createRow(0);
        createTextCell(header, 0, "Mã", headerStyle);
        createTextCell(header, 1, "Tên danh mục", headerStyle);
        createTextCell(header, 2, "Số tiền", headerStyle);
        int r = 1;
        BigDecimal total = BigDecimal.ZERO;
        for (CategoryBreakdownItemResponse item : items) {
            Row row = sheet.createRow(r++);
            createTextCell(row, 0, item.categoryCode(), null);
            createTextCell(row, 1, item.categoryName(), null);
            createMoneyCell(row, 2, item.amount(), moneyStyle);
            total = total.add(item.amount());
        }
        Row totalRow = sheet.createRow(r);
        createTextCell(totalRow, 0, "TỔNG", headerStyle);
        createMoneyCell(totalRow, 2, total, moneyStyle);
        freezeAndFilter(sheet, 3);
        autosize(sheet, 3);
    }

    private void writeBalancesSheet(
            Workbook workbook,
            List<AccountBalanceResponse> balances,
            CellStyle headerStyle,
            CellStyle moneyStyle,
            CellStyle dateStyle
    ) {
        Sheet sheet = workbook.createSheet("SỐ DƯ TÀI KHOẢN");
        String[] headers = {"Mã", "Tên", "Loại", "Số dư đầu", "Thu", "Chi", "Số dư", "Ngày chốt"};
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            createTextCell(header, i, headers[i], headerStyle);
        }
        int r = 1;
        for (AccountBalanceResponse balance : balances) {
            Row row = sheet.createRow(r++);
            createTextCell(row, 0, balance.accountCode(), null);
            createTextCell(row, 1, balance.accountName(), null);
            createTextCell(row, 2, balance.type().name(), null);
            createMoneyCell(row, 3, balance.openingBalance(), moneyStyle);
            createMoneyCell(row, 4, balance.inflow(), moneyStyle);
            createMoneyCell(row, 5, balance.outflow(), moneyStyle);
            createMoneyCell(row, 6, balance.balance(), moneyStyle);
            createDateCell(row, 7, balance.asOfDate(), dateStyle);
        }
        freezeAndFilter(sheet, headers.length);
        autosize(sheet, headers.length);
    }

    private void writeDebtSheet(
            Workbook workbook,
            List<StudentDebtItemResponse> debts,
            CellStyle headerStyle,
            CellStyle moneyStyle
    ) {
        Sheet sheet = workbook.createSheet("CÔNG NỢ HỌC VIÊN");
        String[] headers = {"Mã HV", "Họ tên", "Lớp", "Công nợ", "Số hóa đơn"};
        Row header = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            createTextCell(header, i, headers[i], headerStyle);
        }
        int r = 1;
        for (StudentDebtItemResponse debt : debts) {
            Row row = sheet.createRow(r++);
            createTextCell(row, 0, debt.studentCode(), null);
            createTextCell(row, 1, debt.studentName(), null);
            createTextCell(row, 2, debt.classroomName(), null);
            createMoneyCell(row, 3, debt.remainingDebt(), moneyStyle);
            Cell count = row.createCell(4);
            count.setCellValue(debt.debtInvoiceCount());
        }
        freezeAndFilter(sheet, headers.length);
        autosize(sheet, headers.length);
    }

    private void writeReconciliationSheet(
            Workbook workbook,
            PaymentReconciliationResponse reconciliation,
            CellStyle headerStyle,
            CellStyle moneyStyle
    ) {
        Sheet sheet = workbook.createSheet("ĐỐI SOÁT PAYMENT");
        Row header = sheet.createRow(0);
        createTextCell(header, 0, "Chỉ tiêu", headerStyle);
        createTextCell(header, 1, "Giá trị", headerStyle);

        createTextCell(sheet.createRow(1), 0, "Trạng thái", null);
        createTextCell(sheet.createRow(1), 1, reconciliation.status().name(), null);
        Row r2 = sheet.createRow(2);
        createTextCell(r2, 0, "Tổng tiền VALID Payment", null);
        createMoneyCell(r2, 1, reconciliation.totalPaymentAmount(), moneyStyle);
        Row r3 = sheet.createRow(3);
        createTextCell(r3, 0, "Tổng thu học phí trên sổ", null);
        createMoneyCell(r3, 1, reconciliation.totalTuitionLedgerAmount(), moneyStyle);
        Row r4 = sheet.createRow(4);
        createTextCell(r4, 0, "Chênh lệch", null);
        createMoneyCell(r4, 1, reconciliation.difference(), moneyStyle);

        Row mismatchHeader = sheet.createRow(6);
        createTextCell(mismatchHeader, 0, "Loại lệch", headerStyle);
        createTextCell(mismatchHeader, 1, "Payment", headerStyle);
        createTextCell(mismatchHeader, 2, "Giao dịch", headerStyle);
        createTextCell(mismatchHeader, 3, "Chi tiết", headerStyle);
        int rowIdx = 7;
        for (var item : reconciliation.mismatches()) {
            Row row = sheet.createRow(rowIdx++);
            createTextCell(row, 0, item.mismatchType(), null);
            createTextCell(row, 1, item.paymentCode(), null);
            createTextCell(row, 2, item.transactionCode(), null);
            createTextCell(row, 3, item.detail(), null);
        }
        autosize(sheet, 4);
    }

    private void writeCompareSheet(
            Workbook workbook,
            String sheetName,
            ComparePeriodsResponse compare,
            CellStyle headerStyle,
            CellStyle moneyStyle
    ) {
        Sheet sheet = workbook.createSheet(sheetName);
        Row header = sheet.createRow(0);
        createTextCell(header, 0, "Chỉ tiêu", headerStyle);
        createTextCell(header, 1, compare.currentPeriod().label(), headerStyle);
        createTextCell(header, 2, compare.comparisonPeriod().label(), headerStyle);
        createTextCell(header, 3, "Chênh lệch", headerStyle);
        createTextCell(header, 4, "%", headerStyle);

        writeCompareRow(sheet, 1, "Thu", compare.current().income(), compare.comparison().income(),
                compare.income().amount(), compare.income().percentage(), moneyStyle);
        writeCompareRow(sheet, 2, "Chi", compare.current().expense(), compare.comparison().expense(),
                compare.expense().amount(), compare.expense().percentage(), moneyStyle);
        writeCompareRow(sheet, 3, "Dòng tiền ròng", compare.current().netCashFlow(), compare.comparison().netCashFlow(),
                compare.netCashFlow().amount(), compare.netCashFlow().percentage(), moneyStyle);
        writeCompareRow(sheet, 4, "Số dư cuối", compare.current().closingBalance(), compare.comparison().closingBalance(),
                compare.closingBalance().amount(), compare.closingBalance().percentage(), moneyStyle);
        writeCompareRow(sheet, 5, "Công nợ", compare.current().outstandingDebt(), compare.comparison().outstandingDebt(),
                compare.outstandingDebt().amount(), compare.outstandingDebt().percentage(), moneyStyle);

        if (compare.message() != null) {
            createTextCell(sheet.createRow(7), 0, compare.message(), null);
        }
        autosize(sheet, 5);
    }

    private void writeCompareRow(
            Sheet sheet,
            int rowIdx,
            String label,
            BigDecimal current,
            BigDecimal previous,
            BigDecimal absDiff,
            BigDecimal pct,
            CellStyle moneyStyle
    ) {
        Row row = sheet.createRow(rowIdx);
        createTextCell(row, 0, label, null);
        createMoneyCell(row, 1, current, moneyStyle);
        createMoneyCell(row, 2, previous, moneyStyle);
        createMoneyCell(row, 3, absDiff, moneyStyle);
        if (pct != null) {
            Cell cell = row.createCell(4);
            cell.setCellValue(pct.doubleValue());
        } else {
            createTextCell(row, 4, null, null);
        }
    }

    private CellStyle headerStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }

    private CellStyle moneyStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        style.setDataFormat(workbook.createDataFormat().getFormat("#,##0.00"));
        return style;
    }

    private CellStyle dateStyle(Workbook workbook) {
        CellStyle style = workbook.createCellStyle();
        CreationHelper helper = workbook.getCreationHelper();
        style.setDataFormat(helper.createDataFormat().getFormat("dd/mm/yyyy"));
        return style;
    }

    private void createTextCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value);
        }
        if (style != null) {
            cell.setCellStyle(style);
        }
    }

    private void createMoneyCell(Row row, int col, BigDecimal value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(value.doubleValue());
        }
        cell.setCellStyle(style);
    }

    private void createDateCell(Row row, int col, LocalDate value, CellStyle style) {
        Cell cell = row.createCell(col);
        if (value != null) {
            cell.setCellValue(java.sql.Date.valueOf(value));
        }
        cell.setCellStyle(style);
    }

    private void freezeAndFilter(Sheet sheet, int columns) {
        sheet.createFreezePane(0, 1);
        if (sheet.getPhysicalNumberOfRows() > 0) {
            sheet.setAutoFilter(new CellRangeAddress(0, Math.max(sheet.getLastRowNum(), 0), 0, columns - 1));
        }
    }

    private void autosize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
            int width = sheet.getColumnWidth(i);
            sheet.setColumnWidth(i, Math.min(width + 1000, 15000));
        }
    }
}
