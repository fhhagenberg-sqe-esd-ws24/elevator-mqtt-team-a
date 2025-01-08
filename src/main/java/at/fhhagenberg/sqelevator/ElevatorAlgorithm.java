package at.fhhagenberg.sqelevator;

import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

public class ElevatorAlgorithm extends BaseMQTT {

  private static Logger logger = LogManager.getLogger(ElevatorAlgorithm.class);

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

  public static final int AVG_PASSENGER_WEIGHT = 135;

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
      logger.error("{}", e.toString());
      Thread.currentThread().interrupt();
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

  protected void askForCurrentState() {
    publishMQTT(TOPIC_BUILDING_PUBLISH_CURRENT_STATE + TOPIC_SEP + "request", "needUpdate");
  }

  protected void subscribeToInitials() {
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
                logger.error("Error subscribing to TOPIC_BUILDING_ELEVATORS: {}", e.toString());
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

    } catch (InterruptedException e) {
      logger.info("Interrupted!");
      cleanup();
      Thread.currentThread().interrupt();
    } catch (Exception e) {
      logger.error("Error in subscribeToInitials: {}", e.toString());
    }
  }

  private void subscribeToVariables() {
    try {

      for (int floorNr = 0; floorNr < this.mNrOfFloors; floorNr++) {
        // subscribe to Up buttons
        this.subscribeMQTT(TOPIC_BUILDING_FLOORS + TOPIC_SEP + floorNr + TOPIC_SEP + SUBTOPIC_FLOORS_BUTTONUPPRESSED,
            this::updateTopicWrapped);
        // subscribe to Down buttons
        this.subscribeMQTT(TOPIC_BUILDING_FLOORS + TOPIC_SEP + floorNr + TOPIC_SEP + SUBTOPIC_FLOORS_BUTTONDOWNPRESSED,
            this::updateTopicWrapped);

        for (int elevNr = 0; elevNr < this.mNrOfElevators; elevNr++) {
          // subscribe to Floors Serviced
          this.subscribeMQTT(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP
              + SUBTOPIC_ELEVATORS_ELEVATOR_FLOORSERVICED + TOPIC_SEP + floorNr, this::updateTopicWrapped);
          // subscribe to Floors Requested
          this.subscribeMQTT(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP
              + SUBTOPIC_ELEVATORS_ELEVATOR_FLOORREQUESTED + TOPIC_SEP + floorNr, this::updateTopicWrapped);
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
      logger.error("{}", e.toString());
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
        logger.error("{}", e.toString());
      }
    });
  }

  private void updateTopicWrapped(String topic, String message) {
    try {
      updateTopic(topic, message);
    } catch (Exception e) {
      logger.error("{}", e.toString());
    }
  }

  private void updateTopic(String topic, String message) {
    synchronized (this) {
      logger.info("Topic: {}, Message: {}", topic, message);
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
   * This contains knut's elevator algorithm.
   */
  protected void doAlgorithm() {
    Building currentStatus = new Building(this.mBuilding);
    List<Integer> alreadyServedFloor = new ArrayList<>();

    // Iterate through all elevators
    for (int elevNr = 0; elevNr < mNrOfElevators; elevNr++) {
      ElevatorDataModell elevator = currentStatus.getElevator(elevNr);
      int currentFloor = elevator.getCurrentFloor();
      int direction = elevator.getDirection();
      int newTargetFloor = currentFloor;

      // check if doors are open (else break)
      if (currentStatus.getElevator(elevNr).getDoorStatus() != ELEVATOR_DOORS_OPEN) {
        continue;
      }

      if (direction == ELEVATOR_DIRECTION_UNCOMMITTED) {
        newTargetFloor = handleUncommittedDirection(currentStatus, elevNr, currentFloor, alreadyServedFloor);
      }
      else {
        // Check requests in the current direction
        newTargetFloor = handleCurrentDirection(currentStatus, elevNr, currentFloor, direction, alreadyServedFloor);

        // If no requests in the current direction, reverse direction or idle
        if (newTargetFloor == currentFloor) {
          newTargetFloor = handleReverseOrIdle(currentStatus, elevNr, currentFloor, direction, alreadyServedFloor);
        }
      }

      // Add the target floor to the served list if valid
      if (newTargetFloor != -1 && newTargetFloor != currentFloor) {
        alreadyServedFloor.add(newTargetFloor);
      }
    }
  }

  /**
   * 
   * @param building
   * @param elevNr
   * @param currentFloor
   */
  private int handleUncommittedDirection(Building building, int elevNr, int currentFloor, List<Integer> alreadyServedFloors) {
    // Check for the nearest request (up or down)
    int nearestRequest = findNearestRequest(building, elevNr, currentFloor, alreadyServedFloors);
    if (nearestRequest != -1) {
      int dir = nearestRequest > currentFloor ? ELEVATOR_DIRECTION_UP : ELEVATOR_DIRECTION_DOWN;
      logger.info("Nearest Request: {}", nearestRequest);
      publishMQTT(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP
          + SUBTOPIC_ELEVATORS_ELEVATOR_SETCOMMITTEDDIRECTION, dir);
      publishMQTT(TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_SETTARGET,
          nearestRequest);
    }
    return nearestRequest;
  }

  /**
   * 
   * @param building
   * @param elevNr
   * @param currentFloor
   * @param direction
   * @return
   */
  private int handleCurrentDirection(Building building, int elevNr, int currentFloor, int direction, List<Integer> alreadyServedFloors) {
    int newTargetFloor = currentFloor;
    int startFloor = direction == ELEVATOR_DIRECTION_UP ? currentFloor + 1 : currentFloor - 1;
    int endFloor = direction == ELEVATOR_DIRECTION_UP ? mNrOfFloors : -1;
    int step = direction == ELEVATOR_DIRECTION_UP ? 1 : -1;

    for (int floor = startFloor; floor != endFloor; floor += step) {
      // skip already served floors
      // check if current floor needs servicing
      if (shouldServiceFloor(building, elevNr, floor) && !alreadyServedFloors.contains(floor)) {
        publishMQTT(
            TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_SETTARGET,
            floor
        );
        newTargetFloor = floor;
        break; // Exit loop once a target is found
      }
    }
    return newTargetFloor;
  }

  /**
   * 
   * @param building
   * @param elevNr
   * @param currentFloor
   * @param direction
   */
  private int handleReverseOrIdle(Building building, int elevNr, int currentFloor, int direction, List<Integer> alreadyServedFloors) {
    int newTargetFloor = currentFloor;
    int startFloor = direction == ELEVATOR_DIRECTION_UP ? currentFloor - 1 : currentFloor + 1;
    int endFloor = direction == ELEVATOR_DIRECTION_UP ? -1 : mNrOfFloors;
    int step = direction == ELEVATOR_DIRECTION_UP ? -1 : 1;

    for (int floor = startFloor; floor != endFloor; floor += step) {
      // skip already served floors
      // check if current floor needs servicing
      if (shouldServiceFloor(building, elevNr, floor) && !alreadyServedFloors.contains(floor)) {
        int newDirection = direction == ELEVATOR_DIRECTION_UP ? ELEVATOR_DIRECTION_DOWN : ELEVATOR_DIRECTION_UP;
        publishMQTT(
            TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_SETCOMMITTEDDIRECTION,
            newDirection
        );
        publishMQTT(
            TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_SETTARGET,
            floor
        );
        newTargetFloor = floor;
        break;
      }
    }

    if (newTargetFloor == currentFloor) {
        publishMQTT(
            TOPIC_BUILDING_ELEVATORS + TOPIC_SEP + elevNr + TOPIC_SEP + SUBTOPIC_ELEVATORS_ELEVATOR_SETCOMMITTEDDIRECTION,
            ELEVATOR_DIRECTION_UNCOMMITTED
        );
    }
    return newTargetFloor;
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

    int currentWeight = building.getElevator(elevNr).getCurrentPassengersWeight();
    int maxWeight = building.getElevator(elevNr).getMaxPassengers();
    boolean isFull = (currentWeight / AVG_PASSENGER_WEIGHT) > maxWeight;
    boolean floorRequestedAllowed = (!isFull && (floorUpRequested || floorDownRequested));

    // Check if the elevator is assigned to service this floor
    boolean elevatorServicesFloor = building.getElevator(elevNr).getFloorToService(floor);

    return elevatorServicesFloor && (floorRequestedAllowed || floorRequestedByPassengers);
  }

  /**
   * Finds the nearest floor with a request for an idle elevator.
   *
   * @param building     The building instance
   * @param elevator     The elevator instance
   * @param currentFloor The elevator's current floor
   * @return The nearest floor with a request, or -1 if no requests exist
   */
  protected int findNearestRequest(Building building, int elevNr, int currentFloor, List<Integer> alreadyServedFloors) {
    int nearestFloor = -1;
    int minDistance = Integer.MAX_VALUE;

    for (int floor = 0; floor < building.getNrFloors(); floor++) {
      // skip already served floors
      if (alreadyServedFloors.contains(floor)) {
        continue;
      };
      // check if current floor needs servicing
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
}
