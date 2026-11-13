package com.smartslot.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.smartslot.dto.VenueSaveDto;
import com.smartslot.entity.Venue;

import java.util.List;

public interface VenueService extends IService<Venue> {

    List<Venue> listVenues(Long categoryId, String keyword);

    Page<Venue> pageVenues(Page<Venue> page, Long categoryId, String keyword);

    Venue getVenueDetail(Long id);

    void saveOrUpdateVenue(VenueSaveDto dto);

    void deleteVenue(Long id);
}
