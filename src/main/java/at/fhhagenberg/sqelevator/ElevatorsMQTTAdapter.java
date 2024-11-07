package at.fhhagenberg.sqelevator;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.lang.Thread;
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
  public void updateState() {
    for (int elevnr = 0; elevnr < this.building.getNrElevators(); elevnr++) {

      System.out.println("Polling Elevator Nr. " + elevnr);      
      pollAndUpdateElevator(elevnr);

    }
  }

  /**
   * Polls a value from the PLC and executes a function if there is a difference
   * 
   * @param param1    Value from the Building
   * @param param2    Value from the PLC
   * @param function  Function to execute if there is a difference
   * @param elevnr    Elevator Number
   * @param <T>       Type of the value
   */
  public <T> void pollAndExecute (T param1, T param2 , BiConsumer<Integer,T> function,int elevnr, String MqttTopicForPublish) throws RemoteException
  {
    if (param1 != param2) {
      function.accept(elevnr, param2);
      // Publish over MQTT
      this.publishMQTT(elevnr, MqttTopicForPublish, param2);
      
    }

  }

  /**
   * Polls the Floors requested from the PLC and updates the Building
   * @param elevnr Elevator Number
   * @throws RemoteException
   */
  public void pollAndExecuteFloorsRequested(int elevnr) throws RemoteException{ 
    for(int floornr = 0; floornr < this.building.getNrFloors(); floornr++)
      {
        //must call in extra function as there are no TriConsumer in Java ( ._.)
        boolean remoteFloorRequested = this.controller.getElevatorButton(elevnr,floornr);
        if (this.building.getElevator(elevnr).getFloorToService(floornr) != remoteFloorRequested) {
          this.building.updateElevatorFloorRequested(elevnr,floornr, remoteFloorRequested);
          // Publish over MQTT
          publishMQTT(elevnr, "FloorRequested/"+floornr, remoteFloorRequested);
        }
      }
  }

  /**
   * Polls the Floors to service from the PLC and updates the Building
   * @param elevnr Elevator Number
   * @throws RemoteException
   */
  public void pollAndExecuteFloorsToService(int elevnr) throws RemoteException{ 
    for(int floornr = 0; floornr < this.building.getNrFloors(); floornr++)
      {
        //must call in extra function as there are no TriConsumer in Java ( ._.)
        boolean remoteFloorToService = this.controller.getServicesFloors(elevnr,floornr);
        if (this.building.getElevator(elevnr).getFloorToService(floornr) != remoteFloorToService) {
          this.building.updateElevatorFloorToService(elevnr,floornr, remoteFloorToService);
          // Publish over MQTT
          publishMQTT(elevnr, "FloorToService/"+floornr, remoteFloorToService);
        }
      }
  }

  /**
   * Polls an Elevator from the PLC and updates the Building
   */
  public void pollAndUpdateElevator(int elevnr) {

    try{

      pollAndExecute(this.building.getElevator(elevnr).getDirection(), this.controller.getCommittedDirection(elevnr) , this.building::updateElevatorDirection, elevnr, "ElevatorDirection");
      pollAndExecute(this.building.getElevator(elevnr).getDoorStatus(), this.controller.getElevatorDoorStatus(elevnr) , this.building::updateElevatorDoorStatus, elevnr, "ElevatorDoorStatus");
      pollAndExecute(this.building.getElevator(elevnr).getTargetFloor(), this.controller.getTarget(elevnr) , this.building::updateElevatorTargetFloor, elevnr, "ElevatorTargetFloor");
      pollAndExecute(this.building.getElevator(elevnr).getCurrentFloor(), this.controller.getElevatorFloor(elevnr) , this.building::updateElevatorCurrentFloor, elevnr, "ElevatorCurrentFloor");
      pollAndExecute(this.building.getElevator(elevnr).getAcceleration(), this.controller.getElevatorAccel(elevnr) , this.building::updateElevatorAcceleration, elevnr, "ElevatorAcceleration");
      pollAndExecute(this.building.getElevator(elevnr).getSpeed(), this.controller.getElevatorSpeed(elevnr) , this.building::updateElevatorSpeed, elevnr, "ElevatorSpeed");
      
      pollAndExecuteFloorsRequested(elevnr);
      pollAndExecuteFloorsToService(elevnr);

      pollAndExecute(this.building.getElevator(elevnr).getCurrentHeight(), this.controller.getElevatorPosition(elevnr) , this.building::updateElevatorCurrentHeight, elevnr, "ElevatorCurrentHeight");
        
      pollAndExecute(this.building.getElevator(elevnr).getCurrentPassengersWeight(), this.controller.getElevatorWeight(elevnr) , this.building::updateElevatorCurrentPassengersWeight, elevnr, "ElevatorCurrentPassengersWeight");
      pollAndExecute(this.building.getElevator(elevnr).getCurrentHeight(), this.controller.getElevatorPosition(elevnr) , this.building::updateElevatorCurrentHeight, elevnr, "ElevatorCurrentHeight");
       

      } catch (Exception e) {
        System.out.println("Java RMI request failed!");
      }
    }
  
  /**
   * Publish updates over MQTT for a specific Elevator, if there
   * are changes
   * 
   * @param elevnr Elevator to update
   * @return 0 on success, otherwise error
   */
  public <T> int publishMQTT(int elevnr, String topic, T data) {
    String fullTopic = "elevators/" + elevnr + "/" + topic;

    System.out.println("Publishing \"" + fullTopic + ": " + data + "\"");    
    
    //return mqttClient.publish(fullTopic, data.toString());
    return 0;
    
  }
}
