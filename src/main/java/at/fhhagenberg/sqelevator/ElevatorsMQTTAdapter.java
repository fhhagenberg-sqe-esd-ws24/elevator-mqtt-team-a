package at.fhhagenberg.sqelevator;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.lang.Thread;
import java.util.function.BiConsumer;
import java.util.ArrayList;
import java.util.List;

/**
 * ElevatorsMQTTAdapter which takes data from the PLC and publishes it over MQTT
 */
public class ElevatorsMQTTAdapter {
  private IElevator controller;
  private Building building;
  private DummyMQTT dummyMQTT = new DummyMQTT();

  /**
   * CTOR
   */
  public ElevatorsMQTTAdapter(IElevator controller) {
    this.controller = controller;
    try{
      int ElevatorCnt = controller.getElevatorNum();
      List<Integer> ElevatorCapacitys = new ArrayList<>(ElevatorCnt);
      for (int i = 0; i < ElevatorCnt; i++)
      {
        ElevatorCapacitys.add(controller.getElevatorCapacity(i));
      }
      this.building = new Building(controller.getElevatorNum(), controller.getFloorNum(), ElevatorCapacitys);
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
   * Polls the Floors to service from the PLC and updates the Building
   * @param elevnr Elevator Number
   * @throws RemoteException
   */
  private void pollAndExecuteForFloorButtons() throws RemoteException{ 
    for(int floornr = 0; floornr < this.building.getNrFloors(); floornr++)
    {
      //must call in extra function as there are no TriConsumer in Java ( ._.)
      boolean floorUpButton = this.controller.getFloorButtonUp(floornr);
      if (this.building.getUpButtonState(floornr) != floorUpButton) {
        this.building.updateUpButtonState(floornr, floorUpButton);
        // Publish over MQTT
        publishMQTT("floors/" + floornr + "/ButtonUpPressed/", floorUpButton);
      }

      boolean floorDownButton = this.controller.getFloorButtonDown(floornr);
      if (this.building.getDownButtonState(floornr) != floorUpButton) {
        this.building.updateDownButtonState(floornr, floorUpButton);
        // Publish over MQTT
        publishMQTT("floors/" + floornr + "/ButtonDownPressed/", floorDownButton);
      }
    }
  }

  /**
   * Updates the State of the Elevators (polls from PLC) and updates data over
   * MQTT
   * if there is a difference
   */
  public void updateState() {

    // update everything that is specific to an elevator
    for (int elevnr = 0; elevnr < this.building.getNrElevators(); elevnr++) {

      System.out.println("Polling Elevator Nr. " + elevnr);      
      pollAndUpdateElevator(elevnr);

    }

    // update everything that is specific to a floor
    try {
      pollAndExecuteForFloorButtons();
    } catch (Exception e) {
      System.out.println("Java RMI request failed!");
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
  private <T> void pollAndExecute (T param1, T param2 , BiConsumer<Integer,T> function,int elevnr, String MqttTopicForPublish) throws RemoteException
  {
    if (param1 != param2) {
      function.accept(elevnr, param2);
      // Publish over MQTT
      this.publishMQTT("elevators/" + elevnr + "/" + MqttTopicForPublish, param2);
    }

  }

  /**
   * Polls the Floors requested from the PLC and updates the Building
   * @param elevnr Elevator Number
   * @throws RemoteException
   */
  private void pollAndExecuteFloorsRequested(int elevnr) throws RemoteException{ 
    for(int floornr = 0; floornr < this.building.getNrFloors(); floornr++)
      {
        //must call in extra function as there are no TriConsumer in Java ( ._.)
        boolean remoteFloorRequested = this.controller.getElevatorButton(elevnr,floornr);
        if (this.building.getElevator(elevnr).getFloorToService(floornr) != remoteFloorRequested) {
          this.building.updateElevatorFloorRequested(elevnr,floornr, remoteFloorRequested);
          // Publish over MQTT
          publishMQTT("elevators/" + elevnr + "/FloorRequested/"+floornr, remoteFloorRequested);
        }
      }
  }

  /**
   * Polls the Floors serviced from the PLC and updates the Building
   * @param elevnr Elevator Number
   * @throws RemoteException
   */
  private void pollAndExecuteFloorsServiced(int elevnr) throws RemoteException{ 
    for(int floornr = 0; floornr < this.building.getNrFloors(); floornr++)
      {
        //must call in extra function as there are no TriConsumer in Java ( ._.)
        boolean remoteFloorServiced = this.controller.getServicesFloors(elevnr,floornr);
        if (this.building.getElevator(elevnr).getFloorToService(floornr) != remoteFloorServiced) {
          this.building.updateElevatorFloorRequested(elevnr,floornr, remoteFloorServiced);
          // Publish over MQTT
          publishMQTT("elevators/" + elevnr + "/FloorServiced/" + floornr, remoteFloorServiced);
        }
      }
  }

  /**
   * Polls an Elevator from the PLC and updates the Building
   */
  private void pollAndUpdateElevator(int elevnr) {

    try{

      pollAndExecute(this.building.getElevator(elevnr).getDirection(), this.controller.getCommittedDirection(elevnr) , this.building::updateElevatorDirection, elevnr, "ElevatorDirection");
      pollAndExecute(this.building.getElevator(elevnr).getDoorStatus(), this.controller.getElevatorDoorStatus(elevnr) , this.building::updateElevatorDoorStatus, elevnr, "ElevatorDoorStatus");
      pollAndExecute(this.building.getElevator(elevnr).getTargetFloor(), this.controller.getTarget(elevnr) , this.building::updateElevatorTargetFloor, elevnr, "ElevatorTargetFloor");
      pollAndExecute(this.building.getElevator(elevnr).getCurrentFloor(), this.controller.getElevatorFloor(elevnr) , this.building::updateElevatorCurrentFloor, elevnr, "ElevatorCurrentFloor");
      pollAndExecute(this.building.getElevator(elevnr).getAcceleration(), this.controller.getElevatorAccel(elevnr) , this.building::updateElevatorAcceleration, elevnr, "ElevatorAcceleration");
      pollAndExecute(this.building.getElevator(elevnr).getSpeed(), this.controller.getElevatorSpeed(elevnr) , this.building::updateElevatorSpeed, elevnr, "ElevatorSpeed");

      pollAndExecuteFloorsRequested(elevnr);
      pollAndExecuteFloorsServiced(elevnr);

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
   * @param topic contains the topic string 
   * @param T data for the topic
   * @return 0 on success, otherwise error
   */
  private <T> int publishMQTT(String topic, T data) {

    System.out.println("Publishing \"" + topic + ": " + data + "\"");

    dummyMQTT.Publish(topic);
    
    //return mqttClient.publish(fullTopic, data.toString());
    return 0;
  }
}
