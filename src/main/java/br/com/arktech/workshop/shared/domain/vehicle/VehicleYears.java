package br.com.arktech.workshop.shared.domain.vehicle;

import java.time.Year;

public final class VehicleYears {

    private static final int FIRST = 1981;

    private VehicleYears() {}

    public static boolean isValid(int year) {
        return year >= FIRST && year <= Year.now().getValue() + 1;
    }
}
