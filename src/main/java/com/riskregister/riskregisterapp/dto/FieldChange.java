package com.riskregister.riskregisterapp.dto;

public record FieldChange(
    String field,     // internal field name, e.g. "inherentLikelihood"
    String label,     // display label, e.g. "Inherent Likelihood"
    String oldValue,  // string representation of old value
    String newValue   // string representation of new value
) {}
