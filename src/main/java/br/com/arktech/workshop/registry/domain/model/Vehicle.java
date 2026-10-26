package br.com.arktech.workshop.registry.domain.model;

import br.com.arktech.workshop.shared.domain.BaseEntity;
import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.vehicle.Chassis;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Year;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.jspecify.annotations.Nullable;

@Entity
@Table(name = "vehicle")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Vehicle extends BaseEntity {

    public static final int FIRST_YEAR = 1981;

    private Long customerId;

    private LicensePlate licensePlate;

    private @Nullable Chassis chassis;

    private String brand;

    private String model;

    private int year;

    private String color;

    public Vehicle(
            Long customerId,
            LicensePlate licensePlate,
            @Nullable Chassis chassis,
            String brand,
            String model,
            int year,
            String color) {
        this.customerId = customerId;
        this.licensePlate = licensePlate;
        this.chassis = chassis;
        this.brand = brand;
        this.model = model;
        this.year = requireValidYear(year);
        this.color = color;
    }

    public static boolean isValidYear(int year) {
        return year >= FIRST_YEAR && year <= Year.now().getValue() + 1;
    }

    private static int requireValidYear(int year) {
        if (!isValidYear(year)) {
            throw new BusinessRuleException("vehicle.invalidYear");
        }
        return year;
    }
}
