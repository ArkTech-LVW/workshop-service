package br.com.arktech.workshop.shared.domain.vehicle;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class VehicleConvertersTest {

    private final ChassisConverter chassisConverter = new ChassisConverter();
    private final LicensePlateConverter licensePlateConverter = new LicensePlateConverter();

    @Test
    void shouldConvertChassisToAndFromColumn() {
        assertThat(chassisConverter.convertToDatabaseColumn(new Chassis("1HGCM82633A004352")))
                .isEqualTo("1HGCM82633A004352");
        assertThat(chassisConverter.convertToEntityAttribute("1HGCM82633A004352"))
                .isEqualTo(new Chassis("1HGCM82633A004352"));
    }

    @Test
    void shouldConvertLicensePlateToAndFromColumn() {
        assertThat(licensePlateConverter.convertToDatabaseColumn(new LicensePlate("ABC1D23")))
                .isEqualTo("ABC1D23");
        assertThat(licensePlateConverter.convertToEntityAttribute("ABC1D23"))
                .isEqualTo(new LicensePlate("ABC1D23"));
    }

    @Test
    void shouldKeepNullOnBothSides() {
        assertThat(chassisConverter.convertToDatabaseColumn(null)).isNull();
        assertThat(chassisConverter.convertToEntityAttribute(null)).isNull();
        assertThat(licensePlateConverter.convertToDatabaseColumn(null)).isNull();
        assertThat(licensePlateConverter.convertToEntityAttribute(null)).isNull();
    }
}
