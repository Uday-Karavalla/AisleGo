package com.aislego.catalogue.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import java.util.List;

public record BulkProductImportRequest(
        @NotEmpty @Size(max = 500) List<@Valid CreateProductRequest> products
) {
}
