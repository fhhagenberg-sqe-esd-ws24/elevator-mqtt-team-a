package at.fhhagenberg.sqelevator;

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5Publish;
import com.hivemq.client.mqtt.mqtt5.message.publish.Mqtt5PublishResult;
import com.hivemq.client.mqtt.MqttClientState;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.CompletableFuture;

import static org.mockito.Mockito.*;

class BaseMQTTTest {

  @Mock
  private Mqtt5AsyncClient mqttClientMock;

  @Mock
  private Logger loggerMock;

  private BaseMQTT baseMQTT;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    when(mqttClientMock.connect()).thenReturn(CompletableFuture.completedFuture(null));
    baseMQTT = new BaseMQTT(mqttClientMock);
  }

  @Test
  void testPublishMQTT() {
    String topic = "test/topic";
    String data = "Test Message";

    when(mqttClientMock.getState()).thenReturn(MqttClientState.CONNECTED);

    CompletableFuture<Mqtt5PublishResult> futureMock = CompletableFuture
        .completedFuture(mock(Mqtt5PublishResult.class));
    when(mqttClientMock.publish(any(Mqtt5Publish.class))).thenReturn(futureMock);

    baseMQTT.publishMQTT(topic, data);

    verify(mqttClientMock).publish(argThat(publish -> publish.getTopic().toString().equals(topic) &&
        new String(publish.getPayloadAsBytes()).equals(data) &&
        publish.getQos() == MqttQos.AT_LEAST_ONCE &&
        !publish.isRetain()));
  }

  @Test
  void testPublishRetainedMQTT() {
    String topic = "test/retained";
    String data = "Retained Message";

    when(mqttClientMock.getState()).thenReturn(MqttClientState.CONNECTED);

    CompletableFuture<Mqtt5PublishResult> futureMock = CompletableFuture
        .completedFuture(mock(Mqtt5PublishResult.class));
    when(mqttClientMock.publish(any(Mqtt5Publish.class))).thenReturn(futureMock);

    baseMQTT.publishRetainedMQTT(topic, data);

    verify(mqttClientMock).publish(argThat(publish -> publish.getTopic().toString().equals(topic) &&
        new String(publish.getPayloadAsBytes()).equals(data) &&
        publish.getQos() == MqttQos.AT_LEAST_ONCE &&
        publish.isRetain()));
  }

  @Test
  void testCloseConnection() {
    when(mqttClientMock.getState()).thenReturn(MqttClientState.CONNECTED);

    CompletableFuture<Void> futureMock = CompletableFuture.completedFuture(null);
    when(mqttClientMock.disconnect()).thenReturn(futureMock);

    baseMQTT.closeConnection();

    verify(mqttClientMock).disconnect();
  }

  @Test
  void testFinalize() throws Throwable {
    when(mqttClientMock.disconnect()).thenReturn(CompletableFuture.completedFuture(null));

    baseMQTT.finalize();

    verify(mqttClientMock).disconnect();
  }
}
