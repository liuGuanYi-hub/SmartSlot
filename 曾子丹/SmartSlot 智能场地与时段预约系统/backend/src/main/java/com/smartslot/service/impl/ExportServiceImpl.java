package com.smartslot.service.impl;

import com.alibaba.excel.EasyExcel;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.smartslot.dto.BookingOrderExportDto;
import com.smartslot.dto.VenueExportDto;
import com.smartslot.entity.BookingOrder;
import com.smartslot.entity.SysUser;
import com.smartslot.entity.Venue;
import com.smartslot.entity.VenueCategory;
import com.smartslot.mapper.BookingOrderMapper;
import com.smartslot.mapper.SysUserMapper;
import com.smartslot.mapper.VenueCategoryMapper;
import com.smartslot.mapper.VenueMapper;
import com.smartslot.service.ExportService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExportServiceImpl implements ExportService {

    private final BookingOrderMapper orderMapper;
    private final VenueMapper venueMapper;
    private final VenueCategoryMapper categoryMapper;
    private final SysUserMapper userMapper;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public void exportBookingOrders(HttpServletResponse response, String startDate, String endDate, Integer status) throws IOException {
        LambdaQueryWrapper<BookingOrder> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(startDate)) {
            wrapper.ge(BookingOrder::getBookDate, LocalDate.parse(startDate));
        }
        if (StringUtils.hasText(endDate)) {
            wrapper.le(BookingOrder::getBookDate, LocalDate.parse(endDate));
        }
        if (status != null) {
            wrapper.eq(BookingOrder::getOrderStatus, status);
        }
        wrapper.orderByDesc(BookingOrder::getId);

        List<BookingOrder> orders = orderMapper.selectList(wrapper);

        // 缓存场馆与会员字典以高性能装配
        Map<Long, String> venueMap = venueMapper.selectList(null).stream()
                .collect(Collectors.toMap(Venue::getId, Venue::getName, (k1, k2) -> k1));
        Map<Long, String> userMap = userMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysUser::getId, SysUser::getUsername, (k1, k2) -> k1));

        List<BookingOrderExportDto> dtoList = new ArrayList<>();
        for (BookingOrder order : orders) {
            dtoList.add(BookingOrderExportDto.builder()
                    .orderNo(order.getOrderNo())
                    .username(userMap.getOrDefault(order.getUserId(), "未知会员"))
                    .contactName(order.getContactName())
                    .contactPhone(order.getContactPhone())
                    .venueName(venueMap.getOrDefault(order.getVenueId(), "已下架场地"))
                    .bookDate(order.getBookDate() != null ? order.getBookDate().toString() : "-")
                    .timeSlot(order.getTimeSlot())
                    .totalAmount(order.getTotalAmount())
                    .payStatusDesc(formatPayStatus(order.getPayStatus()))
                    .orderStatusDesc(formatOrderStatus(order.getOrderStatus()))
                    .verifyCode(order.getVerifyCode() != null ? order.getVerifyCode() : "-")
                    .createTime(order.getCreateTime() != null ? order.getCreateTime().format(TIME_FORMATTER) : "-")
                    .verifyTime(order.getVerifyTime() != null ? order.getVerifyTime().format(TIME_FORMATTER) : "-")
                    .build());
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("SmartSlot_订单流水对账单_" + System.currentTimeMillis(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), BookingOrderExportDto.class)
                .sheet("预约流水对账单")
                .doWrite(dtoList);
    }

    @Override
    public void exportVenues(HttpServletResponse response) throws IOException {
        List<Venue> venues = venueMapper.selectList(new LambdaQueryWrapper<Venue>().orderByAsc(Venue::getId));
        Map<Long, String> catMap = categoryMapper.selectList(null).stream()
                .collect(Collectors.toMap(VenueCategory::getId, VenueCategory::getName, (k1, k2) -> k1));

        List<VenueExportDto> dtoList = new ArrayList<>();
        for (Venue v : venues) {
            dtoList.add(VenueExportDto.builder()
                    .id(v.getId())
                    .name(v.getName())
                    .categoryName(catMap.getOrDefault(v.getCategoryId(), "常规"))
                    .capacity(v.getCapacity())
                    .pricePerHour(v.getPricePerHour())
                    .openTime(v.getOpenTime() != null ? v.getOpenTime().toString() : "09:00")
                    .closeTime(v.getCloseTime() != null ? v.getCloseTime().toString() : "22:00")
                    .statusDesc(v.getStatus() != null && v.getStatus() == 1 ? "正常营业" : "维护关闭")
                    .facilities(v.getFacilities() != null ? v.getFacilities() : "常规配套")
                    .build());
        }

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("utf-8");
        String fileName = URLEncoder.encode("SmartSlot_场地排期总表_" + System.currentTimeMillis(), StandardCharsets.UTF_8).replaceAll("\\+", "%20");
        response.setHeader("Content-disposition", "attachment;filename*=utf-8''" + fileName + ".xlsx");

        EasyExcel.write(response.getOutputStream(), VenueExportDto.class)
                .sheet("场地配置与排期表")
                .doWrite(dtoList);
    }

    private String formatPayStatus(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "未支付";
            case 1 -> "已支付";
            case 2 -> "已退款";
            default -> "未知";
        };
    }

    private String formatOrderStatus(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 0 -> "待支付锁定中";
            case 1 -> "预约成功(待核销)";
            case 2 -> "已完成(已核销)";
            case 3 -> "已取消";
            default -> "未知";
        };
    }
}
