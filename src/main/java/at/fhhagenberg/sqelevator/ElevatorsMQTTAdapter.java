package at.fhhagenberg.sqelevator;

import java.util.Vector;
import java.rmi.Naming;
import java.lang.Thread;

/**
 * ElevatorsMQTTAdapter which takes data from the PLC and publishes it over MQTT
 */
public class ElevatorsMQTTAdapter {
  private IElevator controller;
  private Building building;

  /**
   * CTOR
   */
  public ElevatorsMQTTAdapter(IElevator controller) {
    this.controller = controller;
  }

  /**
   * Main function which polls data and publishes over MQTT
   * 
   * @param args arguments passed to main function
   */
  public static void main(String[] args) {
    try {
      IElevator controller = (IElevator) Naming.lookup("rmi://localhost/ElevatorSim");
      ElevatorsMQTTAdapter client = new ElevatorsMQTTAdapter(controller);

      client.run();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Runner
   */
  private void run() {
    // Loop Forever
    while (true) {
      this.updateState();
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
      }
    }
  }

  /**
   * Updates the State of the Elevators (polls from PLC) and updates data over
   * MQTT
   * if there is a difference
   */
  private void updateState() {
    for (int elevnr = 0; elevnr < this.elevators.size(); elevnr++) {
      System.out.println("Polling Elevator Nr. " + elevnr);
      // Poll Elevator Data from PLC
      var tmp = this.pollElevator(elevnr);
      if (!tmp.equals(this.elevators.get(elevnr))) {
        System.out.println("Elevator Nr. " + elevnr + " changed");
        // store updated data
        this.elevators.set(elevnr, tmp);
        // Publish over MQTT if there is a difference in data
        this.publishMQTT(elevnr);
      }
    }
  }

  /**
   * Publish updates over MQTT for a specific Elevator, if there
   * are changes
   * 
   * @param elevnr Elevator to update
   * @return 0 on success, otherwise error
   */
  private int publishMQTT(int elevnr) {
    System.out.println("Publishing Elevator Nr. " + elevnr + " over MQTT :)");
    return 0;
  }

  /**
   * Polls an Elevator from the PLC and returns an ElevatorDataModell
   */
  private ElevatorDataModell pollElevator(int number) {
    var elev = new ElevatorDataModell();
    try {
      elev.direction = controller.getCommittedDirection(number);
      elev.doorStatus = controller.getElevatorDoorStatus(number);
      elev.targetFloor = controller.getTarget(number);
      elev.currentFloor = controller.getElevatorFloor(number);
      elev.acceleration = controller.getElevatorAccel(number);
      elev.speed = controller.getElevatorSpeed(number);

      for (int floor = 0; floor < controller.getFloorNum(); floor++) {
        if (controller.getElevatorButton(number, floor)) {
          elev.floorsRequested.add(floor);
        }
        if (controller.getServicesFloors(number, floor)) {
          elev.floorsToService.add(floor);
        }
      }

      elev.currentHeight = controller.getElevatorPosition(number);
      elev.currentPassengersWeight = controller.getElevatorWeight(number);
      elev.maxPassengers = controller.getElevatorCapacity(number);

    } catch (Exception e) {
      System.out.println("Java RMI request failed!");
    }
    return elev;
  }
}
