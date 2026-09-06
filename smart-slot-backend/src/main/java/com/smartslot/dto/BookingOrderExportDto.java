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
public class BookingOrderExportDto {

    @ExcelProperty(value = "订单流水号", index = 0)
    @ColumnWidth(26)
    private String orderNo;

    @ExcelProperty(value = "会员账号", index = 1)
    @ColumnWidth(16)
    private String username;

    @ExcelProperty(value = "联系人", index = 2)
    @ColumnWidth(14)
    private String contactName;

    @ExcelProperty(value = "联系手机", index = 3)
    @ColumnWidth(16)
    private String contactPhone;

    @ExcelProperty(value = "预订场馆/场地", index = 4)
    @ColumnWidth(22)
    private String venueName;

    @ExcelProperty(value = "预约日期", index = 5)
    @ColumnWidth(15)
    private String bookDate;

    @ExcelProperty(value = "预约时段", index = 6)
    @ColumnWidth(16)
    private String timeSlot;

    @ExcelProperty(value = "实付金额(元)", index = 7)
    @ColumnWidth(15)
    private BigDecimal totalAmount;

    @ExcelProperty(value = "支付状态", index = 8)
    @ColumnWidth(14)
    private String payStatusDesc;

    @ExcelProperty(value = "订单履约状态", index = 9)
    @ColumnWidth(16)
    private String orderStatusDesc;

    @ExcelProperty(value = "专属核销码", index = 10)
    @ColumnWidth(14)
    private String verifyCode;

    @ExcelProperty(value = "下单时间", index = 11)
    @ColumnWidth(22)
    private String createTime;

    @ExcelProperty(value = "核销入场时间", index = 12)
    @ColumnWidth(22)
    private String verifyTime;
}
