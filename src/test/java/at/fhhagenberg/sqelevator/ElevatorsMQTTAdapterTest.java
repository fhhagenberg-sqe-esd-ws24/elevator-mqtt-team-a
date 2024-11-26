package at.fhhagenberg.sqelevator;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;

import java.lang.reflect.Array;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.hivemq.client.mqtt.mqtt5.Mqtt5AsyncClient;

@ExtendWith(MockitoExtension.class)
public class ElevatorsMQTTAdapterTest {
    
    @Mock
    private IElevator mockedIElevator;

    @Mock
    private DummyMQTT mockedDummyMQTT;

    // TODO
    // @Mock
    // private Mqtt5AsyncClient mockedMqttClient;

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

    @BeforeEach
    public void Setup() throws RemoteException {

        MockitoAnnotations.openMocks(this);

        SetExpectedDefaults();
    }

    @Test
    public void testInitialGet() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mockedDummyMQTT);

        // the constructor should call the following functions
        Mockito.verify(mockedIElevator).getElevatorNum();
        Mockito.verify(mockedIElevator).getFloorNum();
        Mockito.verify(mockedIElevator).getElevatorCapacity(0);
        Mockito.verify(mockedIElevator).getElevatorCapacity(1);
    }

    @Test
    public void testUpdateCommittedDirection() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mockedDummyMQTT);

        // define what the return parameters
        Mockito.when(mockedIElevator.getCommittedDirection(0)).thenReturn(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED);
        Mockito.when(mockedIElevator.getCommittedDirection(1)).thenReturn(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED);
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getCommittedDirection(0); // elevator 1 
        Mockito.verify(mockedIElevator).getCommittedDirection(1); // elevator 1

        // reset internal expected queues to 
        Mockito.reset(mockedDummyMQTT);
        Mockito.reset(mockedIElevator);
        SetExpectedDefaults();

        // change what the return parameters
        Mockito.when(mockedIElevator.getCommittedDirection(0)).thenReturn(IElevator.ELEVATOR_DIRECTION_UP);
        Mockito.when(mockedIElevator.getCommittedDirection(1)).thenReturn(IElevator.ELEVATOR_DIRECTION_DOWN);

        // update again
        adapter.updateState();

        // verify getter again
        Mockito.verify(mockedIElevator).getCommittedDirection(0); // elevator 1 
        Mockito.verify(mockedIElevator).getCommittedDirection(1); // elevator 1

        // verify that mqtt topic has been published because state has changed
        Mockito.verify(mockedDummyMQTT).Publish("elevators/0/ElevatorDirection");
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorDirection");
    }

    @Test
    public void testUpdateElevatorAccel() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mockedDummyMQTT);

        // define what the return parameters
        Mockito.when(mockedIElevator.getElevatorAccel(0)).thenReturn(10);
        Mockito.when(mockedIElevator.getElevatorAccel(1)).thenReturn(5);
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorAccel(0); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorAccel(1); // elevator 1

        // reset internal expected queues to 
        Mockito.reset(mockedDummyMQTT);
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
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorAcceleration");
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorAcceleration");
    }

    @Test
    public void testUpdateElevatorButton() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mockedDummyMQTT);

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
        Mockito.reset(mockedDummyMQTT);
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
            Mockito.verify(mockedDummyMQTT).Publish("elevators/1/FloorRequested/" + floor);
            Mockito.verify(mockedDummyMQTT).Publish("elevators/1/FloorRequested/" + floor);
        }
    }

    @Test
    public void testUpdateElevatorDoorStatus() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mockedDummyMQTT);

        // define what the return parameters
        Mockito.when(mockedIElevator.getElevatorDoorStatus(0)).thenReturn(IElevator.ELEVATOR_DOORS_CLOSED);
        Mockito.when(mockedIElevator.getElevatorDoorStatus(1)).thenReturn(IElevator.ELEVATOR_DOORS_OPEN);
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorDoorStatus(0); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorDoorStatus(1); // elevator 1

        // reset internal expected queues to 
        Mockito.reset(mockedDummyMQTT);
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
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorDoorStatus");
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorDoorStatus");
    }

    @Test
    public void testUpdateElevatorFloor() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mockedDummyMQTT);

        // define what the return parameters
        Mockito.when(mockedIElevator.getElevatorFloor(0)).thenReturn(0);
        Mockito.when(mockedIElevator.getElevatorFloor(1)).thenReturn(FloorCnt-1);
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorFloor(0); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorFloor(1); // elevator 1

        // reset internal expected queues to 
        Mockito.reset(mockedDummyMQTT);
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
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorCurrentFloor");
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorCurrentFloor");
    }

    @Test
    public void testUpdateElevatorPosition() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mockedDummyMQTT);

        // define what the return parameters
        Mockito.when(mockedIElevator.getElevatorPosition(0)).thenReturn(0);
        Mockito.when(mockedIElevator.getElevatorPosition(1)).thenReturn(10); // 10 feet height
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorPosition(0); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorPosition(1); // elevator 1

        // reset internal expected queues to 
        Mockito.reset(mockedDummyMQTT);
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
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorCurrentHeight");
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorCurrentHeight");
    }

    @Test
    public void testUpdateElevatorSpeed() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mockedDummyMQTT);

        // define what the return parameters
        Mockito.when(mockedIElevator.getElevatorSpeed(0)).thenReturn(0);
        Mockito.when(mockedIElevator.getElevatorSpeed(1)).thenReturn(10); // 10 m/s
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorSpeed(0); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorSpeed(1); // elevator 1

        // reset internal expected queues to 
        Mockito.reset(mockedDummyMQTT);
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
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorSpeed");
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorSpeed");
    }

    @Test
    public void testUpdateElevatorWeight() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mockedDummyMQTT);

        // define what the return parameters
        Mockito.when(mockedIElevator.getElevatorWeight(0)).thenReturn(0);
        Mockito.when(mockedIElevator.getElevatorWeight(1)).thenReturn(10); // 10 kg
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorWeight(0); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorWeight(1); // elevator 1

        // reset internal expected queues to 
        Mockito.reset(mockedDummyMQTT);
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
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorCurrentPassengersWeight");
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorCurrentPassengersWeight");
    }

    @Test
    public void testUpdateFloorButtonUpDown() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mockedDummyMQTT);

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
        Mockito.reset(mockedDummyMQTT);
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
            Mockito.verify(mockedDummyMQTT).Publish("floors/" + floor + "/ButtonUpPressed/");
            Mockito.verify(mockedDummyMQTT).Publish("floors/" + floor + "/ButtonDownPressed/");
        }
    }

    @Test
    public void testUpdateServicedFloors() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mockedDummyMQTT);

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
        Mockito.reset(mockedDummyMQTT);
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
            Mockito.verify(mockedDummyMQTT).Publish("elevators/1/FloorServiced/" + floor);
            Mockito.verify(mockedDummyMQTT).Publish("elevators/1/FloorServiced/" + floor);
        }
    }

    @Test
    public void testUpdateTarget() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator, mockedDummyMQTT);

        // define what the return parameters
        Mockito.when(mockedIElevator.getTarget(0)).thenReturn(0);
        Mockito.when(mockedIElevator.getTarget(1)).thenReturn(0); // 10 kg
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getTarget(0); // elevator 1 
        Mockito.verify(mockedIElevator).getTarget(1); // elevator 1

        // reset internal expected queues to 
        Mockito.reset(mockedDummyMQTT);
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
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorTargetFloor");
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorTargetFloor");
    }
}
