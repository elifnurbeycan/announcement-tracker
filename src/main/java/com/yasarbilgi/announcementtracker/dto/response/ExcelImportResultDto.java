package com.yasarbilgi.announcementtracker.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ExcelImportResultDto {

    private int totalRows;
    private int successCount;
    private int errorCount;
    private List<String> errors;
}
