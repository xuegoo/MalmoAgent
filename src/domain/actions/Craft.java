package domain.actions;

import com.microsoft.msr.malmo.AgentHost;
import domain.AbstractAction;
import domain.AtomicFluent;
import domain.fluents.HasNumberOfItem;
import javafx.util.Pair;
import main.Observations;

import java.util.*;

public class Craft extends AbstractAction {
    public final static Map<String, List<Pair<String, Integer>>> CRAFTS;
    static {
        Map<String, List<Pair<String, Integer>>> crafts = new HashMap<>();
        crafts.put("diamond_pixaxe", Collections.unmodifiableList(Arrays.asList(new Pair<>("diamond", 3), new Pair<>("stick", 2))));
        //TODO fill the necessary crafts
        CRAFTS = Collections.unmodifiableMap(crafts);
    }

    private final String mItem;

    public Craft(AgentHost agentHost, String item) {
        super(agentHost);
        mItem = item;
        if (!CRAFTS.containsKey(item))
            throw new IllegalArgumentException("The item " + item + " cannot be crafted! (or isn't in the list of crafts");
        String name;
        int quantity;
        for(Pair<String, Integer> pair : CRAFTS.get(item)){
            name = pair.getKey();
            quantity = pair.getValue();
            this.preconditions.add(new HasNumberOfItem(name, quantity));
            this.effects.add(new HasNumberOfItem(name, getObservations().numberOf(name) - quantity));
        }
        this.effects.add(new HasNumberOfItem(item, getObservations().numberOf(item) + 1));
    }

    @Override
    public void doAction(Observations observations) {
        agentHost.sendCommand("craft "+mItem);
    }
}
