package org.example;

import lombok.Getter;
import lombok.Setter;
import org.joml.Vector2d;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class Node {

    private double x, y;
    private double weight;
    private Set<Node> neighbors=new HashSet<Node>();
    private boolean isHole=false;
    private int color;

    public Node(double x, double y) {
        this.x = x;
        this.y = y;
    }

    public void displace(Vector2d displacement) {
        x += displacement.x;
        y += displacement.y;
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + weight + ")";
    }
}
