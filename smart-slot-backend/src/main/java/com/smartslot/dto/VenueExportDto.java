package com.smartslot.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import com.alibaba.excel.annotation.write.style.ColumnWidth;
import com.alibaba.excel.annotation.write.style.ContentRowHeight;
import com.alibaba.excel.annotation.write.style.HeadRowHeight;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ContentRowHeight(20)
@HeadRowHeight(25)
@ColumnWidth(20)
public class VenueExportDto {

    @ExcelProperty(value = "场地编号", index = 0)
    @ColumnWidth(12)
    private Long id;

    @ExcelProperty(value = "场地名称", index = 1)
    @ColumnWidth(25)
    private String name;

    @ExcelProperty(value = "分类类别", index = 2)
    @ColumnWidth(16)
    private String categoryName;

    @ExcelProperty(value = "容纳人数", index = 3)
    @ColumnWidth(12)
    private Integer capacity;

    @ExcelProperty(value = "时租单价(元/时)", index = 4)
    @ColumnWidth(18)
    private BigDecimal pricePerHour;

    @ExcelProperty(value = "开放时间", index = 5)
    @ColumnWidth(14)
    private String openTime;

    @ExcelProperty(value = "打烊时间", index = 6)
    @ColumnWidth(14)
    private String closeTime;

    @ExcelProperty(value = "运营状态", index = 7)
    @ColumnWidth(14)
    private String statusDesc;

    @ExcelProperty(value = "场馆配套设施", index = 8)
    @ColumnWidth(30)
    private String facilities;
}
