package odometry;

import utility.RotationUtil;
import utility.pose;

public abstract class OdometryWheel {
    int ticksPerRev = 1024;
    double radius = 3; 
    pose offset;

    private double prevTicks = 0;
    protected double deltaTicks = 0;

    abstract long getRaw();

    // Offset is x and y displacement from center of rotation
    // and angle that wheel is facing (in trig coordinates).
    // For axially aligned wheel angles, choose the angle that
    // faces the POSITIVE direction of the axis
    public OdometryWheel(pose offset){
        this.offset = offset;
    }

    /**
     * Request and calculate the change in ticks of this odo wheel
     */
    void updateDelta(){
        prevTicks += deltaTicks;
        //get ticks
        double measurement = getRaw();
        //deltaTicks = RotationUtil.turnLeftOrRight(prevTicks, measurement, ticksPerRev);
        deltaTicks = measurement - prevTicks;
    }

    /**
     * @return the difference in ticks between the most recent time
     * updateDelta was called and the time before that
     */
    double getDeltaTicks(){
        return deltaTicks;
    }

    double getDeltaPosition(){
        return getDeltaTicks() / ticksPerRev * 2 * Math.PI * radius;
    }

    double distanceTraveledTowardsAngle(double deltaPosition, double targetAngle){
        return deltaPosition / cos(targetAngle - offset.r);
    }

    //effect of the bot trans vector on wheel
    //or WheelVec DOT odoWheelVec
    //inverse of distanceTraveledTowardsAngle
    double dotProduct(double botTransMag, double botTransDir){
        return botTransMag * Math.cos(botTransDir - offset.r);
    }

    double ccTangentDir(double xCenter, double yCenter){
        double directionFromCenter = Math.atan2(offset.y - yCenter, offset.x - xCenter);
        return directionFromCenter + Math.PI/2;
    }

    double arclengthToAngle (double arclength, double xCenter, double yCenter){
        return arclength / Math.hypot(offset.x - xCenter, offset.y - yCenter);
    }
    double angleToArclength(double angle, double xCenter, double yCenter){
        return angle * Math.hypot(offset.x - xCenter, offset.y - yCenter);
    }

    /**
     * Change in the odometry wheel's tracked distance converted
     * to radians of rotation around the robot's center of rotation
     * @param deltaPosition odo wheel distance change
     * @param xCenter center of rotation of robot
     * @param yCenter center of rotation of robot
     * @return change in robot angle about center of rotation
     */
    double odoDeltaToBotAngle(double deltaPosition, double xCenter, double yCenter){
        double arclength = distanceTraveledTowardsAngle(
                deltaPosition,
                ccTangentDir(xCenter, yCenter));
        return arclengthToAngle(arclength, xCenter, yCenter);
    }

    double robotAngleToOdoDelta(double angle, double xCenter, double yCenter){
        double arclength = angleToArclength(angle, xCenter, yCenter);
        return dotProduct(arclength, ccTangentDir(xCenter, yCenter));
    }

    double cos(double v){
        //if v = π/2 + nπ
        //if cos(v) pretty close to 0 return 0
        if((Math.abs(v-Math.PI/2) % Math.PI) < 0.01)
            return 0;

        return Math.cos(v);
    }

    double distanceToCenter(){
        return Math.hypot(offset.x, offset.y);
    }
}
