package at.fhhagenberg.sqelevator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import at.fhhagenberg.sqelevator.Building;
import at.fhhagenberg.sqelevator.ElevatorDataModell;

class BuildingTest {

  private Building building;

  @BeforeEach
  void setUp() {
    List<Integer> maxPassengers = new ArrayList<>(3);
    maxPassengers.add(10);
    maxPassengers.add(10);
    maxPassengers.add(10);
    building = new Building(3, 5, maxPassengers); // 3 elevators, 5 floors, max 10 passengers
  }

  @Test
  void testGetElevators() {
    List<ElevatorDataModell> elevators = building.getElevators();
    assertNotNull(elevators);
    assertEquals(3, elevators.size());
  }

  @Test
  void testGetElevator() {
    ElevatorDataModell elevator = building.getElevator(0);
    assertNotNull(elevator);
    assertEquals(0, elevator.getElevatorNumber());
  }

  @Test
  void testGetNrElevators() {
    assertEquals(3, building.getNrElevators());
  }

  @Test
  void testGetNrFloors() {
    assertEquals(5, building.getNrFloors());
  }

  @Test
  void testGetMaxPassengers() {
    assertEquals(10, building.getMaxPassengers());
  }

  @Test
  void testUpdateElevatorDirection() {
    building.updateElevatorDirection(1, 1);
    assertEquals(1, building.getElevator(1).getDirection());

    assertThrows(IllegalArgumentException.class, () -> building.updateElevatorDirection(-1, 1));
    assertThrows(IllegalArgumentException.class, () -> building.updateElevatorDirection(4, 1));
  }

  @Test
  void testUpdateElevatorDoorStatus() {
    building.updateElevatorDoorStatus(2, 1);
    assertEquals(1, building.getElevator(2).getDoorStatus());

    assertThrows(IllegalArgumentException.class, () -> building.updateElevatorDoorStatus(-1, 1));
    assertThrows(IllegalArgumentException.class, () -> building.updateElevatorDoorStatus(5, 1));
  }

  @Test
  void testUpdateElevatorTargetFloor() {
    building.updateElevatorTargetFloor(0, 3);
    assertEquals(3, building.getElevator(0).getTargetFloor());

    assertThrows(IllegalArgumentException.class, () -> building.updateElevatorTargetFloor(-1, 3));
    assertThrows(IllegalArgumentException.class, () -> building.updateElevatorTargetFloor(3, 3));
  }

  @Test
  void testUpdateElevatorCurrentFloor() {
    building.updateElevatorCurrentFloor(1, 2);
    assertEquals(2, building.getElevator(1).getCurrentFloor());

    assertThrows(IllegalArgumentException.class, () -> building.updateElevatorCurrentFloor(-1, 2));
    assertThrows(IllegalArgumentException.class, () -> building.updateElevatorCurrentFloor(4, 2));
  }

  @Test
  void testUpdateElevatorAcceleration() {
    building.updateElevatorAcceleration(0, 5);
    assertEquals(5, building.getElevator(0).getAcceleration());

    assertThrows(IllegalArgumentException.class, () -> building.updateElevatorAcceleration(-1, 5));
    assertThrows(IllegalArgumentException.class, () -> building.updateElevatorAcceleration(3, 5));
  }

  @Test
  void testUpdateElevatorSpeed() {
    building.updateElevatorSpeed(2, 15);
    assertEquals(15, building.getElevator(2).getSpeed());

    assertThrows(IllegalArgumentException.class, () -> building.updateElevatorSpeed(-1, 15));
    assertThrows(IllegalArgumentException.class, () -> building.updateElevatorSpeed(3, 15));
  }

  @Test
  void testUpdateElevatorFloorRequested() {
    // Test valid elevator number and floor request update
    building.updateElevatorFloorRequested(0, 3, true);
    assertTrue(building.getElevator(0).getFloorRequested(3));

    building.updateElevatorFloorRequested(1, 2, false);
    assertFalse(building.getElevator(1).getFloorRequested(2));

    // Test invalid elevator numbers
    assertThrows(IllegalArgumentException.class, () -> building.updateElevatorFloorRequested(-1, 3, true));
    assertThrows(IllegalArgumentException.class, () -> building.updateElevatorFloorRequested(2, 6, true));
  }

  @Test
  void testUpdateElevatorFloorToService() {
    // Test valid elevator number and floor to service update
    building.updateElevatorFloorToService(0, 4, true);
    assertTrue(building.getElevator(0).getFloorToService(4));

    building.updateElevatorFloorToService(1, 1, false);
    assertFalse(building.getElevator(1).getFloorToService(1));

    // Test invalid elevator numbers
    assertThrows(IllegalArgumentException.class, () -> building.updateElevatorFloorToService(-1, 4, true));
    assertThrows(IllegalArgumentException.class, () -> building.updateElevatorFloorToService(2, 6, true));
  }

  @Test
  void testUpdateElevatorCurrentHeight() {
    building.updateElevatorCurrentHeight(1, 120);
    assertEquals(120, building.getElevator(1).getCurrentHeight());

    assertThrows(IllegalArgumentException.class, () -> building.updateElevatorCurrentHeight(-1, 120));
    assertThrows(IllegalArgumentException.class, () -> building.updateElevatorCurrentHeight(3, 120));
  }

  @Test
  void testUpdateElevatorCurrentPassengersWeight() {
    building.updateElevatorCurrentPassengersWeight(2, 500);
    assertEquals(500, building.getElevator(2).getCurrentPassengersWeight());

    assertThrows(IllegalArgumentException.class, () -> building.updateElevatorCurrentPassengersWeight(-1, 500));
    assertThrows(IllegalArgumentException.class, () -> building.updateElevatorCurrentPassengersWeight(3, 500));
  }

  @Test
  void testCopyCTor() {
    Building buildingCopy = new Building(building);
    assertEquals(building.getNrElevators(), buildingCopy.getNrElevators());
    assertEquals(building.getNrFloors(), buildingCopy.getNrFloors());
    assertEquals(building.getMaxPassengers(), buildingCopy.getMaxPassengers());
  }

  @Test
  void testCopyCTorException() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      new Building(null);
    });
    assertEquals("Building cannot be null", exception.getMessage());
  }

  @Test
  void testUpdateElevatorDirectionInvalidElevator() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      building.updateElevatorDirection(-1, 1);
    });
    assertEquals("Invalid Elevator Number", exception.getMessage());

    exception = assertThrows(IllegalArgumentException.class, () -> {
      building.updateElevatorDirection(3, 1);
    });
    assertEquals("Invalid Elevator Number", exception.getMessage());
  }

  @Test
  void testUpdateElevatorDoorStatusInvalidElevator() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      building.updateElevatorDoorStatus(-1, 1);
    });
    assertEquals("Invalid Elevator Number", exception.getMessage());

    exception = assertThrows(IllegalArgumentException.class, () -> {
      building.updateElevatorDoorStatus(3, 1);
    });
    assertEquals("Invalid Elevator Number", exception.getMessage());
  }

  @Test
  void testUpdateElevatorTargetFloorInvalidElevator() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      building.updateElevatorTargetFloor(-1, 1);
    });
    assertEquals("Invalid Elevator Number", exception.getMessage());

    exception = assertThrows(IllegalArgumentException.class, () -> {
      building.updateElevatorTargetFloor(3, 1);
    });
    assertEquals("Invalid Elevator Number", exception.getMessage());
  }

  @Test
  void testUpdateElevatorCurrentFloorInvalidElevator() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      building.updateElevatorCurrentFloor(-1, 1);
    });
    assertEquals("Invalid Elevator Number", exception.getMessage());

    exception = assertThrows(IllegalArgumentException.class, () -> {
      building.updateElevatorCurrentFloor(3, 1);
    });
    assertEquals("Invalid Elevator Number", exception.getMessage());
  }

  @Test
  void testUpdateUpButtonStateInvalidFloor() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      building.updateUpButtonState(-1, true);
    });
    assertEquals("Invalid Floor Number Was -1", exception.getMessage());

    exception = assertThrows(IllegalArgumentException.class, () -> {
      building.updateUpButtonState(6, true);
    });
    assertEquals("Invalid Floor Number Was 6", exception.getMessage());
  }

  @Test
  void testUpdateDownButtonStateInvalidFloor() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      building.updateDownButtonState(-1, true);
    });
    assertEquals("Invalid Floor Number Was -1", exception.getMessage());

    exception = assertThrows(IllegalArgumentException.class, () -> {
      building.updateDownButtonState(6, true);
    });
    assertEquals("Invalid Floor Number Was 6", exception.getMessage());
  }

  @Test
  void testGetUpButtonStateInvalidFloor() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      building.getUpButtonState(-1);
    });
    assertEquals("Invalid Floor Number Was -1", exception.getMessage());

    exception = assertThrows(IllegalArgumentException.class, () -> {
      building.getUpButtonState(6);
    });
    assertEquals("Invalid Floor Number Was 6", exception.getMessage());
  }

  @Test
  void testGetDownButtonStateInvalidFloor() {
    Exception exception = assertThrows(IllegalArgumentException.class, () -> {
      building.getDownButtonState(-1);
    });
    assertEquals("Invalid Floor Number Was -1", exception.getMessage());

    exception = assertThrows(IllegalArgumentException.class, () -> {
      building.getDownButtonState(6);
    });
    assertEquals("Invalid Floor Number Was 6", exception.getMessage());
  }

}
