package br.com.arktech.workshop.registry.application.listcustomers;

import br.com.arktech.workshop.registry.application.customer.CustomerResult;
import br.com.arktech.workshop.registry.domain.repository.CustomerFilter;
import br.com.arktech.workshop.registry.domain.repository.CustomerRepository;
import br.com.arktech.workshop.shared.application.UseCase;
import br.com.arktech.workshop.shared.domain.document.CPFCNPJ;
import br.com.arktech.workshop.shared.domain.pagination.Page;
import br.com.arktech.workshop.shared.domain.pagination.Pagination;

import lombok.RequiredArgsConstructor;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ListCustomersUseCase implements UseCase<ListCustomersQuery, Page<CustomerResult>> {

    private final CustomerRepository customerRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<CustomerResult> execute(ListCustomersQuery query) {
        CustomerFilter filter = new CustomerFilter(query.document() == null ? null : CPFCNPJ.of(query.document()));
        Pagination pagination = new Pagination(query.page(), query.size());
        return customerRepository.findPage(filter, pagination).map(CustomerResult::from);
    }
}
