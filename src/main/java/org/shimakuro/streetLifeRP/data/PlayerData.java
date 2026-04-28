package org.shimakuro.streetLifeRP.data;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class PlayerData {
    private final UUID uuid;

    private String firstName;
    private String lastName;
    private String idNumber;
    private String phoneNumber;

    private double cash;
    private double bank;

    private String job;
    private long lastWorkAtMillis;

    private boolean unconscious;
    private long unconsciousAtMillis;

    private boolean cuffed;

    private double fineAmount;
    private String fineIssuer;
    private String fineReason;
    private long fineIssuedAtMillis;

    private List<String> ownedVehicles = new ArrayList<>();
    private String activeVehicleUuid;

    public PlayerData(UUID uuid) {
        this.uuid = uuid;
    }

    public UUID uuid() {
        return uuid;
    }

    public boolean hasCharacter() {
        return firstName != null && !firstName.isBlank() && lastName != null && !lastName.isBlank();
    }

    public String rpNameOrNull() {
        if (!hasCharacter()) return null;
        return firstName + " " + lastName;
    }

    public String firstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String lastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String idNumber() {
        return idNumber;
    }

    public void setIdNumber(String idNumber) {
        this.idNumber = idNumber;
    }

    public String phoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public double cash() {
        return cash;
    }

    public void setCash(double cash) {
        this.cash = cash;
    }

    public double bank() {
        return bank;
    }

    public void setBank(double bank) {
        this.bank = bank;
    }

    public String job() {
        return job;
    }

    public void setJob(String job) {
        this.job = job;
    }

    public long lastWorkAtMillis() {
        return lastWorkAtMillis;
    }

    public void setLastWorkAtMillis(long lastWorkAtMillis) {
        this.lastWorkAtMillis = lastWorkAtMillis;
    }

    public boolean unconscious() {
        return unconscious;
    }

    public void setUnconscious(boolean unconscious) {
        this.unconscious = unconscious;
    }

    public long unconsciousAtMillis() {
        return unconsciousAtMillis;
    }

    public void setUnconsciousAtMillis(long unconsciousAtMillis) {
        this.unconsciousAtMillis = unconsciousAtMillis;
    }

    public boolean cuffed() {
        return cuffed;
    }

    public void setCuffed(boolean cuffed) {
        this.cuffed = cuffed;
    }

    public boolean hasFine() {
        return fineAmount > 0.0;
    }

    public double fineAmount() {
        return fineAmount;
    }

    public void setFineAmount(double fineAmount) {
        this.fineAmount = fineAmount;
    }

    public String fineIssuer() {
        return fineIssuer;
    }

    public void setFineIssuer(String fineIssuer) {
        this.fineIssuer = fineIssuer;
    }

    public String fineReason() {
        return fineReason;
    }

    public void setFineReason(String fineReason) {
        this.fineReason = fineReason;
    }

    public long fineIssuedAtMillis() {
        return fineIssuedAtMillis;
    }

    public void setFineIssuedAtMillis(long fineIssuedAtMillis) {
        this.fineIssuedAtMillis = fineIssuedAtMillis;
    }

    public List<String> ownedVehicles() {
        return ownedVehicles;
    }

    public void setOwnedVehicles(List<String> ownedVehicles) {
        this.ownedVehicles = ownedVehicles != null ? new ArrayList<>(ownedVehicles) : new ArrayList<>();
    }

    public String activeVehicleUuid() {
        return activeVehicleUuid;
    }

    public void setActiveVehicleUuid(String activeVehicleUuid) {
        this.activeVehicleUuid = activeVehicleUuid;
    }
}
