import java.util.ArrayList;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Elevator implements Runnable {
	// semaphores for elevator
	private Semaphore open = new Semaphore(0, true);
	private Semaphore move = new Semaphore(0, true);
	public Semaphore doneBoarding = new Semaphore(-6, true);
	private Semaphore elevatorCapacity = new Semaphore(0, true);

	// array list to keep track of the people who board and the people in the
	// elevator who need to exit at a particular floor
	ArrayList<Person> peopleInElevator = new ArrayList<Person>();
	ArrayList<Person> peopleToExit = new ArrayList<Person>();

	private int elevatorFloor = 1;
	private int capacity = 7;
	private boolean active = true;
	Thread activeElevator;
	Person person;
	Person lastPerson;

	public Semaphore getCapacity() {
		return elevatorCapacity;
	}

	public void resetDb() {
		Semaphore sem = new Semaphore(-6, true);
		doneBoarding = sem;
	}

	// decrement semaphore permit
	public void wait(Semaphore s) throws InterruptedException {
		s.acquire();
	}

	// increment semaphore permit
	public void signal(Semaphore s) {
		s.release();
	}

	// when person enters add person to array list
	public void enter(Person person) {
		peopleInElevator.add(person);
	}

	// when person exits remove person from array list
	public void exit(Person person) {
		peopleInElevator.remove(person);
	}

	public void openDoor() {
		System.out.printf("Elevator door opens at floor %d\n", elevatorFloor);
	}

	public void closeDoor() {
		System.out.println("Elevator door closes");
	}

	public void moveUp() {
		elevatorFloor++;
	}

	// update the number of people needing elevator service and go back to first
	// floor
	public void returnToFirstFloor() {
		elevatorFloor = 1;
	}

	// at each floor, check with persons to see if there's someone who needs to
	// exit, if so, add them to array list
	public void checkIfPersonExits() throws InterruptedException {
		for (int i = 0; i < peopleInElevator.size(); i++) {
			if (elevatorFloor == peopleInElevator.get(i).getDestination()) {
				peopleToExit.add(peopleInElevator.get(i));
			}
		}

		// if there are people who need to exit, keep not of the last person in
		// the list who will exit
		// this person is the one the elevator is waiting on before it can
		// proceed to close the door and move on
		if (peopleToExit.size() > 0) {
			int last = peopleToExit.size() - 1;
			lastPerson = peopleToExit.get(last);
		}
	}

	// indicate to the person who needs to exit that elevator is at their
	// destination
	public void notifyPersonToLeave() {
		for (Person p : peopleToExit) {
			p.leave.release();
		}
		peopleToExit.clear();
	}

	public void run() {
		while (active) {
			try {
				// open elevator door
				signal(open);
				openDoor();

				// allow limited people to enter
				elevatorCapacity.release(capacity);

				// check if Person's are done boarding
				wait(doneBoarding);

				// close door
				wait(open);
				closeDoor();

				// move
				signal(move);

				// take everyone in elevator to their destination
				while (peopleInElevator.size() > 0) {

					// go to next floor
					moveUp();

					// check if that floor is a destination where someone exits
					checkIfPersonExits();

					// if there are people that need to exit let them out
					if (peopleToExit.size() > 0) {

						// stop
						wait(move);

						// open door
						signal(open);
						openDoor();

						// tell person to exit because they are at destination
						notifyPersonToLeave();

						// wait until all person exit before proceeding to close
						// door
						wait(lastPerson.leave);

						// close door
						wait(open);
						closeDoor();

						// move
						signal(move);
					}
				}
				// exited while loop mean elevator delivered all the persons
				// inside the elevator
				// return to the first floor
				returnToFirstFloor();
				resetDb();

				// stop
				wait(move);

				// if there aren't any more persons at the first floor to board,
				// then elevator is done and can stop
				if (Person.numberOfPeople == 0) {
					active = false;
				}

			} catch (InterruptedException ex) {
				Logger.getLogger(Elevator.class.getName()).log(Level.SEVERE,
						null, ex);
			}

		}
		System.out.println("Simulation done\n");
	}

	public void start() {
		// if elevator thread has not been started, start it
		if (activeElevator == null) {
			activeElevator = new Thread(this);
			activeElevator.start();
		}
	}
}
