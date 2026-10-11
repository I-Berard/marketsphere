package rw.ac.rca.marketsphere.admin;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(@NotBlank String name) {}
