package model;

import java.util.HashMap;
import java.util.Map;

public class AddressMapper {

    private final Map<String, Integer> addressToId;
    private final Map<Integer, String> idToAddress;
    private int nextId;


    public AddressMapper(Map<String, Integer> addressToId, Map<Integer, String> idToAddress) {
        this.addressToId = new HashMap<>(16_000_000);
        this.idToAddress = new HashMap<>(16_000_000);
        this.nextId = 0;
    }

    public int getOrCreateId(String address) {

        // handle wrong addresses
        if(address == null || address.isEmpty()) {
            return -1;
        }

        //addr are case insensitive
        String normalized = address.toLowerCase();


        // check the addr
        Integer existingId = addressToId.get(normalized);
        if(existingId != null) {
            return existingId;          //reuse same addr for existing
        }

        int newId = nextId++;

        addressToId.put(normalized, newId);
        idToAddress.put(newId, address);
        return newId;
    }
}
