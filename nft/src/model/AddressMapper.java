package model;

import java.util.HashMap;
import java.util.Map;

public class AddressMapper {

    private final Map<String, Integer> addressToId;
    private int nextId;


    public AddressMapper() {
        this.addressToId = new HashMap<>();
        this.nextId = 0;
    }

    public int getOrCreateId(String address) {

        if(address == null || address.isEmpty()) {
            return -1;
        }

        //addr are case insensitive
        String normalized = address.trim().toLowerCase();

        // check the addr
        Integer existingId = addressToId.get(normalized);
        if(existingId != null) {
            return existingId;
        }

        int newId = nextId++;

        addressToId.put(normalized, newId);
        return newId;
    }

    public int size() {
        return nextId;
    }

    public void clear() {
        addressToId.clear();
    }
}
