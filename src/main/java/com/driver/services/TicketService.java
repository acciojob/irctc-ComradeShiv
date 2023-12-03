package com.driver.services;


import com.driver.EntryDto.BookTicketEntryDto;
import com.driver.model.Passenger;
import com.driver.model.Ticket;
import com.driver.model.Train;
import com.driver.repository.PassengerRepository;
import com.driver.repository.TicketRepository;
import com.driver.repository.TrainRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
public class TicketService {

    @Autowired
    TicketRepository ticketRepository;

    @Autowired
    TrainRepository trainRepository;

    @Autowired
    PassengerRepository passengerRepository;


    public Integer bookTicket(BookTicketEntryDto bookTicketEntryDto)throws Exception{

        //Check for validity
        //Use bookedTickets List from the TrainRepository to get bookings done against that train
        // In case the there are insufficient tickets
        // throw new Exception("Less tickets are available");
        //otherwise book the ticket, calculate the price and other details
        //Save the information in corresponding DB Tables
        //Fare System : Check problem statement
        //In case the train doesn't pass through the requested stations
        //throw new Exception("Invalid stations");
        //Save the bookedTickets in the train Object
        //Also in the passenger Entity change the attribute bookedTickets by using the attribute bookingPersonId.
       //And the end return the ticketId that has come from db


        // check trainId validity
        Optional<Train> optionalTrain = trainRepository.findById(bookTicketEntryDto.getTrainId());
        if(!optionalTrain.isPresent()) {
            throw new RuntimeException("Invalid Train ID !!");
        }

        Train train = optionalTrain.get();

        // find list of passengers
        List<Passenger> passengerList = new ArrayList<>();
        for(Integer passengerId: bookTicketEntryDto.getPassengerIds()) {
            Optional<Passenger> optionalPassenger = passengerRepository.findById(passengerId);
            if(!optionalPassenger.isPresent()) {
                throw new RuntimeException("Invalid Passenger ID !!");
            }
            Passenger passenger = optionalPassenger.get();
            passengerList.add(passenger);
        }

        if(bookTicketEntryDto.getFromStation() == bookTicketEntryDto.getToStation()) {
            throw new RuntimeException("Same station given");
        }

        // find list of all stations in between
        String[] totalStationList = train.getRoute().split(",");
        List<String> stationList = new ArrayList<>();
        boolean valid = false;
        for(String station: totalStationList) {
            if(station.equals(bookTicketEntryDto.getFromStation().toString())) {
                valid = true;
            }
            if(valid) {
                stationList.add(station);
            }
            if(station.equals(bookTicketEntryDto.getToStation().toString())) {
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

        int noOfSeatsAvailable = (int) totalNoOfPossibleSeats;

        if(noOfSeatsAvailable < bookTicketEntryDto.getNoOfSeats()) {
            throw new RuntimeException("Less tickets are available");
        }

        // calculate total fare
        int totalFare = (stationList.size() - 1) * passengerList.size() * 300;

        // create booked ticket
        Ticket ticket = new Ticket();
        ticket.setPassengersList(passengerList);
        ticket.setTrain(train);
        ticket.setFromStation(bookTicketEntryDto.getFromStation());
        ticket.setTotalFare(totalFare);
        ticket.setToStation(bookTicketEntryDto.getToStation());

        train.getBookedTickets().add(ticket);
        Train train1 = trainRepository.save(train);
        Ticket bookedTicket = train1.getBookedTickets().get(train1.getBookedTickets().size() -1 );

        // update booked tickets in booking person entity
        Optional<Passenger> optionalBookingPerson = passengerRepository.findById(bookTicketEntryDto.getBookingPersonId());
        if(!optionalBookingPerson.isPresent()) {
            throw new RuntimeException("Invalid booking Person ID !!");
        }
        Passenger bookingPerson = optionalBookingPerson.get();
        bookingPerson.getBookedTickets().add(bookedTicket);
        Passenger savedBookingPerson = passengerRepository.save(bookingPerson);

       return bookedTicket.getTicketId();

    }
}
