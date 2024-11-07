package at.fhhagenberg.sqelevator;

import static org.mockito.Mockito.times;

import java.rmi.RemoteException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class ElevatorsMQTTAdapterTest {
    
    @Mock
    private IElevator mockedIElevator;

    @Mock
    private DummyMQTT mockedDummyMQTT;

    private static final int ElevatorCnt = 2;
    private static final int FloorCnt = 6;
    private static final int ElevatorCapacity = 10;

    @BeforeEach
    public void testSetup() throws RemoteException {

        // invariants
        Mockito.when(mockedIElevator.getElevatorNum()).thenReturn(ElevatorCnt);
        Mockito.when(mockedIElevator.getFloorNum()).thenReturn(FloorCnt);
        Mockito.when(mockedIElevator.getElevatorCapacity(1)).thenReturn(ElevatorCapacity);
        Mockito.when(mockedIElevator.getElevatorCapacity(2)).thenReturn(ElevatorCapacity);
    }

    @Test
    public void testInitialGet() throws RemoteException {

        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        // the constructor should call the following functions
        Mockito.verify(mockedIElevator).getElevatorNum();
        Mockito.verify(mockedIElevator).getFloorNum();
        Mockito.verify(mockedIElevator).getElevatorCapacity(0);
        Mockito.verify(mockedIElevator).getElevatorCapacity(1);
    }

    @Test
    public void testUpdateCommittedDirection() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        // define what the return parameters
        Mockito.when(mockedIElevator.getCommittedDirection(1)).thenReturn(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED);
        Mockito.when(mockedIElevator.getCommittedDirection(2)).thenReturn(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED);
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getCommittedDirection(1); // elevator 1 
        Mockito.verify(mockedIElevator).getCommittedDirection(2); // elevator 2

        // change what the return parameters
        Mockito.when(mockedIElevator.getCommittedDirection(1)).thenReturn(IElevator.ELEVATOR_DIRECTION_UP);
        Mockito.when(mockedIElevator.getCommittedDirection(2)).thenReturn(IElevator.ELEVATOR_DIRECTION_DOWN);

        // update again
        adapter.updateState();

        // verify getter again
        Mockito.verify(mockedIElevator).getCommittedDirection(1); // elevator 1 
        Mockito.verify(mockedIElevator).getCommittedDirection(2); // elevator 2

        // verify that mqtt topic has been published because state has changed
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorDirection");
        Mockito.verify(mockedDummyMQTT).Publish("elevators/2/ElevatorDirection");
    }

    @Test
    public void testUpdateElevatorAccel() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        // define what the return parameters
        Mockito.when(mockedIElevator.getElevatorAccel(1)).thenReturn(10);
        Mockito.when(mockedIElevator.getElevatorAccel(2)).thenReturn(5);
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorAccel(1); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorAccel(2); // elevator 2

        // change what the return parameters
        Mockito.when(mockedIElevator.getElevatorAccel(1)).thenReturn(2);
        Mockito.when(mockedIElevator.getElevatorAccel(2)).thenReturn(1);

        // update again
        adapter.updateState();

        // verify getter again
        Mockito.verify(mockedIElevator).getElevatorAccel(1); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorAccel(2); // elevator 2

        // verify that mqtt topic has been published because state has changed
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorAcceleration");
        Mockito.verify(mockedDummyMQTT).Publish("elevators/2/ElevatorAcceleration");
    }

    @Test
    public void testUpdateElevatorButton() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        for (int floor = 0; floor < FloorCnt; floor++) {
            // define what the return parameters
            Mockito.when(mockedIElevator.getElevatorButton(1, floor)).thenReturn(false);
            Mockito.when(mockedIElevator.getElevatorButton(2, floor)).thenReturn(false);
        
            // update state
            adapter.updateState();

            // verify that getter is called
            Mockito.verify(mockedIElevator).getElevatorButton(1, floor); // elevator 1 
            Mockito.verify(mockedIElevator).getElevatorButton(2, floor); // elevator 2

            // change what the return parameters
            Mockito.when(mockedIElevator.getElevatorButton(1, floor)).thenReturn(true);
            Mockito.when(mockedIElevator.getElevatorButton(2, floor)).thenReturn(true);

            // update again
            adapter.updateState();

            // verify getter again
            Mockito.verify(mockedIElevator).getElevatorButton(1, floor); // elevator 1 
            Mockito.verify(mockedIElevator).getElevatorButton(2, floor); // elevator 2

            // verify that mqtt topic has been published because state has changed
            Mockito.verify(mockedDummyMQTT).Publish("elevators/1/FloorRequested/" + floor);
            Mockito.verify(mockedDummyMQTT).Publish("elevators/2/FloorRequested/" + floor);
        }
    }

    @Test
    public void testUpdateElevatorDoorStatus() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        // define what the return parameters
        Mockito.when(mockedIElevator.getElevatorDoorStatus(1)).thenReturn(IElevator.ELEVATOR_DOORS_CLOSED);
        Mockito.when(mockedIElevator.getElevatorDoorStatus(2)).thenReturn(IElevator.ELEVATOR_DOORS_OPEN);
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorDoorStatus(1); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorDoorStatus(2); // elevator 2

        // change what the return parameters
        Mockito.when(mockedIElevator.getElevatorDoorStatus(1)).thenReturn(IElevator.ELEVATOR_DOORS_OPENING);
        Mockito.when(mockedIElevator.getElevatorDoorStatus(2)).thenReturn(IElevator.ELEVATOR_DOORS_CLOSING);

        // update again
        adapter.updateState();

        // verify getter again
        Mockito.verify(mockedIElevator).getElevatorDoorStatus(1); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorDoorStatus(2); // elevator 2

        // verify that mqtt topic has been published because state has changed
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorDoorStatus");
        Mockito.verify(mockedDummyMQTT).Publish("elevators/2/ElevatorDoorStatus");
    }

    @Test
    public void testUpdateElevatorFloor() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        // define what the return parameters
        Mockito.when(mockedIElevator.getElevatorFloor(1)).thenReturn(0);
        Mockito.when(mockedIElevator.getElevatorFloor(2)).thenReturn(FloorCnt-1);
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorFloor(1); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorFloor(2); // elevator 2

        // change what the return parameters
        Mockito.when(mockedIElevator.getElevatorFloor(1)).thenReturn(FloorCnt-1);
        Mockito.when(mockedIElevator.getElevatorFloor(2)).thenReturn(0);

        // update again
        adapter.updateState();

        // verify getter again
        Mockito.verify(mockedIElevator).getElevatorFloor(1); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorFloor(2); // elevator 2

        // verify that mqtt topic has been published because state has changed
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorCurrentFloor");
        Mockito.verify(mockedDummyMQTT).Publish("elevators/2/ElevatorCurrentFloor");
    }

    @Test
    public void testUpdateElevatorPosition() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        // define what the return parameters
        Mockito.when(mockedIElevator.getElevatorPosition(1)).thenReturn(0);
        Mockito.when(mockedIElevator.getElevatorPosition(2)).thenReturn(10); // 10 feet height
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorPosition(1); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorPosition(2); // elevator 2

        // change what the return parameters
        Mockito.when(mockedIElevator.getElevatorPosition(1)).thenReturn(4);
        Mockito.when(mockedIElevator.getElevatorPosition(2)).thenReturn(0);

        // update again
        adapter.updateState();

        // verify getter again
        Mockito.verify(mockedIElevator).getElevatorPosition(1); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorPosition(2); // elevator 2

        // verify that mqtt topic has been published because state has changed
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorCurrentHeight");
        Mockito.verify(mockedDummyMQTT).Publish("elevators/2/ElevatorCurrentHeight");
    }

    @Test
    public void testUpdateElevatorSpeed() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        // define what the return parameters
        Mockito.when(mockedIElevator.getElevatorSpeed(1)).thenReturn(0);
        Mockito.when(mockedIElevator.getElevatorSpeed(2)).thenReturn(10); // 10 m/s
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorSpeed(1); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorSpeed(2); // elevator 2

        // change what the return parameters
        Mockito.when(mockedIElevator.getElevatorSpeed(1)).thenReturn(4);
        Mockito.when(mockedIElevator.getElevatorSpeed(2)).thenReturn(0);

        // update again
        adapter.updateState();

        // verify getter again
        Mockito.verify(mockedIElevator).getElevatorSpeed(1); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorSpeed(2); // elevator 2

        // verify that mqtt topic has been published because state has changed
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorCurrentSpeed");
        Mockito.verify(mockedDummyMQTT).Publish("elevators/2/ElevatorCurrentSpeed");
    }

    @Test
    public void testUpdateElevatorWeight() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        // define what the return parameters
        Mockito.when(mockedIElevator.getElevatorWeight(1)).thenReturn(0);
        Mockito.when(mockedIElevator.getElevatorWeight(2)).thenReturn(10); // 10 kg
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorWeight(1); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorWeight(2); // elevator 2

        // change what the return parameters
        Mockito.when(mockedIElevator.getElevatorWeight(1)).thenReturn(4);
        Mockito.when(mockedIElevator.getElevatorWeight(2)).thenReturn(0);

        // update again
        adapter.updateState();

        // verify getter again
        Mockito.verify(mockedIElevator).getElevatorWeight(1); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorWeight(2); // elevator 2

        // verify that mqtt topic has been published because state has changed
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorCurrentPassengersWeight");
        Mockito.verify(mockedDummyMQTT).Publish("elevators/2/ElevatorCurrentPassengersWeight");
    }

    @Test
    public void testUpdateFloorButtonUpDown() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        for (int floor = 0; floor < FloorCnt; floor++) {
            // define what the return parameters
            Mockito.when(mockedIElevator.getFloorButtonUp(floor)).thenReturn(false);
            Mockito.when(mockedIElevator.getFloorButtonDown(floor)).thenReturn(false);
        
            // update state
            adapter.updateState();

            // verify that getter is called
            Mockito.verify(mockedIElevator).getFloorButtonUp(floor); // elevator 1 
            Mockito.verify(mockedIElevator).getFloorButtonDown(floor); // elevator 2

            // change what the return parameters
            Mockito.when(mockedIElevator.getFloorButtonUp(floor)).thenReturn(true);
            Mockito.when(mockedIElevator.getFloorButtonDown(floor)).thenReturn(true);

            // update again
            adapter.updateState();

            // verify getter again
            Mockito.verify(mockedIElevator).getFloorButtonUp(floor); // elevator 1 
            Mockito.verify(mockedIElevator).getFloorButtonDown(floor); // elevator 2

            // verify that mqtt topic has been published because state has changed
            Mockito.verify(mockedDummyMQTT).Publish("floors/" + floor + "/ButtonUpPressed/true");
            Mockito.verify(mockedDummyMQTT).Publish("floors/" + floor + "/ButtonDownPressed/true");
        }
    }

    @Test
    public void testUpdateServicedFloors() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        for (int floor = 0; floor < FloorCnt; floor++) {
            // define what the return parameters
            Mockito.when(mockedIElevator.getServicesFloors(1, floor)).thenReturn(true);
            Mockito.when(mockedIElevator.getServicesFloors(2, floor)).thenReturn(true);
        
            // update state
            adapter.updateState();

            // verify that getter is called
            Mockito.verify(mockedIElevator).getServicesFloors(1, floor); // elevator 1 
            Mockito.verify(mockedIElevator).getServicesFloors(2, floor); // elevator 2

            // change what the return parameters
            Mockito.when(mockedIElevator.getServicesFloors(1, floor)).thenReturn(false);
            Mockito.when(mockedIElevator.getServicesFloors(2, floor)).thenReturn(false);

            // update again
            adapter.updateState();

            // verify getter again
            Mockito.verify(mockedIElevator).getServicesFloors(1, floor); // elevator 1 
            Mockito.verify(mockedIElevator).getServicesFloors(2, floor); // elevator 2

            // verify that mqtt topic has been published because state has changed
            Mockito.verify(mockedDummyMQTT).Publish("elevators/1/FloorServiced/" + floor);
            Mockito.verify(mockedDummyMQTT).Publish("elevators/2/FloorServiced/" + floor);
        }
    }

    @Test
    public void testUpdateTarget() throws RemoteException {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        // define what the return parameters
        Mockito.when(mockedIElevator.getTarget(1)).thenReturn(0);
        Mockito.when(mockedIElevator.getTarget(2)).thenReturn(0); // 10 kg
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getTarget(1); // elevator 1 
        Mockito.verify(mockedIElevator).getTarget(2); // elevator 2

        // change what the return parameters
        Mockito.when(mockedIElevator.getTarget(1)).thenReturn(FloorCnt-1);
        Mockito.when(mockedIElevator.getTarget(2)).thenReturn(FloorCnt-1);

        // update again
        adapter.updateState();

        // verify getter again
        Mockito.verify(mockedIElevator).getTarget(1); // elevator 1 
        Mockito.verify(mockedIElevator).getTarget(2); // elevator 2

        // verify that mqtt topic has been published because state has changed
        Mockito.verify(mockedDummyMQTT).Publish("elevators/1/ElevatorTargetFloor");
        Mockito.verify(mockedDummyMQTT).Publish("elevators/2/ElevatorTargetFloor");
    }
}
