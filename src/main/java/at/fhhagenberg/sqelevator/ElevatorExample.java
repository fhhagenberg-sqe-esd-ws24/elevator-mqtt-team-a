package at.fhhagenberg.sqelevator;

import java.rmi.Naming;
import java.rmi.RemoteException;

import sqelevator.IElevator;

import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;


public class ElevatorExample {

	private static Logger logger = LogManager.getLogger(ElevatorExample.class);

	private IElevator controller;

	public ElevatorExample(IElevator controller) {
		this.controller = controller;
	}

	public static void main(String[] args) {

		try {
			IElevator controller = (IElevator) Naming.lookup("rmi://localhost/ElevatorSim");
			ElevatorExample client = new ElevatorExample(controller);

			client.displayElevatorSettings();
			client.runExample();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void displayElevatorSettings() throws RemoteException {
		logger.info("ELEVATOR SETTINGS");
		
		logger.info("Current clock tick: " + controller.getClockTick());
		
		logger.info("Number of elevators: " + controller.getElevatorNum());
		logger.info("Number of floor: " + controller.getFloorNum());
		logger.info("Floor height: " + controller.getFloorHeight());
		
		String output  = new String("Floor buttons Up pressed: ");
		for (int floor=0; floor<controller.getFloorNum(); floor++) {
			output += (controller.getFloorButtonUp(floor) ? "1" : "0");
		}
		logger.info(output);

		output  = new String("Floor buttons Down pressed: ");
		for (int floor=0; floor<controller.getFloorNum(); floor++) {
			output += (controller.getFloorButtonDown(floor) ? "1" : "0");
		}
		logger.info(output);		
		
		for (int elevator=0; elevator<controller.getElevatorNum(); elevator++) {
			logger.info("Settings of elevator number: " + elevator);
			logger.info("  Floor: " + controller.getElevatorFloor(elevator));
			logger.info("  Position: " + controller.getElevatorPosition(elevator));
			logger.info("  Target: " + controller.getTarget(elevator));
			logger.info("  Committed direction: " + controller.getCommittedDirection(elevator));
			logger.info("  Door status: " + controller.getElevatorDoorStatus(elevator));		
			logger.info("  Speed: " + controller.getElevatorSpeed(elevator));
			logger.info("  Acceleration: " + controller.getElevatorAccel(elevator));
			logger.info("  Capacity: " + controller.getElevatorCapacity(elevator));
			logger.info("  Weight: " + controller.getElevatorWeight(elevator));
			output  = new String("  Buttons pressed: ");
			for (int floor=0; floor<controller.getFloorNum(); floor++) {
				output += (controller.getElevatorButton(elevator, floor) ? "1" : "0");
			}
			logger.info(output);
		}
		
	}

	private void runExample() throws RemoteException {
		
		final int elevator = 0;
		final int numberOfFloors = controller.getFloorNum();
		final int sleepTime = 60;
		
		// First: Starting from ground floor, go up to the top floor, stopping in each floor
		
		// Set the committed direction displayed on the elevator to up
		controller.setCommittedDirection(elevator, IElevator.ELEVATOR_DIRECTION_UP);
		
		for (int nextFloor=1; nextFloor<numberOfFloors; nextFloor++) {
			
			// Set the target floor to the next floor above
			controller.setTarget(elevator, nextFloor);
			
			// Wait until closest floor is the target floor and speed is back to zero 
			while (controller.getElevatorFloor(elevator) < nextFloor || controller.getElevatorSpeed(elevator) > 0) {
				try { 
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {}
			}
			
			// Wait until doors are open before setting the next direction
			while (controller.getElevatorDoorStatus(elevator) != IElevator.ELEVATOR_DOORS_OPEN) {
				try {
					Thread.sleep(sleepTime);
				} catch (InterruptedException e) {}
			}
		}
		
		// Second, go back from the top floor to the ground floor in one move
		
		// Set the committed direction displayed on the elevator to down
		controller.setCommittedDirection(elevator, IElevator.ELEVATOR_DIRECTION_DOWN);
		
		// Set the target floor to the ground floor (floor number 0)
		controller.setTarget(elevator, 0);
		
		// Wait until ground floor has been reached
		while (controller.getElevatorFloor(elevator) > 0 || controller.getElevatorSpeed(elevator) > 0) {
			try { 
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {}
		}
		
		// Set the committed direction to uncommitted when back at the ground floor
		controller.setCommittedDirection(elevator, IElevator.ELEVATOR_DIRECTION_UNCOMMITTED);
		
	}
	
}