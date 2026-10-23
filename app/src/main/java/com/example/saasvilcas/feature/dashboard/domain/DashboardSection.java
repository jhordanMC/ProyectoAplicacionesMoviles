package com.example.saasvilcas.feature.dashboard.domain;

/** Each card can load or fail independently. Current inventory is not period-dependent. */
public enum DashboardSection {
    SUMMARY(true), INVENTORY(false), PRODUCTS(true), CATEGORIES(true), PAYMENTS(true),
    EXPENSES(false), TREND(true), CATALOG(false), NO_SALES(false), RETURNS(false);
    public final boolean periodDependent;
    DashboardSection(boolean periodDependent) { this.periodDependent = periodDependent; }
}
