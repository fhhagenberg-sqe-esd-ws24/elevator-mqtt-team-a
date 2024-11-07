package at.fhhagenberg.sqelevator;

import java.util.List;
import java.util.ArrayList;

/**
 * ElevatorDataModell represents the state and properties of an elevator.
 */
public class ElevatorDataModell {
  private int elevatorNumber = -1; // Elevator number
  private int direction = 0; // Elevator moving direction, 0 = idle, -1 = down, 1 = up
  private int doorStatus = 0; // 0 = closed, 1 = open
  private int targetFloor = 0; // Next floor to reach
  private int currentFloor = 0; // Current floor level
  private int acceleration = 0; // Elevator acceleration
  private int speed = 0; // Current speed of the elevator
  private List<Boolean> floorsRequested; // List of floors requested by passengers
  private List<Boolean> floorsToService; // List of floors the elevator will service
  private int currentHeight = 0; // Position in feet from ground level
  private int currentPassengersWeight = 0; // Current weight of passengers in elevator
  private int maxPassengers = 0; // Maximum allowed passengers

  /**
   * Creates a new ElevatorDataModell Instance
   *
   * @param floorsToService List of floors the elevator will service
   * @param maxPassengers   Maximum allowed passengers
   */
  public ElevatorDataModell(int elevatorNumber, int nrFloors, int maxPassengers) {
    this.elevatorNumber = elevatorNumber;
    this.floorsRequested = new ArrayList<>(nrFloors);
    this.floorsToService = new ArrayList<>(nrFloors);
    this.maxPassengers = maxPassengers;

    // Populate the lists with default values (e.g., null or 0)
    for (int i = 0; i < nrFloors; i++) {
      this.floorsRequested.add(null); // or 0 if you prefer an integer placeholder
      this.floorsToService.add(null); // or 0 if needed
    }
  }

  /**
   * Gets the elevator's current moving direction.
   *
   * @return direction (0 = idle, -1 = down, 1 = up)
   */
  public int getDirection() {
    return direction;
  }

  /**
   * Sets the elevator's moving direction.
   *
   * @param direction (0 = idle, -1 = down, 1 = up)
   */
  public void setDirection(int direction) {
    this.direction = direction;
  }

  /**
   * Gets the elevator's door status.
   */
  public int getDoorStatus() {
    return doorStatus;
  }

  /**
   * Sets the elevator's door status.
   * 
   * @param doorStatus (0 = closed, 1 = open)
   */
  public void setDoorStatus(int doorStatus) {
    this.doorStatus = doorStatus;
  }

  /**
   * Gets the elevator's target floor.
   */
  public int getTargetFloor() {
    return targetFloor;
  }

  /**
   * Sets the elevator's target floor.
   * 
   * @param targetFloor Target floor
   */
  public void setTargetFloor(int targetFloor) {
    this.targetFloor = targetFloor;
  }

  /**
   * Gets the elevator's current floor.
   */
  public int getCurrentFloor() {
    return currentFloor;
  }

  /**
   * Sets the elevator's current floor.
   * 
   * @param currentFloor Current floor
   */
  public void setCurrentFloor(int currentFloor) {
    this.currentFloor = currentFloor;
  }

  /**
   * Gets the elevator's acceleration.
   */
  public int getAcceleration() {
    return acceleration;
  }

  /**
   * Sets the elevator's acceleration.
   * 
   * @param acceleration Acceleration
   */
  public void setAcceleration(int acceleration) {
    this.acceleration = acceleration;
  }

  /**
   * Gets the elevator's current speed.
   */
  public int getSpeed() {
    return speed;
  }

  /**
   * Sets the elevator's current speed.
   * 
   * @param speed Current speed
   */
  public void setSpeed(int speed) {
    this.speed = speed;
  }

  /**
  * Gets if a floor is requested for the elevator
  * 
  * @param floorToService Floor to service
  *
  * @return if the floor is requested or not
  */
  public Boolean getFloorRequested(int floorToService) {
    return floorsRequested.get(floorToService);
  }

  /**
   * Set a requested floor
   * 
   * @param floorToService Floor to service
   * @param isRequested    Request the floor or not
   */
  public void setFloorRequested(int floorToService, boolean isRequested) {
    // Access by Index
    this.floorsRequested.set(floorToService,isRequested);
  }

  /**
   * Gets if a floor will be serviced by the elevator
   * 
   * @param floorToService Floor to service
   * 
   * @return if the floor will be serviced or not
   */
  public Boolean getFloorToService(int floorToService) {
    return floorsToService.get(floorToService);
  }

  /**
   * Set a floor to service
   * 
   * @param floorToService Floor to service
   * @param doService      Service the floor or not
   */
  public void setFloorToService(int floorToService, boolean doService) {
    this.floorsToService.set(floorToService,doService);
  }

  /**
   * Gets the current height of the elevator.
   * 
   * @return Current height
   */
  public int getCurrentHeight() {
    return currentHeight;
  }

  /**
   * Sets the current height of the elevator.
   * 
   * @param currentHeight Current height
   */
  public void setCurrentHeight(int currentHeight) {
    this.currentHeight = currentHeight;
  }

  /**
   * Gets the current weight of passengers in the elevator.
   */
  public int getCurrentPassengersWeight() {
    return currentPassengersWeight;
  }

  /**
   * Sets the current weight of passengers in the elevator.
   * 
   * @param currentPassengersWeight Current weight
   */
  public void setCurrentPassengersWeight(int currentPassengersWeight) {
    this.currentPassengersWeight = currentPassengersWeight;
  }

  /**
   * Gets the maximum allowed passengers.
   * 
   * @return Maximum allowed passengers
   */
  public int getMaxPassengers() {
    return maxPassengers;
  }

  /**
   * Gets the elevator number.
   * 
   * @return Elevator number
   */
  public int getElevatorNumber() {
    return this.elevatorNumber;
  }
}
