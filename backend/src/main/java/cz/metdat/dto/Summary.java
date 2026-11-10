package cz.metdat.dto;

import java.util.Map;

public record Summary(long total, Map<String, Long> byStatus) {}
