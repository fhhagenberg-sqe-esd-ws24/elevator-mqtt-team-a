package at.fhhagenberg.sqelevator;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import at.fhhagenberg.sqelevator.DummyMQTT;
import at.fhhagenberg.sqelevator.ElevatorsMQTTAdapter;
import at.fhhagenberg.sqelevator.IElevator;

import org.mockito.Mock;
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

    @BeforeAll 
    public void testSetup() {

        // invariants
        Mockito.when(mockedIElevator).getElevatorNum().thenAnswer(ElevatorCnt);
        Mockito.when(mockedIElevator).getFloorNum().thenAnswer(FloorCnt);
        Mockito.when(mockedIElevator).getElevatorCapacity().thenAnswer(ElevatorCapacity);
    }

    @Test
    public void testInitialGet() {

        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        // the constructor should call the following functions
        Mockito.verify(mockedIElevator).getElevatorNum();
        Mockito.verify(mockedIElevator).getFloorNum();
        Mockito.verify(mockedIElevator).getElevatorCapacity(1);
        Mockito.verify(mockedIElevator).getElevatorCapacity(2);
    }

    @Test
    public void testUpdateCommittedDirection() {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        // define what the return parameters
        Mockito.when(mockedIElevator).getCommittedDirection(1).thenAnswer(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED);
        Mockito.when(mockedIElevator).getCommittedDirection(2).thenAnswer(IElevator.ELEVATOR_DIRECTION_UNCOMMITTED);
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getCommittedDirection(1); // elevator 1 
        Mockito.verify(mockedIElevator).getCommittedDirection(2); // elevator 2

        // change what the return parameters
        Mockito.when(mockedIElevator).getCommittedDirection(1).thenAnswer(IElevator.ELEVATOR_DIRECTION_UP);
        Mockito.when(mockedIElevator).getCommittedDirection(2).thenAnswer(IElevator.ELEVATOR_DIRECTION_DOWN);

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
    public void testUpdateElevatorAccel() {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        // define what the return parameters
        Mockito.when(mockedIElevator).getElevatorAccel(1).thenAnswer(10);
        Mockito.when(mockedIElevator).getElevatorAccel(2).thenAnswer(5);
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorAccel(1); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorAccel(2); // elevator 2

        // change what the return parameters
        Mockito.when(mockedIElevator).getElevatorAccel(1).thenAnswer(2);
        Mockito.when(mockedIElevator).getElevatorAccel(2).thenAnswer(1);

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
    public void testUpdateElevatorButton() {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        for (int floor = 0; floor < FloorCnt; floor++) {
            // define what the return parameters
            Mockito.when(mockedIElevator).getElevatorButton(1, floor).thenAnswer(false);
            Mockito.when(mockedIElevator).getElevatorButton(2, floor).thenAnswer(false);
        
            // update state
            adapter.updateState();

            // verify that getter is called
            Mockito.verify(mockedIElevator).getElevatorButton(1, floor); // elevator 1 
            Mockito.verify(mockedIElevator).getElevatorButton(2, floor); // elevator 2

            // change what the return parameters
            Mockito.when(mockedIElevator).getElevatorButton(1, floor).thenAnswer(true);
            Mockito.when(mockedIElevator).getElevatorButton(2, floor).thenAnswer(true);

            // update again
            adapter.updateState();

            // verify getter again
            Mockito.verify(mockedIElevator).getElevatorButton(1); // elevator 1 
            Mockito.verify(mockedIElevator).getElevatorButton(2); // elevator 2

            // verify that mqtt topic has been published because state has changed
            Mockito.verify(mockedDummyMQTT).Publish("elevators/1/FloorRequested/" + floor);
            Mockito.verify(mockedDummyMQTT).Publish("elevators/2/FloorRequested/" + floor);
        }
    }

    @Test
    public void testUpdateElevatorDoorStatus() {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        // define what the return parameters
        Mockito.when(mockedIElevator).getElevatorDoorStatus(1).thenAnswer(IElevator.ELEVATOR_DOORS_CLOSED);
        Mockito.when(mockedIElevator).getElevatorDoorStatus(2).thenAnswer(IElevator.ELEVATOR_DOORS_OPEN);
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorDoorStatus(1); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorDoorStatus(2); // elevator 2

        // change what the return parameters
        Mockito.when(mockedIElevator).getElevatorDoorStatus(1).thenAnswer(IElevator.ELEVATOR_DOORS_OPENING);
        Mockito.when(mockedIElevator).getElevatorDoorStatus(2).thenAnswer(IElevator.ELEVATOR_DOORS_CLOSING);

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
    public void testUpdateElevatorFloor() {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        // define what the return parameters
        Mockito.when(mockedIElevator).getElevatorFloor(1).thenAnswer(0);
        Mockito.when(mockedIElevator).getElevatorFloor(2).thenAnswer(FloorCnt-1);
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorFloor(1); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorFloor(2); // elevator 2

        // change what the return parameters
        Mockito.when(mockedIElevator).getElevatorFloor(1).thenAnswer(FloorCnt-1);
        Mockito.when(mockedIElevator).getElevatorFloor(2).thenAnswer(0);

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
    public void testUpdateElevatorPosition() {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        // define what the return parameters
        Mockito.when(mockedIElevator).getElevatorPosition(1).thenAnswer(0);
        Mockito.when(mockedIElevator).getElevatorPosition(2).thenAnswer(10); // 10 feet height
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorPosition(1); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorPosition(2); // elevator 2

        // change what the return parameters
        Mockito.when(mockedIElevator).getElevatorPosition(1).thenAnswer(4);
        Mockito.when(mockedIElevator).getElevatorPosition(2).thenAnswer(0);

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
    public void testUpdateElevatorSpeed() {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        // define what the return parameters
        Mockito.when(mockedIElevator).getElevatorSpeed(1).thenAnswer(0);
        Mockito.when(mockedIElevator).getElevatorSpeed(2).thenAnswer(10); // 10 m/s
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorSpeed(1); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorSpeed(2); // elevator 2

        // change what the return parameters
        Mockito.when(mockedIElevator).getElevatorSpeed(1).thenAnswer(4);
        Mockito.when(mockedIElevator).getElevatorSpeed(2).thenAnswer(0);

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
    public void testUpdateElevatorWeight() {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        // define what the return parameters
        Mockito.when(mockedIElevator).getElevatorWeight(1).thenAnswer(0);
        Mockito.when(mockedIElevator).getElevatorWeight(2).thenAnswer(10); // 10 kg
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getElevatorWeight(1); // elevator 1 
        Mockito.verify(mockedIElevator).getElevatorWeight(2); // elevator 2

        // change what the return parameters
        Mockito.when(mockedIElevator).getElevatorWeight(1).thenAnswer(4);
        Mockito.when(mockedIElevator).getElevatorWeight(2).thenAnswer(0);

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
    public void testUpdateFloorButtonUpDown() {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        for (int floor = 0; floor < FloorCnt; floor++) {
            // define what the return parameters
            Mockito.when(mockedIElevator).getElevatorButtonUp(floor).thenAnswer(false);
            Mockito.when(mockedIElevator).getElevatorButtonDown(floor).thenAnswer(false);
        
            // update state
            adapter.updateState();

            // verify that getter is called
            Mockito.verify(mockedIElevator).getElevatorButtonUp(floor); // elevator 1 
            Mockito.verify(mockedIElevator).getElevatorButtonDown(floor); // elevator 2

            // change what the return parameters
            Mockito.when(mockedIElevator).getElevatorButtonUp(floor).thenAnswer(true);
            Mockito.when(mockedIElevator).getElevatorButtonDown(floor).thenAnswer(true);

            // update again
            adapter.updateState();

            // verify getter again
            Mockito.verify(mockedIElevator).getElevatorButtonUp(floor); // elevator 1 
            Mockito.verify(mockedIElevator).getElevatorButtonDown(floor); // elevator 2

            // verify that mqtt topic has been published because state has changed
            Mockito.verify(mockedDummyMQTT).Publish("floors/" + floor + "/ButtonUpPressed/true");
            Mockito.verify(mockedDummyMQTT).Publish("floors/" + floor + "/ButtonDownPressed/true");
        }
    }

    @Test
    public void testUpdateServicedFloors() {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        for (int floor = 0; floor < FloorCnt; floor++) {
            // define what the return parameters
            Mockito.when(mockedIElevator).getServicesFloors(1, floor).thenAnswer(true);
            Mockito.when(mockedIElevator).getServicesFloors(2, floor).thenAnswer(true);
        
            // update state
            adapter.updateState();

            // verify that getter is called
            Mockito.verify(mockedIElevator).getServicesFloors(1, floor); // elevator 1 
            Mockito.verify(mockedIElevator).getServicesFloors(2, floor); // elevator 2

            // change what the return parameters
            Mockito.when(mockedIElevator).getServicesFloors(1, floor).thenAnswer(false);
            Mockito.when(mockedIElevator).getServicesFloors(2, floor).thenAnswer(false);

            // update again
            adapter.updateState();

            // verify getter again
            Mockito.verify(mockedIElevator).getServicesFloors(1); // elevator 1 
            Mockito.verify(mockedIElevator).getServicesFloors(2); // elevator 2

            // verify that mqtt topic has been published because state has changed
            Mockito.verify(mockedDummyMQTT).Publish("elevators/1/FloorServiced/" + floor);
            Mockito.verify(mockedDummyMQTT).Publish("elevators/2/FloorServiced/" + floor);
        }
    }

    @Test
    public void testUpdateTarget() {
        ElevatorsMQTTAdapter adapter = new ElevatorsMQTTAdapter(mockedIElevator);

        // define what the return parameters
        Mockito.when(mockedIElevator).getTarget(1).thenAnswer(0);
        Mockito.when(mockedIElevator).getTarget(2).thenAnswer(0); // 10 kg
    
        // update state
        adapter.updateState();

        // verify that getter is called
        Mockito.verify(mockedIElevator).getTarget(1); // elevator 1 
        Mockito.verify(mockedIElevator).getTarget(2); // elevator 2

        // change what the return parameters
        Mockito.when(mockedIElevator).getTarget(1).thenAnswer(FloorCnt-1);
        Mockito.when(mockedIElevator).getTarget(2).thenAnswer(FloorCnt-1);

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
