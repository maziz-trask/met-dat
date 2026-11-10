package cz.metdat.dto;

import java.util.List;

public record Lookups(List<String> statuses, List<String> results, List<String> departments, List<String> responsiblePersons,
                      List<String> allowedExtensions, long maxFileSizeBytes) {}
