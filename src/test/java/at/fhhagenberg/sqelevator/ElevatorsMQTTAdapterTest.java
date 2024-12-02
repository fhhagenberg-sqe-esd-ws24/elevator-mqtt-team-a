package at.fhhagenberg.sqelevator;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

import java.rmi.RemoteException;
import java.util.stream.IntStream;
import java.util.Hashtable;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import com.hivemq.client.internal.mqtt.MqttBlockingClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5BlockingClient;
import com.hivemq.client.mqtt.MqttGlobalPublishFilter;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import static org.junit.jupiter.api.Assertions.*;
import java.util.Optional;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.hivemq.HiveMQContainer;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

@Testcontainers
@ExtendWith(MockitoExtension.class)
public class ElevatorsMQTTAdapterTest {



  @Mock
  private IElevator mockedIElevator;

  private Mqtt5AsyncClient asyncMqttClient;

  @Container
  public static HiveMQContainer hiveMQContainer = new HiveMQContainer(
      DockerImageName.parse("hivemq/hivemq-ce:latest"));

  private static final int ElevatorCnt = 2;
  private static final int FloorCnt = 6;
  private static final int ElevatorCapacity = 10;
  private static final int POLL_INTERVAL = 250;

  private void setExpectedDefaults() throws RemoteException {
    lenient().when(mockedIElevator.getElevatorNum()).thenReturn(ElevatorCnt);
    lenient().when(mockedIElevator.getFloorNum()).thenReturn(FloorCnt);
    lenient().when(mockedIElevator.getElevatorCapacity(0)).thenReturn(ElevatorCapacity);
    lenient().when(mockedIElevator.getElevatorCapacity(1)).thenReturn(ElevatorCapacity);

    for (int elev = 0; elev < ElevatorCnt; elev++) {
      lenient().when(mockedIElevator.getCommittedDirection(elev)).thenReturn(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED);
      lenient().when(mockedIElevator.getElevatorDoorStatus(elev)).thenReturn(IElevator.ELEVATOR_DOORS_CLOSED);
      lenient().when(mockedIElevator.getTarget(elev)).thenReturn(0);
      lenient().when(mockedIElevator.getElevatorFloor(elev)).thenReturn(0);
      lenient().when(mockedIElevator.getElevatorAccel(elev)).thenReturn(0);
      lenient().when(mockedIElevator.getElevatorSpeed(elev)).thenReturn(0);
      lenient().when(mockedIElevator.getElevatorPosition(elev)).thenReturn(0);
      lenient().when(mockedIElevator.getElevatorWeight(elev)).thenReturn(0);

      for (int floor = 0; floor < FloorCnt; floor++) {
        lenient().when(mockedIElevator.getServicesFloors(elev, floor)).thenReturn(true);
        lenient().when(mockedIElevator.getElevatorButton(elev, floor)).thenReturn(false);
      }
    }
  }

  @BeforeEach
  public void setUp() throws Exception {
    MockitoAnnotations.openMocks(this);

    // Create the Async MQTT client
    asyncMqttClient = MqttClient.builder()
        .useMqttVersion5()
        .identifier("AsyncTestClient")
        .serverHost(hiveMQContainer.getHost())
        .serverPort(hiveMQContainer.getMqttPort())
        .buildAsync();

    setExpectedDefaults();
  }

  @Test
  public void testInitialGet() throws Exception {

    // Create the adapter
    ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, asyncMqttClient, POLL_INTERVAL);

    assertTrue(asyncMqttClient.getState().isConnected(), "MQTT client should be connected to the broker.");

    // Verify interactions with the mocked IElevator (from ElevatorsMQTTAdapter)
    Mockito.verify(mockedIElevator).getElevatorNum();
    Mockito.verify(mockedIElevator).getFloorNum();
    Mockito.verify(mockedIElevator).getElevatorCapacity(0);
    Mockito.verify(mockedIElevator).getElevatorCapacity(1);
  }

  @Test
  public void testUpdateCommittedDirection() throws RemoteException {
    ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, asyncMqttClient, POLL_INTERVAL);

    Mockito.when(mockedIElevator.getCommittedDirection(0)).thenReturn(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED);
    Mockito.when(mockedIElevator.getCommittedDirection(1)).thenReturn(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED);

    adapter.updateState();

    Mockito.verify(mockedIElevator).getCommittedDirection(0);
    Mockito.verify(mockedIElevator).getCommittedDirection(1);

    Mockito.reset(mockedIElevator);
    setExpectedDefaults();

    Mockito.when(mockedIElevator.getCommittedDirection(0)).thenReturn(IElevator.ELEVATOR_DIRECTION_UP);
    Mockito.when(mockedIElevator.getCommittedDirection(1)).thenReturn(IElevator.ELEVATOR_DIRECTION_DOWN);

    adapter.updateState();

    Mockito.verify(mockedIElevator).getCommittedDirection(0);
    Mockito.verify(mockedIElevator).getCommittedDirection(1);

  }

  @Test
  public void testUpdateElevatorButton() throws RemoteException {
    ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, asyncMqttClient, POLL_INTERVAL);

    IntStream.range(0, FloorCnt).forEach(floor -> {
      try {
        Mockito.when(mockedIElevator.getElevatorButton(0, floor)).thenReturn(false);
        Mockito.when(mockedIElevator.getElevatorButton(1, floor)).thenReturn(false);
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    });

    adapter.updateState();

    IntStream.range(0, FloorCnt).forEach(floor -> {
      try {
        Mockito.verify(mockedIElevator).getElevatorButton(0, floor);
        Mockito.verify(mockedIElevator).getElevatorButton(1, floor);
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    });

    Mockito.reset(mockedIElevator);
    setExpectedDefaults();

    IntStream.range(0, FloorCnt).forEach(floor -> {
      try {
        Mockito.when(mockedIElevator.getElevatorButton(0, floor)).thenReturn(true);
        Mockito.when(mockedIElevator.getElevatorButton(1, floor)).thenReturn(true);
      } catch (RemoteException e) {
        e.printStackTrace();
      }
    });

    adapter.updateState();

    IntStream.range(0, FloorCnt).forEach(floor -> {
      try {
        Mockito.verify(mockedIElevator).getElevatorButton(0, floor);
        Mockito.verify(mockedIElevator).getElevatorButton(1, floor);

      } catch (RemoteException e) {
        e.printStackTrace();
      }
    });
  }

  @Test
  public void testUpdateElevatorDoorStatus() throws RemoteException {
    ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, asyncMqttClient, POLL_INTERVAL);

    Mockito.when(mockedIElevator.getElevatorDoorStatus(0)).thenReturn(IElevator.ELEVATOR_DOORS_CLOSED);
    Mockito.when(mockedIElevator.getElevatorDoorStatus(1)).thenReturn(IElevator.ELEVATOR_DOORS_OPEN);

    adapter.updateState();

    Mockito.verify(mockedIElevator).getElevatorDoorStatus(0);
    Mockito.verify(mockedIElevator).getElevatorDoorStatus(1);

    Mockito.reset(mockedIElevator);
    setExpectedDefaults();

    Mockito.when(mockedIElevator.getElevatorDoorStatus(0)).thenReturn(IElevator.ELEVATOR_DOORS_OPENING);
    Mockito.when(mockedIElevator.getElevatorDoorStatus(1)).thenReturn(IElevator.ELEVATOR_DOORS_CLOSING);

    adapter.updateState();

    Mockito.verify(mockedIElevator).getElevatorDoorStatus(0);
    Mockito.verify(mockedIElevator).getElevatorDoorStatus(1);
  }

  @Test
  public void testUpdateElevatorFloor() throws RemoteException {
    ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, asyncMqttClient, POLL_INTERVAL);

    Mockito.when(mockedIElevator.getElevatorFloor(0)).thenReturn(0);
    Mockito.when(mockedIElevator.getElevatorFloor(1)).thenReturn(FloorCnt - 1);

    adapter.updateState();

    Mockito.verify(mockedIElevator).getElevatorFloor(0);
    Mockito.verify(mockedIElevator).getElevatorFloor(1);

    Mockito.reset(mockedIElevator);
    setExpectedDefaults();

    Mockito.when(mockedIElevator.getElevatorFloor(0)).thenReturn(FloorCnt - 1);
    Mockito.when(mockedIElevator.getElevatorFloor(1)).thenReturn(0);

    adapter.updateState();

    Mockito.verify(mockedIElevator).getElevatorFloor(0);
    Mockito.verify(mockedIElevator).getElevatorFloor(1);
  }

  @Test
  public void testUpdateElevatorPosition() throws RemoteException {
    ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, asyncMqttClient, POLL_INTERVAL);

    Mockito.when(mockedIElevator.getElevatorPosition(0)).thenReturn(0);
    Mockito.when(mockedIElevator.getElevatorPosition(1)).thenReturn(10);

    adapter.updateState();

    Mockito.verify(mockedIElevator).getElevatorPosition(0);
    Mockito.verify(mockedIElevator).getElevatorPosition(1);

    Mockito.reset(mockedIElevator);
    setExpectedDefaults();

    Mockito.when(mockedIElevator.getElevatorPosition(0)).thenReturn(4);
    Mockito.when(mockedIElevator.getElevatorPosition(1)).thenReturn(0);

    adapter.updateState();

    Mockito.verify(mockedIElevator).getElevatorPosition(0);
    Mockito.verify(mockedIElevator).getElevatorPosition(1);
  }

  @Test
  public void testUpdateElevatorSpeed() throws RemoteException {
    ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, asyncMqttClient, POLL_INTERVAL);

    Mockito.when(mockedIElevator.getElevatorSpeed(0)).thenReturn(0);
    Mockito.when(mockedIElevator.getElevatorSpeed(1)).thenReturn(10);

    adapter.updateState();

    Mockito.verify(mockedIElevator).getElevatorSpeed(0);
    Mockito.verify(mockedIElevator).getElevatorSpeed(1);

    Mockito.reset(mockedIElevator);
    setExpectedDefaults();

    Mockito.when(mockedIElevator.getElevatorSpeed(0)).thenReturn(4);
    Mockito.when(mockedIElevator.getElevatorSpeed(1)).thenReturn(0);

    adapter.updateState();

    Mockito.verify(mockedIElevator).getElevatorSpeed(0);
    Mockito.verify(mockedIElevator).getElevatorSpeed(1);
  }

  @Test
  public void testUpdateElevatorWeight() throws RemoteException {
    ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, asyncMqttClient, POLL_INTERVAL);

    Mockito.when(mockedIElevator.getElevatorWeight(0)).thenReturn(0);
    Mockito.when(mockedIElevator.getElevatorWeight(1)).thenReturn(10);

    adapter.updateState();

    Mockito.verify(mockedIElevator).getElevatorWeight(0);
    Mockito.verify(mockedIElevator).getElevatorWeight(1);

    Mockito.reset(mockedIElevator);
    setExpectedDefaults();

    Mockito.when(mockedIElevator.getElevatorWeight(0)).thenReturn(4);
    Mockito.when(mockedIElevator.getElevatorWeight(1)).thenReturn(0);

    adapter.updateState();

    Mockito.verify(mockedIElevator).getElevatorWeight(0);
    Mockito.verify(mockedIElevator).getElevatorWeight(1);
  }

  @Test
  public void testUpdateFloorButtonUpDown() throws RemoteException {
    ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, asyncMqttClient, POLL_INTERVAL);

    for (int floor = 0; floor < FloorCnt; floor++) {
      Mockito.when(mockedIElevator.getFloorButtonUp(floor)).thenReturn(false);
      Mockito.when(mockedIElevator.getFloorButtonDown(floor)).thenReturn(false);
    }

    adapter.updateState();

    for (int floor = 0; floor < FloorCnt; floor++) {
      Mockito.verify(mockedIElevator).getFloorButtonUp(floor);
      Mockito.verify(mockedIElevator).getFloorButtonDown(floor);
    }

    Mockito.reset(mockedIElevator);
    setExpectedDefaults();

    for (int floor = 0; floor < FloorCnt; floor++) {
      Mockito.when(mockedIElevator.getFloorButtonUp(floor)).thenReturn(true);
      Mockito.when(mockedIElevator.getFloorButtonDown(floor)).thenReturn(true);
    }

    adapter.updateState();

    for (int floor = 0; floor < FloorCnt; floor++) {
      Mockito.verify(mockedIElevator).getFloorButtonUp(floor);
      Mockito.verify(mockedIElevator).getFloorButtonDown(floor);
    }
  }

  @Test
  public void testUpdateServicedFloors() throws RemoteException {
    ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, asyncMqttClient, POLL_INTERVAL);

    for (int floor = 0; floor < FloorCnt; floor++) {
      Mockito.when(mockedIElevator.getServicesFloors(0, floor)).thenReturn(true);
      Mockito.when(mockedIElevator.getServicesFloors(1, floor)).thenReturn(true);
    }

    adapter.updateState();

    for (int floor = 0; floor < FloorCnt; floor++) {
      Mockito.verify(mockedIElevator).getServicesFloors(0, floor);
      Mockito.verify(mockedIElevator).getServicesFloors(1, floor);
    }

    Mockito.reset(mockedIElevator);
    setExpectedDefaults();

    for (int floor = 0; floor < FloorCnt; floor++) {
      Mockito.when(mockedIElevator.getServicesFloors(0, floor)).thenReturn(false);
      Mockito.when(mockedIElevator.getServicesFloors(1, floor)).thenReturn(false);
    }

    adapter.updateState();

    for (int floor = 0; floor < FloorCnt; floor++) {
      Mockito.verify(mockedIElevator).getServicesFloors(0, floor);
      Mockito.verify(mockedIElevator).getServicesFloors(1, floor);
    }
  }

  @Test
  public void testUpdateTarget() throws RemoteException {
    ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, asyncMqttClient, POLL_INTERVAL);

    Mockito.when(mockedIElevator.getTarget(0)).thenReturn(0);
    Mockito.when(mockedIElevator.getTarget(1)).thenReturn(0);

    adapter.updateState();

    Mockito.verify(mockedIElevator).getTarget(0);
    Mockito.verify(mockedIElevator).getTarget(1);

    Mockito.reset(mockedIElevator);
    setExpectedDefaults();

    Mockito.when(mockedIElevator.getTarget(0)).thenReturn(FloorCnt - 1);
    Mockito.when(mockedIElevator.getTarget(1)).thenReturn(FloorCnt - 1);

    adapter.updateState();

    Mockito.verify(mockedIElevator).getTarget(0);
    Mockito.verify(mockedIElevator).getTarget(1);
  }

  @Test
  public void testPublish() throws Exception {

    Mqtt5AsyncClient testClient = MqttClient.builder()
        .useMqttVersion5()
        .identifier("SubscriberTestClient")
        .serverHost(hiveMQContainer.getHost())
        .serverPort(hiveMQContainer.getMqttPort())
        .buildAsync();

    CompletableFuture<Void> testClientConnectFuture = testClient.connect()
        .thenAccept(connAck -> {
            //System.out.println("Connected to host " + hiveMQContainer.getHost() + " on port " + hiveMQContainer.getMqttPort());
        })
        .exceptionally(throwable -> {
          //System.err.println("Connection failed: " + throwable.getMessage());
          return null;
        });

    testClientConnectFuture.join();

    // A Hashtable of topics to subscribe to
    // The key is the topic, the value is the expected message
    Hashtable<String, String> topicsToSubscribe = new Hashtable<String, String>();
    topicsToSubscribe.put(ElevatorsMQTTAdapter.TOPIC_BUILDING_NR_ELEVATORS, String.valueOf(ElevatorCnt));
    topicsToSubscribe.put(ElevatorsMQTTAdapter.TOPIC_BUILDING_NR_FLOORS, String.valueOf(FloorCnt));
    topicsToSubscribe.put(ElevatorsMQTTAdapter.TOPIC_BUILDING_ELEVATORS+ElevatorsMQTTAdapter.TOPIC_SEP+0+ElevatorsMQTTAdapter.TOPIC_SEP+ElevatorsMQTTAdapter.SUBTOPIC_ELEVATORS_ELEVATOR_CAPACITY, String.valueOf(ElevatorCapacity));
    topicsToSubscribe.put(ElevatorsMQTTAdapter.TOPIC_BUILDING_ELEVATORS+ElevatorsMQTTAdapter.TOPIC_SEP+1+ElevatorsMQTTAdapter.TOPIC_SEP+ElevatorsMQTTAdapter.SUBTOPIC_ELEVATORS_ELEVATOR_CAPACITY, String.valueOf(ElevatorCapacity));
     
    // A Hashtable to store the received messages
    // The key is the topic, the value is the received message
    Hashtable<String, String> receivedTopicsMsg = new Hashtable<String, String>();

    topicsToSubscribe.forEach((topic,expectedvalue) -> {
        testClient.subscribeWith()
        .topicFilter(topic)
        .qos(MqttQos.AT_LEAST_ONCE) // QoS level 1
        .callback(publish -> {
          String message = new String(publish.getPayloadAsBytes());
          receivedTopicsMsg.put(publish.getTopic().toString(), message);
        })
        .send()
        .whenComplete((subAck, throwable) -> {
          if (throwable != null) {
            // Handle subscription failure
            //System.err.println("Failed to subscribe: " + throwable.getMessage());
          } else {
            // Handle successful subscription
            //System.out.println("TestClient subscribed successfully to topic: " + topic);
          }
        });
    });

    ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, asyncMqttClient, POLL_INTERVAL);

    // wait for all publishes to finish (if 1 second is not enough, get a better PC)
    TimeUnit.MILLISECONDS.sleep(1000);

    topicsToSubscribe.forEach((topic, expectedvalue) -> {
      assertTrue(receivedTopicsMsg.containsKey(topic), "Topic " + topic + " was not received.");
      assertEquals(expectedvalue, receivedTopicsMsg.get(topic), "Topic " + topic + " has the wrong value.");
    });

    testClient.disconnect();
  }
  
  @Test
  public void testSubscribe() throws Exception {

    Mqtt5AsyncClient testClient = MqttClient.builder()
        .useMqttVersion5()
        .identifier("PublisherTestClient")
        .serverHost(hiveMQContainer.getHost())
        .serverPort(hiveMQContainer.getMqttPort())
        .buildAsync();

    CompletableFuture<Void> testClientConnectFuture = testClient.connect()
        .thenAccept(connAck -> {
            //System.out.println("Connected to host " + hiveMQContainer.getHost() + " on port " + hiveMQContainer.getMqttPort());
        })
        .exceptionally(throwable -> {
          //System.err.println("Connection failed: " + throwable.getMessage());
          return null;
        });

    testClientConnectFuture.join();

    ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, asyncMqttClient, POLL_INTERVAL);

    // wait for all publishes to finish (if 1 second is not enough, get a better PC)
    TimeUnit.MILLISECONDS.sleep(1000);
    
    Mockito.reset(mockedIElevator);

    String topic = ElevatorsMQTTAdapter.TOPIC_BUILDING_ELEVATORS + ElevatorsMQTTAdapter.TOPIC_SEP + 0 + ElevatorsMQTTAdapter.TOPIC_SEP + ElevatorsMQTTAdapter.SUBTOPIC_ELEVATORS_ELEVATOR_SETTARGET;
    String data = Integer.toString(1);
    CompletableFuture<Void> testClientPublishFuture = testClient.publishWith()
        .topic(topic)
        .payload(data.getBytes())
        .qos(MqttQos.AT_LEAST_ONCE)
        .retain(true)
        .send()
        .thenAccept(pubAck -> System.out.println("Published message: " + data + " to topic: " + topic))
        .exceptionally(throwable -> {
          System.err.println("Failed to publish: " + throwable.getMessage());
          return null;
        });

    testClientPublishFuture.join();

    String topic2 = ElevatorsMQTTAdapter.TOPIC_BUILDING_ELEVATORS + ElevatorsMQTTAdapter.TOPIC_SEP + 1 + ElevatorsMQTTAdapter.TOPIC_SEP + ElevatorsMQTTAdapter.SUBTOPIC_ELEVATORS_ELEVATOR_SETTARGET;
    String data2 = Integer.toString(2);
    testClientPublishFuture = testClient.publishWith()
        .topic(topic2)
        .payload(data2.getBytes())
        .qos(MqttQos.AT_LEAST_ONCE)
        .retain(true)
        .send()
        .thenAccept(pubAck -> System.out.println("Published message: " + data2 + " to topic: " + topic2))
        .exceptionally(throwable -> {
          System.err.println("Failed to publish: " + throwable.getMessage());
          return null;
        });

    testClientPublishFuture.join();

    // wait for all publishes to finish (if 1 second is not enough, get a better PC)
    TimeUnit.MILLISECONDS.sleep(1000);

    Mockito.verify(mockedIElevator).setTarget(1, 2);

    testClient.disconnect();
  }
  
}
