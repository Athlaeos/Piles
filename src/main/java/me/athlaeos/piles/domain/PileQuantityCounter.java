package me.athlaeos.piles.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PileQuantityCounter {
    private final Map<UUID, Integer> pileQuantities = new HashMap<>();

    public Map<UUID, Integer> getPileQuantities() { return pileQuantities; }
}
