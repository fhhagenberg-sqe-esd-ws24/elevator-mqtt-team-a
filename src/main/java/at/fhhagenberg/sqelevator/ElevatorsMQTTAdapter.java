package at.fhhagenberg.sqelevator;

import java.rmi.Naming;
import java.rmi.RemoteException;
import java.io.FileInputStream;
import java.util.function.BiConsumer;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

import sqelevator.IElevator;

import java.util.concurrent.CompletableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.io.InputStream;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * ElevatorsMQTTAdapter which takes data from the PLC and publishes it over MQTT
 */
public class ElevatorsMQTTAdapter extends BaseMQTT {

  private static Logger logger = LogManager.getLogger(ElevatorsMQTTAdapter.class);
  
  private IElevator controller;
  private Building building;
  private Mqtt5AsyncClient mqttClient;
  private int pollingIntervall;

  @FunctionalInterface
  public interface MessageHandler {
    void handleMessage(String topic, String message);
  }

  /**
   * CTOR
   */
  public ElevatorsMQTTAdapter(IElevator controller, Mqtt5AsyncClient usedMqttClient, int pollingIntervall) {
    super(usedMqttClient);
    this.controller = controller;
    this.pollingIntervall = pollingIntervall;

    try {
      // fetch number of elevators and publish to subscribers
      int elevatorCnt = controller.getElevatorNum();
      this.publishRetainedMQTT(TOPIC_BUILDING_NR_ELEVATORS, elevatorCnt);

      // fetch capacities of elevators and publish to subscribers
      List<Integer> elevatorCapacitys = new ArrayList<>(elevatorCnt);
      for (int i = 0; i < elevatorCnt; i++) {
        int capacity = controller.getElevatorCapacity(i);
        elevatorCapacitys.add(capacity);
        this.publishRetainedMQTT(
            TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + i + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_CAPACITY, capacity);
      }

      // fetch number of floors and publish to subscribers
      int floorNumber = controller.getFloorNum();
      this.building = new Building(elevatorCnt, floorNumber, elevatorCapacitys);
      this.publishRetainedMQTT(TOPIC_BUILDING_NR_FLOORS, floorNumber);

      // subscribe to the current state publish request
      this.subscribeMQTT(TOPIC_BUILDING_PUBLISH_CURRENT_STATE + TOPIC_SEP + "request", (topic, message) -> {
        if (message.equals("needUpdate")) {
          publishCurrentState();
          this.publishMQTT(TOPIC_BUILDING_PUBLISH_CURRENT_STATE + TOPIC_SEP + "response", "done");
        }
      });

      // subscribe SetTarget
      this.building.getElevators().forEach((elevator) -> {
        this.subscribeMQTT(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevator.getElevatorNumber() + TOPIC_SEP
            + SUBTOPIC_ELEVATORS_ELEVATOR_SETTARGET, (topic, message) -> {
              try {
                logger.info("Set Target: {}", message);
                this.controller.setTarget(elevator.getElevatorNumber(), Integer.parseInt(message));
              } catch (Exception e) {
                logger.error(e.toString());
              }
            });
      });

      // subscribe SetCommittedDirection
      this.building.getElevators().forEach((elevator) -> {
        this.subscribeMQTT(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevator.getElevatorNumber() + TOPIC_SEP
            + SUBTOPIC_ELEVATORS_ELEVATOR_SETCOMMITTEDDIRECTION, (topic, message) -> {
              try {
                this.controller.setCommittedDirection(elevator.getElevatorNumber(), Integer.parseInt(message));
              } catch (Exception e) {
                logger.error(e.toString());
              }
            });
      });
    } catch (Exception e) {
      logger.error(e.toString());
    }
  }

  /**
   * Main function which polls data and publishes over MQTT
   * 
   * @param args arguments passed to main function
   */
  public static void main(String[] args) {
    ElevatorsMQTTAdapter client = null;
    try {

      Properties appProps = new Properties();
      try (InputStream inputStream = Thread.currentThread().getContextClassLoader()
          .getResourceAsStream("Elevators.properties")) {
        if (inputStream == null) {
          throw new IllegalArgumentException("Elevators.properties not found in resources");
        }
        appProps.load(inputStream);
      }

      IElevator controller = (IElevator) Naming.lookup(appProps.getProperty("IElevatorRMI"));

      // Create an MQTT client
      Mqtt5AsyncClient mqttClient = MqttClient.builder()
          .automaticReconnectWithDefaultConfig()
          .useMqttVersion5()
          .identifier(appProps.getProperty("MqttIdentifier") + "_adapter")
          .serverHost(appProps.getProperty("MqttHost")) // Public HiveMQ broker
          .serverPort(Integer.parseInt(appProps.getProperty("MqttPort"))) // Default MQTT port
          .buildAsync();

      client = new ElevatorsMQTTAdapter(controller, mqttClient,
          Integer.parseInt(appProps.getProperty("PollingIntervall")));

      client.run();

    } catch (InterruptedException e) {
      if (client != null) {
        client.closeConnection();
      }
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Runner
   */
  private void run() throws InterruptedException {
    // Loop Forever
    while (true) {
      this.updateState();
      try {
        Thread.sleep(this.pollingIntervall);
      } catch (InterruptedException e) {
        logger.info("Thread was interrupted");
        throw e;
      }
    }
  }

  private void publishCurrentState() {
    for (int elevNr = 0; elevNr < this.building.getNrElevators(); elevNr++) {
      publishMQTT(
          TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORDIRECTION,
          this.building.getElevator(elevNr).getDirection());
      publishMQTT(
          TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORDOORSTATUS,
          this.building.getElevator(elevNr).getDoorStatus());
      publishMQTT(
          TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORTARGETFLOOR,
          this.building.getElevator(elevNr).getTargetFloor());
      publishMQTT(
          TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORCURRENTFLOOR,
          this.building.getElevator(elevNr).getCurrentFloor());
      publishMQTT(
          TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORACCELERATION,
          this.building.getElevator(elevNr).getAcceleration());
      publishMQTT(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORSPEED,
          this.building.getElevator(elevNr).getSpeed());

      for (int floorNr = 0; floorNr < this.building.getNrFloors(); floorNr++) {
        publishMQTT(
            TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_FLOORREQUESTED +
                TOPIC_SEP + floorNr,
            this.building.getElevator(elevNr).getFloorRequested(floorNr));
        publishMQTT(
            TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_FLOORSERVICED +
                TOPIC_SEP + floorNr,
            this.building.getElevator(elevNr).getFloorToService(floorNr));
      }

      publishMQTT(
          TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORCURRENTHEIGHT,
          this.building.getElevator(elevNr).getCurrentHeight());
      publishMQTT(
          TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP
              + SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORCURRENTPASSENGERWEIGHT,
          this.building.getElevator(elevNr).getCurrentPassengersWeight());
    }

    for (int floorNr = 0; floorNr < this.building.getNrFloors(); floorNr++) {
      publishMQTT(TOPIC_BUILDING_FLOORS + TOPIC_SEP + floorNr + TOPIC_SEP + SUBTOPIC_FLOORS_BUTTONUPPRESSED,
          this.building.getUpButtonState(floorNr));
      publishMQTT(TOPIC_BUILDING_FLOORS + TOPIC_SEP + floorNr + TOPIC_SEP + SUBTOPIC_FLOORS_BUTTONDOWNPRESSED,
          this.building.getDownButtonState(floorNr));
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
        publishMQTT(TOPIC_BUILDING_FLOORS + TOPIC_SEP + floornr + TOPIC_SEP + SUBTOPIC_FLOORS_BUTTONUPPRESSED,
            floorUpButton);
      }

      boolean floorDownButton = this.controller.getFloorButtonDown(floornr);
      if (this.building.getDownButtonState(floornr) != floorUpButton) {
        this.building.updateDownButtonState(floornr, floorUpButton);
        // Publish over MQTT
        publishMQTT(TOPIC_BUILDING_FLOORS + TOPIC_SEP + floornr + TOPIC_SEP + SUBTOPIC_FLOORS_BUTTONDOWNPRESSED,
            floorDownButton);
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
      logger.info("Polling Elevator Nr. {}", elevnr);
      pollAndUpdateElevator(elevnr);
    }

    // update everything that is specific to a floor
    try {
      pollAndExecuteForFloorButtons();
    } catch (Exception e) {
      logger.info(e.toString());
    }
  }

  /**
   * Polls a value from the PLC and executes a function if there is a difference
   * 
   * throws RemoteException is needed because the given function can throw it
   * 
   * @param param1   Value from the Building
   * @param param2   Value from the PLC
   * @param function Function to execute if there is a difference
   * @param elevnr   Elevator Number
   * @param <T>      Type of the value
   */
  private <T> void pollAndExecute(T param1, T param2, BiConsumer<Integer, T> function, int elevnr,
      String mqttTopicForPublish) throws RemoteException {
    if (!param1.equals(param2)) {
      function.accept(elevnr, param2);
      // Publish over MQTT
      this.publishMQTT(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevnr + TOPIC_SEP + mqttTopicForPublish, param2);

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
      if (!this.building.getElevator(elevnr).getFloorRequested(floornr).equals(remoteFloorRequested)) {
        this.building.updateElevatorFloorRequested(elevnr, floornr, remoteFloorRequested);
        // Publish over MQTT
        publishMQTT(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevnr + TOPIC_SEP
            + SUBTOPIC_ELEVATORS_ELEVATOR_FLOORREQUESTED + TOPIC_SEP + floornr, remoteFloorRequested);
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
      if (!this.building.getElevator(elevnr).getFloorToService(floornr).equals(remoteFloorServiced)) {
        this.building.updateElevatorFloorRequested(elevnr, floornr, remoteFloorServiced);
        // Publish over MQTT
        publishMQTT(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevnr + TOPIC_SEP
            + SUBTOPIC_ELEVATORS_ELEVATOR_FLOORSERVICED + TOPIC_SEP + floornr, remoteFloorServiced);
      }
    }
  }

  /**
   * Polls an Elevator from the PLC and updates the Building
   */
  private void pollAndUpdateElevator(int elevnr) {

    try {

      pollAndExecute(this.building.getElevator(elevnr).getDirection(), this.controller.getCommittedDirection(elevnr),
          this.building::updateElevatorDirection, elevnr, SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORDIRECTION);
      pollAndExecute(this.building.getElevator(elevnr).getDoorStatus(), this.controller.getElevatorDoorStatus(elevnr),
          this.building::updateElevatorDoorStatus, elevnr, SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORDOORSTATUS);
      pollAndExecute(this.building.getElevator(elevnr).getTargetFloor(), this.controller.getTarget(elevnr),
          this.building::updateElevatorTargetFloor, elevnr, SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORTARGETFLOOR);
      pollAndExecute(this.building.getElevator(elevnr).getCurrentFloor(), this.controller.getElevatorFloor(elevnr),
          this.building::updateElevatorCurrentFloor, elevnr, SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORCURRENTFLOOR);
      pollAndExecute(this.building.getElevator(elevnr).getAcceleration(), this.controller.getElevatorAccel(elevnr),
          this.building::updateElevatorAcceleration, elevnr, SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORACCELERATION);
      pollAndExecute(this.building.getElevator(elevnr).getSpeed(), this.controller.getElevatorSpeed(elevnr),
          this.building::updateElevatorSpeed, elevnr, SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORSPEED);

      pollAndExecuteFloorsRequested(elevnr);
      pollAndExecuteFloorsServiced(elevnr);

      pollAndExecute(this.building.getElevator(elevnr).getCurrentHeight(), this.controller.getElevatorPosition(elevnr),
          this.building::updateElevatorCurrentHeight, elevnr, SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORCURRENTHEIGHT);
      pollAndExecute(this.building.getElevator(elevnr).getCurrentPassengersWeight(),
          this.controller.getElevatorWeight(elevnr), this.building::updateElevatorCurrentPassengersWeight, elevnr,
          SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORCURRENTPASSENGERWEIGHT);

    } catch (Exception e) {
      logger.error(e.toString());
    }
  }
}
