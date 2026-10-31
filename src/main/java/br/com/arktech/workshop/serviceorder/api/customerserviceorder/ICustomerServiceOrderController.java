package br.com.arktech.workshop.serviceorder.api.customerserviceorder;

import br.com.arktech.workshop.serviceorder.api.serviceorder.RejectQuoteRequest;
import br.com.arktech.workshop.shared.api.security.PublicEndpoint;
import br.com.arktech.workshop.shared.api.validation.CPFOrCNPJ;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

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

    @Operation(
            summary = "${openapi.customerServiceOrder.approveQuote}",
            description = "${openapi.customerServiceOrder.approveQuoteDescription}")
    @ApiResponse(responseCode = "204")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{publicId}/approve")
    void approve(
            @Parameter(description = "${openapi.serviceOrder.publicId}", example = "3f1c2b8e-7d4a-4c9e-9b1f-2a6d8e5c4b3a")
                    @PathVariable UUID publicId,
            @Parameter(description = "${openapi.customerServiceOrder.customerDocument}", example = "529.982.247-25")
                    @RequestHeader(CUSTOMER_DOCUMENT_HEADER) @CPFOrCNPJ String customerDocument);

    @Operation(
            summary = "${openapi.customerServiceOrder.approveOriginalOnly}",
            description = "${openapi.customerServiceOrder.approveOriginalOnlyDescription}")
    @ApiResponse(responseCode = "204")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{publicId}/approveOriginalOnly")
    void approveOriginalOnly(
            @Parameter(description = "${openapi.serviceOrder.publicId}", example = "3f1c2b8e-7d4a-4c9e-9b1f-2a6d8e5c4b3a")
                    @PathVariable UUID publicId,
            @Parameter(description = "${openapi.customerServiceOrder.customerDocument}", example = "529.982.247-25")
                    @RequestHeader(CUSTOMER_DOCUMENT_HEADER) @CPFOrCNPJ String customerDocument);

    @Operation(
            summary = "${openapi.customerServiceOrder.rejectQuote}",
            description = "${openapi.customerServiceOrder.rejectQuoteDescription}")
    @ApiResponse(responseCode = "204")
    @ApiResponse(responseCode = "400", ref = "BadRequest")
    @ApiResponse(responseCode = "404", ref = "NotFound")
    @ApiResponse(responseCode = "422", ref = "BusinessRule")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PostMapping("/{publicId}/reject")
    void reject(
            @Parameter(description = "${openapi.serviceOrder.publicId}", example = "3f1c2b8e-7d4a-4c9e-9b1f-2a6d8e5c4b3a")
                    @PathVariable UUID publicId,
            @Parameter(description = "${openapi.customerServiceOrder.customerDocument}", example = "529.982.247-25")
                    @RequestHeader(CUSTOMER_DOCUMENT_HEADER) @CPFOrCNPJ String customerDocument,
            @Valid @RequestBody RejectQuoteRequest request);
}
