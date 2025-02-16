package com.gregtechceu.gtceu.api.quickhull3d;

/*
 * #%L
 * A Robust 3D Convex Hull Algorithm in Java
 * %%
 * Copyright (C) 2004 - 2014 John E. Lloyd
 * %%
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents the half-edges that surround each face in a counter-clockwise
 * direction.
 *
 * @author John E. Lloyd, Fall 2004
 */
@NoArgsConstructor
public class HalfEdge {

    /**
     * The vertex associated with the head of this half-edge.
     */
    protected Vertex vertex;

    /**
     * Triangular face associated with this half-edge.
     */
    @Getter
    protected Face face;

    /**
     * Next half-edge in the triangle.
     */
    @Getter
    @Setter
    protected HalfEdge next;

    /**
     * Previous half-edge in the triangle.
     */
    @Getter
    @Setter
    protected HalfEdge prev;

    /**
     * Half-edge associated with the opposite triangle adjacent to this edge.
     */
    @Getter
    protected HalfEdge opposite;

    /**
     * Constructs a HalfEdge with head vertex <code>v</code> and left-hand
     * triangular face <code>f</code>.
     *
     * @param v head vertex
     * @param f left-hand triangular face
     */
    public HalfEdge(Vertex v, Face f) {
        vertex = v;
        face = f;
    }

    /**
     * Sets the half-edge opposite to this half-edge.
     *
     * @param edge opposite half-edge
     */
    public void setOpposite(HalfEdge edge) {
        opposite = edge;
        edge.opposite = this;
    }

    /**
     * Returns the head vertex associated with this half-edge.
     *
     * @return head vertex
     */
    public Vertex head() {
        return vertex;
    }

    /**
     * Returns the tail vertex associated with this half-edge.
     *
     * @return tail vertex
     */
    public Vertex tail() {
        return prev != null ? prev.vertex : null;
    }

    /**
     * Returns the opposite triangular face associated with this half-edge.
     *
     * @return opposite triangular face
     */
    public Face oppositeFace() {
        return opposite != null ? opposite.face : null;
    }

    /**
     * Produces a string identifying this half-edge by the point index values of
     * its tail and head vertices.
     *
     * @return identifying string
     */
    public String getVertexString() {
        if (tail() != null) {
            return tail().index + "-" + head().index;
        } else {
            return "?-" + head().index;
        }
    }

    /**
     * Returns the length of this half-edge.
     *
     * @return half-edge length
     */
    public double length() {
        if (tail() != null) {
            return head().pnt.distance(tail().pnt);
        } else {
            return -1;
        }
    }

    /**
     * Returns the length squared of this half-edge.
     *
     * @return half-edge length squared
     */
    public double lengthSquared() {
        if (tail() != null) {
            return head().pnt.distanceSquared(tail().pnt);
        } else {
            return -1;
        }
    }
}
