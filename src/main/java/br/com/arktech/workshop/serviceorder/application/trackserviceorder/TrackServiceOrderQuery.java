package br.com.arktech.workshop.serviceorder.application.trackserviceorder;

import java.util.UUID;

public record TrackServiceOrderQuery(UUID publicId, String customerDocument) {
}
