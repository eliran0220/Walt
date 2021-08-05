package com.walt.model;

public class DriverDistanceUnit implements DriverDistance{

    private Driver driver;
    private Long totalDistance;

    public DriverDistanceUnit(Driver d, Long distance){
        this.driver = d;
        this.totalDistance = distance;
    }

    @Override
    public Driver getDriver() {
        return this.driver;
    }

    @Override
    public Long getTotalDistance() {
        return this.totalDistance;
    }
}
