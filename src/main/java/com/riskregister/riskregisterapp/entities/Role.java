package com.riskregister.riskregisterapp.entities;


// TODO: Every user registered will be by default no access and only an admin can allow them to login and assign them a role. This is to prevent unauthorized access to the system and to ensure that only authorized users can access the system.
public enum Role {
    MEMBER,
    APPROVER,
    AUDITOR,
    ADMIN
}
