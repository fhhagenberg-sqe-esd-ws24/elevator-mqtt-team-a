package at.fhhagenberg.sqelevator;

import java.util.List;
import java.util.Vector;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.lang.Thread;
import java.util.function.Function;
import java.util.function.BiConsumer;

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
    try{ this.building = new Building(controller.getElevatorNum(), controller.getFloorNum(), controller.getElevatorCapacity(0));
    } catch (Exception e) {
      System.out.println("Java RMI request failed!");
    }
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
    for (int elevnr = 0; elevnr < this.building.getNrElevators(); elevnr++) {
      System.out.println("Polling Elevator Nr. " + elevnr);
      // Poll Elevator Data from PLC and update Building
    try{

      pollAndExecute(this.building.getElevator(elevnr).getDirection(), this.controller.getCommittedDirection(elevnr) , this.building::updateElevatorDirection, elevnr);
      pollAndExecute(this.building.getElevator(elevnr).getDoorStatus(), this.controller.getElevatorDoorStatus(elevnr) , this.building::updateElevatorDoorStatus, elevnr);
      pollAndExecute(this.building.getElevator(elevnr).getTargetFloor(), this.controller.getTarget(elevnr) , this.building::updateElevatorTargetFloor, elevnr);
      pollAndExecute(this.building.getElevator(elevnr).getCurrentFloor(), this.controller.getElevatorFloor(elevnr) , this.building::updateElevatorCurrentFloor, elevnr);
      pollAndExecute(this.building.getElevator(elevnr).getAcceleration(), this.controller.getElevatorAccel(elevnr) , this.building::updateElevatorAcceleration, elevnr);
      pollAndExecute(this.building.getElevator(elevnr).getSpeed(), this.controller.getElevatorAccel(elevnr) , this.building::updateElevatorAcceleration, elevnr);
      
      for(int floornr = 0; floornr < this.building.getNrFloors(); floornr++)
      {
        pollAndExecute(this.building.getElevator(elevnr).getFloorRequested(floornr), this.controller.getElevatorButton(elevnr, floornr) , this.building::updateElevatorFloorsRequested, elevnr);
      }
      //pollAndExecute(this.building.getElevator(elevnr).getFloorsRequested(), this.controller.getFloorsRequested(elevnr) , this.building::updateElevatorFloorsRequested, elevnr); 
      //pollAndExecute(this.building.getElevator(elevnr).getFloorsToService(), this.controller.getElevatorAccel(elevnr) , this.building::updateElevatorAcceleration, elevnr);
      
      pollAndExecute(this.building.getElevator(elevnr).getAcceleration(), this.controller.getElevatorAccel(elevnr) , this.building::updateElevatorAcceleration, elevnr);
        
      

      } catch (Exception e) {
        System.out.println("Java RMI request failed!");
      }
    }

  }

  public static <T> void pollAndExecute (T param1, T param2 , BiConsumer<Integer,T> function,int elevnr) throws RemoteException
  {
    if (param1 != param2) {
      function.accept(elevnr, param2);
      // Publish over MQTT
    }

  }
  public static <T> void pollAndExecute (T param1, T param2 , BiConsumer<class {Integer,Integer},T> function,int elevnr) throws RemoteException
  {
    if (param1 != param2) {
      function.accept(elevnr, param2);
      // Publish over MQTT
    }

  }


  /**
   * Polls an Elevator from the PLC and updates the Building
   */
  private void pollElevator(int number) {

    // Poll Elevator Data from PLC and update Building
    updateElevatorDirection(number);
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
}
