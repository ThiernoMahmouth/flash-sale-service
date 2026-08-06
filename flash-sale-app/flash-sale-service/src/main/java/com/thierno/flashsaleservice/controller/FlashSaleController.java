package com.thierno.flashsaleservice.controller;

import com.thierno.flashsaleservice.dto.CreateFlashSaleRequest;
import com.thierno.flashsaleservice.dto.CreatePurchaseRequestDto;
import com.thierno.flashsaleservice.entity.FlashSale;
import com.thierno.flashsaleservice.entity.PurchaseRequest;
import com.thierno.flashsaleservice.service.FlashSaleService;
import com.thierno.flashsaleservice.service.PurchaseRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/flash-sales")
@RequiredArgsConstructor
@Tag(name = "Flash Sale API", description = "Create flash sales and purchase during their window")
public class FlashSaleController {

    private final FlashSaleService flashSaleService;
    private final PurchaseRequestService purchaseRequestService;

    @Operation(summary = "Create a flash sale")
    @ResponseStatus(HttpStatus.CREATED)
    @PostMapping
    public FlashSale create(@Valid @RequestBody CreateFlashSaleRequest request) {
        return flashSaleService.create(request);
    }

    @Operation(summary = "List all flash sales")
    @GetMapping
    public List<FlashSale> getAll() {
        return flashSaleService.findAll();
    }

    @Operation(summary = "Get a flash sale by id")
    @GetMapping("/{id}")
    public FlashSale getById(@PathVariable UUID id) {
        return flashSaleService.findById(id);
    }

    @Operation(summary = "Submit a purchase request for a flash sale",
            description = "Validates the sale window synchronously and enqueues the request; " +
                    "a scheduled processor allocates remaining stock in priority order.")
    @PostMapping("/{id}/purchase-requests")
    public ResponseEntity<PurchaseRequest> submitPurchaseRequest(
            @PathVariable UUID id,
            @Valid @RequestBody CreatePurchaseRequestDto request) {
        PurchaseRequest saved = purchaseRequestService.submit(id, request);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(saved);
    }

    @Operation(summary = "Poll the outcome of a purchase request")
    @GetMapping("/{id}/purchase-requests/{requestId}")
    public PurchaseRequest getPurchaseRequest(
            @PathVariable UUID id,
            @PathVariable UUID requestId) {
        return purchaseRequestService.findById(requestId);
    }
}
