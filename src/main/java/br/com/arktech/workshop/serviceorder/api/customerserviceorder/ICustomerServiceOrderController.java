package br.com.arktech.workshop.serviceorder.api.customerserviceorder;

import br.com.arktech.workshop.shared.api.security.PublicEndpoint;
import br.com.arktech.workshop.shared.api.validation.CPFOrCNPJ;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;

@Tag(name = "${openapi.customerServiceOrder.tag}")
@PublicEndpoint
@RequestMapping("/customer/serviceOrders")
interface ICustomerServiceOrderController {

    String CUSTOMER_DOCUMENT_HEADER = "X-Customer-Document";

    @Operation(
            summary = "${openapi.customerServiceOrder.track}",
            description = "${openapi.customerServiceOrder.trackDescription}")
    @ApiResponse(responseCode = "200")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @GetMapping("/{publicId}")
    CustomerServiceOrderResponse track(
            @Parameter(description = "${openapi.serviceOrder.publicId}", example = "3f1c2b8e-7d4a-4c9e-9b1f-2a6d8e5c4b3a")
                    @PathVariable UUID publicId,
            @Parameter(description = "${openapi.customerServiceOrder.customerDocument}", example = "529.982.247-25")
                    @RequestHeader(CUSTOMER_DOCUMENT_HEADER) @CPFOrCNPJ String customerDocument);
}
