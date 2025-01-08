package at.fhhagenberg.sqelevator;

import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;

import java.util.function.BiConsumer;
import java.util.concurrent.CompletableFuture;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;

/**
 * BaseMQTT class to handle the MQTT connection and publish/subscribe to topics
 */
public class BaseMQTT {

  private static Logger logger = LogManager.getLogger(BaseMQTT.class);

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

  protected final Mqtt5AsyncClient mqttClient;

  /**
   * Constructor for BaseMQTT.
   *
   * @param mqttClient The MQTT client to use for publishing and subscribing
   */
  public BaseMQTT(Mqtt5AsyncClient mqttClient) {
    this.mqttClient = mqttClient;

    // Connect to the broker
    CompletableFuture<Void> connectFuture = mqttClient.connect()
        .thenAccept(connAck -> {
          logger.info("Connected successfully!");
        })
        .exceptionally(throwable -> {
          logger.error("Connection failed: {}", throwable.getMessage());
          return null;
        });

    // Wait for the connection to complete
    connectFuture.join();
  }

  /**
   * Publishes a message to a specific topic.
   *
   * @param topic  The topic to publish to
   * @param data   The message payload
   * @param retain Whether the message should be retained
   */
  public <T> void publishMQTTHelper(String topic, T data, boolean retain) {

    logger.info("Publishing \"{}: {}\"", topic, data);

    if (mqttClient.getState() != MqttClientState.CONNECTED) {
      throw new IllegalStateException("Client not connected to Broker!");
    }

    Mqtt5Publish publishMessage = Mqtt5Publish.builder()
        .topic(topic)
        .payload(data.toString().getBytes())
        .qos(MqttQos.AT_LEAST_ONCE)
        .retain(retain)
        .build();

    mqttClient.publish(publishMessage)
        .thenAccept(pubAck -> logger.info("Published message: {} to topic: {}", data, topic))
        .exceptionally(throwable -> {
          logger.error("Failed to publish: {}", throwable.getMessage());
          return null;
        });
  }

  /**
   * Overloaded method to publish a non-retained message.
   *
   * @param topic The topic to publish to
   * @param data  The message payload
   */
  public <T> void publishMQTT(String topic, T data) {
    publishMQTTHelper(topic, data, false);
  }

  /**
   * Publish updates over MQTT for a specific Elevator, if there
   * are changes
   * 
   * @param topic contains the topic string
   * @param T     data for the topic
   */
  public <T> void publishRetainedMQTT(String topic, T data) {

    this.publishMQTTHelper(topic, data, true);
  }

  /**
   * Subscribes to a specific topic with a provided message handler.
   *
   * @param topic          The topic to subscribe to
   * @param messageHandler A BiConsumer that processes the topic and message
   */
  public void subscribeMQTT(String topic, BiConsumer<String, String> messageHandler) {
    mqttClient.subscribeWith()
        .topicFilter(topic)
        .qos(MqttQos.AT_LEAST_ONCE)
        .callback(publish -> {
          String message = new String(publish.getPayloadAsBytes());
          messageHandler.accept(publish.getTopic().toString(), message);
        })
        .send()
        .whenComplete((subAck, throwable) -> {
          if (throwable != null) {
            logger.error("Failed to subscribe: {}", throwable.getMessage());
          } else {
            logger.info("Subscribed successfully to topic: {}", topic);
          }
        }).join();
  }

  /**
   * Closes the connection to the MQTT broker.
   */
  public void closeConnection() {
    try {
      if (mqttClient.getState() == MqttClientState.CONNECTED) {
        mqttClient.disconnect().thenRun(() -> {
          logger.info("Disconnected from MQTT broker.");
        }).exceptionally(throwable -> {
          logger.error("Failed to disconnect: {}", throwable.getMessage());
          return null;
        });
      }
    } catch (Exception e) {
      logger.error("Error while closing MQTT connection: {}", e.getMessage());
    }
  }

  /**
   * Disconnects the MQTT client during cleanup.
   */
  protected void cleanup() {
    try {
      mqttClient.disconnect();
    } catch (Exception e) {
      logger.error(e.toString());
    }
  }
}
