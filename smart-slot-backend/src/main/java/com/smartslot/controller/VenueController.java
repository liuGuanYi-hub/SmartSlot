package com.smartslot.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartslot.common.PageResult;
import com.smartslot.common.Result;
import com.smartslot.common.UserContext;
import com.smartslot.dto.VenueSaveDto;
import com.smartslot.entity.OrderReview;
import com.smartslot.entity.Venue;
import com.smartslot.entity.VenueCategory;
import com.smartslot.mapper.OrderReviewMapper;
import com.smartslot.service.BookingOrderService;
import com.smartslot.service.VenueCategoryService;
import com.smartslot.service.VenueService;
import com.smartslot.vo.SlotMatrixVo;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "场地与时段矩阵接口")
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class VenueController {

    private final VenueService venueService;
    private final VenueCategoryService categoryService;
    private final BookingOrderService bookingOrderService;
    private final OrderReviewMapper reviewMapper;

    @Operation(summary = "获取所有场地分类")
    @GetMapping("/venues/categories")
    public Result<List<VenueCategory>> listCategories() {
        return Result.success(categoryService.listActiveCategories());
    }

    @Operation(summary = "获取场地列表(可按分类筛选)")
    @GetMapping("/venues")
    public Result<List<Venue>> listVenues(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        return Result.success(venueService.listVenues(categoryId, keyword));
    }

    @Operation(summary = "获取日历时段矩阵看板 (核心亮点组件数据接口)")
    @GetMapping("/venues/matrix")
    public Result<SlotMatrixVo> getSlotMatrix(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate bookDate,
            @RequestParam(required = false) Long categoryId) {
        Long currentUserId = UserContext.getUserId();
        return Result.success(bookingOrderService.getSlotMatrix(bookDate, categoryId, currentUserId));
    }

    @Operation(summary = "获取场地详情")
    @GetMapping("/venues/{id}")
    public Result<Venue> getVenueDetail(@PathVariable Long id) {
        return Result.success(venueService.getVenueDetail(id));
    }

    @Operation(summary = "获取场地的历史用户评价列表")
    @GetMapping("/venues/{id}/reviews")
    public Result<List<OrderReview>> getVenueReviews(@PathVariable Long id) {
        return Result.success(reviewMapper.selectReviewsByVenueId(id));
    }

    // ==========================================
    // 管理后台场地管理 (MyBatis-Plus 分页插件)
    // ==========================================

    @Operation(summary = "管理端: 场地分页查询(支持条件筛选)")
    @GetMapping("/admin/venues/page")
    public Result<PageResult<Venue>> pageAdminVenues(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword) {
        Page<Venue> page = venueService.pageVenues(new Page<>(current, size), categoryId, keyword);
        return Result.success(new PageResult<>(page.getRecords(), page.getTotal(), page.getCurrent(), page.getSize(), page.getPages()));
    }

    @Operation(summary = "管理端: 新增或修改场地")
    @PostMapping("/admin/venues")
    public Result<Void> saveOrUpdateVenue(@Valid @RequestBody VenueSaveDto dto) {
        venueService.saveOrUpdateVenue(dto);
        return Result.success("保存场地信息成功", null);
    }

    @Operation(summary = "管理端: 删除场地")
    @DeleteMapping("/admin/venues/{id}")
    public Result<Void> deleteVenue(@PathVariable Long id) {
        venueService.deleteVenue(id);
        return Result.success("删除成功", null);
    }
}
