package com.aislego.catalogue.dto;

import java.util.List;

public record BulkProductImportResponse(int importedCount, List<OwnerProductResponse> products) {
}
