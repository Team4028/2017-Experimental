package org.usfirst.frc.team4028.robot.paths.profiles;

import java.util.ArrayList;

import org.usfirst.frc.team4028.robot.Constants;
import org.usfirst.frc.team4028.robot.paths.PathBuilder;
import org.usfirst.frc.team4028.robot.paths.PathBuilder.Waypoint;
import org.usfirst.frc.team4028.util.control.Path;
import org.usfirst.frc.team4028.util.motion.RigidTransform;
import org.usfirst.frc.team4028.util.motion.Rotation;
import org.usfirst.frc.team4028.util.motion.Translation;

public class PathAdapter {
	static final RobotProfile kRobotProfile = new PracticeBot();
    static final FieldProfile kFieldProfile = new PracticeField();

    // Path Variables
    static final double kLargeRadius = 45;
    static final double kModerateRadius = 30;
    static final double kNominalRadius = 20;
    static final double kSmallRadius = 10;
    static final double kSpeed = 80;

    // Don't mess with these
    static final double kPegOffsetX = 17.77; // center of airship to boiler peg
    static final double kPegOffsetY = 30.66; // front of airship to boiler
                                             // pegkRobotProfile.getBlueBoilerGearXCorrection()
    static final Rotation kRedPegHeading = Rotation.fromDegrees(240);
    static final Rotation kBluePegHeading = Rotation.fromDegrees(125);
    static final Rotation kRedHopperHeading = Rotation.fromDegrees(45); // angle to hit the red hopper at
    static final Rotation kBlueHopperHeading = Rotation.fromDegrees(315); // angle to hit the blue hopper at
    static final Rotation kStartHeading = Rotation.fromDegrees(180); // start angle (backwards)
    static final double kGearPlacementDist = Constants.kCenterToRearBumperDistance + 10; // distance away from the
                                                                                         // airship wall to place the
                                                                                         // gear at
    static final double kHopperOffsetX = 3.0; // How far from the closest edge of the hopper to aim
    static final double kHopperSkew = 6.0; // How far into the wall to place the final point (to ensure we keep nudging
                                           // into the wall)
    static final double kFrontDist = Constants.kCenterToIntakeDistance;
    static final double kSideDist = Constants.kCenterToSideBumperDistance;
    static final double kHopperTurnDistance = 40; // how long the third segment in the hopper path should be
    static final double kGearTurnDistance = 24; // how long the first segment in the hopper path should be
    static final double kEndHopperPathX = 84.5; // X position we want the hopper path to end at
    static final double kFieldHeight = 324; // total height of the field in inches (doesn't really have to be accurate,
                                            // everything is relative)

    public static Translation getRedHopperPosition() {
        Translation contactPoint = new Translation(
                kFieldProfile.getRedWallToHopper() + kHopperOffsetX + kRobotProfile.getRedHopperXOffset(),
                kFieldHeight / 2 - kFieldProfile.getRedCenterToHopper() - kRobotProfile.getRedHopperYOffset());
        Translation robotOffset = new Translation(kFrontDist, kSideDist);
        robotOffset = robotOffset.direction().rotateBy(kRedHopperHeading).toTranslation().scale(robotOffset.norm());
        return contactPoint.translateBy(robotOffset);
    }

    // third point in the hopper path
    public static Translation getRedHopperTurnPosition() {
        Translation hopperPosition = getRedHopperPosition();
        Translation turnOffset = new Translation(kRedHopperHeading.cos() * kHopperTurnDistance,
                kRedHopperHeading.sin() * kHopperTurnDistance);
        return hopperPosition.translateBy(turnOffset);
    }

    // second point in the hopper path
    public static Translation getRedGearTurnPosition() {
        Translation gearPosition = getRedGearPosition();
        Translation turnOffset = new Translation(kRedPegHeading.cos() * kGearTurnDistance,
                kRedPegHeading.sin() * kGearTurnDistance);
        return gearPosition.translateBy(turnOffset);
    }

    public static Translation getRedGearCorrection() {
        return RigidTransform.fromRotation(kRedPegHeading)
                .transformBy(RigidTransform
                        .fromTranslation((new Translation(-kRobotProfile.getRedBoilerGearXCorrection(),
                                -kRobotProfile.getRedBoilerGearYCorrection()))))
                .getTranslation();
    }

    // final position in the gear path, first position in the hopper path
    public static Translation getRedGearPosition() {
        Translation pegPosition = new Translation(kFieldProfile.getRedWallToAirship() + kPegOffsetX,
                kFieldHeight / 2 - kPegOffsetY);
        Translation robotOffset = new Translation(kRedPegHeading.cos() * kGearPlacementDist,
                kRedPegHeading.sin() * kGearPlacementDist);
        return pegPosition.translateBy(robotOffset);
    }

    private static Translation getRedGearPositionCorrected() {
        return getRedGearPosition().translateBy(getRedGearCorrection());
    }

    // first position in the gear path
    public static RigidTransform getRedStartPose() {
        return new RigidTransform(new Translation(Constants.kCenterToFrontBumperDistance,
                kFieldHeight / 2 - kFieldProfile.getRedCenterToBoiler() + Constants.kCenterToSideBumperDistance),
                kStartHeading);
    }

    // second position in the gear path
    private static Translation getRedCenterPosition() {
        RigidTransform end = new RigidTransform(getRedGearPositionCorrected(), kRedPegHeading);
        return getRedStartPose().intersection(end);
    }

    private static Path sRedGearPath = null;

    public static Path getRedGearPath() {
        if (sRedGearPath == null) {
            ArrayList<Waypoint> sWaypoints = new ArrayList<Waypoint>();
            sWaypoints.add(new Waypoint(getRedStartPose().getTranslation(), 0, kSpeed));
            sWaypoints.add(new Waypoint(getRedCenterPosition(), kLargeRadius, kSpeed));
            sWaypoints.add(new Waypoint(getRedGearPositionCorrected(), 0, kSpeed));

            sRedGearPath = PathBuilder.buildPathFromWaypoints(sWaypoints);
        }
        return sRedGearPath;
    }

    private static Path sRedHopperPath = null;

    public static Path getRedHopperPath() {
        if (sRedHopperPath == null) {
            ArrayList<Waypoint> sWaypoints = new ArrayList<Waypoint>();
            sWaypoints.add(new Waypoint(getRedGearPosition(), 0, kSpeed));
            sWaypoints.add(new Waypoint(getRedGearTurnPosition(), kSmallRadius, kSpeed));
            sWaypoints.add(new Waypoint(getRedHopperTurnPosition(), kModerateRadius, kSpeed));
            sWaypoints.add(new Waypoint(getRedHopperPosition(), kSmallRadius, kSpeed));

            Translation redHopperEndPosition = new Translation(getRedHopperPosition());
            redHopperEndPosition.setX(kEndHopperPathX); // move X position to desired place
            redHopperEndPosition.setY(redHopperEndPosition.y() - kHopperSkew); // TODO make constant
            sWaypoints.add(new Waypoint(redHopperEndPosition, 0, kSpeed));
            sRedHopperPath = PathBuilder.buildPathFromWaypoints(sWaypoints);
        }
        return sRedHopperPath;

    }

    public static Translation getBlueHopperPosition() {
        Translation contactPoint = new Translation(
                kFieldProfile.getBlueWallToHopper() + kHopperOffsetX + kRobotProfile.getBlueHopperXOffset(),
                kFieldHeight / 2 + kFieldProfile.getBlueCenterToHopper() + kRobotProfile.getBlueHopperYOffset());
        Translation robotOffset = new Translation(kFrontDist, -kSideDist);
        robotOffset = robotOffset.direction().rotateBy(kBlueHopperHeading).toTranslation().scale(robotOffset.norm());
        return contactPoint.translateBy(robotOffset);
    }

    public static Translation getBlueHopperTurnPosition() {
        Translation hopperPosition = getBlueHopperPosition();
        Translation turnOffset = new Translation(kBlueHopperHeading.cos() * kHopperTurnDistance,
                kBlueHopperHeading.sin() * kHopperTurnDistance);
        return hopperPosition.translateBy(turnOffset);
    }

    public static Translation getBlueGearTurnPosition() {
        Translation gearPosition = getBlueGearPosition();
        Translation turnOffset = new Translation(kBluePegHeading.cos() * kGearTurnDistance,
                kBluePegHeading.sin() * kGearTurnDistance);
        return gearPosition.translateBy(turnOffset);
    }

    public static Translation getBlueGearCorrection() {
        return RigidTransform.fromRotation(kBluePegHeading)
                .transformBy(RigidTransform
                        .fromTranslation((new Translation(-kRobotProfile.getBlueBoilerGearXCorrection(),
                                -kRobotProfile.getBlueBoilerGearYCorrection()))))
                .getTranslation();
    }

    private static Translation getBlueGearPosition() {
        Translation pegPosition = new Translation(kFieldProfile.getBlueWallToAirship() + kPegOffsetX,
                kFieldHeight / 2 + kPegOffsetY);
        Translation robotOffset = new Translation(kBluePegHeading.cos() * kGearPlacementDist,
                kBluePegHeading.sin() * kGearPlacementDist);
        return pegPosition.translateBy(robotOffset);
    }

    private static Translation getBlueGearPositionCorrected() {
        return getBlueGearPosition().translateBy(getBlueGearCorrection());
    }

    public static RigidTransform getBlueStartPose() {
        return new RigidTransform(new Translation(Constants.kCenterToFrontBumperDistance,
                kFieldHeight / 2 + kFieldProfile.getBlueCenterToBoiler() - Constants.kCenterToSideBumperDistance),
                kStartHeading);
    }

    private static Translation getBlueCenterPosition() {
        RigidTransform end = new RigidTransform(getBlueGearPositionCorrected(), kBluePegHeading);
        return getBlueStartPose().intersection(end);
    }

    private static Path sBlueGearPath = null;

    public static Path getBlueGearPath() {
        if (sBlueGearPath == null) {
            ArrayList<Waypoint> sWaypoints = new ArrayList<Waypoint>();
            sWaypoints.add(new Waypoint(getBlueStartPose().getTranslation(), 0, kSpeed));
            sWaypoints.add(new Waypoint(getBlueCenterPosition(), kLargeRadius, kSpeed));
            sWaypoints.add(new Waypoint(getBlueGearPositionCorrected(), 0, kSpeed));

            sBlueGearPath = PathBuilder.buildPathFromWaypoints(sWaypoints);
        }
        return sBlueGearPath;
    }

    private static Path sBlueHopperPath = null;

    public static Path getBlueHopperPath() {
        if (sBlueHopperPath == null) {
            ArrayList<Waypoint> sWaypoints = new ArrayList<Waypoint>();
            sWaypoints.add(new Waypoint(getBlueGearPosition(), 0, 0));
            sWaypoints.add(new Waypoint(getBlueGearTurnPosition(), kSmallRadius, kSpeed));
            sWaypoints.add(new Waypoint(getBlueHopperTurnPosition(), kModerateRadius, kSpeed));
            sWaypoints.add(new Waypoint(getBlueHopperPosition(), kSmallRadius, kSpeed));

            Translation blueHopperEndPosition = new Translation(getBlueHopperPosition());
            blueHopperEndPosition.setX(kEndHopperPathX); // move x position to desired place
            blueHopperEndPosition.setY(blueHopperEndPosition.y() + kHopperSkew);
            sWaypoints.add(new Waypoint(blueHopperEndPosition, 0, kSpeed));

            sBlueHopperPath = PathBuilder.buildPathFromWaypoints(sWaypoints);
        }
        return sBlueHopperPath;
    }

    public static void calculatePaths() {
        getBlueHopperPath();
        getRedHopperPath();
        getBlueGearPath();
        getRedGearPath();
    }

    public static void main(String[] args) {
        System.out.println("Red:\n" + getRedStartPose().getTranslation());
        System.out.println("Center: " + getRedCenterPosition());
        System.out.println("Gear: " + getRedGearPositionCorrected());
        System.out.println("Gear turn: " + getRedGearTurnPosition());
        System.out.println("Hopper turn: " + getRedHopperTurnPosition());
        System.out.println("Hopper: " + getRedHopperPosition());
        System.out.println("Start to boiler gear path:\n" + getRedGearPath());
        System.out.println("Boiler gear to hopper path:\n" + getRedHopperPath());
        System.out.println("\nBlue:\n" + getBlueStartPose().getTranslation());
        System.out.println("Center: " + getBlueCenterPosition());
        System.out.println("Gear: " + getBlueGearPositionCorrected());
        System.out.println("Gear turn: " + getBlueGearTurnPosition());
        System.out.println("Hopper turn: " + getBlueHopperTurnPosition());
        System.out.println("Hopper: " + getBlueHopperPosition());
        System.out.println("Start to boiler gear path:\n" + getBlueGearPath());
        System.out.println("Boiler gear to hopper path:\n" + getBlueHopperPath());
    }
}