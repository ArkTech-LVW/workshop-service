package br.com.arktech.workshop.serviceorder.application.openserviceorder;

public record OpenServiceOrderCommand(String customerDocument, Vehicle vehicle, String problemDescription) {

    public record Vehicle(String licensePlate, String brand, String model, int year, String color) {
    }
}
