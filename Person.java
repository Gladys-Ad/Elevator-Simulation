import java.util.concurrent.Semaphore;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Person implements Runnable {
	// semaphores for person
	private Semaphore waiting = new Semaphore(0, true);
	private Semaphore inElevator = new Semaphore(1, true);
	public Semaphore leave = new Semaphore(0, true);

	public static int numberOfPeople = 49;
	Random floorNumber = new Random();
	Thread activePerson;
	Elevator elevator;
	private int personId;
	private int origin = 1;
	private int destination;
	private boolean active = true;

	// initiate person id and assign destination
	public Person(int personId) {
		this.personId = personId;
		this.destination = floorNumber.nextInt(10 - 2 + 1) + 2;
	}

	public int getDestination() {
		return destination;
	}

	// decrement semaphore permit
	public void wait(Semaphore s) throws InterruptedException {
		s.acquire();
	}

	// increment semaphore permit
	public void signal(Semaphore s) {
		s.release();
	}

	// indicate which elevator person is to board
	public void elevatorToBoard(Elevator elevator) throws InterruptedException {
		this.elevator = elevator;
	}

	public void enterElevator() throws InterruptedException {
		System.out.printf("Person %d enters elevator to go to floor %d\n",
				personId, destination);
		// enter the elevator
		elevator.enter(this);
	}

	public void leaveElevator() {
		System.out.printf("Person %d leaves elevator\n", personId);
		// exit the elevator
		elevator.exit(this);
		// Person has reached destination so stop person's thread
		active = false;
	}

	public void start() {
		// if thread has not been started, start
		if (activePerson == null) {
			activePerson = new Thread(this);
			activePerson.start();
		}
	}

	public void run() {
		while (active) {
			try {
				// try to board elevator
				wait(elevator.getCapacity());

				// move into elevator
				signal(waiting);
				enterElevator();

				// change status to indicate you are inside elevator
				wait(inElevator);

				// wait inside elevator until person gets to destination
				wait(waiting);

				// last person to board in each batch will let elevator know it
				// can proceed to close door
				signal(elevator.doneBoarding);

				// waiting to get signal from elevator that person is at
				// destination
				wait(leave);

				// no longer waiting
				signal(waiting);

				// leaves elevator
				signal(inElevator);
				leaveElevator();

				// tell elevator person has left
				signal(leave);

				// update numberOfPeople left
				numberOfPeople--;

			} catch (InterruptedException ex) {
				Logger.getLogger(Person.class.getName()).log(Level.SEVERE,
						null, ex);
			}

		}
	}
}
