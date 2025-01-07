package at.fhhagenberg.sqelevator;

import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

import java.util.function.BiConsumer;
import java.util.concurrent.CompletableFuture;

public class BaseMQTT {

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
          System.out.println("Connected successfully!");
        })
        .exceptionally(throwable -> {
          System.err.println("Connection failed: " + throwable.getMessage());
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
  public <T> void publishMQTT(String topic, T data, boolean retain) {
    if (mqttClient.getState() != MqttClientState.CONNECTED) {
      throw new IllegalStateException("Client not connected to Broker!");
    }

    mqttClient.publishWith()
        .topic(topic)
        .payload(data.toString().getBytes())
        .qos(MqttQos.AT_LEAST_ONCE)
        .retain(retain)
        .send()
        .thenAccept(pubAck -> System.out.println("Published to topic: " + topic + " with message: " + data))
        .exceptionally(throwable -> {
          System.err.println("Failed to publish to topic: " + topic + " - " + throwable.getMessage());
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
    publishMQTT(topic, data, false);
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
            System.err.println("Failed to subscribe to topic: " + topic + " - " + throwable.getMessage());
          } else {
            System.out.println("Subscribed successfully to topic: " + topic);
          }
        });
  }

  /**
   * Disconnects the MQTT client during cleanup.
   */
  protected void finalize() {
    try {
      mqttClient.disconnect();
      System.out.println("MQTT client disconnected.");
    } catch (Exception e) {
      System.err.println("Error during MQTT disconnect: " + e.getMessage());
    }
  }
}
