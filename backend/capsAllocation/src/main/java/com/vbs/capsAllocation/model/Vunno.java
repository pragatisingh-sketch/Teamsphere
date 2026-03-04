package com.vbs.capsAllocation.model;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import java.util.Date;

@Entity
@Table(name="vbs_who")
public class Vunno {
    @Id
    @Column(name = "ldap")
    private String ldap;

    @Column(name = "name")
    private String name;

    @Column(name = "role")
    private String role;

    @Column(name = "locales")
    private String locales;

    @Column(name = "primary_language")
    private String primaryLanguage;

    @Column(name = "secondary_language")
    private String secondaryLanguage;

    @Column(name = "location")
    private String location;

    @Column(name = "status")
    private String status;

    @Column(name = "start_date")
    private Date startDate;

    @Column(name = "teams")
    private String teams;

    @Column(name = "primary_program_alignment")
    private String primaryProgramAlignment;

    @Column(name = "email_address")
    private String emailAddress;

    @Column(name = "tenure")
    private String tenure;

    @Column(name = "tenure_months")
    private String tenureMonths;

    @Column(name="lead")
    private String lead;

    // Default constructor
    public Vunno() {
    }

    public Vunno(String ldap, String name, String role, String email, String programAlignment, String team) {
        this.ldap = ldap;
        this.name = name;
        this.role = role;
        this.emailAddress = email;
        this.primaryProgramAlignment = programAlignment;
        this.teams = team;
    }

    // Getters and Setters
    public String getLdap() { return ldap; }
    public void setLdap(String ldap) { this.ldap = ldap; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getLocales() { return locales; }
    public void setLocales(String locales) { this.locales = locales; }

    public String getPrimaryLanguage() { return primaryLanguage; }
    public void setPrimaryLanguage(String primaryLanguage) { this.primaryLanguage = primaryLanguage; }

    public String getSecondaryLanguage() { return secondaryLanguage; }
    public void setSecondaryLanguage(String secondaryLanguage) { this.secondaryLanguage = secondaryLanguage; }

    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }

    public String getTeams() { return teams; }
    public void setTeams(String teams) { this.teams = teams; }

    public String getPrimaryProgramAlignment() { return primaryProgramAlignment; }
    public void setPrimaryProgramAlignment(String primaryProgramAlignment) { this.primaryProgramAlignment = primaryProgramAlignment; }

    public String getEmailAddress() { return emailAddress; }
    public void setEmailAddress(String emailAddress) { this.emailAddress = emailAddress; }

    public String getTenure() { return tenure; }
    public void setTenure(String tenure) { this.tenure = tenure; }

    public String getTenureMonths() { return tenureMonths; }
    public void setTenureMonths(String tenureMonths) { this.tenureMonths = tenureMonths; }
    public String getLead() { return lead; }
    public void setLead(String lead) { this.lead = lead; }
}
