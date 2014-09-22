import model.*;

import static java.lang.StrictMath.*;

public final class MyStrategy implements Strategy {
    private static final double STRIKE_ANGLE = 1.0D * PI / 180.0D;

    @Override
    public void move(Hockeyist self, World world, Game game, Move move) {
        if (self.getState() == HockeyistState.SWINGING) {
            move.setAction(ActionType.STRIKE);
            return;
        }

        if (world.getPuck().getOwnerPlayerId() == self.getPlayerId()) {
            if (world.getPuck().getOwnerHockeyistId() == self.getId()) {
                Player opponentPlayer = world.getOpponentPlayer();

                double netX = 0.5D * (opponentPlayer.getNetBack() + opponentPlayer.getNetFront());
                double netY = 0.5D * (opponentPlayer.getNetBottom() + opponentPlayer.getNetTop());
                netY += (self.getY() < netY ? 0.5D : -0.5D) * game.getGoalNetHeight();

                double angleToNet = self.getAngleTo(netX, netY);
                move.setTurn(angleToNet);

                if (abs(angleToNet) < STRIKE_ANGLE) {
                    move.setAction(ActionType.SWING);
                }
            } else {
                Hockeyist nearestOpponent = getNearestOpponent(self.getX(), self.getY(), world);
                if (nearestOpponent != null) {
                    if (self.getDistanceTo(nearestOpponent) > game.getStickLength()) {
                        move.setSpeedUp(1.0D);
                    } else if (abs(self.getAngleTo(nearestOpponent)) < 0.5D * game.getStickSector()) {
                        move.setAction(ActionType.STRIKE);
                    }
                    move.setTurn(self.getAngleTo(nearestOpponent));
                }
            }
        } else {
            move.setSpeedUp(1.0D);
            move.setTurn(self.getAngleTo(world.getPuck()));
            move.setAction(ActionType.TAKE_PUCK);
        }
    }

    private static Hockeyist getNearestOpponent(double x, double y, World world) {
        Hockeyist nearestOpponent = null;
        double nearestOpponentRange = 0.0D;

        for (Hockeyist hockeyist : world.getHockeyists()) {
            if (hockeyist.isTeammate() || hockeyist.getType() == HockeyistType.GOALIE
                    || hockeyist.getState() == HockeyistState.KNOCKED_DOWN
                    || hockeyist.getState() == HockeyistState.RESTING) {
                continue;
            }

            double opponentRange = hypot(x - hockeyist.getX(), y - hockeyist.getY());

            if (nearestOpponent == null || opponentRange < nearestOpponentRange) {
                nearestOpponent = hockeyist;
                nearestOpponentRange = opponentRange;
            }
        }

        return nearestOpponent;
    }
}