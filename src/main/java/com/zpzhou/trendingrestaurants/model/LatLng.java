package com.zpzhou.trendingrestaurants.model;

import lombok.Data;
import lombok.RequiredArgsConstructor;

import java.math.BigDecimal;

@Data
@RequiredArgsConstructor
public class LatLng {
    private final BigDecimal lat;
    private final BigDecimal lng;
}
