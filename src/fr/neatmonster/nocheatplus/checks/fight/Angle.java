package fr.neatmonster.nocheatplus.checks.fight;

import java.util.TreeMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import fr.neatmonster.nocheatplus.actions.ParameterName;
import fr.neatmonster.nocheatplus.checks.Check;
import fr.neatmonster.nocheatplus.checks.CheckEvent;
import fr.neatmonster.nocheatplus.players.Permissions;
import fr.neatmonster.nocheatplus.utilities.LagMeasureTask;

/*
 * MMP"""""""MM                   dP          
 * M' .mmmm  MM                   88          
 * M         `M 88d888b. .d8888b. 88 .d8888b. 
 * M  MMMMM  MM 88'  `88 88'  `88 88 88ooood8 
 * M  MMMMM  MM 88    88 88.  .88 88 88.  ... 
 * M  MMMMM  MM dP    dP `8888P88 dP `88888P' 
 * MMMMMMMMMMMM               .88             
 *                        d8888P              
 */
/**
 * A check used to verify if the player isn't using a forcefield in order to attack multiple entities at the same time.
 * 
 * Thanks @asofold for the original idea!
 */
public class Angle extends Check {

    /**
     * The event triggered by this check.
     */
    public class AngleEvent extends CheckEvent {

        /**
         * Instantiates a new angle event.
         * 
         * @param player
         *            the player
         */
        public AngleEvent(final Player player) {
            super(player);
        }
    }

    /**
     * Checks a player.
     * 
     * @param player
     *            the player
     * @return true, if successful
     */
    public boolean check(final Player player) {
        final FightConfig cc = FightConfig.getConfig(player);
        final FightData data = FightData.getData(player);

        boolean cancel = false;

        // Remove the old locations from the map.
        for (final long time : new TreeMap<Long, Location>(data.angleHits).navigableKeySet())
            if (System.currentTimeMillis() - time > 1000L)
                data.angleHits.remove(time);

        // Add the new location to the map.
        data.angleHits.put(System.currentTimeMillis(), player.getLocation());

        // Not enough data to calculate deltas.
        if (data.angleHits.size() < 2)
            return false;

        // Declare variables.
        double deltaMove = 0D;
        long deltaTime = 0L;
        float deltaYaw = 0f;

        // Browse the locations of the map.
        long previousTime = 0L;
        Location previousLocation = null;
        for (final long time : data.angleHits.descendingKeySet()) {
            final Location location = data.angleHits.get(time);
            // We need a previous location to calculate deltas.
            if (previousLocation != null) {
                // Calculate the distance between the two locations.
                deltaMove += previousLocation.distanceSquared(location);
                // Calculate the time elapsed between the two hits.
                deltaTime += previousTime - time;
                // Calculate the difference of the yaw between the two locations.
                deltaYaw += (previousLocation.getYaw() - location.getYaw()) % 360f;
            }
            // Remember the current time and location.
            previousTime = time;
            previousLocation = location;
        }

        // Let's calculate the average move.
        final double averageMove = deltaMove / (data.angleHits.size() - 1);

        // And the average time elapsed.
        final double averageTime = deltaTime / (data.angleHits.size() - 1);

        // And the average yaw delta.
        final double averageYaw = deltaYaw / (data.angleHits.size() - 1);

        // Declare the variable.
        double violation = 0D;

        // If the average move is between 0 and 0.2 block(s), add it to the violation.
        if (averageMove > 0D && averageMove < 0.2D)
            violation += 200D * (0.2D - averageMove) / 0.2D;

        // If the average time elapsed is between 0 and 150 millisecond(s), add it to the violation.
        if (averageTime > 0L && averageTime < 150L)
            violation += 500D * (150L - averageTime) / 150L;

        // If the average difference of yaw is superior to 50 degrees, add it to the violation.
        if (averageYaw > 50f)
            violation += 300D * (360f - averageYaw) / 360f;

        // Transform the violation into a percentage.
        violation /= 10D;

        // Is the violation is superior to the threshold defined in the configuration?
        if (violation > cc.angleThreshold) {
            // Has the server lagged?
            if (!LagMeasureTask.skipCheck())
                // If it hasn't, increment the violation level.
                data.angleVL += violation;

            // Dispatch a angle event (API).
            final AngleEvent e = new AngleEvent(player);
            Bukkit.getPluginManager().callEvent(e);

            // Execute whatever actions are associated with this check and the violation level and find out if we should
            // cancel the event.
            cancel = !e.isCancelled() && executeActions(player, cc.angleActions, data.angleVL);
        } else
            // Reward the player by lowering his violation level.
            data.angleVL *= 0.98D;

        return cancel;
    }

    /* (non-Javadoc)
     * @see fr.neatmonster.nocheatplus.checks.Check#getParameter(fr.neatmonster.nocheatplus.actions.ParameterName, org.bukkit.entity.Player)
     */
    @Override
    public String getParameter(final ParameterName wildcard, final Player player) {
        if (wildcard == ParameterName.VIOLATIONS)
            return String.valueOf(Math.round(FightData.getData(player).angleVL));
        else
            return super.getParameter(wildcard, player);
    }

    /* (non-Javadoc)
     * @see fr.neatmonster.nocheatplus.checks.Check#isEnabled(org.bukkit.entity.Player)
     */
    @Override
    protected boolean isEnabled(final Player player) {
        return !player.hasPermission(Permissions.FIGHT_ANGLE) && FightConfig.getConfig(player).angleCheck;
    }
}
