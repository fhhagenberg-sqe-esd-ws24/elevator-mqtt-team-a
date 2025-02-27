package at.fhhagenberg.sqelevator;

import java.util.List;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Building Class, which represents a building and it's
 * Elevators
 */
public class Building {

  private static final String INVALID_FLOOR_NUMBER_WAS = "Invalid Floor Number Was ";
  private static final String INVALID_ELEVATOR_NUMBER = "Invalid Elevator Number";

  /** Hold information of Elevators in the Building */
  private final List<ElevatorDataModell> elevators;

  /** Hold information of Button state for each floor */
  private final List<Boolean> floorUpButtonsPressed;
  private final List<Boolean> floorDownButtonsPressed;

  /** Stores number of floors in the building */
  private final int nrFloors;

  /**
   * Creates a new Building Instance
   * 
   * @param nrElevators   Number of elevators in the building
   * @param nrFloors      Number of floors in the building
   * @param maxPassengers Maximum allowed passengers
   */
  public Building(int nrElevators, int nrFloors, List<Integer> maxPassengers) {
    this.nrFloors = nrFloors;
    this.elevators = new ArrayList<>();
    this.floorUpButtonsPressed = Arrays.asList(new Boolean[nrFloors]);
    this.floorDownButtonsPressed = Arrays.asList(new Boolean[nrFloors]);
    for (int i = 0; i < nrFloors; i++) {
      this.floorDownButtonsPressed.set(i, false);
      this.floorUpButtonsPressed.set(i, false);
    }
    for (int i = 0; i < nrElevators; i++) {
      elevators.add(new ElevatorDataModell(i, nrFloors, maxPassengers.get(i)));
    }
  }

  public Building(Building other) {
    if (other == null) {
      throw new IllegalArgumentException("Building cannot be null");
    }
    // copy elevators
    this.elevators = new ArrayList<>();
    for (int i = 0; i < other.elevators.size(); i++) {
      this.elevators.add(new ElevatorDataModell(other.elevators.get(i)));
    }
    this.nrFloors = other.nrFloors;
    this.floorUpButtonsPressed = new ArrayList<Boolean>(other.floorUpButtonsPressed);
    this.floorDownButtonsPressed = new ArrayList<Boolean>(other.floorDownButtonsPressed);
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
    return this.nrFloors;
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
      throw new IllegalArgumentException(INVALID_ELEVATOR_NUMBER);
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
      throw new IllegalArgumentException(INVALID_ELEVATOR_NUMBER);
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
      throw new IllegalArgumentException(INVALID_ELEVATOR_NUMBER);
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
      throw new IllegalArgumentException(INVALID_ELEVATOR_NUMBER);
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
      throw new IllegalArgumentException(INVALID_ELEVATOR_NUMBER);
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
      throw new IllegalArgumentException(INVALID_ELEVATOR_NUMBER);
    }
    elevators.get(elevatorNr).setSpeed(speed);
  }

  /**
   * Updates the floors requested of a specific Elevator
   * 
   * @param elevatorNr     Elevator number
   * @param floorRequested Floor requested by the Elevator
   * @param isRequested    Request the floor or not
   * @throws IllegalArgumentException if the Elevator Number is invalid
   */
  public void updateElevatorFloorRequested(int elevatorNr, int floorRequested, boolean isRequested) {
    if (elevatorNr < 0 || elevatorNr >= elevators.size()) {
      throw new IllegalArgumentException(INVALID_ELEVATOR_NUMBER);
    }
    elevators.get(elevatorNr).setFloorRequested(floorRequested, isRequested);
  }

  /**
   * Updates the floors to service of a specific Elevator
   * 
   * @param elevatorNr     Elevator number
   * @param floorToService Floor to service by the Elevator
   * @param doService      Service the floor or not
   * @throws IllegalArgumentException if the Elevator Number is invalid
   */
  public void updateElevatorFloorToService(int elevatorNr, int floorToService, boolean doService) {
    if (elevatorNr < 0 || elevatorNr >= elevators.size()) {
      throw new IllegalArgumentException(INVALID_ELEVATOR_NUMBER);
    }
    elevators.get(elevatorNr).setFloorToService(floorToService, doService);
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
      throw new IllegalArgumentException(INVALID_ELEVATOR_NUMBER);
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
      throw new IllegalArgumentException(INVALID_ELEVATOR_NUMBER);
    }
    elevators.get(elevatorNr).setCurrentPassengersWeight(currentPassengersWeight);
  }

  /**
   * Updates the up button state of a specific floor
   * 
   * @param floorNr Floor number
   * @param state   Button state of the floor
   * @throws IllegalArgumentException if the Floor Number is invalid
   */
  public void updateUpButtonState(int floorNr, boolean state) {
    if (floorNr < 0 || floorNr >= nrFloors) {
      throw new IllegalArgumentException(INVALID_FLOOR_NUMBER_WAS + floorNr);
    }
    floorUpButtonsPressed.set(floorNr, state);
  }

  /**
   * Gets the up button state of a specific floor
   * 
   * @param floorNr Floor number
   * @return Button state of the floor
   * @throws IllegalArgumentException if the Floor Number is invalid
   */
  public boolean getUpButtonState(int floorNr) {
    if (floorNr < 0 || floorNr >= nrFloors) {
      throw new IllegalArgumentException(INVALID_FLOOR_NUMBER_WAS + floorNr);
    }
    return floorUpButtonsPressed.get(floorNr);
  }

  /**
   * Updates the down button state of a specific floor
   * 
   * @param floorNr Floor number
   * @param state   Button state of the floor
   * @throws IllegalArgumentException if the Floor Number is invalid
   */
  public void updateDownButtonState(int floorNr, boolean state) {
    if (floorNr < 0 || floorNr >= nrFloors) {
      throw new IllegalArgumentException(INVALID_FLOOR_NUMBER_WAS + floorNr);
    }
    floorDownButtonsPressed.set(floorNr, state);
  }

  /**
   * Gets the down button state of a specific floor
   * 
   * @param floorNr Floor number
   * @return Button state of the floor
   * @throws IllegalArgumentException if the Floor Number is invalid
   */
  public boolean getDownButtonState(int floorNr) {
    if (floorNr < 0 || floorNr >= nrFloors) {
      throw new IllegalArgumentException(INVALID_FLOOR_NUMBER_WAS + floorNr);
    }
    return floorDownButtonsPressed.get(floorNr);
  }
}
