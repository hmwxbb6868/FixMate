package com.fixmate.dto.user;

import jakarta.validation.constraints.NotNull;

public class SafetyAnswersDTO {

    @NotNull
    private Boolean smokeOrBurningSmell;

    @NotNull
    private Boolean electricLeakage;

    @NotNull
    private Boolean gasSmell;

    @NotNull
    private Boolean suspectedRefrigerantLeak;

    @NotNull
    private Boolean severeWaterIngress;

    public Boolean getSmokeOrBurningSmell() {
        return smokeOrBurningSmell;
    }

    public void setSmokeOrBurningSmell(Boolean smokeOrBurningSmell) {
        this.smokeOrBurningSmell = smokeOrBurningSmell;
    }

    public Boolean getElectricLeakage() {
        return electricLeakage;
    }

    public void setElectricLeakage(Boolean electricLeakage) {
        this.electricLeakage = electricLeakage;
    }

    public Boolean getGasSmell() {
        return gasSmell;
    }

    public void setGasSmell(Boolean gasSmell) {
        this.gasSmell = gasSmell;
    }

    public Boolean getSuspectedRefrigerantLeak() {
        return suspectedRefrigerantLeak;
    }

    public void setSuspectedRefrigerantLeak(Boolean suspectedRefrigerantLeak) {
        this.suspectedRefrigerantLeak = suspectedRefrigerantLeak;
    }

    public Boolean getSevereWaterIngress() {
        return severeWaterIngress;
    }

    public void setSevereWaterIngress(Boolean severeWaterIngress) {
        this.severeWaterIngress = severeWaterIngress;
    }

    public boolean anyTrue() {
        return Boolean.TRUE.equals(smokeOrBurningSmell)
                || Boolean.TRUE.equals(electricLeakage)
                || Boolean.TRUE.equals(gasSmell)
                || Boolean.TRUE.equals(suspectedRefrigerantLeak)
                || Boolean.TRUE.equals(severeWaterIngress);
    }
}
