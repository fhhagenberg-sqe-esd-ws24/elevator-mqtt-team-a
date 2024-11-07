import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.ArrayList;
import at.fhhagenberg.sqelevator.ElevatorDataModell;

class ElevatorDataModellTest {

  private ElevatorDataModell elevatorDataModel;

  @BeforeEach
  void setUp() {
    elevatorDataModel = new ElevatorDataModell(1, 10, 15);
  }

  @Test
  void testGetAndSetDirection() {
    elevatorDataModel.setDirection(1);
    assertEquals(1, elevatorDataModel.getDirection());

    elevatorDataModel.setDirection(-1);
    assertEquals(-1, elevatorDataModel.getDirection());
  }

  @Test
  void testGetAndSetDoorStatus() {
    elevatorDataModel.setDoorStatus(1);
    assertEquals(1, elevatorDataModel.getDoorStatus());

    elevatorDataModel.setDoorStatus(0);
    assertEquals(0, elevatorDataModel.getDoorStatus());
  }

  @Test
  void testGetAndSetTargetFloor() {
    elevatorDataModel.setTargetFloor(5);
    assertEquals(5, elevatorDataModel.getTargetFloor());
  }

  @Test
  void testGetAndSetCurrentFloor() {
    elevatorDataModel.setCurrentFloor(3);
    assertEquals(3, elevatorDataModel.getCurrentFloor());
  }

  @Test
  void testGetAndSetAcceleration() {
    elevatorDataModel.setAcceleration(2);
    assertEquals(2, elevatorDataModel.getAcceleration());
  }

  @Test
  void testGetAndSetSpeed() {
    elevatorDataModel.setSpeed(10);
    assertEquals(10, elevatorDataModel.getSpeed());
  }

  @Test
  void testSetAndGetFloorsRequested() {
    elevatorDataModel.setFloorRequested(2);
    assertEquals(2, elevatorDataModel.getFloorsRequested().get(2));
  }

  @Test
  void testSetFloorRequestedInvalid() {
    assertThrows(IllegalArgumentException.class, () -> {
      elevatorDataModel.setFloorRequested(-1);
    });

    assertThrows(IllegalArgumentException.class, () -> {
      elevatorDataModel.setFloorRequested(15);
    });
  }

  @Test
  void testSetAndGetFloorsToService() {
    List<Integer> floorsToService = new ArrayList<>();
    floorsToService.add(0);
    floorsToService.add(1);
    elevatorDataModel.setFloorsToService(floorsToService);

    assertEquals(floorsToService, elevatorDataModel.getFloorsToService());
  }

  @Test
  void testGetAndSetCurrentHeight() {
    elevatorDataModel.setCurrentHeight(100);
    assertEquals(100, elevatorDataModel.getCurrentHeight());
  }

  @Test
  void testGetAndSetCurrentPassengersWeight() {
    elevatorDataModel.setCurrentPassengersWeight(600);
    assertEquals(600, elevatorDataModel.getCurrentPassengersWeight());
  }

  @Test
  void testGetMaxPassengers() {
    assertEquals(15, elevatorDataModel.getMaxPassengers());
  }

  @Test
  void testGetElevatorNumber() {
    assertEquals(1, elevatorDataModel.getElevatorNumber());
  }
}
