package com.smartslot.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartslot.entity.VenueCategory;
import com.smartslot.mapper.VenueCategoryMapper;
import com.smartslot.service.VenueCategoryService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class VenueCategoryServiceImpl extends ServiceImpl<VenueCategoryMapper, VenueCategory> implements VenueCategoryService {

    @Override
    public List<VenueCategory> listActiveCategories() {
        return list(new LambdaQueryWrapper<VenueCategory>()
                .eq(VenueCategory::getStatus, 1)
                .orderByAsc(VenueCategory::getSort));
    }
}
