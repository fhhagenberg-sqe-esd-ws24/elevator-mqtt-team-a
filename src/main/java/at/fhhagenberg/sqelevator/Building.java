package at.fhhagenberg.sqelevator;

import java.util.List;
import java.util.ArrayList;

/**
 * Building Class, which represents a building and it's
 * Elevators
 */
public class Building {
  private final List<ElevatorDataModell> elevators;

  /**
   * Creates a new Building Instance
   * 
   * @param nrElevators   Number of elevators in the building
   * @param nrFloors      Number of floors in the building
   * @param maxPassengers Maximum allowed passengers
   */
  public Building(int nrElevators, int nrFloors, int maxPassengers) {
    this.elevators = new ArrayList<>(nrElevators);
    for (int i = 0; i < nrElevators; i++) {
      elevators.add(new ElevatorDataModell(i, nrFloors, maxPassengers));
    }
  }

  /**
   * Gets the Elevators in the Building
   * 
   * @return List of Elevators
   */
  public List<ElevatorDataModell> getElevators() {
    return elevators;
  }

  /**
   * Gets the Elevator at a specific index
   * 
   * @param index Index of the elevator
   * @return Elevator at the index
   */
  public ElevatorDataModell getElevator(int index) {
    return elevators.get(index);
  }

  /**
   * Gets the number of Elevators in the Building
   * 
   * @return Number of Elevators
   */
  public int getNrElevators() {
    return elevators.size();
  }

  /**
   * Gets the number of Floors in the Building
   * 
   * @return Number of Floors
   */
  public int getNrFloors() {
    return elevators.get(0).getFloorsRequested().size();
  }

  /**
   * Gets the Maximum allowed Passengers in the Building
   * 
   * @return Maximum allowed Passengers
   */
  public int getMaxPassengers() {
    return elevators.get(0).getMaxPassengers();
  }

  /**
   * Updates the direction of a specific Elevator
   * 
   * @param elevatorNr Elevator Number
   * @param direction  Direction of the Elevator
   * @throws IllegalArgumentException if the Elevator Number is invalid
   */
  public void updateElevatorDirection(int elevatorNr, int direction) {
    if (elevatorNr < 0 || elevatorNr >= elevators.size()) {
      throw new IllegalArgumentException("Invalid Elevator Number");
    }
    elevators.get(elevatorNr).setDirection(direction);
  }

  /**
   * Updates the door status of a specific Elevator
   * 
   * @param elevatorNr Elevator number
   * @param doorStatus Door status of the Elevator
   * @throws IllegalArgumentException if the Elevator Number is invalid
   */
  public void updateElevatorDoorStatus(int elevatorNr, int doorStatus) {
    if (elevatorNr < 0 || elevatorNr >= elevators.size()) {
      throw new IllegalArgumentException("Invalid Elevator Number");
    }
    elevators.get(elevatorNr).setDoorStatus(doorStatus);
  }

  /**
   * Updates the target floor of a specific Elevator
   * 
   * @param elevatorNr  Elevator number
   * @param targetFloor Target floor of the Elevator
   * @throws IllegalArgumentException if the Elevator Number is invalid
   */
  public void updateElevatorTargetFloor(int elevatorNr, int targetFloor) {
    if (elevatorNr < 0 || elevatorNr >= elevators.size()) {
      throw new IllegalArgumentException("Invalid Elevator Number");
    }
    elevators.get(elevatorNr).setTargetFloor(targetFloor);
  }

  /**
   * Updates the current floor of a specific Elevator
   * 
   * @param elevatorNr   Elevator number
   * @param currentFloor Current floor of the Elevator
   * @throws IllegalArgumentException if the Elevator Number is invalid
   */
  public void updateElevatorCurrentFloor(int elevatorNr, int currentFloor) {
    if (elevatorNr < 0 || elevatorNr >= elevators.size()) {
      throw new IllegalArgumentException("Invalid Elevator Number");
    }
    elevators.get(elevatorNr).setCurrentFloor(currentFloor);
  }

  /**
   * Updates the acceleration of a specific Elevator
   * 
   * @param elevatorNr   Elevator number
   * @param acceleration Acceleration of the Elevator
   * @throws IllegalArgumentException if the Elevator Number is invalid
   */
  public void updateElevatorAcceleration(int elevatorNr, int acceleration) {
    if (elevatorNr < 0 || elevatorNr >= elevators.size()) {
      throw new IllegalArgumentException("Invalid Elevator Number");
    }
    elevators.get(elevatorNr).setAcceleration(acceleration);
  }

  /**
   * Updates the speed of a specific Elevator
   * 
   * @param elevatorNr Elevator number
   * @param speed      Speed of the Elevator
   * @throws IllegalArgumentException if the Elevator Number is invalid
   */
  public void updateElevatorSpeed(int elevatorNr, int speed) {
    if (elevatorNr < 0 || elevatorNr >= elevators.size()) {
      throw new IllegalArgumentException("Invalid Elevator Number");
    }
    elevators.get(elevatorNr).setSpeed(speed);
  }

  /**
   * Updates the floors requested of a specific Elevator
   * 
   * @param elevatorNr      Elevator number
   * @param floorsRequested Floors requested by the Elevator
   * @throws IllegalArgumentException if the Elevator Number is invalid
   */
  public void updateElevatorFloorsRequested(int elevatorNr, List<Integer> floorsRequested) {
    if (elevatorNr < 0 || elevatorNr >= elevators.size()) {
      throw new IllegalArgumentException("Invalid Elevator Number");
    }
    elevators.get(elevatorNr).setFloorsRequested(floorsRequested);
  }

  /**
   * Updates the floors to service of a specific Elevator
   * 
   * @param elevatorNr      Elevator number
   * @param floorsToService Floors to service by the Elevator
   * @throws IllegalArgumentException if the Elevator Number is invalid
   */
  public void updateElevatorFloorsToService(int elevatorNr, List<Integer> floorsToService) {
    if (elevatorNr < 0 || elevatorNr >= elevators.size()) {
      throw new IllegalArgumentException("Invalid Elevator Number");
    }
    elevators.get(elevatorNr).setFloorsToService(floorsToService);
  }

  /**
   * Updates the current height of a specific Elevator
   * 
   * @param elevatorNr    Elevator number
   * @param currentHeight Current height of the Elevator
   * @throws IllegalArgumentException if the Elevator Number is invalid
   */
  public void updateElevatorCurrentHeight(int elevatorNr, int currentHeight) {
    if (elevatorNr < 0 || elevatorNr >= elevators.size()) {
      throw new IllegalArgumentException("Invalid Elevator Number");
    }
    elevators.get(elevatorNr).setCurrentHeight(currentHeight);
  }

  /**
   * Updates the current passengers weight of a specific Elevator
   * 
   * @param elevatorNr              Elevator number
   * @param currentPassengersWeight Current passengers weight of the Elevator
   * @throws IllegalArgumentException if the Elevator Number is invalid
   */
  public void updateElevatorCurrentPassengersWeight(int elevatorNr, int currentPassengersWeight) {
    if (elevatorNr < 0 || elevatorNr >= elevators.size()) {
      throw new IllegalArgumentException("Invalid Elevator Number");
    }
    elevators.get(elevatorNr).setCurrentPassengersWeight(currentPassengersWeight);
  }

  /**
   * Gets the Elevator at a specific index
   * 
   * @param index Index of the elevator
   * @return Elevator at the index
   */
}
