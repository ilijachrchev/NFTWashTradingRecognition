import mpi.MPI;

public class DistributedMain {

    public static void main(String[] args) {
        MPI.Init(args);

        int rank = MPI.COMM_WORLD.Rank();
        int size = MPI.COMM_WORLD.Size();

        System.out.println("Hello from rank: " + rank + " of size " + size);

        MPI.Finalize();
    }
}
