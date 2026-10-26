package br.com.arktech.workshop.inventory.api.part;

import br.com.arktech.workshop.inventory.application.createpart.CreatePartCommand;
import br.com.arktech.workshop.inventory.application.createpart.CreatePartUseCase;
import br.com.arktech.workshop.inventory.application.findpart.FindPartQuery;
import br.com.arktech.workshop.inventory.application.findpart.FindPartUseCase;
import br.com.arktech.workshop.inventory.application.listparts.ListPartsQuery;
import br.com.arktech.workshop.inventory.application.listparts.ListPartsUseCase;
import br.com.arktech.workshop.inventory.domain.model.PartType;
import br.com.arktech.workshop.shared.api.pagination.PageResponse;

import lombok.RequiredArgsConstructor;

import org.jspecify.annotations.Nullable;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
class PartController implements IPartController {

    private final CreatePartUseCase createPartUseCase;
    private final ListPartsUseCase listPartsUseCase;
    private final FindPartUseCase findPartUseCase;

    @Override
    public PartResponse create(PartRequest request) {
        CreatePartCommand command = new CreatePartCommand(
                request.sku(),
                request.name(),
                request.type(),
                request.unit(),
                request.salePrice(),
                request.minimumStock());
        return PartResponse.from(createPartUseCase.execute(command));
    }

    @Override
    public PageResponse<PartResponse> list(
            @Nullable String name, @Nullable String sku, @Nullable PartType type, int page, int size) {
        ListPartsQuery query = new ListPartsQuery(name, sku, type, page, size);
        return PageResponse.from(listPartsUseCase.execute(query), PartResponse::from);
    }

    @Override
    public PartResponse findById(Long id) {
        return PartResponse.from(findPartUseCase.execute(new FindPartQuery(id)));
    }
}
