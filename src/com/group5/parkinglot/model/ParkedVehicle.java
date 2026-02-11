package com.group5.parkinglot.model;

import java.time.LocalDateTime;

public record ParkedVehicle(
    String licensePlate,
    String vehicleType,
    String spotId,
    LocalDateTime entryTime
) {}
