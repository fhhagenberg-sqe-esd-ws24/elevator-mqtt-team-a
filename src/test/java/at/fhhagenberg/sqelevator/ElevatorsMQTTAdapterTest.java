package at.fhhagenberg.sqelevator;

import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

import java.rmi.RemoteException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;


import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;
import com.hivemq.client.mqtt.MqttClient;

import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.hivemq.HiveMQContainer;

@Testcontainers
@ExtendWith(MockitoExtension.class)
public class ElevatorsMQTTAdapterTest {

    @Mock
    private IElevator mockedIElevator;

    @Container
    private HiveMQContainer mqttContainer;

    private Mqtt5AsyncClient mqttClient;

    private static final int ElevatorCnt = 2;
    private static final int FloorCnt = 6;
    private static final int ElevatorCapacity = 10;

    private void SetExpectedDefaults() throws RemoteException {
        // defaults
        lenient().when(mockedIElevator.getElevatorNum()).thenReturn(ElevatorCnt);
        lenient().when(mockedIElevator.getFloorNum()).thenReturn(FloorCnt);
        lenient().when(mockedIElevator.getElevatorCapacity(0)).thenReturn(ElevatorCapacity);
        lenient().when(mockedIElevator.getElevatorCapacity(1)).thenReturn(ElevatorCapacity);
        // defaults for variants
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

    @AfterEach
    public void tearDown() {
        mqttContainer.close();
    }

    @BeforeEach
    public void Setup() throws RemoteException {
        MockitoAnnotations.openMocks(this);

        // Initialize HiveMQ Testcontainer and MQTT client
        mqttContainer = new HiveMQContainer(DockerImageName.parse("hivemq/hivemq-ce:latest"));
        mqttContainer.start();
        mqttClient = MqttClient.builder()
                .useMqttVersion5()
                .serverHost(mqttContainer.getHost())
                .serverPort(mqttContainer.getMqttPort())
                .buildAsync();

        SetExpectedDefaults();
    }

    @Test
    public void testInitialGet() throws RemoteException {
        @SuppressWarnings("unused")
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mqttClient,250);

        // the constructor should call the following functions
        Mockito.verify(mockedIElevator).getElevatorNum();
        Mockito.verify(mockedIElevator).getFloorNum();
        Mockito.verify(mockedIElevator).getElevatorCapacity(0);
        Mockito.verify(mockedIElevator).getElevatorCapacity(1);
    }

    @Test
    public void testUpdateCommittedDirection() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mqttClient, 250);

        // define what the return parameters
        Mockito.when(mockedIElevator.getCommittedDirection(0)).thenReturn(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED);
        Mockito.when(mockedIElevator.getCommittedDirection(1)).thenReturn(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED);

        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getCommittedDirection(0);
        Mockito.verify(mockedIElevator).getCommittedDirection(1);

        // change return parameters
        Mockito.when(mockedIElevator.getCommittedDirection(0)).thenReturn(IElevator.ELEVATOR_DIRECTION_UP);
        Mockito.when(mockedIElevator.getCommittedDirection(1)).thenReturn(IElevator.ELEVATOR_DIRECTION_DOWN);

        // update again
        adapter.updateState();

        // verify MQTT topics published
        // Add any assertion framework checks if needed
    }

    @Test
    public void testUpdateElevatorAccel() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mqttClient, 250);

        // define what the return parameters
        Mockito.when(mockedIElevator.getElevatorAccel(0)).thenReturn(10);
        Mockito.when(mockedIElevator.getElevatorAccel(1)).thenReturn(5);
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorAccel(0); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorAccel(1); // elevator 1

        // reset internal expected queues to 
        // Mockito.reset(mockedDummyMQTT);
        Mockito.reset(mockedIElevator);
        SetExpectedDefaults();

        // change what the return parameters
        Mockito.when(mockedIElevator.getElevatorAccel(0)).thenReturn(1);
        Mockito.when(mockedIElevator.getElevatorAccel(1)).thenReturn(1);

        // update again
        adapter.updateState();

        // verify getter again
        Mockito.verify(mockedIElevator).getElevatorAccel(0); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorAccel(1); // elevator 1

        // verify that mqtt topic has been published because state has changed
        Mockito.verify(mqttClient).publishWith().topic("elevators/0/ElevatorAcceleration").send();
        Mockito.verify(mqttClient).publishWith().topic("elevators/1/ElevatorAcceleration").send();
    }

    @Test
    public void testUpdateElevatorButton() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mqttClient, 250);

        for (int floor = 0; floor < FloorCnt; floor++) {
            // define what the return parameters
            Mockito.when(mockedIElevator.getElevatorButton(0, floor)).thenReturn(false);
            Mockito.when(mockedIElevator.getElevatorButton(1, floor)).thenReturn(false);
        }
        // update state
        adapter.updateState();

        for (int floor = 0; floor < FloorCnt; floor++) {
            // verify that getter is called
            Mockito.verify(mockedIElevator).getElevatorButton(0, floor); // elevator 1 
            Mockito.verify(mockedIElevator).getElevatorButton(1, floor); // elevator 2
        }

        // reset internal expected queues to 
        //Mockito.reset(mockedDummyMQTT);
        Mockito.reset(mockedIElevator);
        SetExpectedDefaults();

        for (int floor = 0; floor < FloorCnt; floor++) {
            // change what the return parameters
            Mockito.when(mockedIElevator.getElevatorButton(0, floor)).thenReturn(true);
            Mockito.when(mockedIElevator.getElevatorButton(1, floor)).thenReturn(true);
        }
        // update again
        adapter.updateState();

        for (int floor = 0; floor < FloorCnt; floor++) {
            // verify getter again
            Mockito.verify(mockedIElevator).getElevatorButton(0, floor); // elevator 1 
            Mockito.verify(mockedIElevator).getElevatorButton(1, floor); // elevator 2

            // verify that mqtt topic has been published because state has changed
            Mockito.verify(mqttClient).publishWith().topic("elevators/0/FloorRequested/" + floor).send();
            Mockito.verify(mqttClient).publishWith().topic("elevators/1/FloorRequested/" + floor).send();
        }
    }

    @Test
    public void testUpdateElevatorDoorStatus() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mqttClient, 250);

        // define what the return parameters
        Mockito.when(mockedIElevator.getElevatorDoorStatus(0)).thenReturn(IElevator.ELEVATOR_DOORS_CLOSED);
        Mockito.when(mockedIElevator.getElevatorDoorStatus(1)).thenReturn(IElevator.ELEVATOR_DOORS_OPEN);
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorDoorStatus(0); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorDoorStatus(1); // elevator 1

        // reset internal expected queues to 
        //Mockito.reset(mockedDummyMQTT);
        Mockito.reset(mockedIElevator);
        SetExpectedDefaults();

        // change what the return parameters
        Mockito.when(mockedIElevator.getElevatorDoorStatus(0)).thenReturn(IElevator.ELEVATOR_DOORS_OPENING);
        Mockito.when(mockedIElevator.getElevatorDoorStatus(1)).thenReturn(IElevator.ELEVATOR_DOORS_CLOSING);

        // update again
        adapter.updateState();

        // verify getter again
        Mockito.verify(mockedIElevator).getElevatorDoorStatus(0); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorDoorStatus(1); // elevator 1

        // verify that mqtt topic has been published because state has changed
        Mockito.verify(mqttClient).publishWith().topic("elevators/0/ElevatorDoorStatus").send();
        Mockito.verify(mqttClient).publishWith().topic("elevators/1/ElevatorDoorStatus").send();
    }

    @Test
    public void testUpdateElevatorFloor() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mqttClient, 250);

        // define what the return parameters
        Mockito.when(mockedIElevator.getElevatorFloor(0)).thenReturn(0);
        Mockito.when(mockedIElevator.getElevatorFloor(1)).thenReturn(FloorCnt-1);
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorFloor(0); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorFloor(1); // elevator 1

        // reset internal expected queues to 
        //Mockito.reset(mockedDummyMQTT);
        Mockito.reset(mockedIElevator);
        SetExpectedDefaults();

        // change what the return parameters
        Mockito.when(mockedIElevator.getElevatorFloor(0)).thenReturn(FloorCnt-1);
        Mockito.when(mockedIElevator.getElevatorFloor(1)).thenReturn(0);

        // update again
        adapter.updateState();

        // verify getter again
        Mockito.verify(mockedIElevator).getElevatorFloor(0); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorFloor(1); // elevator 1

        // verify that mqtt topic has been published because state has changed
        Mockito.verify(mqttClient).publishWith().topic("elevators/0/ElevatorCurrentFloor").send();
        Mockito.verify(mqttClient).publishWith().topic("elevators/1/ElevatorCurrentFloor").send();
    }

    @Test
    public void testUpdateElevatorPosition() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mqttClient, 250);

        // define what the return parameters
        Mockito.when(mockedIElevator.getElevatorPosition(0)).thenReturn(0);
        Mockito.when(mockedIElevator.getElevatorPosition(1)).thenReturn(10); // 10 feet height
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorPosition(0); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorPosition(1); // elevator 1

        // reset internal expected queues to 
        //Mockito.reset(mockedDummyMQTT);
        Mockito.reset(mockedIElevator);
        SetExpectedDefaults();

        // change what the return parameters
        Mockito.when(mockedIElevator.getElevatorPosition(0)).thenReturn(4);
        Mockito.when(mockedIElevator.getElevatorPosition(1)).thenReturn(0);

        // update again
        adapter.updateState();

        // verify getter again
        Mockito.verify(mockedIElevator).getElevatorPosition(0); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorPosition(1); // elevator 1

        // verify that mqtt topic has been published because state has changed
        Mockito.verify(mqttClient).publishWith().topic("elevators/0/ElevatorCurrentHeight").send();
        Mockito.verify(mqttClient).publishWith().topic("elevators/1/ElevatorCurrentHeight").send();
    }

    @Test
    public void testUpdateElevatorSpeed() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mqttClient, 250);

        // define what the return parameters
        Mockito.when(mockedIElevator.getElevatorSpeed(0)).thenReturn(0);
        Mockito.when(mockedIElevator.getElevatorSpeed(1)).thenReturn(10); // 10 m/s
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorSpeed(0); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorSpeed(1); // elevator 1

        // reset internal expected queues to 
        //Mockito.reset(mockedDummyMQTT);
        Mockito.reset(mockedIElevator);
        SetExpectedDefaults();
        
        // change what the return parameters
        Mockito.when(mockedIElevator.getElevatorSpeed(0)).thenReturn(4);
        Mockito.when(mockedIElevator.getElevatorSpeed(1)).thenReturn(0);

        // update again
        adapter.updateState();

        // verify getter again
        Mockito.verify(mockedIElevator).getElevatorSpeed(0); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorSpeed(1); // elevator 1

        // verify that mqtt topic has been published because state has changed
        Mockito.verify(mqttClient).publishWith().topic("elevators/0/ElevatorSpeed").send();
        Mockito.verify(mqttClient).publishWith().topic("elevators/1/ElevatorSpeed").send();
    }

    @Test
    public void testUpdateElevatorWeight() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mqttClient, 250);

        // define what the return parameters
        Mockito.when(mockedIElevator.getElevatorWeight(0)).thenReturn(0);
        Mockito.when(mockedIElevator.getElevatorWeight(1)).thenReturn(10); // 10 kg
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorWeight(0); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorWeight(1); // elevator 1

        // reset internal expected queues to 
        //Mockito.reset(mockedDummyMQTT);
        Mockito.reset(mockedIElevator);
        SetExpectedDefaults();

        // change what the return parameters
        Mockito.when(mockedIElevator.getElevatorWeight(0)).thenReturn(4);
        Mockito.when(mockedIElevator.getElevatorWeight(1)).thenReturn(0);

        // update again
        adapter.updateState();

        // verify getter again
        Mockito.verify(mockedIElevator).getElevatorWeight(0); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorWeight(1); // elevator 1

        // verify that mqtt topic has been published because state has changed
        Mockito.verify(mqttClient).publishWith().topic("elevators/0/ElevatorCurrentPassengersWeight").send();
        Mockito.verify(mqttClient).publishWith().topic("elevators/1/ElevatorCurrentPassengersWeight").send();
    }

    @Test
    public void testUpdateFloorButtonUpDown() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mqttClient, 250);

        for (int floor = 0; floor < FloorCnt; floor++) {
            // define what the return parameters
            Mockito.when(mockedIElevator.getFloorButtonUp(floor)).thenReturn(false);
            Mockito.when(mockedIElevator.getFloorButtonDown(floor)).thenReturn(false);
        }
            // update state
            adapter.updateState();

        for (int floor = 0; floor < FloorCnt; floor++) {
            // verify that getter is called
            Mockito.verify(mockedIElevator).getFloorButtonUp(floor); // elevator 1 
            Mockito.verify(mockedIElevator).getFloorButtonDown(floor); // elevator 1
        }

        // reset internal expected queues to 
        //Mockito.reset(mockedDummyMQTT);
        Mockito.reset(mockedIElevator);
        SetExpectedDefaults();

        for (int floor = 0; floor < FloorCnt; floor++) {
            // change what the return parameters
            Mockito.when(mockedIElevator.getFloorButtonUp(floor)).thenReturn(true);
            Mockito.when(mockedIElevator.getFloorButtonDown(floor)).thenReturn(true);
        }

            // update again
            adapter.updateState();

        for (int floor = 0; floor < FloorCnt; floor++) {
            // verify getter again
            Mockito.verify(mockedIElevator).getFloorButtonUp(floor); // elevator 1 
            Mockito.verify(mockedIElevator).getFloorButtonDown(floor); // elevator 1

            // verify that mqtt topic has been published because state has changed
            Mockito.verify(mqttClient).publishWith().topic("floors/" + floor + "/ButtonUpPressed/").send();
            Mockito.verify(mqttClient).publishWith().topic("floors/" + floor + "/ButtonDownPressed/").send();
        }
    }

    @Test
    public void testUpdateServicedFloors() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mqttClient, 250);

        for (int floor = 0; floor < FloorCnt; floor++) {
            // define what the return parameters
            Mockito.when(mockedIElevator.getServicesFloors(0, floor)).thenReturn(true);
            Mockito.when(mockedIElevator.getServicesFloors(1, floor)).thenReturn(true);
        }
        // update state
        adapter.updateState();

        for (int floor = 0; floor < FloorCnt; floor++) {
            // verify that getter is called
            Mockito.verify(mockedIElevator).getServicesFloors(0, floor); // elevator 1 
            Mockito.verify(mockedIElevator).getServicesFloors(1, floor); // elevator 2
        }

        // reset internal expected queues to 
        //Mockito.reset(mockedDummyMQTT);
        Mockito.reset(mockedIElevator);
        SetExpectedDefaults();

        for (int floor = 0; floor < FloorCnt; floor++) {
            // change what the return parameters
            Mockito.when(mockedIElevator.getServicesFloors(0, floor)).thenReturn(false);
            Mockito.when(mockedIElevator.getServicesFloors(1, floor)).thenReturn(false);
        }
        // update again
        adapter.updateState();

        for (int floor = 0; floor < FloorCnt; floor++) {
            // verify getter again
            Mockito.verify(mockedIElevator).getServicesFloors(0, floor); // elevator 1 
            Mockito.verify(mockedIElevator).getServicesFloors(1, floor); // elevator 2

            // verify that mqtt topic has been published because state has changed
            Mockito.verify(mqttClient).publishWith().topic("elevators/0/FloorServiced/" + floor).send();
            Mockito.verify(mqttClient).publishWith().topic("elevators/1/FloorServiced/" + floor).send();
        }
    }

    @Test
    public void testUpdateTarget() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mqttClient, 250);

        // define what the return parameters
        Mockito.when(mockedIElevator.getTarget(0)).thenReturn(0);
        Mockito.when(mockedIElevator.getTarget(1)).thenReturn(0); // 10 kg
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getTarget(0); // elevator 1 
        Mockito.verify(mockedIElevator).getTarget(1); // elevator 1

        // reset internal expected queues to 
        //Mockito.reset(mockedDummyMQTT);
        Mockito.reset(mockedIElevator);
        SetExpectedDefaults();

        // change what the return parameters
        Mockito.when(mockedIElevator.getTarget(0)).thenReturn(FloorCnt-1);
        Mockito.when(mockedIElevator.getTarget(1)).thenReturn(FloorCnt-1);

        // update again
        adapter.updateState();

        // verify getter again
        Mockito.verify(mockedIElevator).getTarget(0); // elevator 1 
        Mockito.verify(mockedIElevator).getTarget(1); // elevator 1

        // verify that mqtt topic has been published because state has changed
        Mockito.verify(mqttClient).publishWith().topic("elevators/0/ElevatorTargetFloor").send();
        Mockito.verify(mqttClient).publishWith().topic("elevators/1/ElevatorTargetFloor").send();
    }
}
