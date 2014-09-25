import java.awt.Point;

import model.*;
import java.lang.Math.*;
import static java.lang.StrictMath.*;


public final class MyStrategy implements Strategy {
  private static final double STRIKE_ANGLE = 1.0D * PI / 180.0D;

  private static int side = 0;
  private static long defender = 0;
  private static long attacker = 0;
  private static int checkRoleTick = -1;
  private static int swingingTick = -1;


  @Override
  public void move(Hockeyist self, World world, Game game, Move move) {
    System.out.println("----------------------------------------");
    System.out.println("ШАГ ИГРЫ             " + world.getTick());
    System.out.println("----------------------------------------");
    // Определяем с какой стороны мы играем
    if (side == 0) {
      if (world.getOpponentPlayer().getNetFront() > 600)
        side = -1;
      else
        side = 1;
    }


    if (checkRoleTick != world.getTick()) {
      // Определяем кто атакующий, а кто защитник
      // если мы владеем шайбой
      if (world.getPuck().getOwnerPlayerId() == self.getPlayerId()) {
        attacker = world.getPuck().getOwnerHockeyistId();
        for (Hockeyist hockeyist : world.getHockeyists()) {
          if (hockeyist.isTeammate() && hockeyist.getType() != HockeyistType.GOALIE
              && hockeyist.getId() != attacker) {
            defender = hockeyist.getId();
          }
        }
      } else {
        Hockeyist tAttacker = null;
        double tDist = Double.MAX_VALUE;
        for (Hockeyist hockeyist : world.getHockeyists()) {
          if (hockeyist.isTeammate() && hockeyist.getType() != HockeyistType.GOALIE) {
            if (tAttacker == null) {
              tAttacker = hockeyist;
              tDist = getDistanceBetweenUnits(tAttacker, world.getPuck());
            } else {
              if (getDistanceBetweenUnits(hockeyist, world.getPuck()) < tDist) {
                defender = tAttacker.getId();
                attacker = hockeyist.getId();
              } else {
                attacker = tAttacker.getId();
                defender = hockeyist.getId();
              }
            }
          }
        }
      }
      checkRoleTick = world.getTick();
      System.out.println("Роли распределены: " + attacker + " - атакующий, " + defender
          + " - защитник");
    }


    if (self.getId() == attacker) {
      startAttackerStrategy(self, world, game, move);
    } else {
      startDefenseStrategy(self, world, game, move);
    }
    System.out.println("------------Конец промежуточного хода -----------------");
  }



  /*****************************
   ** ЗАЩИТНАЯ СТРАТЕГИЯ **
   *****************************/

  private void startDefenseStrategy(Hockeyist self, World world, Game game, Move move) {

    // Точка игры защитника
    Point baseDefencePoint =
        new Point((int) game.getWorldWidth() / 2 + side * 430, (int) game.getWorldHeight() / 2 + 60);

    // если шайбой владеет чужая команда
    if (world.getPuck().getOwnerPlayerId() != self.getPlayerId()) {
      // получаем ближайшего хоккеиста оппонента
      Hockeyist nearestOpponent = getNearestOpponent(self.getX(), self.getY(), world);
      Hockeyist nearestTeammate = getNearestTeammate(self.getX(), self.getY(), world);
      // если ближайший оппонент найден
      if (nearestOpponent != null) {
        // если ближайший тиммейт найден
        if (nearestTeammate != null) {
          // если до чужого ближе чем до своего
          // if (getDistanceBetweenUnits(self, nearestTeammate) > getDistanceBetweenUnits(self,
          // nearestOpponent)) {
          // если шайба на моей половине поля
          if ((world.getPuck().getX() - game.getWorldWidth() / 2) * side >= 5) {
            System.out.println("    Я ЗАЩИТНИК " + defender + " иду отбирать шайбу!");
            // ИГРАЙ ДРУГ!!!
            move.setSpeedUp(1.0D);
            move.setTurn(self.getAngleTo(world.getPuck()));
            move.setAction(ActionType.TAKE_PUCK);
            return;
          }
        }
      }
    }

    Player opponentPlayer = world.getOpponentPlayer();
    // если на месте то остановиться для защиты
    if ((max(baseDefencePoint.x, self.getX()) - min(baseDefencePoint.x, self.getX()) < self
        .getRadius() / 2)
        && (max(baseDefencePoint.y, self.getY()) - min(baseDefencePoint.y, self.getY()) < self
            .getRadius() / 2)) {
 /*     double angleToOpponent =
          self.getAngleTo(opponentPlayer.getNetFront(),
              0.5D * (opponentPlayer.getNetBottom() + opponentPlayer.getNetTop())); */
      double angleToPack = self.getAngleTo(world.getPuck());
      move.setTurn(angleToPack);
      move.setSpeedUp(0.0D);
      System.out.println("  Я ЗАЩИТНИК " + defender + " Остановился на точке защиты!                          "+self.getX() + " : "+self.getY());
    } else {
      // идти на точку защиты
      // Если точка защиты по Y за спиной хоккеиста идти развернуться спиной к точке и идти задом
      // Если точка защиты по Y перед хоккеистом развернуться лицом к ней и идти вперед
      if (side * baseDefencePoint.x < side * self.getX()) {
        move.setSpeedUp(1.0D * getBrakingCoef(self, baseDefencePoint));
        move.setTurn(self.getAngleTo(baseDefencePoint.x, baseDefencePoint.y));
      } else {
        move.setSpeedUp(-1.0D * getBrakingCoef(self, baseDefencePoint));
        move.setTurn(self.getAngleTo(baseDefencePoint.x, baseDefencePoint.y)>0 ? self.getAngleTo(baseDefencePoint.x, baseDefencePoint.y)-PI:self.getAngleTo(baseDefencePoint.x, baseDefencePoint.y)+PI);
      }
      move.setAction(ActionType.TAKE_PUCK);
      System.out.println("  Я ЗАЩИТНИК " + defender + " иду на точку защиты!");
    }
  }



  private double getBrakingCoef(Hockeyist self, Point baseDefencePoint) {
    double k =
        abs(hypot(self.getX() - baseDefencePoint.getX(), self.getY() - baseDefencePoint.getY()));
    if (k > 150) {
      return 1;
    } else {
      return k / 200.0D;
    }
  }



  /*****************************
   ** АТАКУЮЩАЯ СТРАТЕГИЯ **
   *****************************/

  private void startAttackerStrategy(Hockeyist self, World world, Game game, Move move) {
    // если текущий хоккеист замахивается то ударить по воротам
    if (self.getState() == HockeyistState.SWINGING) {
      System.out.println("  Я НАПАДАЮЩИЙ " + attacker + " Бью по воротам!");
      move.setAction(ActionType.STRIKE);
      return;
    }

    // определение ударной точки
    Point attackPoint =
        new Point((int) (game.getWorldWidth() / 2 - side * 150),
            (int) (game.getWorldHeight() * 0.25));


    swingingTick = -1;
    // получить оппонента
    Player opponentPlayer = world.getOpponentPlayer();

    // если моя команда владеет шайбой
    if (world.getPuck().getOwnerPlayerId() == self.getPlayerId()) {
      // если текущий игрок владеет шайбой
      if (world.getPuck().getOwnerHockeyistId() == self.getId()) {
        System.out.println("  Я НАПАДАЮЩИЙ " + attacker + " ВЛАДЕЮ ШАЙБОЙ!");
        // если на месте для удара то ударить
        if ((max(attackPoint.x, self.getX()) - min(attackPoint.x, self.getX()) < self.getRadius() * 2)
            && (max(attackPoint.y, self.getY()) - min(attackPoint.y, self.getY()) < self
                .getRadius() * 2)) {
          // пытаемся ударить
          // получили координаты центра ворот противника
          double netX = 0.5D * (opponentPlayer.getNetBack() + opponentPlayer.getNetFront());
          double netY = 0.5D * (opponentPlayer.getNetBottom() + opponentPlayer.getNetTop());
          // расстояние от хоккеиста до дальней штанги противника
          netY += (self.getY() < netY ? 0.5D : -0.5D) * game.getGoalNetHeight();
          // угол до центра ворот
          double angleToNet = self.getAngleTo(netX, netY);
          // начинай поворачиваться к центру ворот
          move.setTurn(angleToNet);
          // если угол до ворот меньше ударного то начинай замах для удара
          if (abs(angleToNet) < STRIKE_ANGLE) {
            System.out.println("  Я НАПАДАЮЩИЙ " + attacker + " НАИНАЮ замахиваться для броска!");
            move.setAction(ActionType.SWING);
            swingingTick++;
          }
          // идти на точку удара
        } else {
          move.setSpeedUp(1.0D);
          move.setTurn(self.getAngleTo(attackPoint.x, attackPoint.y));
          move.setAction(ActionType.TAKE_PUCK);
          System.out.println("  Я ЗАЩИТНИК " + defender + " иду на точку удара!");
        }


        // если шайбой владеет сокомандник
      } else {
        // получаем ближайшего хоккеиста оппонента
        Hockeyist nearestOpponent = getNearestOpponent(self.getX(), self.getY(), world);
        // если ближайший оппонент найден
        if (nearestOpponent != null) {
          // если оппонент вне досягаемости клюшки
          if (self.getDistanceTo(nearestOpponent) > game.getStickLength()) {
            // беги с макс скоростью
            move.setSpeedUp(1.0D);
            System.out.println("  Я НАПАДАЮЩИЙ " + attacker + " Бегу отбирать шайбу");
            // иначе если могу ударить по шайбе - бить
          } else if (abs(self.getAngleTo(nearestOpponent)) < 0.5D * game.getStickSector()) {
            System.out.println("  Я НАПАДАЮЩИЙ " + attacker + " пытаюсь выбить шайбу!");
            move.setAction(ActionType.STRIKE);
          }
          // поворачивайся к хоккеисту чтоб бить
          move.setTurn(self.getAngleTo(nearestOpponent));
        }
      }
    } else {
      System.out.println("  Я НАПАДАЮЩИЙ " + attacker + " бегу утанавливать контроль над шайбой!");
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

  private static Hockeyist getNearestTeammate(double x, double y, World world) {
    Hockeyist nearestTeammate = null;
    double nearestTeammateRange = 0.0D;

    for (Hockeyist hockeyist : world.getHockeyists()) {
      if (!hockeyist.isTeammate() || hockeyist.getType() == HockeyistType.GOALIE
          || hockeyist.getState() == HockeyistState.KNOCKED_DOWN
          || hockeyist.getState() == HockeyistState.RESTING) {
        continue;
      }
      // исключаем самого себя
      if (hockeyist.getX() == x && hockeyist.getY() == y) {
        continue;
      }

      double teammateRange = hypot(x - hockeyist.getX(), y - hockeyist.getY());

      if (nearestTeammate == null || teammateRange < nearestTeammateRange) {
        nearestTeammate = hockeyist;
        nearestTeammateRange = teammateRange;
      }
    }

    return nearestTeammate;
  }


  // Расстояние между двумя хоккеистами
  private double getDistanceBetweenUnits(Unit h1, Unit h2) {
    return hypot(h1.getX() - h2.getX(), h1.getY() - h2.getY());
  }
}
