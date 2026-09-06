package com.smartslot.service;

import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public interface ExportService {

    void exportBookingOrders(HttpServletResponse response, String startDate, String endDate, Integer status) throws IOException;

    void exportVenues(HttpServletResponse response) throws IOException;
}
