package rw.ac.rca.cloudsphere.admin;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(@NotBlank String name) {}
