package com.vbs.capsAllocation.util;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class GeoUtil {

    public static boolean isPointInsidePolygon(double userLat, double userLng, List<double[]> polygon) {
        int n = polygon.size();
        boolean inside = false;

        for (int i = 0, j = n - 1; i < n; j = i++) {
            double xi = polygon.get(i)[0], yi = polygon.get(i)[1];
            double xj = polygon.get(j)[0], yj = polygon.get(j)[1];

            boolean intersect = ((yi > userLng) != (yj > userLng)) &&
                    (userLat < (xj - xi) * (userLng - yi) / (yj - yi) + xi);
            if (intersect) inside = !inside;
        }

        return inside;
    }

    public static List<double[]> getOfficePolygon() {
        return List.of(
                new double[]{28.401818, 77.037728},
                new double[]{28.402564, 77.037684},
                new double[]{28.402576, 77.039084},
                new double[]{28.401924, 77.039105}
        );
    }
}
