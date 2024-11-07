package at.fhhagenberg.sqelevator;

import java.util.List;
import java.util.ArrayList;

/**
 * Building Class, which represents a building and it's
 * Elevators
 */
public class Building {

  /** State variable for a button of a floor when it is pressed up */
  public final static int BUTTON_PRESSED_DIRECTION_UP = 0;
  /** State variable for a button of a floor when it is pressed down */
  public final static int BUTTON_PRESSED_DIRECTION_DOWN = 1;
  /** State variable for a button of a floor when it is not pressed */
  public final static int BUTTON_NOT_PRESSED = 2;

  /** Hold information of Elevators in the Building */
  private final List<ElevatorDataModell> elevators;

  /** Hold information of Button state for each floor */
  private final List<Integer> buttonsPressed;

  /** Stores number of floors in the building */
  private final int nrFloors;

  /**
   * Creates a new Building Instance
   * 
   * @param nrElevators   Number of elevators in the building
   * @param nrFloors      Number of floors in the building
   * @param maxPassengers Maximum allowed passengers
   */
  public Building(int nrElevators, int nrFloors, int maxPassengers) {
    this.nrFloors = nrFloors;
    this.elevators = new ArrayList<>(nrElevators);
    this.buttonsPressed = new ArrayList<>(nrFloors);
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
   * @param elevatorNr     Elevator number
   * @param floorRequested Floor requested by the Elevator
   * @param isRequested    Request the floor or not
   * @throws IllegalArgumentException if the Elevator Number is invalid
   */
  public void updateElevatorFloorRequested(int elevatorNr, int floorRequested, boolean isRequested) {
    if (elevatorNr < 0 || elevatorNr >= elevators.size()) {
      throw new IllegalArgumentException("Invalid Elevator Number");
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
      throw new IllegalArgumentException("Invalid Elevator Number");
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
   * Updates the button state of a specific floor
   * 
   * @param floorNr Floor number
   * @param state   Button state of the floor
   * @throws IllegalArgumentException if the Floor Number is invalid
   */
  public void updateButtonState(int floorNr, int state) {
    if (floorNr < 0 || floorNr >= buttonsPressed.size()) {
      throw new IllegalArgumentException("Invalid Floor Number");
    }
    buttonsPressed.set(floorNr, state);
  }

  /**
   * Gets the button state of a specific floor
   * 
   * @param floorNr Floor number
   * @return Button state of the floor
   * @throws IllegalArgumentException if the Floor Number is invalid
   */
  public int getButtonState(int floorNr) {
    if (floorNr < 0 || floorNr >= buttonsPressed.size()) {
      throw new IllegalArgumentException("Invalid Floor Number");
    }
    return buttonsPressed.get(floorNr);
  }
}
