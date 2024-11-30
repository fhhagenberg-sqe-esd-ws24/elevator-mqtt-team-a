package at.fhhagenberg.sqelevator;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.io.FileInputStream;
import java.lang.Thread;
import java.util.function.BiConsumer;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import java.util.concurrent.CompletableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * ElevatorsMQTTAdapter which takes data from the PLC and publishes it over MQTT
 */
public class ElevatorsMQTTAdapter {
  private IElevator controller;
  private Building building;
  private Mqtt5AsyncClient mqttClient;
  private int PollingIntervall;

  @FunctionalInterface
  public interface MessageHandler {
    void handleMessage(String topic, String message);
  }

  /**
   * CTOR
   */
  public ElevatorsMQTTAdapter(IElevator controller, Mqtt5AsyncClient _mqttClient, int PollingIntervall) {
    this.controller = controller;
    this.mqttClient = _mqttClient;
    this.PollingIntervall = PollingIntervall;

    // Connect to the broker
    CompletableFuture<Void> connectFuture = mqttClient.connect()
        .thenAccept(connAck -> {
          System.out.println("Connected successfully!");
        })
        .exceptionally(throwable -> {
          System.err.println("Connection failed: " + throwable.getMessage());
          return null;
        });

    connectFuture.join();

    try {
      // fetch number of elevators and publish to subscribers
      // TODO: revise Topics (/building-id/elevators ...)
      int ElevatorCnt = controller.getElevatorNum();
      this.publishRetainedMQTT("elevators/NrElevators", ElevatorCnt);

      // fetch capacities of elevators and publish to subscribers
      List<Integer> ElevatorCapacitys = new ArrayList<>(ElevatorCnt);
      for (int i = 0; i < ElevatorCnt; i++) {
        int capacity = controller.getElevatorCapacity(i);
        ElevatorCapacitys.add(capacity);
        this.publishRetainedMQTT("elevators/" + i + "/ElevatorCapacity", capacity);
      }

      // fetch number of floors and publish to subscribers
      int floorNumber = controller.getFloorNum();
      this.building = new Building(ElevatorCnt, floorNumber, ElevatorCapacitys);
      this.publishRetainedMQTT("elevators/NrFloors", floorNumber);

      // subscribe SetTarget
      this.building.getElevators().forEach((elevator) -> {
        this.subscribeMQTT("elevators/" + elevator.getElevatorNumber() + "/SetTarget", (topic, message) -> {
          try {
            this.controller.setTarget(elevator.getElevatorNumber(), Integer.parseInt(message));
          } catch (Exception e) {
            System.err.println(e.toString());
          }
        });
      });
    } catch (Exception e) {
      System.out.println(e.toString());
    }
  }

  /**
   * Main function which polls data and publishes over MQTT
   * 
   * @param args arguments passed to main function
   */
  public static void main(String[] args) {
    try {

      String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
      String appConfigPath = rootPath + "Elevators.properties";

      Properties appProps = new Properties();
      appProps.load(new FileInputStream(appConfigPath));

      IElevator controller = (IElevator) Naming.lookup(appProps.getProperty("IElevatorRMI"));

      // Create an MQTT client
      Mqtt5AsyncClient mqttClient = MqttClient.builder()
          .automaticReconnectWithDefaultConfig()
          .useMqttVersion5()
          .identifier(appProps.getProperty("MqttIdentifier"))
          .serverHost(appProps.getProperty("MqttHost")) // Public HiveMQ broker
          .serverPort((Integer) appProps.get("MqttPort")) // Default MQTT port
          .buildAsync();

      ElevatorsMQTTAdapter client = new ElevatorsMQTTAdapter(controller, mqttClient,
          (Integer) appProps.get("PollingIntervall"));

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
        Thread.sleep(this.PollingIntervall);
      } catch (InterruptedException e) {
      }
    }
  }

  /**
   * Polls the Floors to service from the PLC and updates the Building
   * 
   * @param elevnr Elevator Number
   * @throws RemoteException
   */
  private void pollAndExecuteForFloorButtons() throws RemoteException {
    for (int floornr = 0; floornr < this.building.getNrFloors(); floornr++) {
      // must call in extra function as there are no TriConsumer in Java ( ._.)
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
      System.out.println(e.toString());
    }
  }

  /**
   * Polls a value from the PLC and executes a function if there is a difference
   * 
   * @param param1   Value from the Building
   * @param param2   Value from the PLC
   * @param function Function to execute if there is a difference
   * @param elevnr   Elevator Number
   * @param <T>      Type of the value
   */
  private <T> void pollAndExecute(T param1, T param2, BiConsumer<Integer, T> function, int elevnr,
      String MqttTopicForPublish) throws RemoteException {
    if (param1 != param2) {
      function.accept(elevnr, param2);
      // Publish over MQTT
      this.publishMQTT("elevators/" + elevnr + "/" + MqttTopicForPublish, param2);
    }

  }

  /**
   * Polls the Floors requested from the PLC and updates the Building
   * 
   * @param elevnr Elevator Number
   * @throws RemoteException
   */
  private void pollAndExecuteFloorsRequested(int elevnr) throws RemoteException {
    for (int floornr = 0; floornr < this.building.getNrFloors(); floornr++) {
      // must call in extra function as there are no TriConsumer in Java ( ._.)
      boolean remoteFloorRequested = this.controller.getElevatorButton(elevnr, floornr);
      if (this.building.getElevator(elevnr).getFloorRequested(floornr) != remoteFloorRequested) {
        this.building.updateElevatorFloorRequested(elevnr, floornr, remoteFloorRequested);
        // Publish over MQTT
        publishMQTT("elevators/" + elevnr + "/FloorRequested/" + floornr, remoteFloorRequested);
      }
    }
  }

  /**
   * Polls the Floors serviced from the PLC and updates the Building
   * 
   * @param elevnr Elevator Number
   * @throws RemoteException
   */
  private void pollAndExecuteFloorsServiced(int elevnr) throws RemoteException {
    for (int floornr = 0; floornr < this.building.getNrFloors(); floornr++) {
      // must call in extra function as there are no TriConsumer in Java ( ._.)
      boolean remoteFloorServiced = this.controller.getServicesFloors(elevnr, floornr);
      if (this.building.getElevator(elevnr).getFloorToService(floornr) != remoteFloorServiced) {
        this.building.updateElevatorFloorRequested(elevnr, floornr, remoteFloorServiced);
        // Publish over MQTT
        publishMQTT("elevators/" + elevnr + "/FloorServiced/" + floornr, remoteFloorServiced);
      }
    }
  }

  /**
   * Polls an Elevator from the PLC and updates the Building
   */
  private void pollAndUpdateElevator(int elevnr) {

    try {

      pollAndExecute(this.building.getElevator(elevnr).getDirection(), this.controller.getCommittedDirection(elevnr),
          this.building::updateElevatorDirection, elevnr, "ElevatorDirection");
      pollAndExecute(this.building.getElevator(elevnr).getDoorStatus(), this.controller.getElevatorDoorStatus(elevnr),
          this.building::updateElevatorDoorStatus, elevnr, "ElevatorDoorStatus");
      pollAndExecute(this.building.getElevator(elevnr).getTargetFloor(), this.controller.getTarget(elevnr),
          this.building::updateElevatorTargetFloor, elevnr, "ElevatorTargetFloor");
      pollAndExecute(this.building.getElevator(elevnr).getCurrentFloor(), this.controller.getElevatorFloor(elevnr),
          this.building::updateElevatorCurrentFloor, elevnr, "ElevatorCurrentFloor");
      pollAndExecute(this.building.getElevator(elevnr).getAcceleration(), this.controller.getElevatorAccel(elevnr),
          this.building::updateElevatorAcceleration, elevnr, "ElevatorAcceleration");
      pollAndExecute(this.building.getElevator(elevnr).getSpeed(), this.controller.getElevatorSpeed(elevnr),
          this.building::updateElevatorSpeed, elevnr, "ElevatorSpeed");

      pollAndExecuteFloorsRequested(elevnr);
      pollAndExecuteFloorsServiced(elevnr);

      pollAndExecute(this.building.getElevator(elevnr).getCurrentHeight(), this.controller.getElevatorPosition(elevnr),
          this.building::updateElevatorCurrentHeight, elevnr, "ElevatorCurrentHeight");
      pollAndExecute(this.building.getElevator(elevnr).getCurrentPassengersWeight(),
          this.controller.getElevatorWeight(elevnr), this.building::updateElevatorCurrentPassengersWeight, elevnr,
          "ElevatorCurrentPassengersWeight");

    } catch (Exception e) {
      System.out.println(e.toString());
    }
  }

  /**
   * Publish updates over MQTT for a specific Elevator, if there
   * are changes
   * 
   * @param topic  contains the topic string
   * @param T      data for the topic
   * @param retain determines if the message should be retained
   * @throws IllegalStateException is thrown when not connected to broker
   */
  private <T> void publishMQTTHelper(String topic, T data, boolean retain) throws IllegalStateException {

    System.out.println("Publishing \"" + topic + ": " + data + "\"");

    if (this.mqttClient.getState() != MqttClientState.CONNECTED) {
      throw new IllegalStateException("Client not connected to Broker!");
    }

    this.mqttClient.publishWith()
        .topic(topic)
        .payload(data.toString().getBytes())
        .qos(MqttQos.AT_LEAST_ONCE)
        .retain(retain)
        .send()
        .thenAccept(pubAck -> System.out.println("Published message: " + data.toString() + " to topic: " + topic))
        .exceptionally(throwable -> {
          System.err.println("Failed to publish: " + throwable.getMessage());
          return null;
        });
  }

  /**
   * Publish updates over MQTT for a specific Elevator, if there
   * are changes
   * 
   * @param topic contains the topic string
   * @param T     data for the topic
   */
  private <T> void publishRetainedMQTT(String topic, T data) {

    this.publishMQTTHelper(topic, data, true);
  }

  /**
   * Publish updates over MQTT for a specific Elevator, if there
   * are changes
   * 
   * @param topic contains the topic string
   * @param T     data for the topic
   */
  private <T> void publishMQTT(String topic, T data) {

    this.publishMQTTHelper(topic, data, false);
  }

  /**
   * 
   */
  private void subscribeMQTT(String topic, MessageHandler messageHandler) {
    // Subscribe to a topic
    mqttClient.subscribeWith()
        .topicFilter(topic)
        .qos(MqttQos.AT_LEAST_ONCE) // QoS level 1
        .callback(publish -> {
          String message = new String(publish.getPayloadAsBytes());
          messageHandler.handleMessage(topic, message);
        }) // Use the provided message handler
        .send()
        .whenComplete((subAck, throwable) -> {
          if (throwable != null) {
            // Handle subscription failure
            System.err.println("Failed to subscribe: " + throwable.getMessage());
          } else {
            // Handle successful subscription
            System.out.println("Subscribed successfully to topic: " + topic);
          }
        });
  }

  // DTOR
  protected void finalize() {
    try {
      this.mqttClient.disconnect();
    } catch (Exception e) {
      System.out.println(e.toString());
    }
  }
}
