package com.riskregister.riskregisterapp.entities;

import jakarta.persistence.Id;

public class Asset {
    @Id
    private Long id;
    private String name;
    private String description;
    private String type; // This is the type of the asset, e.g., "Hardware", "Software", "Data", "Service", "Facility", "People".
    private String status; // This is the status of the asset, e.g., "Active", "Retired", "Archived", etc.
    private String location;
    private int confidentiality;
    private int integrity;
    private int availability;
}
