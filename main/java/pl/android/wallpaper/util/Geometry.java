package pl.android.wallpaper.util;

import static android.opengl.Matrix.multiplyMV;

/**
 * Created by Maciek on 2017-02-13.
 */

public class Geometry {
    public static class Point{
        public final float x, y, z;
        public Point(float x, float y, float z){
            this.x = x;
            this.y = y;
            this.z = z;
        }
        public Point translateY(float distance){
            return new Point(x, y+distance, z);
        }
        public Point translate(Vector vector){
            return new Point(x + vector.x, y+vector.y, z+vector.z);
        }
    }
    public static class Circle{
        public final Point center;
        public final float radius;

        public Circle(Point center, float radius){
            this.center = center;
            this.radius = radius;
        }
        public Circle scale(float scale){
            return new Circle(center, radius * scale);
        }
    }
    public static class Cylinder{
        public final Point center;
        public final float radius;
        public final float height;

        public Cylinder(Point center, float radius, float height){
            this.center = center;
            this.radius = radius;
            this.height = height;
        }
    }
    public static class Ray{
        public final Point point;
        public final Vector vector;

        public Ray(Point point, Vector vector){
            this.point = point;
            this.vector = vector;
        }
    }
    public static class Vector{
        public final float x, y, z;

        public Vector(float x, float y, float z){
            this.x = x;
            this.y = y;
            this.z = z;
        }
        public float length(){
            return (float) Math.sqrt(x*x + y*y + z*z);
        }
        public Vector crossProduct(Vector other){
            return new Vector(
                    (y * other.z) - (z * other.y),
                    (z * other.x) - (x * other.z),
                    (x * other.y) - (y * other.x));
        }
        public float dotProduct(Vector vector){
            return x * vector.x + y*vector.y + z*vector.z;
        }
        public Vector scale(float f){
            return new Vector(x*f, y*f, z*f);
        }
        public Vector normalize(){
            float len = this.length();
            return new Vector(x/len, y/len, z/len);
        }
    }
    public static class Sphere{
        public final Point center;
        public final float radius;

        public Sphere(Point center, float radius){
            this.center = center;
            this.radius = radius;
        }
    }

    public static Vector vectorBetween(Point from, Point to){
        return new Vector(to.x - from.x, to.y - from.y, to.z - from.z);
    }
    public static boolean intersects(Sphere sphere, Ray ray){
        return distanceBetween(sphere.center, ray) < sphere.radius;
    }
    public static float distanceBetween(Point point, Ray ray){
        Vector p1ToPoint = vectorBetween(ray.point, point);
        Vector p2ToPoint = vectorBetween(ray.point.translate(ray.vector), point);

        float areaOfTriangleTimesTwo = p1ToPoint.crossProduct(p2ToPoint).length();
        float lengthOfBase = ray.vector.length();

        float distanceFromPointtoRay = areaOfTriangleTimesTwo / lengthOfBase;
        return distanceFromPointtoRay;
    }
    public static class Plane{
        public final Point point;
        public final Vector normal;

        public Plane(Point point, Vector normal) {
            this.point = point;
            this.normal = normal;
        }
    }
    public static Point intersectionPoint(Ray ray, Plane plane){
        Vector rayToPlaneVector = vectorBetween(ray.point, plane.point);

        float scaleFactor = rayToPlaneVector.dotProduct(plane.normal) / ray.vector.dotProduct(plane.normal);

        Point intersectionPoint = ray.point.translate(ray.vector.scale(scaleFactor));
        return intersectionPoint;
    }
    public static Point getWorldPointFromNormalized(final float[] invertedViewProjectionMatrix, float X, float Y, float Z){
        final float[] pointNdc = {X, Y, Z, 1};
        final float[] pointWorld = new float[4];

        multiplyMV(pointWorld, 0, invertedViewProjectionMatrix, 0, pointNdc, 0);

        pointWorld[0] /= pointWorld[3];
        pointWorld[1] /= pointWorld[3];
        pointWorld[2] /= pointWorld[3];

        final Point point = new Point(pointWorld[0], pointWorld[1] , pointWorld[2]);
        return point;
    }

    public static Ray convertNormalized2DPointToRay(float normalizedX, float normalizedY, final float[] invertedViewProjectionMatrix){
        final float[] nearPointNdc = {normalizedX, normalizedY, -1, 1};//ndc - normalized device coordinates - (-1, 1)
        final float[] farPointNdc = {normalizedX, normalizedY, 1, 1};

        final float[] nearPointWorld = new float[4];
        final float[] farPointWorld = new float[4];

        multiplyMV(nearPointWorld, 0, invertedViewProjectionMatrix, 0, nearPointNdc, 0);
        multiplyMV(farPointWorld, 0, invertedViewProjectionMatrix, 0, farPointNdc, 0);

        divideByW(nearPointWorld);
        divideByW(farPointWorld);

        Geometry.Point nearPointRay = new Geometry.Point(nearPointWorld[0], nearPointWorld[1], nearPointWorld[2]);
        Geometry.Point farPointRay = new Geometry.Point(farPointWorld[0], farPointWorld[1], farPointWorld[2]);

        return new Geometry.Ray(nearPointRay, Geometry.vectorBetween(nearPointRay, farPointRay));
    }
    public static void divideByW(float[] vector){
        vector[0] /= vector[3];
        vector[1] /= vector[3];
        vector[2] /= vector[3];
    }
}
