package com.example.saasvilcas.feature.dashboard.domain;

public interface DashboardRepository {
    DashboardPanel load(DashboardSection section,DashboardFilter filter);
}
