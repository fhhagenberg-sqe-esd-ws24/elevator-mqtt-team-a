package at.fhhagenberg.sqelevator;

import java.util.List;

/**
 * ElevatorDataModell represents the state and properties of an elevator.
 */
public class ElevatorDataModell {
  public int direction; // Elevator moving direction
  public int doorStatus; // If elevator door is open/closed/..
  public int targetFloor; // next Floor
  public int currentFloor; // Floor level
  public int acceleration; // Elevator acceleration
  public int speed; // Elevator speed
  public List<Integer> floorsRequested; // List of Floors to 'travel' to
  public List<Integer> floorsToService; // Floors the elevator is going to
  public int currentHeight; // Position in feet from ground
  public int currentPassengersWeight; // Weight of passengers
  public int maxPassengers; // Maximum number of passengers which fit in the Elevator
}
