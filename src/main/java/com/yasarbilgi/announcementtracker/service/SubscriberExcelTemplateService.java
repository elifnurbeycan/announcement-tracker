package com.yasarbilgi.announcementtracker.service;

import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class SubscriberExcelTemplateService {

    public byte[] createTemplate() {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Aboneler");

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("E-Posta");
            header.createCell(1).setCellValue("Ad Soyad");
            header.createCell(2).setCellValue("Departmanlar (Opsiyonel, Örn: Java, Backend)");

            addExampleRow(sheet, 1, "ornek.abone1@kurum.com", "Abone 1", "Java");
            addExampleRow(sheet, 2, "ornek.abone2@kurum.com", "Abone 2", "Java, Backend");
            addExampleRow(sheet, 3, "ornek.abone3@kurum.com", "Abone 3", "");

            sheet.autoSizeColumn(0);
            sheet.autoSizeColumn(1);
            sheet.autoSizeColumn(2);
            workbook.write(output);
            return output.toByteArray();
        } catch (IOException exception) {
            throw new IllegalStateException("Abone Excel şablonu oluşturulamadı.", exception);
        }
    }

    private void addExampleRow(Sheet sheet, int rowIndex, String email, String fullName, String departments) {
        Row row = sheet.createRow(rowIndex);
        row.createCell(0).setCellValue(email);
        row.createCell(1).setCellValue(fullName);
        row.createCell(2).setCellValue(departments);
    }
}
