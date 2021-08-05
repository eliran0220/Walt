package com.walt;

import com.walt.dao.CustomerRepository;
import com.walt.dao.DeliveryRepository;
import com.walt.dao.DriverRepository;
import com.walt.model.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class WaltServiceImpl implements WaltService {

    @Autowired
    CustomerRepository customerRepository;

    @Autowired
    DriverRepository driverRepository;

    @Autowired
    DeliveryRepository deliveryRepository;

    /**
     * Create a delivery based on the parameters given,
     * The delivery will be successfully created if none of the params is null, the customer's city
     * is equal to the restaurant's city & there is a free driver.
     * @param customer     a given customer
     * @param restaurant   a given restaurant
     * @param deliveryTime the date the delivery was submitted
     * @return The new delivery submitted
     */
    @Override
    public Delivery createOrderAndAssignDriver(Customer customer, Restaurant restaurant, Date deliveryTime) {
        if (customer == null || restaurant == null || deliveryTime == null) {
            throw new IllegalArgumentException(Constants.INVALID_PARAM);
        }
        if (!customer.getCity().getId().equals(restaurant.getCity().getId())) {
            throw new RuntimeException(Constants.ADRESS_NOT_EQUAL);
        }
        List<Driver> freeDrivers = getFreeDrivers(customer.getCity(), deliveryTime);
        Driver chosen = chooseLeastBusy(freeDrivers);
        double distance = getRandomDistance();
        Delivery delivery = new Delivery(chosen, restaurant, customer, deliveryTime);
        delivery.setDistance(distance);
        deliveryRepository.save(delivery);
        return delivery;
    }

    /**
     * The function returns a list of DriverDistance which gives a report for each driver, and it's
     * total distance of deliveries in descending order.
     * @return List of DriverInstance which holds all the information about each driver
     */
    @Override
    public List<DriverDistance> getDriverRankReport() {
        List<DriverDistance> distanceByKM = new ArrayList<>();
        Iterable<Driver> drivers = driverRepository.findAll();
        for (Driver driver : drivers) {
            DriverDistance ddu = setTotalDistance(driver);
            distanceByKM.add(ddu);
        }
        distanceByKM.sort(Comparator.comparing(DriverDistance::getTotalDistance).reversed());
        return distanceByKM;
    }

    /**
     * Given the city, the function returns a list of DriverDistance which gives a report for each driver, and it's
     * total distance of deliveries in descending order, specific to the given city.
     * @param city The city which the drivers would be extracted from.
     * @return List of DriverInstance which holds all the information about each driver
     */
    @Override
    public List<DriverDistance> getDriverRankReportByCity(City city) {
        List<DriverDistance> distanceByKM = new ArrayList<>();
        Iterable<Driver> drivers = driverRepository.findAllDriversByCity(city);
        for (Driver driver : drivers) {
            DriverDistance ddu = setTotalDistance(driver);
            distanceByKM.add(ddu);
        }
        distanceByKM.sort(Comparator.comparing(DriverDistance::getTotalDistance).reversed());
        return distanceByKM;
    }

    /**
     * Get all the free drivers available to make the delivery by iterating thorough all the
     * drivers, and check if they can make the delivery in the given time.
     * @param city the city which the delivery will be made
     * @param deliveryTime the date of the delivery
     * @return List of free drivers
     */
    private List<Driver> getFreeDrivers(City city, Date deliveryTime) {
        List<Driver> availableDrivers = new ArrayList<>();
        List<Driver> currentDrivers = driverRepository.findAllDriversByCity(city);
        for (Driver driver : currentDrivers) {
            if (checkAvailability(driver, deliveryTime)) {
                availableDrivers.add(driver);
            }
        }
        if (availableDrivers.size() == 0) {
            throw new RuntimeException(Constants.NO_AVAILABLE_DRIVER);
        }
        return availableDrivers;
    }

    /**
    Check if a driver is available for the delivery with the given time.
    First check if the delivery is already assigned to this driver, if not, check if the gap between
    the time of the wanted delivery, and the iterable delivery collide. If not, we can assign the delivery
    to the driver.
     @param driver the driver to check for availability
     @param deliveryTime the date of the delivery
     @return boolean represents the availability
     */
    private boolean checkAvailability(Driver driver, Date deliveryTime) {
        Iterable<Delivery> currentDeliveries = deliveryRepository.findAll();
        //Check by a given driver if he is available for the new delivery by comparing to all the current deliveries
        for (Delivery delivery : currentDeliveries) {
            // A driver is available if her lives in the same city, and doesn't have a delivery at the same time.
            if (driver.getId().equals(delivery.getDriver().getId()) &&
                    (delivery.getDeliveryTime().getTime() == deliveryTime.getTime())) {
                return false;
            }
        }
        return true;
    }

    /**
    This function chooses the least busy driver according to the number of deliveries he made in the past
    The least busy driver is the one with the least deliveries, in case there is more then one, return the first chosen
    next time, he will have 1 more delivery, so he won't be chosen in case there will be multiple drivers again.
     @param freeDrivers all the free drivers to choose from
     @return Driver instance
     */
    private Driver chooseLeastBusy(List<Driver> freeDrivers) {
        Driver chosenDriver = null;
        int minNumber = Integer.MAX_VALUE;
        for (Driver driver : freeDrivers) {
            int numberDeliveries = getNumberDeliveries(driver);
            if (numberDeliveries < minNumber) {
                chosenDriver = driver;
                minNumber = numberDeliveries;
            }
        }
        return chosenDriver;
    }

    /**
    The function iterates through all the deliveries for a given driver, and counts them.
     @param driver the driver to check for
     @return number of deliveries
     */
    private int getNumberDeliveries(Driver driver) {
        Iterable<Delivery> currentDeliveries = deliveryRepository.findAll();
        int deliveriesNumber = 0;
        for (Delivery delivery : currentDeliveries) {
            if (driver.getId().equals(delivery.getDriver().getId())) {
                deliveriesNumber += 1;
            }
        }
        return deliveriesNumber;
    }

    /**
     * Generate a random number for the distance between 0 - 20
     * @return random distance
     */
    private double getRandomDistance() {
        Random rand = new Random();
        return (new Random().nextDouble() * (Constants.MAX_KM - Constants.MIN_KM)) + Constants.MIN_KM;
    }

    /**
     * A helper function to count the given driver's distance for all the deliveries he made.
     * @param driver the driver to check for
     * @return An instance of DriverDistance that gives a report for the specific driver
     */
    private DriverDistance setTotalDistance(Driver driver) {
        Iterable<Delivery> currentDeliveries = deliveryRepository.findAll();
        Long totalDistance = 0L;
        for (Delivery delivery : currentDeliveries) {
            if (driver.getId().equals(delivery.getDriver().getId())) {
                totalDistance += Double.valueOf(delivery.getDistance()).longValue();
            }
        }
        DriverDistance ddu = new DriverDistanceUnit(driver, totalDistance);
        return ddu;
    }

}
