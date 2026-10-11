package rw.ac.rca.marketsphere.admin;

import jakarta.validation.constraints.NotBlank;

public record ShipRequest(@NotBlank String trackingNo) {}
