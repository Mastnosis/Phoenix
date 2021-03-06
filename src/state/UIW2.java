/*
 * Copyright (C) 2015 joulupunikki joulupunikki@gmail.communist.invalid.
 *
 *  Disclaimer of Warranties and Limitation of Liability.
 *
 *     The creators and distributors offer this software as-is and
 *     as-available, and make no representations or warranties of any
 *     kind concerning this software, whether express, implied, statutory,
 *     or other. This includes, without limitation, warranties of title,
 *     merchantability, fitness for a particular purpose, non-infringement,
 *     absence of latent or other defects, accuracy, or the presence or
 *     absence of errors, whether or not known or discoverable.
 *
 *     To the extent possible, in no event will the creators or distributors
 *     be liable on any legal theory (including, without limitation,
 *     negligence) or otherwise for any direct, special, indirect,
 *     incidental, consequential, punitive, exemplary, or other losses,
 *     costs, expenses, or damages arising out of the use of this software,
 *     even if the creators or distributors have been advised of the
 *     possibility of such losses, costs, expenses, or damages.
 *
 *     The disclaimer of warranties and limitation of liability provided
 *     above shall be interpreted in a manner that, to the extent possible,
 *     most closely approximates an absolute disclaimer and waiver of
 *     all liability.
 *
 */
package state;

import galaxyreader.Unit;
import game.Square;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.util.List;
import util.C;
import util.StackIterator;

/**
 * Unit info window unit dragged
 *
 * @author joulupunikki
 */
public class UIW2 extends State {

    private static UIW2 instance = new UIW2();

    public UIW2() {
    }

    public static State get() {
        return instance;
    }

    public void dragOnWindow(MouseEvent e) {
        Point p = e.getPoint();
//        System.out.println("p = " + p);
        gui.setDragPoint(p);
        gui.getMainWindows().repaint();
    }

    /**
     * Handles dragged unit release on UnitInfoWindow (UIW). We have four rows
     * and five columns of unit positions in UIW, and the units are presented
     * left to right, top to bottom, in stack order with cargo listed in order
     * right after the carrier. So we use a StackIterator and loop first row
     * wise then column wise and check if the event is situated on that unit
     * position. Due to the aforementioned ordering the correct stack unit (if
     * any) should be available currently from the iterator. Now, if the unit
     * under mouse (UUM) is the dragged unit (DU) then do nothing, else if DU is
     * carried, disembark if allowed, if UUM is a carrier and has room, embark,
     * if DU and UUM are of same resource type, open cargo manipulation dialog
     * if DU is resource and UUM is null, open cargo manipulation dialog.
     *
     * stack
     *
     * @param e
     */
    @Override
    public void releaseOnWindow(MouseEvent e) {
        //System.out.println("e = " + e);
        Unit drag_unit = gui.getDragUnit();
        Point q = e.getPoint();
        Point faction = game.getSelectedFaction();
        Point p = game.getSelectedPoint();

        List<Unit> stack = null;
        boolean[] terrain = null;
        int tile_set = -1;
        if (faction.x == -1) {
            stack = game.getPlanetGrid(game.getCurrentPlanetNr()).getHex(p.x, p.y).getStack();
            terrain = game.getPlanetGrid(game.getCurrentPlanetNr()).getHex(p.x, p.y).getTerrain();
            //System.out.println("terrain = " + terrain[C.OCEAN]);
            tile_set = game.getPlanet(game.getCurrentPlanetNr()).tile_set_type;
        } else {
            Square[][] galaxy_grid = game.getGalaxyMap().getGalaxyGrid();
            stack = galaxy_grid[p.x][p.y].parent_planet.space_stacks[faction.y];
        }

        StackIterator iter = new StackIterator(stack);
        Unit u = iter.next();
        for (int i = 0; i < C.STACK_WINDOW_UNITS_Y; i++) {
            for (int j = 0; j < C.STACK_WINDOW_UNITS_X; j++) {
                int dx = (int) (ws.unit_panel_x_offset + j * 3.5 * ws.unit_icon_size);
                int dy = (int) (ws.unit_panel_y_offset + i * 1.15 * ws.unit_icon_size);

                if (dx <= q.x && q.x <= dx + ws.ui_unit_block_w
                        && dy <= q.y && q.y <= dy + ws.unit_icon_size) {
                    if (!drag_unit.equals(u)) { // no self referencing
                        if (drag_unit.carrier != null) { // carried unit
                            if ((faction.x != -1 && !(drag_unit.move_type == C.MoveType.JUMP
                                    || drag_unit.move_type == C.MoveType.LANDER
                                    || drag_unit.move_type == C.MoveType.SPACE))
                                    && (u.type_data.cargo == 0 || u.cargo_list.size() >= u.type_data.cargo)) {
                                gui.showInfoWindow("Cannot unload cargo in space.");
                                zeroDragUnit();
                                return;
                            } else if (faction.x == -1 && terrain[C.OCEAN] == true && tile_set != 4
                                    && (u.type_data.cargo == 0 || u.cargo_list.size() >= u.type_data.cargo)) {
                                gui.showInfoWindow("Cannot unload cargo on the ocean.");
                                zeroDragUnit();
                                return;
                            } else { // disembark
                                Unit carrier = drag_unit.carrier;
                                carrier.disembark(drag_unit);
                                stack.add(drag_unit);
                            }
                        }
                        if (u.type_data.cargo > 0) { // if dragged onto a carrier
                            if (drag_unit.type_data.can_b_cargo == 1) { // and can be cargo
                                if (u.cargo_list.size() < u.type_data.cargo) { // and there is room
                                    if (u.embark(drag_unit)) { // then embark unit
                                        stack.remove(drag_unit);
                                        boolean selected = false;
                                        for (Unit unit : stack) {
                                            if (unit.isSelected() == true) {
                                                selected = true;
                                                break;
                                            }
                                        }
                                        if (!selected) {
                                            stack.get(0).setSelected(true);
                                        }
                                    }
                                } else {
                                    gui.showInfoWindow("Transport is full.");
                                }
                            } else {
                                gui.showInfoWindow("That unit cannot be loaded onto a transport.");
                            }
                        }
                        if (drag_unit.type == C.CARGO_UNIT_TYPE && u.type == C.CARGO_UNIT_TYPE
                                && drag_unit.res_relic == u.res_relic) { // cargo manipulation
                            gui.initCargoWin(drag_unit, u, stack);
                            gui.showCargoWin(true);
                        }
                    }
                    zeroDragUnit();
                    return;
                }
                u = iter.next();
                if (u == null) {
                    if (drag_unit.carrier != null) {
                        if (faction.x != -1 && !(drag_unit.move_type == C.MoveType.JUMP
                                || drag_unit.move_type == C.MoveType.LANDER
                                || drag_unit.move_type == C.MoveType.SPACE)) {
                            gui.showInfoWindow("Cannot unload cargo in space.");
                        } else if (faction.x == -1 && terrain[C.OCEAN] == true && tile_set != 4) {
                            gui.showInfoWindow("Cannot unload cargo on the ocean.");
                        } else {
                            Unit carrier = drag_unit.carrier;
                            carrier.disembark(drag_unit);
                            stack.add(drag_unit);
                        }
                    } else if (drag_unit.type == C.CARGO_UNIT_TYPE) {
                        gui.initCargoWin(drag_unit, null, stack);
                        gui.showCargoWin(true);
                    }
                    zeroDragUnit();
                    return;
                }
            }
        }
        zeroDragUnit();
    }

    protected void zeroDragUnit() {
        gui.setDragUnit(null, null);
        gui.setCurrentState(UIW1.get());
        gui.getMainWindows().repaint();
    }
}
