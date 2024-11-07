package at.fhhagenberg.sqelevator;

import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import at.fhhagenberg.sqelevator.DummyMQTT;
import at.fhhagenberg.sqelevator.ElevatorsMQTTAdapter;
import at.fhhagenberg.sqelevator.IElevator;

@ExtendWith(MockitoExtension.class)
public class TestElevatorsMQTTAdapter {
    
    @Mock
    private IElevator mockedIElevator;

    @Mock
    private DummyMQTT mockedDummyMQTT;

    @BeforeAll 
    public void testSetup() {

        // invariants
        Mockito.when(mockedIElevator).getElevatorNum().thenAnswer(2);
        Mockito.when(mockedIElevator).getFloorNum().thenAnswer(6);
        Mockito.when(mockedIElevator).getElevatorCapacity().thenAnswer(10);
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
}
