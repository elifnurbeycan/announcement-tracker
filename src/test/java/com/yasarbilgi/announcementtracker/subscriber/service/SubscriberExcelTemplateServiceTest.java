package com.yasarbilgi.announcementtracker.subscriber.service;

import com.yasarbilgi.announcementtracker.service.SubscriberExcelTemplateService;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static org.assertj.core.api.Assertions.assertThat;

class SubscriberExcelTemplateServiceTest {

    private final SubscriberExcelTemplateService service = new SubscriberExcelTemplateService();

    @Test
    void createTemplate_ShouldCreateReadableWorkbook() throws Exception {
        byte[] template = service.createTemplate();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(template))) {
            assertThat(workbook.getSheet("Aboneler")).isNotNull();
            assertThat(workbook.getSheet("Aboneler").getRow(0).getCell(0).getStringCellValue())
                    .isEqualTo("E-Posta");
        }
    }
}
