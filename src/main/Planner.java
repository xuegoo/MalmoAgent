package main;

import com.microsoft.msr.malmo.AgentHost;
import com.microsoft.msr.malmo.WorldState;
import domain.Action;
import domain.ActionFactory;
import domain.AtomicFluent;
import domain.ObservationFactory;
import domain.fluents.Have;
import domain.fluents.HaveSelected;
import domain.fluents.IsAt;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Created by Mart on 8.10.2017.
 */
public class Planner {
    private final AtomicFluent currentGoal;
    private final ActionFactory factory;
    private final AgentHost agentHost;
    private List<Action> plan;
    private Observations planObservation;

    public Planner(AtomicFluent currentGoal, AgentHost agentHost) {
        this.currentGoal = currentGoal;
        this.agentHost = agentHost;
        this.factory = new ActionFactory(agentHost);
        planObservation = ObservationFactory.getObservations(agentHost);
        this.plan = determinePlan(currentGoal);
        System.out.println(plan);
    }

    public void execute() {
        WorldState worldState = agentHost.getWorldState();
        while (plan.size() > 0 && worldState.getIsMissionRunning()) {
            Action action = plan.get(0);
            if (action.preconditionsMet()) {
                Action remove = plan.remove(0);
                if (!remove.effectsCompleted() || remove.getEffects().size() == 0) {
                    remove.perform();
                }
            } else {
                List<Action> actions = satisfyConditions(action); //Reevaluate if our preconditions are not met for some reason
                actions.addAll(plan);
                plan = actions;
            }
            System.out.println(plan);
        }
        System.out.println("Done executing");
    }

    public List<Action> determinePlan(AtomicFluent goal) {
        return evaluate(goal);
    }

    private List<Action> evaluate(AtomicFluent fluent) {
        List<Action> actions = factory.createPossibleActions(fluent);
        if (actions.size() < 1) {
            throw new IllegalStateException("I dont know how to solve this");
        }
        Action bestAction = findCheapest(actions);

        if (bestAction.getPreconditions().size() == 0) {
            ArrayList<Action> determinedList = new ArrayList<>();
            determinedList.add(bestAction);
            return determinedList;
        }

        List<Action> collect = satisfyConditions(bestAction);
        collect.add(bestAction);
        updatePlanObservation(bestAction);
        return collect;
    }

    private void updatePlanObservation(Action bestAction) {
        bestAction.getEffects().forEach(effect -> {
            if(effect instanceof Have){
                Have have = (Have) effect;
                int i = planObservation.items.indexOf((have.getItem()));
                if(i != -1){
                    planObservation.nbItems.set(i, have.getmNumberOf());
                } else {
                    int air = planObservation.items.indexOf("air");
                    planObservation.items.set(air, have.getItem());
                    planObservation.nbItems.set(air, have.getmNumberOf());
                }
            }else if(effect instanceof HaveSelected){
                HaveSelected haveSelected = (HaveSelected) effect;
                int i = planObservation.items.indexOf(haveSelected.getItem());
                String itemSwapped = planObservation.items.get(0);
                int quantitySwapped = planObservation.nbItems.get(0);
                planObservation.items.set(0, planObservation.items.get(i));
                planObservation.nbItems.set(0, planObservation.nbItems.get(i));
                planObservation.items.set(i, itemSwapped);
                planObservation.nbItems.set(i, quantitySwapped);
            }else if(effect instanceof IsAt){
                IsAt isAt = (IsAt) effect;
                planObservation.XPos = isAt.getX();
                planObservation.YPos = isAt.getY();
                planObservation.ZPos = isAt.getZ();
            }
        });
    }

    private List<Action> satisfyConditions(Action bestAction) {
        List<Action> test = bestAction.getPreconditions().stream()
                .filter(precondition -> !precondition.test(planObservation))
                .map(this::evaluate)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
        return test;
    }

    private Action findCheapest(List<Action> actions) {
        return actions.get(0); //Whatever, doesn't matter for now
    }
}
