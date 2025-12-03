package models;

import catalogs.PolicyCatalog;
import catalogs.RouteCatalog;

public class Admin extends User {
    public Admin(String userID, String name, String password, String username, String email, String phoneNum) {
        super(userID, name, password, username, email, phoneNum);
    }

    public void updateRoute(Route route, String newSource, String newDestination, double newPrice) {
        if (route != null) {
            route.setSource(newSource);
            route.setDestination(newDestination);
            route.setBasePrice(newPrice);
        }
    }

    public void deleteRoute(RouteCatalog routeCatalog, String routeID) {
        if (routeCatalog != null) {
            routeCatalog.deleteRoute(routeID);
        }
    }


    public void manageSeatAvailability(Schedule schedule, Seat seat, boolean availability) {
        if (schedule != null && seat != null) {
            seat.setAvailability(availability);
        }
    }


    public void setCancellationPolicy(PolicyCatalog policyCatalog, CancellationPolicy policy) {
        if (policyCatalog != null && policy != null) {
            policyCatalog.addToCatalog(policy);
        }
    }

    public void managePromotionalCodes(PromotionalCode code, boolean activate) {
        if (code != null) {
            code.setActive(activate);
        }
    }
}