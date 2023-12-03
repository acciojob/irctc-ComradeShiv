package com.driver.services;

import com.driver.EntryDto.AddTrainEntryDto;
import com.driver.EntryDto.SeatAvailabilityEntryDto;
import com.driver.model.Passenger;
import com.driver.model.Station;
import com.driver.model.Ticket;
import com.driver.model.Train;
import com.driver.repository.TrainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TrainService {

    @Autowired
    TrainRepository trainRepository;

    public Integer addTrain(AddTrainEntryDto trainEntryDto){

        //Add the train to the trainRepository
        //and route String logic to be taken from the Problem statement.
        //Save the train and return the trainId that is generated from the database.
        //Avoid using the lombok library


        // create route string
        String route = "";
        for(Station station: trainEntryDto.getStationRoute()) {
            route += station.toString() + ",";
        }
        route = route.substring(0,route.length() - 1);

        // create train
        Train train = new Train();
        train.setRoute(route);
        train.setDepartureTime(trainEntryDto.getDepartureTime());
        train.setNoOfSeats(trainEntryDto.getNoOfSeats());

        // save train
        Train savedTrain = trainRepository.save(train);
        return savedTrain.getTrainId();
    }

    public Integer calculateAvailableSeats(SeatAvailabilityEntryDto seatAvailabilityEntryDto){

        //Calculate the total seats available
        //Suppose the route is A B C D
        //And there are 2 seats available in total in the train
        //and 2 tickets are booked from A to C and B to D.
        //The seat is available only between A to C and A to B. If a seat is empty between 2 station it will be counted to our final ans
        //even if that seat is booked post the destStation or before the boardingStation
        //In short : a train has totalNo of seats and there are tickets from and to different locations
        //We need to find out the available seats between the given 2 stations.


        if(seatAvailabilityEntryDto.getFromStation() == seatAvailabilityEntryDto.getToStation()) {
            return 0;
        }

        Optional<Train> optionalTrain = trainRepository.findById(seatAvailabilityEntryDto.getTrainId());
        Train train = optionalTrain.get();

        // to get list of valid stations
        String[] totalStationList = train.getRoute().split(",");
        List<String> stationList = new ArrayList<>();
        boolean valid = false;
        for(String station: totalStationList) {
            if(station.equals(seatAvailabilityEntryDto.getFromStation().toString())) {
                valid = true;
            }
            if(valid) {
                stationList.add(station);
            }
            if(station.equals(seatAvailabilityEntryDto.getToStation().toString())) {
                valid = false;
            }
        }
        // given station's validity
        if(valid) {
            throw new RuntimeException("Invalid stations");
        }

        // find total no of seats available by substracting from booked seats from total possible seats
        long totalNoOfPossibleSeats = (long) (Math.pow(2, stationList.size() - 1) - 1) * train.getNoOfSeats();
        for(Ticket bookedTicket: train.getBookedTickets()) {
            boolean started = false;
            int count = 0;
            for(String station: stationList) {
                if(station.equals(bookedTicket.getFromStation().toString())) {
                    started = true;
                }
                if(started) {
                    count += 1;
                }
                if(station.equals((bookedTicket.getToStation().toString()))) {
                    started = false;
                }
            }
            totalNoOfPossibleSeats -= (long) (Math.pow(2, count - 1) - 1) * bookedTicket.getPassengersList().size();
        }

       return (int) totalNoOfPossibleSeats;
    }

    public Integer calculatePeopleBoardingAtAStation(Integer trainId,Station station) throws Exception{

        //We need to find out the number of people who will be boarding a train from a particular station
        //if the trainId is not passing through that station
        //throw new Exception("Train is not passing from this station");
        //  in a happy case we need to find out the number of such people.


        Optional<Train> optionalTrain = trainRepository.findById(trainId);
        if(!optionalTrain.isPresent()) {
            throw new RuntimeException("Invalid train ID !!");
        }

        Train train = optionalTrain.get();

        // check whether station is valid or not
        String[] stationList = train.getRoute().split(",");
        boolean present = false;
        for(String currStation: stationList) {
            if(currStation.equals(station.toString())) {
                present = true;
                break;
            }
        }
        if(present == false) {
            throw new RuntimeException("station not found on train route");
        }

        // find total no of passengers onboarding on given station
        int totalNoOfPassengers = 0;
        for(Ticket bookedTicket : train.getBookedTickets()) {
            if(bookedTicket.getFromStation().equals(station)) {
                totalNoOfPassengers += bookedTicket.getPassengersList().size();
            }
        }

        return totalNoOfPassengers;
    }

    public Integer calculateOldestPersonTravelling(Integer trainId){

        //Throughout the journey of the train between any 2 stations
        //We need to find out the age of the oldest person that is travelling the train
        //If there are no people travelling in that train you can return 0


        Optional<Train> optionalTrain = trainRepository.findById(trainId);
        if(!optionalTrain.isPresent()) {
            throw new RuntimeException("Invalid train ID !!");
        }

        Train train = optionalTrain.get();

        if(train.getBookedTickets().isEmpty()) {
            return 0;
        }

        int oldest = 0;
        for(Ticket bookedTicket: train.getBookedTickets()) {
            for(Passenger passenger: bookedTicket.getPassengersList()) {
                oldest = Math.max(oldest, passenger.getAge());
            }
        }
        return oldest;
    }

    public List<Integer> trainsBetweenAGivenTime(Station station, LocalTime startTime, LocalTime endTime){

        //When you are at a particular station you need to find out the number of trains that will pass through a given station
        //between a particular time frame both start time and end time included.
        //You can assume that the date change doesn't need to be done ie the travel will certainly happen with the same date (More details
        //in problem statement)
        //You can also assume the seconds and milli seconds value will be 0 in a LocalTime format.


        List<Train> trains = trainRepository.findAll();
        List<Integer> availableTrainIds = new ArrayList<>();
        for(Train train: trains) {
            String[] onRouteStationList = train.getRoute().split(",");
            for(int i = 0; i < onRouteStationList.length; i++) {
                String onRouteStation = onRouteStationList[i];
                if(onRouteStation.equals(station.toString())) {
                    int departureTime = ((train.getDepartureTime().getHour() + i) * 60) + train.getDepartureTime().getMinute();
                    int startingTime = (startTime.getHour() * 60) + startTime.getMinute();
                    int endingTime = (endTime.getHour() * 60) + endTime.getMinute();
                    if(departureTime >= startingTime && departureTime <= endingTime) {
                        availableTrainIds.add(train.getTrainId());
                    }
                }
            }
        }
        return availableTrainIds;
    }

}
