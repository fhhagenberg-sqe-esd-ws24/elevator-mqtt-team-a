package at.fhhagenberg.sqelevator;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

import at.fhhagenberg.sqelevator.ElevatorsMQTTAdapter.MessageHandler;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class ElevatorAlgorithm extends BaseMQTT {

  private static Logger logger = LogManager.getLogger(ElevatorAlgorithm.class);

  // all Topics starting with TOPIC_ are finished topics
  // all Topics starting with SUBTOPIC_ are subtopics and need to be appended to
  // the correct finished topic
  public static final String TOPIC_SEP = "/";
         
  public static final String TOPIC_BUILDING = "buildings";
  public static final String TOPIC_BUILDING_ID = "0";
         
  public static final String TOPIC_BUILDING_ELEVATORS = TOPIC_BUILDING + TOPIC_SEP + TOPIC_BUILDING_ID + TOPIC_SEP
      + "elevators";
  public static final String TOPIC_BUILDING_FLOORS = TOPIC_BUILDING + TOPIC_SEP + TOPIC_BUILDING_ID + TOPIC_SEP
      + "floors";
  public static final String TOPIC_BUILDING_NR_ELEVATORS = TOPIC_BUILDING + TOPIC_SEP + TOPIC_BUILDING_ID + TOPIC_SEP
      + "NrElevators";
  public static final String TOPIC_BUILDING_NR_FLOORS = TOPIC_BUILDING + TOPIC_SEP + TOPIC_BUILDING_ID + TOPIC_SEP
      + "NrFloors";

  public static final String SUBTOPIC_ELEVATORS_ELEVATOR_CAPACITY = "ElevatorCapacity";
  public static final String SUBTOPIC_ELEVATORS_ELEVATOR_SETTARGET = "SetTarget";
  public static final String SUBTOPIC_ELEVATORS_ELEVATOR_SETCOMMITTEDDIRECTION = "SetCommittedDirection";
  public static final String SUBTOPIC_ELEVATORS_ELEVATOR_FLOORREQUESTED = "FloorRequested";
  public static final String SUBTOPIC_ELEVATORS_ELEVATOR_FLOORSERVICED = "FloorServiced";
  public static final String SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORDIRECTION = "ElevatorDirection";
  public static final String SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORDOORSTATUS = "ElevatorDoorStatus";
  public static final String SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORTARGETFLOOR = "ElevatorTargetFloor";
  public static final String SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORCURRENTFLOOR = "ElevatorCurrentFloor";
  public static final String SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORACCELERATION = "ElevatorAcceleration";
  public static final String SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORSPEED = "ElevatorSpeed";
  public static final String SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORCURRENTHEIGHT = "ElevatorCurrentHeight";
  public static final String SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORCURRENTPASSENGERWEIGHT = "ElevatorCurrentPassengersWeight";

  public static final String SUBTOPIC_FLOORS_BUTTONDOWNPRESSED = "ButtonDownPressed";
  public static final String SUBTOPIC_FLOORS_BUTTONUPPRESSED = "ButtonUpPressed";

  public static final String TOPIC_BUILDING_PUBLISH_CURRENT_STATE = TOPIC_BUILDING + TOPIC_SEP + TOPIC_BUILDING_ID
      + TOPIC_SEP + "PublishCurrentState";

  /** State variable for elevator doors open. */
  public static final int ELEVATOR_DOORS_OPEN = 1;
  /** State variable for elevator doors closed. */
  public static final int ELEVATOR_DOORS_CLOSED = 2;
  /** State variable for elevator doors opening. */
  public static final int ELEVATOR_DOORS_OPENING = 3;
  /** State variable for elevator doors closing. */
  public static final int ELEVATOR_DOORS_CLOSING = 4;

  /** State variable for elevator status when going up */
  public static final int ELEVATOR_DIRECTION_UP = 0;
  /** State variable for elevator status when going down. */
  public static final int ELEVATOR_DIRECTION_DOWN = 1;
  /** State variables for elevator status stopped and uncommitted. */
  public static final int ELEVATOR_DIRECTION_UNCOMMITTED = 2;

  private Mqtt5AsyncClient mqttClient;

  // default information about the elevator system
  private int mNrOfFloors = 0;
  private int mNrOfElevators = 0;
  private ArrayList<Integer> mElevatorCapacitys;
  AtomicBoolean mInitialized = new AtomicBoolean(false);

  protected Building mBuilding;

  /**
   * CTOR
   */
  public ElevatorAlgorithm(Mqtt5AsyncClient mqttClient) {
    super(mqttClient);
  }

  public static void main(String[] args) {

    try {
      String rootPath = Thread.currentThread().getContextClassLoader().getResource("").getPath();
      String appConfigPath = rootPath + "Elevators.properties";

      Properties appProps = new Properties();
      appProps.load(new FileInputStream(appConfigPath));

      // Create an MQTT client
      Mqtt5AsyncClient mqttClient = MqttClient.builder()
          .automaticReconnectWithDefaultConfig()
          .useMqttVersion5()
          .identifier(appProps.getProperty("MqttIdentifier") + "_algorithm")
          .serverHost(appProps.getProperty("MqttHost")) // Public HiveMQ broker
          .serverPort(Integer.parseInt(appProps.getProperty("MqttPort"))) // Default MQTT port
          .buildAsync();

      ElevatorAlgorithm client = new ElevatorAlgorithm(mqttClient);

      client.run();

    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  /**
   * Main loop
   */
  private void run() {
    // get initial information for building
    subscribeToInitials();

    // subscribe to the variables that can change during operation
    subscribeToVariables();

    // subscribe for the current state topic
    this.subscribeMQTT(TOPIC_BUILDING_PUBLISH_CURRENT_STATE + TOPIC_SEP + "response", (topic, message) -> {
      if (message.equals("done")) {
        mInitialized.set(true);
      }
    });

    // ask all buildings to publish the current state
    askForCurrentState();

    // wait for all parameters to be set
    while (!mInitialized.get())
      ;

    try {
      while (true) {
        doAlgorithm();
        Thread.sleep(1000);
      }
    } catch (InterruptedException e) {
      logger.info("Interrupted!");
      cleanup();
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      logger.error("Error in main loop: {}", e.toString());
      cleanup();
    }
  }

  private void askForCurrentState() {
    publishMQTT(TOPIC_BUILDING_PUBLISH_CURRENT_STATE + TOPIC_SEP + "request", "needUpdate");
  }

  private void subscribeToInitials() {
    try {
      // Subscribe to elevator count
      CountDownLatch latchElevaCnt = new CountDownLatch(1);
      this.subscribeMQTT(TOPIC_BUILDING_NR_ELEVATORS, (topic, message) -> {
        try {
          mNrOfElevators = Integer.parseInt(message);
          latchElevaCnt.countDown();
        } catch (Exception e) {
          logger.error("Error subscribing to TOPIC_BUILDING_NR_ELEVATORS: {}", e.toString());
        }
      });

      latchElevaCnt.await();

      // subscribe to elevator capacity
      mElevatorCapacitys = new ArrayList<>(mNrOfElevators);
      // Pre-fill the list with default values
      for (int i = 0; i < mNrOfElevators; i++) {
        mElevatorCapacitys.add(0);
      }

      CountDownLatch latchElevCap = new CountDownLatch(mNrOfElevators);
      for (int i = 0; i < mNrOfElevators; i++) {
        this.subscribeMQTT(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + i + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_CAPACITY,
            (topic, message) -> {
              try {
                String splittedTopic = topic.split(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP)[1];
                int elevNr = Integer.parseInt(splittedTopic.split(TOPIC_SEP)[0]);
                mElevatorCapacitys.set(elevNr, Integer.parseInt(message));
                latchElevCap.countDown();
              } catch (Exception e) {
                logger.error("Error subscribing to TOPIC_BUILDING_ELEVATORS: {}",e.toString());
              }
            });
      }

      latchElevCap.await();

      // subscribe to floor number
      CountDownLatch latchFloorNr = new CountDownLatch(1);
      this.subscribeMQTT(TOPIC_BUILDING_NR_FLOORS, (topic, message) -> {
        try {
          mNrOfFloors = Integer.parseInt(message);
          latchFloorNr.countDown();
        } catch (Exception e) {
          logger.error("Error subscribing to TOPIC_BUILDING_NR_FLOORS: {}", e.toString());
        }
      });

      latchFloorNr.await();
      this.mBuilding = new Building(mNrOfElevators, mNrOfFloors, mElevatorCapacitys);

    }  catch (InterruptedException e) {
      logger.info("Interrupted!");
      cleanup();
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      logger.info("Error in subscribeToInitials: {}", e);
    }
  }

  private void subscribeToVariables() {
    try {

      // subscribe to Up buttons
      for (int floorNr = 0; floorNr < this.mNrOfFloors; floorNr++) {
        this.subscribeMQTT(TOPIC_BUILDING_FLOORS + TOPIC_SEP + floorNr + TOPIC_SEP + SUBTOPIC_FLOORS_BUTTONUPPRESSED,
            (topic, message) -> {
              try {
                updateTopic(topic, message);
              } catch (Exception e) {
                logger.error("{}",e.toString());
              }
            });
      }

      // subscribe to Down buttons
      for (int floorNr = 0; floorNr < this.mNrOfFloors; floorNr++) {
        this.subscribeMQTT(TOPIC_BUILDING_FLOORS + TOPIC_SEP + floorNr + TOPIC_SEP + SUBTOPIC_FLOORS_BUTTONDOWNPRESSED,
            (topic, message) -> {
              try {
                updateTopic(topic, message);
              } catch (Exception e) {
                logger.error("{}",e.toString());
              }
            });
      }

      // subscribe to Floors Requested
      for (int elevNr = 0; elevNr < this.mNrOfElevators; elevNr++) {
        for (int floorNr = 0; floorNr < this.mNrOfFloors; floorNr++) {
          this.subscribeMQTT(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP
              + SUBTOPIC_ELEVATORS_ELEVATOR_FLOORREQUESTED + TOPIC_SEP + floorNr, (topic, message) -> {
                try {
                  updateTopic(topic, message);
                } catch (Exception e) {
                  logger.error("{}",e.toString());
                }
              });
        }
      }

      // subscribe to Floors Serviced
      for (int elevNr = 0; elevNr < this.mNrOfElevators; elevNr++) {
        for (int floorNr = 0; floorNr < this.mNrOfFloors; floorNr++) {
          this.subscribeMQTT(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP
              + SUBTOPIC_ELEVATORS_ELEVATOR_FLOORSERVICED + TOPIC_SEP + floorNr, (topic, message) -> {
                try {
                  updateTopic(topic, message);
                } catch (Exception e) {
                  logger.error("{}",e.toString());
                }
              });
        }
      }

      // subscribe to Committed Direction
      subscribeAndSetCallbackForAll(SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORDIRECTION, this::updateTopic);

      // subscribe to Door Status
      subscribeAndSetCallbackForAll(SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORDOORSTATUS, this::updateTopic);

      // subscribe to Target Floor
      subscribeAndSetCallbackForAll(SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORTARGETFLOOR, this::updateTopic);

      // subscribe to Current Floor
      subscribeAndSetCallbackForAll(SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORCURRENTFLOOR, this::updateTopic);

      // subscribe to Elevator Acceleration
      subscribeAndSetCallbackForAll(SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORACCELERATION, this::updateTopic);

      // subscribe to Elevator Speed
      subscribeAndSetCallbackForAll(SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORSPEED, this::updateTopic);

      // subscribe to Elevator Passenger Weight
      subscribeAndSetCallbackForAll(SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORCURRENTPASSENGERWEIGHT, this::updateTopic);

    } catch (Exception e) {
      logger.error("{}",e.toString());
    }
  }

  private void subscribeAndSetCallbackForAll(String subTopic, BiConsumer<String, String> callback) {
    for (int elevNr = 0; elevNr < mNrOfElevators; elevNr++) {
      subscribeAndSetCallback(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + subTopic, callback);
    }
  }

  private void subscribeAndSetCallback(String subTopic, BiConsumer<String, String> callback) {
    // subscribe to Target Floor
    this.subscribeMQTT(subTopic, (topic, message) -> {
      try {
        // call callback
        callback.accept(topic, message);
      } catch (Exception e) {
        logger.error("{}",e.toString());
      }
    });
  }

  private void updateTopic(String topic, String message) {
    synchronized (this) {
      logger.info("Topic: {}, Message: {}",topic,message);
      if (topic.contains(TOPIC_BUILDING_FLOORS)) {
        String baseTopic = TOPIC_BUILDING_FLOORS + TOPIC_SEP;
        String topicWithoutBaseTopic = topic.substring(baseTopic.length(), topic.length());
        String[] splittedTopic = topicWithoutBaseTopic.split(TOPIC_SEP);

        // get floor number and sub topic
        int floorNr = Integer.parseInt(splittedTopic[0]);
        String subTopic = splittedTopic[1];

        // convert state to bool
        boolean state = Boolean.parseBoolean(message);

        switch (subTopic) {
          case SUBTOPIC_FLOORS_BUTTONUPPRESSED:
            mBuilding.updateUpButtonState(floorNr, state);
            break;
          case SUBTOPIC_FLOORS_BUTTONDOWNPRESSED:
            mBuilding.updateDownButtonState(floorNr, state);
            break;
          default:
            break;
        }
      } else {
        String baseTopic = TOPIC_BUILDING_ELEVATORS + TOPIC_SEP;
        String topicWithoutBaseTopic = topic.substring(baseTopic.length(), topic.length());
        String[] splittedTopic = topicWithoutBaseTopic.split(TOPIC_SEP);

        Integer elevNr = Integer.parseInt(splittedTopic[0]);
        String subTopic = splittedTopic[1];

        switch (subTopic) {
          case SUBTOPIC_ELEVATORS_ELEVATOR_FLOORREQUESTED:
            mBuilding.updateElevatorFloorRequested(elevNr, Integer.parseInt(splittedTopic[2]),
                Boolean.parseBoolean(message));
            break;
          case SUBTOPIC_ELEVATORS_ELEVATOR_FLOORSERVICED:
            mBuilding.updateElevatorFloorToService(elevNr, Integer.parseInt(splittedTopic[2]),
                Boolean.parseBoolean(message));
            break;
          case SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORDIRECTION:
            mBuilding.updateElevatorDirection(elevNr, Integer.parseInt(message));
            break;
          case SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORDOORSTATUS:
            mBuilding.updateElevatorDoorStatus(elevNr, Integer.parseInt(message));
            break;
          case SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORTARGETFLOOR:
            mBuilding.updateElevatorTargetFloor(elevNr, Integer.parseInt(message));
            break;
          case SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORCURRENTFLOOR:
            mBuilding.updateElevatorCurrentFloor(elevNr, Integer.parseInt(message));
            break;
          case SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORACCELERATION:
            mBuilding.updateElevatorAcceleration(elevNr, Integer.parseInt(message));
            break;
          case SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORSPEED:
            mBuilding.updateElevatorSpeed(elevNr, Integer.parseInt(message));
            break;
          case SUBTOPIC_ELEVATORS_ELEVATOR_ELEVATORCURRENTPASSENGERWEIGHT:
            mBuilding.updateElevatorCurrentPassengersWeight(elevNr, Integer.parseInt((message)));
            break;
          default:
          logger.error("Unsupported topic!");
        }
      }
    }
  }

  /**
   * 
   */
  protected void doAlgorithm() {
    Building currentStatus = new Building(this.mBuilding);

    // TODO: code without break and continue
    // https://sonarcloud.io/organizations/fhhagenberg-sqe-esd-ws24/rules?open=java%3AS135&rule_key=java%3AS135

    // do algorithm stuff

    // Step 1: Iterate through all elevators
    for (int elevNr = 0; elevNr < mNrOfElevators; elevNr++) {
      ElevatorDataModell elevator = mBuilding.getElevator(elevNr);
      int currentFloor = elevator.getCurrentFloor();
      int newTargetFloor = elevator.getCurrentFloor();
      int direction = elevator.getDirection();

      // check if doors are open (else break)
      if (currentStatus.getElevator(elevNr).getDoorStatus() != ELEVATOR_DOORS_OPEN) {
        continue;
      }

      if (direction == ELEVATOR_DIRECTION_UNCOMMITTED) {
        // Check for the nearest request (up or down)
        int nearestRequest = findNearestRequest(currentStatus, elevNr, currentFloor);
        if (nearestRequest != -1) {
          int dir = nearestRequest > currentFloor ? ELEVATOR_DIRECTION_UP : ELEVATOR_DIRECTION_DOWN;
          logger.info("Nearest Request: {}",  nearestRequest);
          publishMQTT(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP
              + SUBTOPIC_ELEVATORS_ELEVATOR_SETCOMMITTEDDIRECTION, dir);
          publishMQTT(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_SETTARGET,
              nearestRequest);
        }
        continue;
      }

      // Step 2: Check requests in the current direction
      if (direction == ELEVATOR_DIRECTION_UP) { // Moving up
        for (int floor = currentFloor + 1; floor < mNrOfFloors; floor++) {
          if (shouldServiceFloor(currentStatus, elevNr, floor)) {
            publishMQTT(
                TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_SETTARGET,
                floor);
            newTargetFloor = floor;
            break;
          }
        }
      } else if (direction == ELEVATOR_DIRECTION_DOWN) { // Moving down
        for (int floor = currentFloor - 1; floor >= 0; floor--) {
          if (shouldServiceFloor(currentStatus, elevNr, floor)) {
            publishMQTT(
                TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_SETTARGET,
                floor);
            newTargetFloor = floor;
            break;
          }
        }
      }

      // Step 3: If no requests in the current direction, reverse direction or idle
      if (newTargetFloor == currentFloor) {
        if (direction == ELEVATOR_DIRECTION_DOWN) { // Reverse to check downward floors
          for (int floor = currentFloor - 1; floor >= 0; floor--) {
            if (shouldServiceFloor(currentStatus, elevNr, floor)) {
              publishMQTT(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP
                  + SUBTOPIC_ELEVATORS_ELEVATOR_SETCOMMITTEDDIRECTION, ELEVATOR_DIRECTION_DOWN);
              publishMQTT(
                  TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_SETTARGET,
                  floor);
              newTargetFloor = floor;
              break;
            }
          }
        } else if (direction == ELEVATOR_DIRECTION_UP) { // Reverse to check upward floors
          for (int floor = currentFloor + 1; floor < mNrOfFloors; floor++) {
            if (shouldServiceFloor(currentStatus, elevNr, floor)) {
              publishMQTT(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP
                  + SUBTOPIC_ELEVATORS_ELEVATOR_SETCOMMITTEDDIRECTION, ELEVATOR_DIRECTION_UP);
              publishMQTT(
                  TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_SETTARGET,
                  floor);
              newTargetFloor = floor;
              break;
            }
          }
        }

        // If still no target, remain idle
        if (newTargetFloor == currentFloor) {
          publishMQTT(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP
              + SUBTOPIC_ELEVATORS_ELEVATOR_SETCOMMITTEDDIRECTION, ELEVATOR_DIRECTION_UNCOMMITTED);
        }
      }
    }
  }

  /**
   * Determines whether the elevator should service a specific floor.
   *
   * @param building The building instance
   * @param elevator The elevator instance
   * @param floor    The floor number to check
   * @return True if the floor should be serviced, otherwise false
   */
  protected boolean shouldServiceFloor(Building building, int elevNr, int floor) {
    // Check if the floor has a request
    boolean floorUpRequested = building.getUpButtonState(floor);
    boolean floorDownRequested = building.getDownButtonState(floor);
    boolean floorRequestedByPassengers = building.getElevator(elevNr).getFloorRequested(floor);

    // Check if the elevator is assigned to service this floor
    boolean elevatorServicesFloor = building.getElevator(elevNr).getFloorToService(floor);

    return elevatorServicesFloor && (floorUpRequested || floorDownRequested || floorRequestedByPassengers);
  }

  /**
   * Finds the nearest floor with a request for an idle elevator.
   *
   * @param building     The building instance
   * @param elevator     The elevator instance
   * @param currentFloor The elevator's current floor
   * @return The nearest floor with a request, or -1 if no requests exist
   */
  protected int findNearestRequest(Building building, int elevNr, int currentFloor) {
    int nearestFloor = -1;
    int minDistance = Integer.MAX_VALUE;

    for (int floor = 0; floor < mNrOfFloors; floor++) {
      if (shouldServiceFloor(building, elevNr, floor) && floor != currentFloor) {
        int distance = Math.abs(floor - currentFloor);
        if (distance < minDistance) {
          minDistance = distance;
          nearestFloor = floor;
        }
      }
    }

    return nearestFloor;
  }

  /**
   * kinda DTOR
   */
  protected void cleanup() {
    try {
      this.mqttClient.disconnect();
    } catch (Exception e) {
      logger.error("{}",e.toString());
    }
  }
}
