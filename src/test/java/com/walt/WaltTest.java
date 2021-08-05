package com.walt;

import com.walt.dao.*;
import com.walt.model.*;
import org.assertj.core.util.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest()
@DirtiesContext(classMode = DirtiesContext.ClassMode.BEFORE_EACH_TEST_METHOD)
public class WaltTest {

    @TestConfiguration
    static class WaltServiceImplTestContextConfiguration {

        @Bean
        public WaltService waltService() {
            return new WaltServiceImpl();
        }
    }

    @Autowired
    WaltService waltService;

    @Resource
    CityRepository cityRepository;

    @Resource
    CustomerRepository customerRepository;

    @Resource
    DriverRepository driverRepository;

    @Resource
    DeliveryRepository deliveryRepository;

    @Resource
    RestaurantRepository restaurantRepository;

    @BeforeEach()
    public void prepareData() {

        City jerusalem = new City("Jerusalem");
        City tlv = new City("Tel-Aviv");
        City bash = new City("Beer-Sheva");
        City haifa = new City("Haifa");
        City rishon = new City("Rishon-Lezion");


        cityRepository.save(jerusalem);
        cityRepository.save(tlv);
        cityRepository.save(bash);
        cityRepository.save(haifa);
        cityRepository.save(rishon);

        createDrivers(jerusalem, tlv, bash, haifa, rishon);

        createCustomers(jerusalem, tlv, haifa, rishon, bash);

        createRestaurant(jerusalem, tlv, rishon, bash);

    }

    private void createRestaurant(City jerusalem, City tlv, City rishon, City bash) {
        Restaurant meat = new Restaurant("meat", jerusalem, "All meat restaurant");
        Restaurant vegan = new Restaurant("vegan", tlv, "Only vegan");
        Restaurant cafe = new Restaurant("cafe", tlv, "Coffee shop");
        Restaurant chinese = new Restaurant("chinese", tlv, "chinese restaurant");
        Restaurant mexican = new Restaurant("restaurant", tlv, "mexican restaurant ");
        //Added by me
        Restaurant indian = new Restaurant("indian", rishon, "indian restaurant");
        Restaurant avocado = new Restaurant("avocado", rishon, "avocado restaurant");
        Restaurant bakery = new Restaurant("bakery", rishon, "bakery restaurant");
        Restaurant breakfast = new Restaurant("breakfast", bash, "breakfast restaurant");


        restaurantRepository.saveAll(Lists.newArrayList(meat, vegan, cafe, chinese, mexican, indian, avocado, bakery, breakfast));
    }

    private void createCustomers(City jerusalem, City tlv, City haifa, City rishon, City bash) {
        Customer beethoven = new Customer("Beethoven", tlv, "Ludwig van Beethoven");
        Customer mozart = new Customer("Mozart", jerusalem, "Wolfgang Amadeus Mozart");
        Customer chopin = new Customer("Chopin", haifa, "Frédéric François Chopin");
        Customer rachmaninoff = new Customer("Rachmaninoff", tlv, "Sergei Rachmaninoff");
        Customer bach = new Customer("Bach", tlv, "Sebastian Bach. Johann");
        //Added by me
        Customer picasso = new Customer("Picasso", rishon, "Pablo Picaso");
        Customer einstein = new Customer("Einstein", rishon, "Albert Einstein");
        Customer galileo = new Customer("Galileo", rishon, "Galileo Galilei");
        Customer obama = new Customer("Obama", bash, "Barak Obama");
        Customer elon = new Customer("Elon", bash, "Elon Musk");

        customerRepository.saveAll(Lists.newArrayList(beethoven, mozart, chopin, rachmaninoff, bach, picasso, einstein, galileo, obama, elon));
    }

    private void createDrivers(City jerusalem, City tlv, City bash, City haifa, City rishon) {
        Driver mary = new Driver("Mary", tlv);
        Driver patricia = new Driver("Patricia", tlv);
        Driver jennifer = new Driver("Jennifer", haifa);
        Driver james = new Driver("James", bash);
        Driver john = new Driver("John", bash);
        Driver robert = new Driver("Robert", jerusalem);
        Driver david = new Driver("David", jerusalem);
        Driver daniel = new Driver("Daniel", tlv);
        Driver noa = new Driver("Noa", haifa);
        Driver ofri = new Driver("Ofri", haifa);
        Driver nata = new Driver("Neta", jerusalem);
        //Added by me
        Driver dany = new Driver("Dany", rishon);


        driverRepository.saveAll(Lists.newArrayList(mary, patricia, jennifer, james, john, robert, david, daniel, noa, ofri, nata, dany));
    }

    @Test
    public void testBasics() {
        assertEquals(((List<City>) cityRepository.findAll()).size(), 5);
        assertEquals((driverRepository.findAllDriversByCity(cityRepository.findByName("Beer-Sheva")).size()), 2);
    }

    /**
     * Test if there is any null argument in the params.
     */
    @Test
    public void testNullRestaurantArg() {
        Restaurant restaurant = restaurantRepository.findByName("meat");
        Date date = new Date();
        Exception exception = assertThrows(RuntimeException.class, () -> {
            waltService.createOrderAndAssignDriver(null, restaurant, date);
        });
        String expectedMessage = Constants.INVALID_PARAM;
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    /**
     * Test if the customer and the restaurant are in the same city.
     */
    @Test
    public void testNotSameCity() {
        Restaurant restaurant = restaurantRepository.findByName("meat");
        Customer customer = customerRepository.findByName("Chopin");
        Date date = new Date();
        Exception exception = assertThrows(RuntimeException.class, () -> {
            waltService.createOrderAndAssignDriver(customer, restaurant, date);
        });
        String expectedMessage = Constants.ADRESS_NOT_EQUAL;
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    /**
     * Test if there are no free drivers, create 3 deliveries and assign them, on the 4th time should be
     * an exception because there are 3 drivers total in TLV and we are using the same time.
     */
    @Test
    public void testNoFreeDriver() {
        Customer customer = customerRepository.findByName("Mozart");
        Restaurant restaurant = restaurantRepository.findByName("meat");
        List<Delivery> temp_list = new ArrayList<>();
        Date date = new Date();
        for (int i = 0; i < 3; i++) {
            Delivery delivery = waltService.createOrderAndAssignDriver(customer, restaurant,
                    date);
            temp_list.add(delivery);
        }
        Exception exception = assertThrows(RuntimeException.class, () -> {
            waltService.createOrderAndAssignDriver(customer, restaurant, date);
        });
        String expectedMessage = Constants.NO_AVAILABLE_DRIVER;
        String actualMessage = exception.getMessage();
        assertTrue(actualMessage.contains(expectedMessage));
    }

    /**
     * Test the Driver rank report, should return a list in desc order.
     */
    @Test
    public void testDriverRankReport() {
        Customer customer1 = customerRepository.findByName("Bach");
        Restaurant restaurant1 = restaurantRepository.findByName("restaurant");
        Customer customer2 = customerRepository.findByName("Mozart");
        Restaurant restaurant2 = restaurantRepository.findByName("meat");
        Date date = new Date();
        waltService.createOrderAndAssignDriver(customer1, restaurant1,
                date);
        waltService.createOrderAndAssignDriver(customer2, restaurant2,
                date);
        List<DriverDistance> driverDistanceList = waltService.getDriverRankReport();
        for (DriverDistance ddu : driverDistanceList) {
            System.out.println("Driver's name: " + ddu.getDriver().getName() + '\n'
                    + "Driver's total distance: " + ddu.getTotalDistance());
        }
        waltService.getDriverRankReport();
    }

    /**
     * Test the Driver rank report by a given city, should return a list in desc order.
     */
    @Test
    public void testDriverRankReportByCity() {
        Date date = new Date();
        Customer customer1 = customerRepository.findByName("Einstein");
        Restaurant restaurant1 = restaurantRepository.findByName("indian");
        Customer customer2 = customerRepository.findByName("Galileo");
        Restaurant restaurant2 = restaurantRepository.findByName("avocado");
        Customer customer3 = customerRepository.findByName("Picasso");
        Restaurant restaurant3 = restaurantRepository.findByName("bakery");
        waltService.createOrderAndAssignDriver(customer1, restaurant1,
                date);
        date = new Date();
        waltService.createOrderAndAssignDriver(customer2, restaurant2,
                date);
        date = new Date();
        waltService.createOrderAndAssignDriver(customer3, restaurant3,
                date);
        List<DriverDistance> driverDistanceList = waltService.getDriverRankReportByCity(cityRepository.findByName("Rishon-Lezion"));
        for (DriverDistance ddu : driverDistanceList) {
            System.out.println("Driver's name: " + ddu.getDriver().getName() + ", "
                    + "Driver's total distance: " + ddu.getTotalDistance());
        }
    }

    /**
     * Test if the least busy driver is John.
     * Beer-Sheva has two drivers, so assign one delivery to James, so John supposed to be
     * least busy driver because he has the lowest number of deliveries.
     */
    @Test
    public void testLeastBusyDriver() {
        Driver james = driverRepository.findByName("James");
        Date date = new Date();
        deliveryRepository.save(new Delivery(james, restaurantRepository.findByName("breakfast"),
                customerRepository.findByName("obama"), date));
        date = new Date();
        Customer customer = customerRepository.findByName("Elon");
        Restaurant restaurant = restaurantRepository.findByName("breakfast");
        Driver driver = waltService.createOrderAndAssignDriver(customer, restaurant, date).getDriver();
        String expectedMessage = "John";
        String actualMessage = driver.getName();
        assertTrue(actualMessage.contains(expectedMessage));
    }
}
