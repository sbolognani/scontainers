package net.runelite.client.plugins.vroedoe;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import lombok.AccessLevel;
import lombok.Getter;
import net.runelite.api.Point;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.*;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.api.widgets.WidgetItem;
import net.runelite.client.Inputter;

import com.google.inject.Provides;
import net.runelite.api.*;

import net.runelite.client.callback.ClientThread;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.menus.WidgetMenuOption;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.overlay.OverlayManager;

import net.runelite.api.MenuEntry;

import net.runelite.client.game.ItemManager;

import javax.inject.Inject;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.lang.reflect.Array;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;


import net.runelite.client.ui.ClientUI;

import net.runelite.client.plugins.vroedoe.Inventory.*;


import net.runelite.http.api.npc.NpcInfo;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.commons.lang3.tuple.Triple;

import static java.lang.Math.*;


@SuppressWarnings("SpellCheckingInspection")
@PluginDescriptor(
        name = "Vroedoe",
        description = "Dit is Fraude",
        tags = {"config", "menu"},
        loadWhenOutdated = true,
        enabledByDefault = false
)


public class VroedoePlugin extends Plugin {

    private static final String CONFIG_GROUP = "vroedoe";
    private static final String GIBS_MAGIC_CONFIG_FLAG = "gibsMagic";


    // Scroller
    // leftUpper to rightBottom Getter
    // Inventory Management
    // Bank Put, Bank WithDraw

    // Skilling Presets
    // Bankstanders
    // Magic
    // Crafting
    // Cooking
    // Herblore
    // Fletching
    // Firemaking

    // Kitten Routine

    // RoW banking
    // RoD banking
    // Glory Banking

    // Combat Presets
    // Combat
    // Air orb bot pking bot URGENT
    // 55, 67, 56, 66, 58, 45, 50, 62, 59, 42, 64, 68 etc.
    // Mage (1-82) trainer, Ranged (1-80) trainer
    // Slayer Tasks

    // TODO isHidden() calls from the clientThread


    @Inject
    private OverlayManager overlayManager;

    @Inject
    private VroedoeOverlay overlay;

    @Inject
    private VroedoeConfig config;

    @Inject
    private ItemManager itemManager;

    public VroedoePlugin() throws AWTException {
//        this.itemManager = itemManager;
        System.out.println("AAAAAAAAAAAAAAAaa");
    }

    @Inject
    private Client client;

    @Inject
    private ClientThread clientThread;

    @Inject
    private ClientUI clientUI;

    @Provides
    VroedoeConfig provideConfig(ConfigManager configManager) {
        return configManager.getConfig(VroedoeConfig.class);
    }

    ExecutorService control = Executors.newFixedThreadPool(1);

//    Player player = client.getLocalPlayer();

    Inputter keyboard = new Inputter();

    Eskedit eskedit = null;
    final int PIXELS_PER_WHEELAMT = 45;

    int logins = 0;
    int login_tries = 0;
    int max_login_tries = 100;
    Rectangle drawer = null;
    Rectangle drawer2 = null;

    String BANKPIN = "1708";

    //    Shape target = null;
    NPC targetNPC = null;
    Player targetPlayer = null;
    TileObject targetTileObject = null;
    java.awt.Point mouseTarget = null;

    int MAX_DISTANCE = 2400;
    List<Integer> go_ids = Arrays.asList(1276);
    Map<Integer, List<TileObject>> matchmap = new HashMap<>();

    HashMap<Integer, TargetItem> withdrawalItems = null;

    int lastMenuOptionX = 0;
    private static final int MAX_ACTOR_VIEW_RANGE = 12;
    private WorldPoint lastPlayerLocation;

    Inventory inventory = new Inventory();

    Comparator ascYLocation = new Comparator<Widget>() {
        @Override
        public int compare(Widget w1, Widget w2) {
            return Integer.compare(w1.getCanvasLocation().getY(), w2.getCanvasLocation().getY());
        }
    };

    @Override
    protected void startUp() throws Exception {

        if (clientUI == null) {
            System.out.println("CLIENTUINULL");
        } else {
            System.out.println("YAS");
            System.out.println(clientUI.getCanvasOffset());
        }

        this.eskedit = new Eskedit(clientUI);

        overlayManager.add(overlay);

        GameState gameState = client.getGameState();
        if (gameState == GameState.LOGIN_SCREEN) {
            if (this.login_tries < this.max_login_tries) {
                login();
            }
        }
    }

    /**
     * The time when the last game tick event ran.
     */
    @Getter(AccessLevel.PACKAGE)
    private Instant lastTickUpdate;

    @Subscribe
    public void onGameTick(GameTick event)
    {
        lastTickUpdate = Instant.now();
        lastPlayerLocation = client.getLocalPlayer().getWorldLocation();
    }

    private static boolean isInViewRange(WorldPoint wp1, WorldPoint wp2)
    {
        int distance = wp1.distanceTo(wp2);
        return distance < MAX_ACTOR_VIEW_RANGE;
    }

    private static int wpDistance(WorldPoint wp1, WorldPoint wp2) {
        return wp1.distanceTo(wp2);
    }



    @Subscribe
    public void onNpcSpawned(NpcSpawned npcSpawned) {
        final NPC npc = npcSpawned.getNpc();
        final String npcName = npc.getName();

        if (npcName == null)
        {
            return;
        }

    }


    @Subscribe
    public void onGameStateChanged(GameStateChanged gameStateChanged) throws InterruptedException {
        GameState gameState = gameStateChanged.getGameState();
        if (gameState == GameState.LOGIN_SCREEN) {
            if (this.login_tries < this.max_login_tries) {
                login();
            } else {
                return;
            }
        } else if (gameState == GameState.LOGGING_IN) {
            this.login_tries++;
        } else if (gameState == GameState.LOGGED_IN) {
            clickHereToPlay();
            this.logins++;
        }
    }

    // --- MACRO ---
    public void login() {
        assert client.getGameState() == GameState.LOGIN_SCREEN;
        int wait_before = ThreadLocalRandom.current().nextInt(3, 6);
        this.control.submit(() -> {
            try {
                TimeUnit.SECONDS.sleep(wait_before);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            keyboard.type("\n");
            keyboard.type("purenummer2@gmail.com\n");
            keyboard.type("whatislove\n");
        });
    }


    // --- MACRO ---
    public void clickHereToPlay() {
        assert client.getGameState() == GameState.LOGGED_IN;
        Rectangle target_box = new Rectangle(268, 293, 220, 90);
        java.awt.Point target_point = eskedit.getClickPoint(target_box);
        this.control.submit(() -> {
            sleeprand(2000, 6000);
            eskedit.mouseMove(target_point);
            eskedit.mousePressAndRelease(1);
        });
    }

    // TODO test
    public synchronized Boolean clickMenuOption(String name) {
        if (!client.isMenuOpen()) {
            System.out.println("No open Menu");
            return null;
        }

        // TODO menu opened at bottom of screen -> bugs
        Point openedPos = client.getMouseCanvasPosition();

        boolean found = false;
        MenuEntry[] menuEntries = client.getMenuEntries();
        int pos = 0;
        for (int i = menuEntries.length - 1; i >= 0; i--) {
            String option = menuEntries[i].getOption();
            if (option.equals(name)) {
                found = true;
//                MenuEntry[] newMenuEntries = new MenuEntry[]{menuEntries[i]};
//                client.setMenuEntries(newMenuEntries);
                break;
            }//123
            pos++;
        }
        if (!found) {
            System.out.println("No match found");
            return null;
        }
        pos = pos + 1;
        System.out.println("Match found at with pos " + pos);
        int target_x = openedPos.getX() + 3 + ThreadLocalRandom.current().nextInt(0, 10);
        int target_y = openedPos.getY() + 18 + (pos * 14) - ThreadLocalRandom.current().nextInt(0, 5);

        eskedit.mouseMove(target_x, target_y);
//        System.out.println(client.getMouseCanvasPosition().getX()+ " vs " +  target_x);
//        System.out.println(client.getMouseCanvasPosition().getY()+ " vs " +  target_y);
        eskedit.mousePressAndRelease(1);
        return true;
    }

    public synchronized void moveAndLeftClick(Rectangle r) {
        java.awt.Point target_point = eskedit.getClickPoint(r);
        eskedit.mouseMove(target_point);
        eskedit.mousePressAndRelease(1);
    }

    public synchronized void moveAndRightClick(Rectangle r) {
        java.awt.Point target_point = eskedit.getClickPoint(r);
        eskedit.mouseMove(target_point);
        eskedit.mousePressAndRelease(3);
    }

    public synchronized void moveRightClickSelectMenuOption(Rectangle r, String name) {
        moveAndRightClick(r);
        if (!client.isMenuOpen()) {
            eskedit.delay(ThreadLocalRandom.current().nextInt(50, 200));
        }

        clickMenuOption(name);
    }

    public synchronized void enterBankPin() {
        Widget bankPinWidget = client.getWidget(WidgetInfo.BANK_PIN_CONTAINER);
        if (bankPinWidget == null) {
            System.out.println("Trying to enter bank pin but BankPinWidget not active");
            return;
        }
        int wait_before = ThreadLocalRandom.current().nextInt(300, 800);
        try {
            TimeUnit.MILLISECONDS.sleep(wait_before);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        keyboard.type(BANKPIN);
    }

    List<NPC> matchedNPCs = new ArrayList<>();

    public NPC findClosestNPC(String name,
//                               boolean object,
//                               boolean npc,
//                               boolean player,
                            String mustHaveOptionString,
                               boolean rotate,
                               boolean mustBeAlive) {
        targetNPC = null;
        matchedNPCs.clear();

        for (NPC npc : client.getNpcs()) {
            if (npc.getName() == null) {
                continue;
            }
            NPCComposition composition = npc.getComposition();
            if (!composition.isInteractible()
                    || !composition.isClickable()
                    || npc.getId() == 8666){ // Fix getActions()
                System.out.println("Bad composition or id");
                System.out.println(composition.isVisible() +" "+ composition.isInteractible() +" "+ composition.isClickable());
                continue;
            }

            if (npc.getName().equals(name)) {
                if (npc.isDead() && mustBeAlive) {
                    System.out.println("Npc is dead");
                    continue;
                }

                for (String action : composition.getActions()) {
                    if (action == null) {
                        continue;
                    } else if (action.equals(mustHaveOptionString)) {
                        System.out.println(npc.getName() + " has " + action);
                        break;
                    }
                }
                matchedNPCs.add(npc);
            }
        }

        if (matchedNPCs.size() == 0) {
            return null;
        }
        int closestDistance = Integer.MAX_VALUE;

        System.out.println("Got heree");

        for (NPC npc : matchedNPCs) {
            int lpdist = lpDistance(client.getLocalPlayer().getLocalLocation(), npc.getLocalLocation());
            int wpdist = wpDistance(lastPlayerLocation, npc.getWorldLocation());
//            System.out.println(lpdist + " lp, and wpdist " + wpdist);
            if (wpdist > MAX_ACTOR_VIEW_RANGE) {
                System.out.println("npc out of actor view range");
                continue;
            }
            if (lpdist < closestDistance) {
                closestDistance = lpdist;
                this.targetNPC = npc;
                System.out.println("Set target npc");
            }
        }
        return targetNPC;
    }

    public int lpDistance(LocalPoint lp1, LocalPoint lp2) {
        return lp1.distanceTo(lp2);
    }


    public void findObject(String name, boolean rotate) {

    }

    public synchronized Boolean findBankerAndBank() {
        NPC banker = findClosestNPC("Banker", "Bank", true, true);
        if (banker == null) {
            System.out.println("Banker not found");
            return null;
        }
        Point clickedPos = clickNPC(banker, 3, 8);
        if (clickedPos == null) {
            return null;
        }
        Boolean clicked = clickMenuOption("Bank");
        if (clicked == null) {
            return null;
        }
        // TODO timeouts here?
        try {
            TimeUnit.MILLISECONDS.sleep(300);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return true;
    }

    public synchronized Point clickNPC(NPC npc, int buttonID, int tries) {
        if (tries == 0) {
            System.out.println("Couldnt reach shape");
            return null;
        }
        Point mousePosition = client.getMouseCanvasPosition();
        Shape s = npc.getConvexHull();
        mouseTarget = eskedit.getClickPoint(s.getBounds());
        eskedit.mouseMove(mouseTarget);
        if (s.contains(mousePosition.getX(), mousePosition.getY())) {
            eskedit.mousePressAndRelease(buttonID);
            return mousePosition;

        } else {
            System.out.println("!!! Not in hull, moving again: " + tries);
            tries--;
            eskedit.delay(ThreadLocalRandom.current().nextInt(50, 100));
            return clickNPC(npc, buttonID, tries);
        }
    }

    @Subscribe
    public void onWidgetLoaded(final WidgetLoaded event) {
        System.out.println(event.toString());
        Widget wat = client.getViewportWidget();
        if (wat != null) {
            System.out.println(wat.getId());
            System.out.println("------");
        }
        if (event.getGroupId() == WidgetID.BANK_PIN_GROUP_ID) {
//            this.control.submit(this::enterBankPin);
        }
//        if (event.getGroupId() == WidgetID.RESIZABLE_VIEWPORT_OLD_SCHOOL_BOX_GROUP_ID)
    }

        public Boolean useID1OnID2(int id1, int id2, boolean closestPair) {
            System.out.println("Using id1 on id2");

            int index1 = inventory.getWidgetIndexByItemID(id1, true, 0);
            int index2 = inventory.getWidgetIndexByItemID(id2, false, index1);
            
            WidgetItem widget1 = inventory.getWidgetByIndex(index1);
            WidgetItem widget2 = inventory.getWidgetByIndex(index2);

            if (widget1 == null || widget2 == null) {
                return null;
            }

            Rectangle firstBox = widget1.getCanvasBounds();
            Rectangle secondBox = widget2.getCanvasBounds();

            drawer = firstBox;
            java.awt.Point first = eskedit.getClickPoint(firstBox);

            eskedit.mouseMove(first);
            eskedit.mousePressAndRelease(1);

            drawer = firstBox;
            java.awt.Point second = eskedit.getClickPoint(secondBox);
            eskedit.mouseMove(second);
            eskedit.mousePressAndRelease(1);
            return true;
        }

    }


    // -- Do not resize the client whilst banking
    // TODO catch nulls
    public synchronized Boolean withdrawBankItems() {

        inventory.updateInventory();
        withdrawalItems = inventory.missingItems;
        if (withdrawalItems.size() == 0) {
            System.out.println("No items being requested");
            return true;
        }

        Boolean banking = findBankerAndBank();
        if (banking == null) {
            return null;
        }
        System.out.println("Getting bank widget");
        // TODO fails to return null if container not open
        Widget bankContainer = getBankContainer(5000);
        if (bankContainer == null) {
            System.out.println("Found banker but no widget!");
            return null;
        }

        Point windowLocation = bankContainer.getCanvasLocation();
        HashMap<Integer, Widget> matchingWidgets = new HashMap<>();
        ArrayList<Widget> matchingWidgetsArray = new ArrayList<>();

        System.out.println("Depositing " + inventory.redundantItems.keySet());

        for (TargetItem depositingItem : Lists.newArrayList(inventory.redundantItems.values())) {
            System.out.println(depositingItem.id);
            WidgetItem depositingWidget = inventory.getWidgetByItemID(depositingItem.id, true, 0);
            if (depositingWidget == null) {
                System.out.println("TODO what is this");
                continue;
            }
            Rectangle firstBox = depositingWidget.getCanvasBounds();
            drawer = firstBox;
            if (depositingItem.depositMethod.equals("LEFT_CLICK")) {
                moveAndLeftClick(firstBox);
            } else if (depositingItem.depositMethod.equals("Deposit-All")) {
                moveRightClickSelectMenuOption(firstBox, depositingItem.depositMethod);
            } else if (depositingItem.depositMethod.equals("Deposit-X")) {
                moveRightClickSelectMenuOption(firstBox, depositingItem.depositMethod);
                int wait_before = ThreadLocalRandom.current().nextInt(800, 1600);
                try {
                    TimeUnit.MILLISECONDS.sleep(wait_before);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                eskedit.type(depositingItem.amount + "\n");
                lastMenuOptionX = depositingItem.amount;
            }
        }
        System.out.println("Finished depositing");

        for (Widget w : bankContainer.getDynamicChildren()) {
            if (w.getCanvasLocation() == null) {
                System.out.println("TODO what is this");
                continue;
            }
            int id = w.getItemId();
            if (withdrawalItems.containsKey(id)) {
                int requestedAmount = withdrawalItems.get(id).amount;
                if (w.getItemQuantity() >= requestedAmount) {
                    matchingWidgets.put(id, w);
                    matchingWidgetsArray.add(w);
                }
            }
        }
        Set<Integer> missingIds = Sets.difference(withdrawalItems.keySet(), matchingWidgets.keySet());
        if (missingIds.size() != 0) {
            System.out.println("Missing items: " + missingIds);
            // TODO buy items instead of null
            // buyItem();
            return null;
        }

        System.out.println("Found " + matchingWidgets.size());

        // Prep mouse for scroll, // TODO depends on currentMousePos

        matchingWidgetsArray.sort(ascYLocation);
        for (Widget w : matchingWidgetsArray) {
            scrollWhileWidgetNotContained(w, bankContainer);
            Rectangle getterBox = new Rectangle(
                    w.getCanvasLocation().getX(),
                    w.getCanvasLocation().getY(),
                    w.getWidth(),
                    w.getHeight()
            );
            this.drawer = getterBox;
            TargetItem targetItem = withdrawalItems.get(w.getItemId());
            System.out.println("Gettin " + targetItem.id);
            if (targetItem.withdrawalMethod.equals("LEFT_CLICK")) {
                moveAndLeftClick(getterBox);
            } else {
                moveRightClickSelectMenuOption(getterBox, targetItem.withdrawalMethod);
                if (targetItem.withdrawalMethod.equals("Withdraw-X")) {
                    int wait_before = ThreadLocalRandom.current().nextInt(800, 1600);
                    try {
                        TimeUnit.MILLISECONDS.sleep(wait_before);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    eskedit.type(Integer.toString(targetItem.amount) + "\n");
                    lastMenuOptionX = targetItem.amount;
                }
            }
        }

        Boolean missing = true;
        for (int i = 0; i < 20; i++) {
            if (inventory.hasMissingItems()) {
                inventory.updateInventory();
                withdrawalItems = inventory.missingItems;
                System.out.println("Waiting for inventory to update, sleeping 200");
                try {
                    TimeUnit.MILLISECONDS.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                missing = false;
                break;
            }
        }
        if (missing) {
            return null;
        }

        eskedit.typeKeycode(KeyEvent.VK_ESCAPE);
        sleeprand(150, 300);
        return true;
    }

    public Integer sleeprand(int minms, int maxms) {
        int wait_before = ThreadLocalRandom.current().nextInt(minms, maxms);
        try {
            TimeUnit.MILLISECONDS.sleep(wait_before);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return wait_before;
    }


    private void scrollWhileWidgetNotContained(Widget w, Widget bankContainer) {

        Point windowLocation = bankContainer.getCanvasLocation();

        Rectangle bankItemsBox = new Rectangle(
                windowLocation.getX(),
                windowLocation.getY(),
                bankContainer.getWidth(),
                bankContainer.getHeight()
        );


        Point canvasLocation = w.getCanvasLocation();
        Rectangle getterBox = new Rectangle(
                canvasLocation.getX(),
                canvasLocation.getY(),
                w.getWidth(),
                w.getHeight()
        );

        while (!bankItemsBox.contains(getterBox)) {

            this.drawer = bankItemsBox;
            eskedit.mouseMove(eskedit.getClickPoint(bankItemsBox));

            windowLocation = bankContainer.getCanvasLocation();

            this.drawer = bankItemsBox;
            canvasLocation = w.getCanvasLocation();

            bankItemsBox = new Rectangle(
                    windowLocation.getX(),
                    windowLocation.getY(),
                    bankContainer.getWidth(),
                    bankContainer.getHeight()
            );
            getterBox = new Rectangle(
                    canvasLocation.getX(),
                    canvasLocation.getY(),
                    w.getWidth(),
                    w.getHeight()
            );

            this.drawer2 = getterBox;

            double widgetTop = getterBox.getY();
            double widgetBottom = widgetTop + getterBox.getHeight();
            double bankBoxTop = bankItemsBox.getY();
            double bankBoxBottom = bankBoxTop + bankItemsBox.getHeight();

            int overshootAmt = ThreadLocalRandom.current().nextInt(0, 4); // Overshoot

            if (widgetTop < bankBoxTop) {
                double ydiff = bankBoxTop - widgetTop;
                int wheelAmt = -(int) ceil(ydiff / PIXELS_PER_WHEELAMT); // Scroll up
                if (wheelAmt < -1) {
                    wheelAmt -= overshootAmt;
                }
                eskedit.mouseWheel(wheelAmt);
            } else if (widgetBottom > bankBoxBottom) {
                double ydiff = widgetBottom - bankBoxBottom;
                int wheelAmt = (int) ceil(ydiff / PIXELS_PER_WHEELAMT); // Scroll down
                if (wheelAmt > 1) {
                    wheelAmt += overshootAmt;
                }
                eskedit.mouseWheel(wheelAmt);
            }

            int wait_before = ThreadLocalRandom.current().nextInt(300, 500);
            try {
                TimeUnit.MILLISECONDS.sleep(wait_before);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public Integer unif(int min, int max) {
        return ThreadLocalRandom.current().nextInt(min, max);
    }



    @Subscribe
    public void onItemContainerChanged(ItemContainerChanged event) {
        if (event.getContainerId() == InventoryID.INVENTORY.getId()) {
                if (this.inventory != null) {
                this.inventory.updateInventory();
            }
        }

        if (event.getContainerId() == InventoryID.BANK.getId()) {
            Item[] items = event.getItemContainer().getItems();
            System.out.println(items.length + " items in the bank boi");
        }

    }


    @Subscribe
    public void onStatChanged(StatChanged event) {
        Skill skill = event.getSkill();
        int xp = event.getXp();
        int level = event.getLevel();

    }

    private Triple<String, Integer, Integer> setFletchingTargetInventory(int currentLevel,
                                                                         boolean stringing,
                                                                         boolean cutting) {
        System.out.println("Welcome to setfletchingInventory");
        HashMap<Integer, TargetItem> requestedItems = new HashMap<>();
        int LOGS_AMOUNT = 0;
        int BOWSTRING_AMOUNT = 0;
        String CUTBUTTON = null;

        int KNIFE_ID = 946;
        int BOWSTRING_ID = 1777;

        if (cutting) {
            if (stringing) {
                LOGS_AMOUNT = 13;
            } else {
                LOGS_AMOUNT = 27;
            }
        }
        if (stringing && cutting) {
            BOWSTRING_AMOUNT = 13;
        } else if (stringing) {
            BOWSTRING_AMOUNT = 14;
        }

        TargetItem bowstring = new TargetItem(BOWSTRING_ID, BOWSTRING_AMOUNT, false);
        if (BOWSTRING_AMOUNT != 0) {
            requestedItems.put(bowstring.id, bowstring);
        }

        int woodID = -1;
        int unstrungID = -1;

        // Normal
        if (currentLevel < 5) {
            woodID = 1511;
            CUTBUTTON = "1";
        } else if (currentLevel < 10) {
            woodID = 1511;
            unstrungID = 50;
            CUTBUTTON = "3";
        } else if (currentLevel < 20) {
            woodID = 1511;
            unstrungID = 48;
            CUTBUTTON = "4";
        }

        // Oak
        else if (currentLevel < 25) {
            woodID = 1521;
            unstrungID = 54;
            CUTBUTTON = "2";
        } else if (currentLevel < 35) {
            woodID = 1521;
            unstrungID = 56;
            CUTBUTTON = "3";
        }

        // Willow
        else if (currentLevel < 40) {
            woodID = 1519;
            unstrungID = 60;
            CUTBUTTON = "2";
        } else if (currentLevel < 50) {
            woodID = 1519;
            unstrungID = 58;
            CUTBUTTON = "3";
        }

        // Maple
        else if (currentLevel < 55) {
            woodID = 1517;
            unstrungID = 64;
            CUTBUTTON = "2";
        } else if (currentLevel < 65) {
            woodID = 1517;
            unstrungID = 62;
            CUTBUTTON = "3";
        }

        // Yew, skipping yew short
        else if (currentLevel < 70) {
//            woodID = 1515;
//            unstrungID = 68;
//            CUTBUTTON = "2";
            woodID = 1517;
            unstrungID = 62;
            CUTBUTTON = "3";
        } else if (currentLevel < 80) {
            woodID = 1515;
            unstrungID = 66;
            CUTBUTTON = "3";
        }

        // Magic, skipping magic short
        else if (currentLevel < 85) {
//            woodID = 1513;
//            unstrungID = 72;
//            CUTBUTTON = "2";
            woodID = 1515;
            unstrungID = 66;
            CUTBUTTON = "3";
        } else if (currentLevel < 99) {
            woodID = 1513;
            unstrungID = 70;
            CUTBUTTON = "3";
        }

        if (cutting) {
            TargetItem knife = new TargetItem(KNIFE_ID, 1, false);
            requestedItems.put(knife.id, knife);
            TargetItem logs = new TargetItem(woodID, LOGS_AMOUNT, false);
            requestedItems.put(logs.id, logs);
        } else {
            TargetItem unstrung = new TargetItem(unstrungID, BOWSTRING_AMOUNT, false);
            requestedItems.put(unstrung.id, unstrung);
        }

        inventory.setRequestedItems(requestedItems);
        inventory.updateInventory();

        return Triple.of(CUTBUTTON, woodID, unstrungID);
    }

    // TODO breaks etc... (Template Class?)
    private synchronized Boolean fletch(int inventories,
                                     boolean stringing,
                                     boolean cutting) {

        if (inventory == null) {
            inventory = new Inventory();
        }
        // inventoryMenu
        eskedit.typeKeycode(KeyEvent.VK_ESCAPE);
        int startTime = (int) (System.currentTimeMillis() / 1000);
        System.out.println("Fletching from " + startTime);

        int KNIFE_ID = 946;
        int BOWSTRING_ID = 1777;

        for (int i = 0; i < inventories; i++) {

            int currentLevel = client.getRealSkillLevel(Skill.FLETCHING);
            if (!(currentLevel == client.getBoostedSkillLevel(Skill.FLETCHING))) {
                System.out.println("TODO: statRestore procedure");
                return null;
            }

            Triple<String, Integer, Integer> buttonWoodUnstrung = setFletchingTargetInventory(currentLevel, stringing, cutting);

            String OPTIONBUTTON = buttonWoodUnstrung.getLeft();
            int WOOD_ID = buttonWoodUnstrung.getMiddle();
            int UNSTRUNG_ID = buttonWoodUnstrung.getRight();

            Boolean withdrew = withdrawBankItems();
            if (withdrew == null) {
                System.out.println("Failed on withdrawBankItems");
                return null;
            }

            if (cutting) {
                useSelectWait(KNIFE_ID, WOOD_ID, OPTIONBUTTON, WOOD_ID, Skill.FLETCHING, 55);
            }

            if (stringing) {
                useSelectWait(BOWSTRING_ID, UNSTRUNG_ID, "1", UNSTRUNG_ID, Skill.FLETCHING, 55);
            }

            System.out.println("Miles Davis");
            sleeprand(0, 3000); // TODO sample skewed normal
        }
        return true;
    }

    // TODO check for targetInventory instead of waitID
    private Boolean useSelectWait(int id1, int id2, String selectOption, int waitID, Skill skill, int maxWaitS) {
        int startLevel = client.getRealSkillLevel(skill);
        Boolean used = inventory.useID1OnID2(id1, id2, true);
        if (used == null) {
            return null;
        }

        // TODO listen for menuWidget event
        int mss = sleeprand(600, 1600);
        eskedit.type(selectOption);
        if (mss < 800) {
            sleeprand(200, 400);
            eskedit.type(selectOption);
        }

        for (int s = 0; s < maxWaitS; s++) {
            // TODO improve levelUp procedure
            if (startLevel != client.getRealSkillLevel(skill)) {
                for (int j=0; j<unif(2, 5); j++){
                    eskedit.type(" ");
                    sleeprand(200, 1000);
                }
                useSelectWait(id1, id2, selectOption, waitID, skill, maxWaitS-s+4);
            }
            inventory.
            if (inventory.isCarrying(waitID)) {
                sleeprand(990, 1010);
            } else {
                break;
            }
        }
        // TODO did not finish in time?
        // TODO is invent target inventory?
        return true;
    }


    // TODO refactor to Banking Module
    public Widget getBankContainer(int timeoutMS) {
        long startTime = System.currentTimeMillis();
        Widget bankContainer = null;
        while (System.currentTimeMillis() < (startTime + timeoutMS)) {
            bankContainer = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
            if (bankContainer == null) {
                System.out.println("Awaiting bank container widget...");
                try {
                    enterBankPin();
                    TimeUnit.MILLISECONDS.sleep(timeoutMS / 3);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                // TODO container not null even if not opened?
                System.out.println("Found bank container");
                return bankContainer;
            }
        }
        return bankContainer;
    }

    @Subscribe
    public void onConfigChanged(ConfigChanged event) {
//        if (event.getGroup().equals("vroedoe")) {
//            boolean enabled = event.getNewValue().equals("true");
//            System.out.println(enabled);
//        }
        if (!CONFIG_GROUP.equals(event.getGroup())) {
            return;
        }
        if (event.getKey() == GIBS_MAGIC_CONFIG_FLAG) {
            System.out.println("Key issa flag");
        }
        if (config.gibsMagic()) {
            System.out.println("Issa gibs Magic");
            System.out.println(event.getNewValue());
        } else {
            System.out.println("Issa not gibs Magic");
            System.out.println(event.getNewValue());
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage event) {
        MessageNode node = event.getMessageNode();
        String name = event.getName();
        String message = event.getMessage();
        ChatMessageType type = event.getType();
        String sender = event.getSender();
//        System.out.println(sender + " " + name + " " + message + " " + node.getName());
        int ts = event.getTimestamp();

//        if (client.isMenuOpen()) {
//            MenuEntry[] menuEntries = client.getMenuEntries();
//            int i = 0;
//            for (MenuEntry entry : menuEntries) {
//                i++;
////                System.out.println(i + " is " + entry.getOption());
//            }


        if (message.contains("123money")) {
            for (NPC npc : client.getNpcs()) {
                if (npc.getName() == null) {
                    System.out.println("Name null");
                    continue;
                }
                System.out.println(npc.getName());
                if (npc.getName().equals("Grand Exchange Clerk")) {
                    System.out.println("Found clerk");
                    this.targetNPC = npc;
                }
                if (npc.getName().equals("Banker")) {
                    System.out.println("Found banker");
                    this.targetNPC = npc;
                }
            }
        }

        if (message.contains("123pc inv")) {
            if (inventory == null) {
                inventory = new Inventory();
            } else {
                inventory.printers();
            }
        }

        if (message.contains("123cut")) {
            this.control.submit(() ->
                    fletch(1000, false, true));
        }
        if (message.contains("123string")) {
            this.control.submit(() ->
                    fletch(1000, true, false));
        }
        if (message.contains("123cringe")) {
            this.control.submit(() ->
                    fletch(1000, true, true));
        }

        if (message.contains("123eskedit")) {
//            this.control.submit(this::findBankerAndBank);
            findBankerAndBank();
        }

        if (message.contains("123swap")) {
            setFletchingTargetInventory(60, true, true);
            this.control.submit((this::withdrawBankItems));
        }

        if (message.contains(" search")) {
            findClosestNPC("Banker", "Bank", true, true);
        }

        if (message.contains("tbank")) {
            this.control.submit((this::findBankerAndBank));
        }

        if (message.contains("123get c")) {
            inventory.updateInventory();
            Widget bankContainer = client.getWidget(WidgetInfo.BANK_ITEM_CONTAINER);
            if (bankContainer == null || bankContainer.isHidden()) {
                System.out.println("No bank container widget");
            }
            this.control.submit(this::withdrawBankItems);
        }
    }


    @Subscribe
    public void onGameObjectChanged(GameObjectChanged event) {
//        System.out.println("GO Changed: " + event.getGameObject().getId());
    }


    private boolean objectIdEquals(TileObject tileObject, int id) {
        if (tileObject == null) {
            return false;
        }

        if (tileObject.getId() == id) {
            return true;
        }

        // Menu action EXAMINE_OBJECT sends the transformed object id, not the base id, unlike
        // all of the GAME_OBJECT_OPTION actions, so check the id against the impostor ids
        final ObjectComposition comp = client.getObjectDefinition(tileObject.getId());

        if (comp.getImpostorIds() != null) {
            for (int impostorId : comp.getImpostorIds()) {
                if (impostorId == id) {
                    return true;
                }
            }
        }

        return false;
    }


    private TileObject findTileObject(Tile tile, int id) {

        if (tile == null) {
            return null;
        }

        final GameObject[] tileGameObjects = tile.getGameObjects();
        final DecorativeObject tileDecorativeObject = tile.getDecorativeObject();
        final WallObject tileWallObject = tile.getWallObject();
        final GroundObject groundObject = tile.getGroundObject();

        if (objectIdEquals(tileWallObject, id)) {
            return tileWallObject;
        }

        if (objectIdEquals(tileDecorativeObject, id)) {
            return tileDecorativeObject;
        }

        if (objectIdEquals(groundObject, id)) {
            return groundObject;
        }

        for (GameObject object : tileGameObjects) {
            if (objectIdEquals(object, id)) {
                return object;
            }
        }

        return null;
    }

}


