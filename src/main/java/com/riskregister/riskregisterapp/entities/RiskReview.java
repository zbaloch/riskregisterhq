package com.riskregister.riskregisterapp.entities;

import jakarta.persistence.Id;

public class RiskReview {
    @Id
    private Long id;
    private Risk risk; // This is the risk that is being reviewed, linked to the risk table.
    private String reviewComments; // This is the comments from the reviewer about the risk review,
    // private User reviewer; // This is the user who is reviewing the risk, linked to the user table. Review is done by committee
    private String reviewOutcome; // This is the outcome of the risk review, e.g., "Risk is still valid and requires mitigation", "Risk is no longer valid and can be closed", "Risk has been mitigated and residual risk is acceptable", etc.
    private String reviewDate; // This is the date when the risk review was conducted.


    // TODO: If the risk is not reviewed since last time based on the review frequency, then we can automatically create a task for the risk owner to review the risk. This can be done using a scheduled job that runs daily and checks for risks that are due for review based on their last review date and review frequency.
    
}
