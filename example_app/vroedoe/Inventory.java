package net.runelite.client.plugins.vroedoe;

import com.google.common.collect.Sets;
import net.runelite.api.Client;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import org.apache.commons.lang3.tuple.Pair;

import javax.inject.Inject;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

import static java.lang.Math.abs;
import static java.lang.Math.floor;

public class Inventory {

    @Inject
    private Client client;

    public int INVENTORY_SIZE = 28;
    public int freeSpace;

    public HashMap<Integer, TargetItem> carryingItems = new HashMap<>();
    public HashMap<Integer, TargetItem> requestedItems = new HashMap<>();
    public HashMap<Integer, TargetItem> redundantItems = new HashMap<>();
    public HashMap<Integer, TargetItem> missingItems = new HashMap<>();

    public Widget inventoryWidget = client.getWidget(WidgetInfo.INVENTORY);
    public int invSpaceNeeded;

    public boolean dropAll = false;
    public boolean bankAll = false;

    public int lastMenuOptionX;

    public void setLastMenuOptionX(int x) {
        this.lastMenuOptionX = x;
    }


    public static class TargetItem {

        public int id;
        public int amount;
        public boolean noted; // Unsupported
//        public boolean stackable;
//        public String name;

        public boolean equipped;

        public String withdrawalMethod;
        public String depositMethod;
        public boolean withdraw;
        public boolean deposit;
        public boolean pickup;
        public boolean equip;
        public boolean drop;
        public boolean alch;
        public boolean use;

        public ArrayList<Pair<Integer, Integer>> inventoryPositions;
        public ArrayList<Integer> inventoryIndeces;

        public TargetItem(int id, int amount, boolean noted) {

            this.withdraw = true;
            this.id = id;
            this.amount = amount;
            this.noted = noted; // Unsupported

            if (this.withdraw) {
                if (amount == 1) {
                    withdrawalMethod = "LEFT_CLICK";
                } else if (amount == 5) {
                    withdrawalMethod = "Withdraw-5";
                } else if (amount == 10) {
                    withdrawalMethod = "Withdraw-10";
                } else if (amount == lastMenuOptionX) {
                    withdrawalMethod = "Withdraw-" + amount;
                } else {
                    withdrawalMethod = "Withdraw-X";
                }
            }
        }
    }

    public void printers() {
        updateInventory();
        System.out.println(requestedItems.size() + ", " + carryingItems.size() + ", " + missingItems.size() + ", " + redundantItems.size());
        for (TargetItem item : carryingItems.values()) {
            System.out.println("Carrying " + item.amount + " of " + item.id + " at " + item.inventoryIndeces);
        }
        for (TargetItem item : missingItems.values()) {
            System.out.println("Missing " + item.amount + " of " + item.id);
        }
    }

    public int setFreeSpace() {
        carryingItems = setCarrying();
        freeSpace = INVENTORY_SIZE;
        for (TargetItem item : carryingItems.values()) {
            freeSpace -= item.inventoryIndeces.size();
        }
        return freeSpace;
    }

    public void setRequestedItems(HashMap<Integer, TargetItem> newRequestedItems) {
        requestedItems.clear();
        requestedItems = newRequestedItems;
        updateInventory();
        printers();
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    // TODO Breaks when some are noted and some are not of same id >>> id noted is id normal + 1?
    public void updateInventory() {
        carryingItems = setCarrying();
        missingItems.clear();
        redundantItems.clear();
        for (TargetItem requestedItem : requestedItems.values()) {
            if (!carryingItems.containsKey(requestedItem.id)) {
                missingItems.put(requestedItem.id, requestedItem);
            } else {
                TargetItem carryingItem = carryingItems.get(requestedItem.id);
                if (carryingItem.amount < requestedItem.amount) {
                    TargetItem missingItem = new TargetItem(
                            requestedItem.id,
                            requestedItem.amount - carryingItem.amount,
                            requestedItem.noted
                    );
                    missingItems.put(missingItem.id, missingItem);
                }
            }
        }
        for (TargetItem carryingItem : carryingItems.values()) {
            if (!requestedItems.containsKey(carryingItem.id)) {
                redundantItems.put(carryingItem.id, carryingItem);
            } else {
                TargetItem requestedItem = requestedItems.get(carryingItem.id);
                if (carryingItem.amount > requestedItem.amount) {
                    TargetItem redundantItem = new TargetItem(
                            carryingItem.id,
                            carryingItem.amount - requestedItem.amount,
                            requestedItem.noted
                    );
                    redundantItems.put(redundantItem.id, redundantItem);
                }
            }
        }
        setDepositMethods();
    }

    public void setDepositMethods() {

        Set<Integer> notRedundant = Sets.difference(carryingItems.keySet(), redundantItems.keySet());
        if (notRedundant.size() == 0) {
            // TODO bank whole invent with widgetbutton
            // Buggy, handle edge: all ids in redundant but want to keep x amount in invent
        }

        for (TargetItem redundantItem : redundantItems.values()) {
            if (redundantItem.drop) {
                System.out.println("Marked to drop " + redundantItem.id);
            } else if (redundantItem.amount == 1) {
                redundantItem.depositMethod = "LEFT_CLICK";
            } else if (redundantItem.amount == carryingItems.get(redundantItem.id).amount) {
                redundantItem.depositMethod = "Deposit-All";
            } else if (redundantItem.amount < carryingItems.get(redundantItem.id).amount) {
                redundantItem.depositMethod = "Deposit-X";
            } else {
                System.out.println("Deposit how?");
            }
        }
    }

    private HashMap<Integer, TargetItem> setCarrying() {
        carryingItems = new HashMap<>();
        for (int i = 0; i < INVENTORY_SIZE; i++) {
            WidgetItem itemWidget = inventoryWidget.getWidgetItem(i);
            int id = itemWidget.getId();
            if (id < 0) {
                continue; // Empty slot
            }
            int amount = itemWidget.getQuantity();
//                ItemComposition itemComposition = itemManager.getItemComposition(id);
//                String name = itemComposition.getName();
            if (!carryingItems.containsKey(id)) {
                ArrayList<Integer> inventoryIndeces = new ArrayList<>();
                ArrayList<Pair<Integer, Integer>> inventoryPositions = new ArrayList<>();
                inventoryIndeces.add(i);
                inventoryPositions.add(inventoryIndexToPosition(i));
                TargetItem item = new TargetItem(id, amount, false);
                item.inventoryIndeces = inventoryIndeces;
                item.inventoryPositions = inventoryPositions;
                carryingItems.put(id, item);
            } else {
                carryingItems.get(id).amount += amount;
                carryingItems.get(id).inventoryIndeces.add(i);
                carryingItems.get(id).inventoryPositions.add(inventoryIndexToPosition(i));
            }
        }
        return carryingItems;
    }

    public Pair<Integer, Integer> inventoryIndexToPosition(int index) {
        int row = (int) floor(index / 4.0);
        int column = index % 4;
        return Pair.of(row, column);
    }

    public Integer inventoryPositionToIndex(Pair<Integer, Integer> pos) {
        int index = 0;
        index += pos.getLeft() * 4;
        index += pos.getRight();
        return index;
    }

    public Integer getWidgetIndexByItemID(int id, boolean lowestIndexFirst, int neighbourIndex) {

        TargetItem targetItem = carryingItems.get(id);
        if (targetItem == null) {
            System.out.println("CarryingItems did not contain key " + id);
            return null;
        }

        if (lowestIndexFirst) {
            return targetItem.inventoryIndeces.get(0);

        } else {
            Pair<Integer, Integer> neighbour = inventoryIndexToPosition(neighbourIndex);
            for (Pair<Integer, Integer> pos : targetItem.inventoryPositions) {
                if ((abs(neighbour.getLeft() - pos.getLeft()) <= 1) &&
                        (abs(neighbour.getRight() - pos.getRight()) <= 1)) {
                    return inventoryPositionToIndex(pos);
                }
            }
        }
        return targetItem.inventoryIndeces.get(0);
    }

    public WidgetItem getWidgetByItemID(int id, boolean lowestIndexFirst, int neighbourIndex) {
        return getWidgetByIndex(getWidgetIndexByItemID(id, lowestIndexFirst, neighbourIndex));
    }

    private WidgetItem getWidgetByIndex(int index) {
        return inventoryWidget.getWidgetItem(index);
    }

    public boolean isCarrying(int id) {
        updateInventory();
        return carryingItems.containsKey(id);
    }

    public boolean hasMissingItems() {
        updateInventory();
        return missingItems.size() > 0;
    }

    public boolean hasRedundantItems() {
        updateInventory();
        return redundantItems.size() > 0;
    }

}

