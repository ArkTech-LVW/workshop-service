package br.com.arktech.workshop.metrics.application.port;

import java.time.Instant;
import java.util.List;

public interface ServiceOrderHistoryReader {

    long countCompletedBetween(Instant start, Instant end);

    List<StatusTime> averageTimeByStatusOfCompletedBetween(Instant start, Instant end);
}
