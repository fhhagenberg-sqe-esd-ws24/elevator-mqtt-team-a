package at.fhhagenberg.sqelevator;

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient.Mqtt5SubscribeAndCallbackBuilder;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5Subscribe;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.Mqtt5SubscriptionBuilder;
import com.hivemq.client.mqtt.mqtt5.message.subscribe.suback.Mqtt5SubAck;
import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

class ElevatorAlgorithmTest {

  @Mock
  private Mqtt5AsyncClient mqttClientMock;

  @Mock
  private Logger loggerMock;

  private ElevatorAlgorithm elevatorAlgorithm;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(mqttClientMock.connect()).thenReturn(CompletableFuture.completedFuture(null));
    elevatorAlgorithm = new ElevatorAlgorithm(mqttClientMock) {
      @Override
      protected void finalize() {
        // Do nothing during tests to avoid mock garbage collection issues
      }
    };
  }

  @Test
  void testAskForCurrentState() {
    when(mqttClientMock.getState()).thenReturn(MqttClientState.CONNECTED);
    when(mqttClientMock.publish(any(Mqtt5Publish.class)))
        .thenReturn(CompletableFuture.completedFuture(mock(Mqtt5PublishResult.class)));

    elevatorAlgorithm.askForCurrentState();

    verify(mqttClientMock).publish(argThat(publish -> publish.getTopic().toString()
        .equals(ElevatorAlgorithm.TOPIC_BUILDING_PUBLISH_CURRENT_STATE + "/request") &&
        new String(publish.getPayloadAsBytes()).equals("needUpdate")));
  }

  /*
  @Test
  void testSubscribeToInitials() throws Exception {
    // Mock the subscribe builder and its chain methods
    Mqtt5SubscribeAndCallbackBuilder.Start subscribeBuilderMock = mock(Mqtt5SubscribeAndCallbackBuilder.Start.class);
    when(mqttClientMock.subscribeWith()).thenReturn(subscribeBuilderMock);

    // Mock the completion of the subscription
    Mqtt5SubscribeAndCallbackBuilder.Start<CompletableFuture<Mqtt5SubAck>> nestedFutureMock = mock(Mqtt5ReactorSubscribe.Nested.class);
    when(subscribeBuilderMock.topicFilter(anyString())).thenAnswer((Answer<Mqtt5SubscribeAndCallbackBuilder.Start>) invocation -> {
        String topic = invocation.getArgument(0, String.class);
        return subscribeBuilderMock;
    });
    when(subscribeBuilderMock.addSubscription()).thenReturn(futureMock);

    // Call the method under test
    elevatorAlgorithm.subscribeToInitials();

    // Verify subscriptions for key topics
    verify(subscribeBuilderMock).topicFilter(ElevatorAlgorithm.TOPIC_BUILDING_NR_ELEVATORS);
    verify(subscribeBuilderMock).topicFilter(ElevatorAlgorithm.TOPIC_BUILDING_NR_FLOORS);
    verify(subscribeBuilderMock).addSubscription();


    // Mock subscribe methods
    //CompletableFuture<Void> futureMock = CompletableFuture.completedFuture(null);
    //when(mqttClientMock.subscribeWith().send()).thenReturn(futureMock);
    //
    //// Call the method under test
    //elevatorAlgorithm.subscribeToInitials();
    //
    //// Verify subscriptions for key topics
    //verify(mqttClientMock.subscribeWith()).topicFilter(ElevatorAlgorithm.
    //TOPIC_BUILDING_NR_ELEVATORS);
    //verify(mqttClientMock.subscribeWith()).topicFilter(ElevatorAlgorithm.
    //TOPIC_BUILDING_NR_FLOORS);
  }*/
  

  /*
   * NOT WORKING - disconnect is not called ?
   * 
   * @Test
   * void testCleanup() {
   * // Mock disconnect method
   * CompletableFuture<Void> futureMock = CompletableFuture.completedFuture(null);
   * when(mqttClientMock.disconnect()).thenReturn(futureMock);
   * 
   * // Call the method under test
   * elevatorAlgorithm.cleanup();
   * 
   * // Verify the disconnect method was called
   * verify(mqttClientMock).disconnect();
   * }
   */

  @Test
  void testShouldServiceFloor() {

    Building buildingMock = mock(Building.class);
    int elevNr = 0;
    ElevatorDataModell elevatorMock = mock(ElevatorDataModell.class);

    when(buildingMock.getUpButtonState(1)).thenReturn(true);
    when(buildingMock.getDownButtonState(1)).thenReturn(false);
    when(elevatorMock.getFloorRequested(1)).thenReturn(true);
    when(elevatorMock.getFloorToService(1)).thenReturn(true);
    when(buildingMock.getElevator(0)).thenReturn(elevatorMock);

    boolean result = elevatorAlgorithm.shouldServiceFloor(buildingMock, elevNr, 1);

    assertTrue(result);
  }

  @Test
  void testShouldServiceFloorNoRequests() {
    Building buildingMock = mock(Building.class);
    int elevNr = 0;
    ElevatorDataModell elevatorMock = mock(ElevatorDataModell.class);

    when(buildingMock.getUpButtonState(1)).thenReturn(false);
    when(buildingMock.getDownButtonState(1)).thenReturn(false);
    when(elevatorMock.getFloorRequested(1)).thenReturn(false);
    when(elevatorMock.getFloorToService(1)).thenReturn(false);
    when(buildingMock.getElevator(elevNr)).thenReturn(elevatorMock);

    boolean result = elevatorAlgorithm.shouldServiceFloor(buildingMock, elevNr, 1);

    assertFalse(result);
  }

  @Test
  void testShouldServiceFloorUpButtonOnly() {
    Building buildingMock = mock(Building.class);
    int elevNr = 0;
    ElevatorDataModell elevatorMock = mock(ElevatorDataModell.class);

    when(buildingMock.getUpButtonState(1)).thenReturn(true);
    when(buildingMock.getDownButtonState(1)).thenReturn(false);
    when(elevatorMock.getFloorRequested(1)).thenReturn(false);
    when(elevatorMock.getFloorToService(1)).thenReturn(true);
    when(buildingMock.getElevator(elevNr)).thenReturn(elevatorMock);

    boolean result = elevatorAlgorithm.shouldServiceFloor(buildingMock, elevNr, 1);

    assertTrue(result);
  }

  @Test
  void testShouldServiceFloorDownButtonOnly() {
    Building buildingMock = mock(Building.class);
    int elevNr = 0;
    ElevatorDataModell elevatorMock = mock(ElevatorDataModell.class);

    when(buildingMock.getUpButtonState(1)).thenReturn(false);
    when(buildingMock.getDownButtonState(1)).thenReturn(true);
    when(elevatorMock.getFloorRequested(1)).thenReturn(false);
    when(elevatorMock.getFloorToService(1)).thenReturn(true);
    when(buildingMock.getElevator(elevNr)).thenReturn(elevatorMock);

    boolean result = elevatorAlgorithm.shouldServiceFloor(buildingMock, elevNr, 1);

    assertTrue(result);
  }

  @Test
  void testShouldServiceFloorElevatorOnly() {
    Building buildingMock = mock(Building.class);
    int elevNr = 0;
    ElevatorDataModell elevatorMock = mock(ElevatorDataModell.class);

    when(buildingMock.getUpButtonState(1)).thenReturn(false);
    when(buildingMock.getDownButtonState(1)).thenReturn(false);
    when(elevatorMock.getFloorRequested(1)).thenReturn(true);
    when(elevatorMock.getFloorToService(1)).thenReturn(true);
    when(buildingMock.getElevator(elevNr)).thenReturn(elevatorMock);

    boolean result = elevatorAlgorithm.shouldServiceFloor(buildingMock, elevNr, 1);

    assertTrue(result);
  }

  @Test
  void testFindNearestRequestSingleRequest() {
    Building buildingMock = mock(Building.class);
    when(buildingMock.getNrFloors()).thenReturn(5);

    when(buildingMock.getUpButtonState(2)).thenReturn(true);
    when(buildingMock.getDownButtonState(2)).thenReturn(false);

    int elevNr = 0;
    ElevatorDataModell elevatorMock = mock(ElevatorDataModell.class);
    when(buildingMock.getElevator(elevNr)).thenReturn(elevatorMock);
    when(elevatorMock.getFloorToService(2)).thenReturn(true);

    int startFloor = 0;
    List<Integer> alreadyServedFloors = new ArrayList<>();
    assertEquals(2, elevatorAlgorithm.findNearestRequest(buildingMock, elevNr, startFloor, alreadyServedFloors));
  }

  @Test
  void testFindNearestRequestNoRequests() {
    Building buildingMock = mock(Building.class);
    when(buildingMock.getNrFloors()).thenReturn(5);

    int elevNr = 0;
    ElevatorDataModell elevatorMock = mock(ElevatorDataModell.class);
    when(buildingMock.getElevator(elevNr)).thenReturn(elevatorMock);

    int startFloor = 0;
    List<Integer> alreadyServedFloors = new ArrayList<>();
    assertEquals(-1, elevatorAlgorithm.findNearestRequest(buildingMock, elevNr, startFloor, alreadyServedFloors)); // No requests
  }

  @Test
  void testFindNearestRequestAllFloorsRequested() {
    Building buildingMock = mock(Building.class);
    when(buildingMock.getNrFloors()).thenReturn(5);

    for (int i = 0; i < 5; i++) {
      when(buildingMock.getUpButtonState(i)).thenReturn(true);
      when(buildingMock.getDownButtonState(i)).thenReturn(false);
    }

    int elevNr = 0;
    ElevatorDataModell elevatorMock = mock(ElevatorDataModell.class);
    when(buildingMock.getElevator(elevNr)).thenReturn(elevatorMock);
    for (int i = 0; i < 5; i++) {
      when(elevatorMock.getFloorToService(i)).thenReturn(true);
    }

    int startFloor = 2;
    List<Integer> alreadyServedFloors = new ArrayList<>();
    assertEquals(1, elevatorAlgorithm.findNearestRequest(buildingMock, elevNr, startFloor, alreadyServedFloors)); // Nearest below
  }

  @Test
  void testFindNearestRequestEdgeFloors() {
    Building buildingMock = mock(Building.class);
    when(buildingMock.getNrFloors()).thenReturn(5);

    when(buildingMock.getUpButtonState(0)).thenReturn(false);
    when(buildingMock.getDownButtonState(4)).thenReturn(true);

    int elevNr = 0;
    ElevatorDataModell elevatorMock = mock(ElevatorDataModell.class);
    when(buildingMock.getElevator(elevNr)).thenReturn(elevatorMock);
    when(elevatorMock.getFloorToService(0)).thenReturn(false);
    when(elevatorMock.getFloorToService(4)).thenReturn(true);

    int startFloor = 2;
    List<Integer> alreadyServedFloors = new ArrayList<>();
    assertEquals(4, elevatorAlgorithm.findNearestRequest(buildingMock, elevNr, startFloor, alreadyServedFloors)); // Edge floor
  }

  @Test
  void testFindNearestRequestBasic() {

    Building buildingMock = mock(Building.class);

    when(buildingMock.getNrFloors()).thenReturn(5);

    when(buildingMock.getUpButtonState(2)).thenReturn(true);
    when(buildingMock.getDownButtonState(2)).thenReturn(false);
    when(buildingMock.getUpButtonState(3)).thenReturn(false);
    when(buildingMock.getDownButtonState(3)).thenReturn(true);

    int elevNr = 0;
    ElevatorDataModell elevatorMock = mock(ElevatorDataModell.class);
    when(buildingMock.getElevator(elevNr)).thenReturn(elevatorMock);
    when(elevatorMock.getFloorToService(2)).thenReturn(true);
    when(elevatorMock.getFloorToService(3)).thenReturn(true);
    when(elevatorMock.getFloorToService(4)).thenReturn(false);
    when(elevatorMock.getFloorToService(5)).thenReturn(false);

    int startFloor = 0;
    List<Integer> alreadyServedFloors = new ArrayList<>();
    assertEquals(2, elevatorAlgorithm.findNearestRequest(buildingMock, elevNr, startFloor, alreadyServedFloors));

    startFloor = 4;
    assertEquals(3, elevatorAlgorithm.findNearestRequest(buildingMock, elevNr, startFloor, alreadyServedFloors));
  }

  @Test
  void testFindNearestRequestBasicAround() {

    Building buildingMock = mock(Building.class);

    when(buildingMock.getNrFloors()).thenReturn(8);

    when(buildingMock.getUpButtonState(2)).thenReturn(true);
    when(buildingMock.getDownButtonState(2)).thenReturn(false);
    when(buildingMock.getUpButtonState(3)).thenReturn(false);
    when(buildingMock.getDownButtonState(3)).thenReturn(true);
    when(buildingMock.getUpButtonState(4)).thenReturn(false);
    when(buildingMock.getDownButtonState(4)).thenReturn(true);
    when(buildingMock.getUpButtonState(5)).thenReturn(false);
    when(buildingMock.getDownButtonState(5)).thenReturn(true);

    int elevNr = 0;
    ElevatorDataModell elevatorMock = mock(ElevatorDataModell.class);
    when(buildingMock.getElevator(elevNr)).thenReturn(elevatorMock);
    when(elevatorMock.getFloorToService(2)).thenReturn(true);
    when(elevatorMock.getFloorToService(3)).thenReturn(true);
    when(elevatorMock.getFloorToService(4)).thenReturn(true);
    when(elevatorMock.getFloorToService(5)).thenReturn(true);
    when(elevatorMock.getFloorToService(6)).thenReturn(true);

    int startFloor = 4;
    List<Integer> alreadyServedFloors = new ArrayList<>();
    assertEquals(3, elevatorAlgorithm.findNearestRequest(buildingMock, elevNr, startFloor, alreadyServedFloors)); // down is preferred

    startFloor = 5;
    assertEquals(4, elevatorAlgorithm.findNearestRequest(buildingMock, elevNr, startFloor, alreadyServedFloors));
  }
}
