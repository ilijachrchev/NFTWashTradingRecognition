package builder;

import model.AddressMapper;
import model.Graph;

public class GraphBuilder {

    private final AddressMapper addressMapper;
    private final Graph graph;

    public GraphBuilder() {
        this.addressMapper = new AddressMapper();
        this.graph = new Graph();
    }





    public AddressMapper getAddressMapper() {
        return addressMapper;
    }
    public Graph getGraph() {
        return graph;
    }
}
