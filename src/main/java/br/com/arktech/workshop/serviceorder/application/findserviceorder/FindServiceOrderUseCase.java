package br.com.arktech.workshop.serviceorder.application.findserviceorder;

import br.com.arktech.workshop.serviceorder.application.serviceorder.ServiceOrderResult;
import br.com.arktech.workshop.serviceorder.domain.repository.ServiceOrderRepository;
import br.com.arktech.workshop.shared.application.UseCase;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class FindServiceOrderUseCase implements UseCase<FindServiceOrderQuery, ServiceOrderResult> {

    private final ServiceOrderRepository serviceOrderRepository;

    @Override
    @Transactional(readOnly = true)
    public ServiceOrderResult execute(FindServiceOrderQuery query) {
        return ServiceOrderResult.from(serviceOrderRepository.findRequiredById(query.id()));
    }
}
