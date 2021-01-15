package net.runelite.client.plugins.vroedoe;

import com.github.joonasvali.naturalmouse.api.MouseMotionFactory;
import com.github.joonasvali.naturalmouse.support.MouseMotionNature;
import com.github.joonasvali.naturalmouse.support.*;
import com.github.joonasvali.naturalmouse.util.FactoryTemplates;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.*;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.logging.Logger;

import com.github.joonasvali.naturalmouse.util.FlowTemplates;
import net.runelite.api.Client;
import net.runelite.api.Constants;
import net.runelite.client.ui.ClientUI;

import static java.lang.Math.abs;

public class Eskedit extends Robot {

    public ThreadGroup eskeditThreads = new ThreadGroup("eskedit");
    public static boolean isActive;
    public static double scale;
    public static Client client;
    public static ClientUI clientUI = null;
    public static final int fixedWidth = Constants.GAME_FIXED_WIDTH;
    public static final int fixedHeight = Constants.GAME_FIXED_HEIGHT;
    public static boolean isStretched;
    public static int minDelay = 25;
//    public FactoryTemplates factoryTemplates;
    public static MouseMotionFactory currentMouseMotionFactory = createFastGamerMotionFactory(new DefaultMouseMotionNature());
    public boolean pausedIndefinitely = false;
    public Robot peer;

    public Point leftTarget = null;
    public Point rightTarget = null;

    private static final HashMap<Character, Character> shiftChars = new HashMap<Character, Character>() {{
        put('!', '1');
        put('@', '2');
        put('#', '3');
        put('$', '4');
    }};

    int minCPM = 350;
    int maxCPM = 800;

    public static MouseMotionFactory createFastGamerMotionFactory(MouseMotionNature nature) {
        MouseMotionFactory factory = new MouseMotionFactory(nature);
        List<Flow> flows = new ArrayList<>(Arrays.asList(
                new Flow(FlowTemplates.variatingFlow()),
                new Flow(FlowTemplates.slowStartupFlow()),
                new Flow(FlowTemplates.slowStartup2Flow()),
                new Flow(FlowTemplates.adjustingFlow()),
                new Flow(FlowTemplates.jaggedFlow())
        ));
        DefaultSpeedManager manager = new DefaultSpeedManager(flows);
        factory.setDeviationProvider(new SinusoidalDeviationProvider(SinusoidalDeviationProvider.DEFAULT_SLOPE_DIVIDER));
        factory.setNoiseProvider(new DefaultNoiseProvider(DefaultNoiseProvider.DEFAULT_NOISINESS_DIVIDER));
        factory.getNature().setReactionTimeVariationMs(100);
        manager.setMouseMovementBaseTimeMs(350);

        DefaultOvershootManager overshootManager = (DefaultOvershootManager) factory.getOvershootManager();
        overshootManager.setOvershoots(4);

        factory.setSpeedManager(manager);
        return factory;
    }

    public Eskedit(ClientUI fromPlugin) throws AWTException
    {
        clientUI = fromPlugin;
        peer = new Robot();
        peer.setAutoWaitForIdle(true);

        if (GraphicsEnvironment.isHeadless())
        {
            throw new AWTException("headless environment");
        }
    }

    private double randCPM() {
        return ThreadLocalRandom.current().nextInt(this.minCPM, this.maxCPM+1);
    }

    private double msPerChar(double cpm) {
        return ((60.0 / cpm) * 1000.0);
    }

    public synchronized void type(String text) {
        double cpm = randCPM();
        double mspc = msPerChar(cpm);
        int minmspc = (int) (mspc * .5);
        int maxmspc = (int) (mspc * 1.5);
        System.out.println(">" + text + "CPM " + cpm + " MSPC " + mspc);

        for (char c : text.toCharArray()) {
            int keycode = KeyEvent.getExtendedKeyCodeForChar(c);
            int msdelay = ThreadLocalRandom.current().nextInt(minmspc, maxmspc + 1);
            boolean shifted = false;
            if (KeyEvent.CHAR_UNDEFINED == keycode) {
                throw new RuntimeException(
                        "Key code not found for character '" + c + "'");
            }
            if (shiftChars.containsKey(c)) {
                shifted = true;
                this.peer.keyPress(KeyEvent.VK_SHIFT);
                int shiftdelay = ThreadLocalRandom.current().nextInt(minmspc, maxmspc + 1);
                this.peer.delay(shiftdelay);
            }
            this.peer.keyPress(keycode);
            this.peer.delay(ThreadLocalRandom.current().nextInt(10, 40));
            this.peer.keyRelease(keycode);
            if (shifted) {
                this.peer.delay(ThreadLocalRandom.current().nextInt(20, 100));
                this.peer.keyRelease(KeyEvent.VK_SHIFT);
            }
            this.peer.delay(msdelay);
        }
    }

    public void typeKeycode(int keycode) {
        this.peer.keyPress(keycode);
        this.peer.delay(ThreadLocalRandom.current().nextInt(10, 40));
        this.peer.keyRelease(keycode);
    }

//    private void init() throws AWTException {
//        System.out.println("ESKEDIT INIT");
//        System.out.println(clientUI);
//        peer = new Robot();
//        System.out.println("ESKEDIT INIT");
//    }

    private void pauseMS(int delayMS)
    {
        long initialMS = System.currentTimeMillis();
        while (System.currentTimeMillis() < initialMS + delayMS)
        {
            try
            {
                Thread.sleep(10);
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
        }
        isActive = false;
    }

    @Override
    public synchronized void mouseMove(int x, int y)
    {
        try
        {
            //TODO: Must be better way to determine titlebar width
            currentMouseMotionFactory.build(
                    ClientUI.frame.getX() + x + determineHorizontalOffset(),
                    ClientUI.frame.getY() + y + determineVerticalOffset()).move();
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }
    }

//    public Point getClickPointInShape(Shape)

    // Gaussian
    public Point getClickPoint(Rectangle r) {
        double xs = 1/(1+Math.exp(-ThreadLocalRandom.current().nextGaussian()));
        double ys = 1/(1+Math.exp(-ThreadLocalRandom.current().nextGaussian()));
        int tx = (int)(r.getX() + r.getWidth()*xs);
        int ty = (int)(r.getY() + r.getHeight()*ys);
        return new Point(tx, ty);
    }

    public synchronized void mouseMove(Point p)
    {
        mouseMove((int) p.getX(), (int) p.getY());
    }

    @Override
    public synchronized void mousePress(int buttonID)
    {
        if (buttonID < 1 || buttonID > 5)
        {
            Logger.getAnonymousLogger().warning("Invalid mouse button ID. please use 1-5.");
            return;
        }
        peer.mousePress(InputEvent.getMaskForButton(buttonID));
    }

    public synchronized void mousePressAndRelease(int buttonID)
    {
//        System.out.println("Pressing " + buttonID);
        if (buttonID < 1 || buttonID > 5)
        {
            Logger.getAnonymousLogger().warning("Invalid mouse button ID. please use 1-5.");
            return;
        }
        peer.mousePress(InputEvent.getMaskForButton(buttonID));
        this.delay(getMinDelay());
        peer.mouseRelease(InputEvent.getMaskForButton(buttonID));
        this.delay(getMinDelay());
    }

//    //TODO: Symbols are nut supported at this time
//    public synchronized void typeMessage(String message)
//    {
//
//        Random r = new Random();
//        char[] charArray = message.toCharArray();
//        for (char c : charArray)
//        {
//            keyPress(KeyEvent.getExtendedKeyCodeForChar(c));
//            this.delay(93 + r.nextInt(getMinDelay()));
//        }
//        keyPress(KeyEvent.VK_ENTER);
//        this.delay(93 + r.nextInt(getMinDelay()));
////        ClientUI.allowInput = true;
//    }


    @Override
    public synchronized void mouseRelease(int buttonID)
    {
        if (buttonID < 1 || buttonID > 5)
        {
            Logger.getAnonymousLogger().warning("Invalid mouse button ID. please use 1-5.");
            return;
        }
        peer.mouseRelease(InputEvent.getMaskForButton(buttonID));
        this.delay(getMinDelay());
    }

    private int getMinDelay()
    {
        Random random = new Random();
        int random1 = random.nextInt(minDelay);
        if (random1 < minDelay / 2)
        {
            random1 = random.nextInt(minDelay / 2) + minDelay / 2 + random.nextInt(minDelay / 2);
        }
        return random1;
    }

    private int getWheelDelay()
    {
        Random random = new Random();
        int random1 = random.nextInt(minDelay);
        if (random1 < minDelay / 2)
        {
            random1 = random.nextInt(minDelay / 2) + minDelay / 2 + random.nextInt(minDelay / 2);
        }
        return random1;
    }

    /**
     * Rotates the scroll wheel on wheel-equipped mice.
     *
     * @param wheelAmt number of "notches" to move the mouse wheel
     *                 Negative values indicate movement up/away from the user,
     *                 positive values indicate movement down/towards the user.
     * @since 1.4
     */
    @Override
    public synchronized void mouseWheel(int wheelAmt)
    {
        int i = 0;
        int step = 1;
        if (wheelAmt < 0) { step = -step; }
        while (i < abs(wheelAmt)) {
            peer.mouseWheel(step);
            this.delay(getWheelDelay());
            i++;
        }
    }



    /**
     * Presses a given key.  The key should be released using the
     * <code>keyRelease</code> method.
     * <p>
     * Key codes that have more than one physical key associated with them
     * (e.g. <code>KeyEvent.VK_SHIFT</code> could mean either the
     * left or right shift key) will map to the left key.
     *
     * @param keycode Key to press (e.g. <code>KeyEvent.VK_A</code>)
     * @throws IllegalArgumentException if <code>keycode</code> is not
     *                                  a valid key
     * @see #keyRelease(int)
     * @see KeyEvent
     */
    @Override
    public synchronized void keyPress(int keycode)
    {
        peer.keyPress(keycode);
        this.delay(getMinDelay());
    }

    @Override
    public synchronized void keyRelease(int keycode)
    {
        peer.keyRelease(keycode);
        this.delay(getMinDelay());
    }

    public synchronized void holdKey(int keycode, int timeMS)
    {
        new Thread(() ->
        {
            peer.keyPress(keycode);
            long startTime = System.currentTimeMillis();
            while ((startTime + timeMS) > System.currentTimeMillis())
            {
            }
            peer.keyRelease(keycode);
            this.delay(getMinDelay());
        }).start();
    }

    public synchronized void holdKeyIndefinitely(int keycode)
    {
        Thread holdKeyThread = new Thread(() ->
        {
            pausedIndefinitely = true;
            peer.keyPress(keycode);
            while (pausedIndefinitely)
            {
                try
                {
                    Thread.sleep(10);
                }
                catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
            }
            peer.keyRelease(keycode);
            this.delay(getMinDelay());
        });
        holdKeyThread.start();

    }

    public Color getPixelColor(int x, int y)
    {
        return peer.getPixelColor(x, y);
    }

    @Override
    public void delay(int ms)
    {
        pauseMS(ms);
    }

    public int determineHorizontalOffset()
    {
//        System.out.println(clientUI.isFocused());
//        System.out.println(clientUI.getCanvasOffset());
        return clientUI.getCanvasOffset().getX();
    }

    public int determineVerticalOffset()
    {
        return clientUI.getCanvasOffset().getY();
    }
}
