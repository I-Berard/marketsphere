package rw.ac.rca.cloudsphere.admin;

import jakarta.validation.constraints.NotBlank;

public record ShipRequest(@NotBlank String trackingNo) {}
