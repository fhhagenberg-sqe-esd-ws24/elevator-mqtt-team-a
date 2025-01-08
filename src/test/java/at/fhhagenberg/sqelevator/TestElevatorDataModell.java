package at.fhhagenberg.sqelevator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

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
  void testSetFloorRequestedInvalid() {
    assertThrows(IllegalArgumentException.class, () -> {
      elevatorDataModel.setFloorRequested(-1, true);
    });

    assertThrows(IllegalArgumentException.class, () -> {
      elevatorDataModel.setFloorRequested(15, true);
    });
  }

  @Test
  void testSetAndGetFloorToService() {
    elevatorDataModel.setFloorToService(3, true);
    elevatorDataModel.setFloorToService(3, false);

    assertFalse(elevatorDataModel.getFloorToService(3));
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

  @Test
  void testCopyCTor() {
    ElevatorDataModell elevatorDataModelCopy = new ElevatorDataModell(elevatorDataModel);
    assertEquals(elevatorDataModel.getElevatorNumber(), elevatorDataModelCopy.getElevatorNumber());
    assertEquals(elevatorDataModel.getMaxPassengers(), elevatorDataModelCopy.getMaxPassengers());
    assertEquals(elevatorDataModel.getCurrentPassengersWeight(), elevatorDataModelCopy.getCurrentPassengersWeight());
  }

  @Test
  void testGetFloorRequestedInvalidFloor() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      elevatorDataModel.getFloorRequested(-1);
    });
    assertEquals("Invalid Elevator Number", exception.getMessage());

    exception = assertThrows(IllegalArgumentException.class, () -> {
      elevatorDataModel.getFloorRequested(11);
    });
    assertEquals("Invalid Elevator Number", exception.getMessage());
  }

  @Test
  void testSetFloorRequestedInvalidFloor() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      elevatorDataModel.setFloorRequested(-1, true);
    });
    assertEquals("Invalid Floor Number requested", exception.getMessage());

    exception = assertThrows(IllegalArgumentException.class, () -> {
      elevatorDataModel.setFloorRequested(11, true);
    });
    assertEquals("Invalid Floor Number requested", exception.getMessage());
  }

  @Test
  void testGetFloorToServiceInvalidFloor() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      elevatorDataModel.getFloorToService(-1);
    });
    assertEquals("Invalid Floor Number", exception.getMessage());

    exception = assertThrows(IllegalArgumentException.class, () -> {
      elevatorDataModel.getFloorToService(11);
    });
    assertEquals("Invalid Floor Number", exception.getMessage());
  }

  @Test
  void testSetFloorToServiceInvalidFloor() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      elevatorDataModel.setFloorToService(-1, true);
    });
    assertEquals("Invalid Floor number", exception.getMessage());

    exception = assertThrows(IllegalArgumentException.class, () -> {
      elevatorDataModel.setFloorToService(11, true);
    });
    assertEquals("Invalid Floor number", exception.getMessage());
  }
}
