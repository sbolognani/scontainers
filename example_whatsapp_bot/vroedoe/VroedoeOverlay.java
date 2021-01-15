package net.runelite.client.plugins.vroedoe;

import net.runelite.api.Client;

import net.runelite.api.NPC;
import net.runelite.api.Point;
import net.runelite.api.TileObject;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayPriority;

import javax.inject.Inject;
import java.awt.*;
import java.util.List;
import java.util.Map;

public class VroedoeOverlay extends Overlay {

    private Client client;
    private VroedoePlugin plugin;

    @Inject
    private VroedoeOverlay(final Client client, final VroedoePlugin plugin)
    {
        setPosition(OverlayPosition.DETACHED);
        setLayer(OverlayLayer.ALWAYS_ON_TOP);
        setPriority(OverlayPriority.HIGH);
        this.client = client;
        this.plugin = plugin;
    }

    @Override
    public Dimension render(Graphics2D graphics) {

        Map<Integer, List<TileObject>> matchmap = plugin.matchmap;
        Point mousePosition = client.getMouseCanvasPosition();

        if (plugin.targetNPC != null) {
            graphics.setColor(Color.CYAN);
            graphics.draw(plugin.targetNPC.getConvexHull());
        }
        graphics.setColor(Color.WHITE);

        for (NPC npc : plugin.matchedNPCs) {
            graphics.draw(npc.getConvexHull());
        }

        if (plugin.drawer != null) {
            graphics.draw(plugin.drawer);
        }
        if (plugin.drawer2 != null) {
            graphics.draw(plugin.drawer);
        }
        return getBounds().getSize();
    }
}
