package fr.neatmonster.nocheatplus.checks.blockplace;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckEvent;
import fr.neatmonster.nocheatplus.players.Permissions;
import fr.neatmonster.nocheatplus.utilities.CheckUtils;

/*
 * M""""""'YMM oo                              dP   oo                   
 * M  mmmm. `M                                 88                        
 * M  MMMMM  M dP 88d888b. .d8888b. .d8888b. d8888P dP .d8888b. 88d888b. 
 * M  MMMMM  M 88 88'  `88 88ooood8 88'  `""   88   88 88'  `88 88'  `88 
 * M  MMMM' .M 88 88       88.  ... 88.  ...   88   88 88.  .88 88    88 
 * M       .MM dP dP       `88888P' `88888P'   dP   dP `88888P' dP    dP 
 * MMMMMMMMMMM                                                           
 */
/**
 * The Direction check will find out if a player tried to interact with something that's not in his field of view.
 */
public class Direction extends Check {

    /**
     * The event triggered by this check.
     */
    public class DirectionEvent extends CheckEvent {

        /**
         * Instantiates a new direction event.
         * 
         * @param player
         *            the player
         */
        public DirectionEvent(final Player player) {
            super(player);
        }
    }

    /**
     * Checks a player.
     * 
     * @param player
     *            the player
     * @param location
     *            the location
     * @return true, if successful
     */
    public boolean check(final Player player, final Location placed, final Location against) {
        final BlockPlaceConfig cc = BlockPlaceConfig.getConfig(player);
        final BlockPlaceData data = BlockPlaceData.getData(player);

        boolean cancel = false;

        // How far "off" is the player with his aim. We calculate from the players eye location and view direction to
        // the center of the target block. If the line of sight is more too far off, "off" will be bigger than 0.
        double off = CheckUtils.directionCheck(player, against.getX() + 0.5D, against.getY() + 0.5D,
                against.getZ() + 0.5D, 1D, 1D, 75);

        // Now check if the player is looking at the block from the correct side.
        double off2 = 0.0D;

        // Find out against which face the player tried to build, and if he
        // stood on the correct side of it
        final Location eyes = player.getEyeLocation();
        if (placed.getX() > against.getX())
            off2 = against.getX() + 0.5D - eyes.getX();
        else if (placed.getX() < against.getX())
            off2 = -(against.getX() + 0.5D - eyes.getX());
        else if (placed.getY() > against.getY())
            off2 = against.getY() + 0.5D - eyes.getY();
        else if (placed.getY() < against.getY())
            off2 = -(against.getY() + 0.5D - eyes.getY());
        else if (placed.getZ() > against.getZ())
            off2 = against.getZ() + 0.5D - eyes.getZ();
        else if (placed.getZ() < against.getZ())
            off2 = -(against.getZ() + 0.5D - eyes.getZ());

        // If he wasn't on the correct side, add that to the "off" value
        if (off2 > 0.0D)
            off += off2;

        if (off > 0.1D) {
            // Player failed the check. Let's try to guess how far he was from looking directly to the block...
            final Vector direction = player.getEyeLocation().getDirection();
            final Vector blockEyes = placed.add(0.5D, 0.5D, 0.5D).subtract(player.getEyeLocation()).toVector();
            final double distance = blockEyes.crossProduct(direction).length() / direction.length();

            // Add the overall violation level of the check.
            data.directionVL += distance;

            // Dispatch a direction event (API).
            final DirectionEvent e = new DirectionEvent(player);
            Bukkit.getPluginManager().callEvent(e);

            // Execute whatever actions are associated with this check and the violation level and find out if we should
            // cancel the event.
            cancel = !e.isCancelled() && executeActions(player, cc.directionActions, data.directionVL);
        } else
            // Player did likely nothing wrong, reduce violation counter to reward him.
            data.directionVL *= 0.9D;

        return cancel;
    }

    /* (non-Javadoc)
     * @see fr.neatmonster.nocheatplus.checks.Check#getParameter(fr.neatmonster.nocheatplus.actions.ParameterName, org.bukkit.entity.Player)
     */
    @Override
    public String getParameter(final ParameterName wildcard, final Player player) {
        if (wildcard == ParameterName.VIOLATIONS)
            return String.valueOf(Math.round(BlockPlaceData.getData(player).directionVL));
        else
            return super.getParameter(wildcard, player);
    }

    /* (non-Javadoc)
     * @see fr.neatmonster.nocheatplus.checks.Check#isEnabled(org.bukkit.entity.Player)
     */
    @Override
    protected boolean isEnabled(final Player player) {
        return !player.hasPermission(Permissions.BLOCKPLACE_DIRECTION)
                && BlockPlaceConfig.getConfig(player).directionCheck;
    }
}
