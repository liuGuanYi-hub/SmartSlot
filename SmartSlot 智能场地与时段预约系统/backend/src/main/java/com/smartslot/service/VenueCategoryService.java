package com.smartslot.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartslot.entity.VenueCategory;

import java.util.List;

public interface VenueCategoryService extends IService<VenueCategory> {

    List<VenueCategory> listActiveCategories();
}
