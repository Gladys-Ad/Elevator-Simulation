public class MainThread {

	public static void main(String[] args) throws InterruptedException {
		// start elevator thread
		Elevator elevator = new Elevator();
		elevator.start();

		// start person threads
		for (int i = 0; i < 49; i++) {
			Person p = new Person(i);
			p.elevatorToBoard(elevator);
			p.start();
		}

	}
}
