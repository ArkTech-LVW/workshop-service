package br.com.arktech.workshop.registry.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.arktech.workshop.shared.domain.exception.BusinessRuleException;
import br.com.arktech.workshop.shared.domain.vehicle.Chassis;
import br.com.arktech.workshop.shared.domain.vehicle.LicensePlate;

import java.time.Year;

import org.junit.jupiter.api.Test;

class VehicleTest {

    private static final int NEXT_YEAR = Year.now().getValue() + 1;

    @Test
    void shouldAcceptYearsFrom1981UntilNextYear() {
        assertThat(Vehicle.isValidYear(1981)).isTrue();
        assertThat(Vehicle.isValidYear(NEXT_YEAR)).isTrue();
        assertThat(Vehicle.isValidYear(1980)).isFalse();
        assertThat(Vehicle.isValidYear(NEXT_YEAR + 1)).isFalse();
    }

    @Test
    void shouldCreateVehicle() {
        Vehicle vehicle = new Vehicle(
                1L, new LicensePlate("ABC1D23"), new Chassis("9BWZZZ372VT004251"), "Volkswagen", "Gol", 1981, "Prata");

        assertThat(vehicle.getCustomerId()).isEqualTo(1L);
        assertThat(vehicle.getLicensePlate()).isEqualTo(new LicensePlate("ABC1D23"));
        assertThat(vehicle.getChassis()).isEqualTo(new Chassis("9BWZZZ372VT004251"));
        assertThat(vehicle.getBrand()).isEqualTo("Volkswagen");
        assertThat(vehicle.getModel()).isEqualTo("Gol");
        assertThat(vehicle.getYear()).isEqualTo(1981);
        assertThat(vehicle.getColor()).isEqualTo("Prata");
    }

    @Test
    void shouldRejectYearOutOfRange() {
        assertThatThrownBy(() -> new Vehicle(1L, new LicensePlate("ABC1D23"), null, "Fiat", "Uno", 1980, "Branco"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("vehicle.invalidYear");
        assertThatThrownBy(() ->
                        new Vehicle(1L, new LicensePlate("ABC1D23"), null, "Fiat", "Uno", NEXT_YEAR + 1, "Branco"))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("vehicle.invalidYear");
    }

    @Test
    void shouldChangeIdentificationAndColorKeepingOwnerAndDescription() {
        Vehicle vehicle = new Vehicle(1L, new LicensePlate("ABC1D23"), null, "Fiat", "Uno", 2010, "Branco");

        vehicle.changeIdentification(new LicensePlate("XYZ9876"), new Chassis("1HGCM82633A004352"));
        vehicle.repaint("Preto");

        assertThat(vehicle.getLicensePlate()).isEqualTo(new LicensePlate("XYZ9876"));
        assertThat(vehicle.getChassis()).isEqualTo(new Chassis("1HGCM82633A004352"));
        assertThat(vehicle.getColor()).isEqualTo("Preto");
        assertThat(vehicle.getCustomerId()).isEqualTo(1L);
        assertThat(vehicle.getBrand()).isEqualTo("Fiat");
        assertThat(vehicle.getModel()).isEqualTo("Uno");
        assertThat(vehicle.getYear()).isEqualTo(2010);
    }
}
