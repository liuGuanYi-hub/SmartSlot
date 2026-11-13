package com.smartslot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartslot.common.BusinessException;
import com.smartslot.dto.VenueSaveDto;
import com.smartslot.entity.Venue;
import com.smartslot.entity.VenueCategory;
import com.smartslot.mapper.VenueCategoryMapper;
import com.smartslot.mapper.VenueMapper;
import com.smartslot.service.VenueService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class VenueServiceImpl extends ServiceImpl<VenueMapper, Venue> implements VenueService {

    private final VenueCategoryMapper categoryMapper;

    @Override
    public List<Venue> listVenues(Long categoryId, String keyword) {
        LambdaQueryWrapper<Venue> wrapper = new LambdaQueryWrapper<Venue>()
                .eq(categoryId != null, Venue::getCategoryId, categoryId)
                .like(StringUtils.hasText(keyword), Venue::getName, keyword)
                .eq(Venue::getStatus, 1)
                .orderByDesc(Venue::getId);
        List<Venue> list = list(wrapper);
        fillCategoryNames(list);
        return list;
    }

    /**
     * 分页查询场地列表 (MyBatis-Plus 分页插件实战)
     */
    @Override
    public Page<Venue> pageVenues(Page<Venue> page, Long categoryId, String keyword) {
        LambdaQueryWrapper<Venue> wrapper = new LambdaQueryWrapper<Venue>()
                .eq(categoryId != null, Venue::getCategoryId, categoryId)
                .like(StringUtils.hasText(keyword), Venue::getName, keyword)
                .orderByDesc(Venue::getId);
        Page<Venue> resultPage = page(page, wrapper);
        fillCategoryNames(resultPage.getRecords());
        return resultPage;
    }

    @Override
    public Venue getVenueDetail(Long id) {
        Venue venue = getById(id);
        if (venue == null) {
            throw new BusinessException("场地不存在");
        }
        VenueCategory cat = categoryMapper.selectById(venue.getCategoryId());
        if (cat != null) {
            venue.setCategoryName(cat.getName());
        }
        return venue;
    }

    @Override
    public void saveOrUpdateVenue(VenueSaveDto dto) {
        Venue venue = Venue.builder()
                .id(dto.getId())
                .categoryId(dto.getCategoryId())
                .name(dto.getName())
                .capacity(dto.getCapacity())
                .pricePerHour(dto.getPricePerHour())
                .coverImage(StringUtils.hasText(dto.getCoverImage()) ? dto.getCoverImage() : "https://images.unsplash.com/photo-1521537634581-0dced2fee2ef?w=800&auto=format&fit=crop&q=60")
                .facilities(dto.getFacilities())
                .description(dto.getDescription())
                .openTime(dto.getOpenTime())
                .closeTime(dto.getCloseTime())
                .status(dto.getStatus() != null ? dto.getStatus() : 1)
                .updateTime(LocalDateTime.now())
                .build();

        if (venue.getId() == null) {
            venue.setCreateTime(LocalDateTime.now());
            save(venue);
        } else {
            updateById(venue);
        }
    }

    @Override
    public void deleteVenue(Long id) {
        removeById(id);
    }

    private void fillCategoryNames(List<Venue> venues) {
        if (venues.isEmpty()) return;
        List<VenueCategory> categories = categoryMapper.selectList(null);
        Map<Long, String> catMap = categories.stream().collect(Collectors.toMap(VenueCategory::getId, VenueCategory::getName, (k1, k2) -> k1));
        venues.forEach(v -> v.setCategoryName(catMap.getOrDefault(v.getCategoryId(), "未知分类")));
    }
}
