/**
 * Copyright 2010 JogAmp Community. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 *    1. Redistributions of source code must retain the above copyright notice, this list of
 *       conditions and the following disclaimer.
 *
 *    2. Redistributions in binary form must reproduce the above copyright notice, this list
 *       of conditions and the following disclaimer in the documentation and/or other materials
 *       provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY JogAmp Community ``AS IS'' AND ANY EXPRESS OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL JogAmp Community OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF
 * ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * The views and conclusions contained in the software and documentation are those of the
 * authors and should not be interpreted as representing official policies, either expressed
 * or implied, of JogAmp Community.
 */
package com.jogamp.graph.curve;

import java.util.ArrayList;
import java.util.List;

import jogamp.graph.curve.opengl.VBORegion2PES2;
import jogamp.graph.curve.opengl.VBORegionSPES2;
import jogamp.graph.geom.plane.AffineTransform;
import jogamp.opengl.Debug;

import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.opengl.math.geom.AABBox;

/**
 * Abstract Outline shape representation define the method an OutlineShape(s)
 * is bound and rendered.
 *
 * @see GLRegion */
public abstract class Region {

    /** Debug flag for region impl (graph.curve) */
    public static final boolean DEBUG = Debug.debug("graph.curve");
    public static final boolean DEBUG_INSTANCE = Debug.debug("graph.curve.instance");

    /** View based Anti-Aliasing, A Two pass region rendering, slower and more
     * resource hungry (FBO), but AA is perfect. Otherwise the default fast one
     * pass MSAA region rendering is being used. */
    public static final int VBAA_RENDERING_BIT = 1 << 0;

    /** Use non uniform weights [0.0 .. 1.9] for curve region rendering.
     * Otherwise the default weight 1.0 for uniform curve region rendering is
     * being applied. */
    public static final int VARIABLE_CURVE_WEIGHT_BIT = 1 << 1;

    public static final int TWO_PASS_DEFAULT_TEXTURE_UNIT = 0;

    private final int renderModes;
    private boolean dirty = true;
    protected int numVertices = 0;
    protected final AABBox box = new AABBox();
    /** FIXME: Think about a rendering storage optimization (VBO ... )! */
    protected ArrayList<Triangle> triangles = new ArrayList<Triangle>();
    /** FIXME: Think about a rendering storage optimization (VBO ... )! */
    protected ArrayList<Vertex> vertices = new ArrayList<Vertex>();

    public static boolean isVBAA(int renderModes) {
        return 0 != (renderModes & Region.VBAA_RENDERING_BIT);
    }

    /** Check if render mode capable of non uniform weights
     *
     * @param renderModes
     *            bit-field of modes, e.g.
     *            {@link Region#VARIABLE_CURVE_WEIGHT_BIT},
     *            {@link Region#VBAA_RENDERING_BIT}
     * @return true of capable of non uniform weights */
    public static boolean isNonUniformWeight(int renderModes) {
        return 0 != (renderModes & Region.VARIABLE_CURVE_WEIGHT_BIT);
    }

    /**
     * Create a Region using the passed render mode
     *
     * <p> In case {@link Region#VBAA_RENDERING_BIT} is being requested the default texture unit
     * {@link Region#TWO_PASS_DEFAULT_TEXTURE_UNIT} is being used.</p>
     *
     * @param rs the RenderState to be used
     * @param renderModes bit-field of modes, e.g. {@link Region#VARIABLE_CURVE_WEIGHT_BIT}, {@link Region#VBAA_RENDERING_BIT}
     */
    public static GLRegion create(int renderModes) {
        if( 0 != ( Region.VBAA_RENDERING_BIT & renderModes ) ){
            return new VBORegion2PES2(renderModes, Region.TWO_PASS_DEFAULT_TEXTURE_UNIT);
        }
        else{
            return new VBORegionSPES2(renderModes);
        }
    }

    protected Region(int regionRenderModes) {
        this.renderModes = regionRenderModes;
    }

    /** Get current Models
     *
     * @return bit-field of render modes */
    public final int getRenderModes() {
        return renderModes;
    }


    public final ArrayList<Triangle> getTriangles() { return triangles; }

    public final ArrayList<Vertex> getVertices() { return vertices; }

    /** Check if current Region is using VBAA
     *
     * @return true if capable of two pass rendering - VBAA */
    public boolean isVBAA() {
        return Region.isVBAA(renderModes);
    }

    /** Check if current instance uses non uniform weights
     *
     * @return true if capable of nonuniform weights */
    public boolean isNonUniformWeight() {
        return Region.isNonUniformWeight(renderModes);
    }

    /** Get the current number of vertices associated with this region. This
     * number is not necessary equal to the OGL bound number of vertices.
     *
     * @return vertices count */
    public final int getNumVertices() {
        return numVertices;
    }

    /** Adds a list of {@link Triangle} objects to the Region These triangles are
     * to be binded to OGL objects on the next call to {@code update}
     *
     * @param tris
     *            a list of triangle objects
     * @param idxOffset TODO
     *
     * @see update(GL2ES2) */
    public void addTriangles(List<Triangle> tris, AffineTransform t, int idxOffset) {
        if( true && null != t ) {
            for(int i=0; i<tris.size(); i++) {
                final Triangle t2 = tris.get(i).transform(t);
                t2.addVertexIndicesOffset(idxOffset);
                triangles.add( t2 );
            }
        } else {
            for(int i=0; i<tris.size(); i++) {
                final Triangle t2 = new Triangle( tris.get(i) );
                t2.addVertexIndicesOffset(idxOffset);
                triangles.add( t2 );
            }
            // triangles.addAll(tris);
        }
        if(DEBUG_INSTANCE) {
            System.err.println("Region.addTriangles(): tris: "+triangles.size()+", verts "+vertices.size());
        }
        setDirty(true);
    }

    /** Adds a {@link Vertex} object to the Region This vertex will be bound to
     * OGL objects on the next call to {@code update}
     *
     * @param vert
     *            a vertex objects
     *
     * @see update(GL2ES2) */
    public void addVertex(Vertex vert, AffineTransform t) {
        final Vertex svert = null != t ? t.transform(vert, null) : vert;
        vertices.add(svert);
        numVertices++;
        assert( vertices.size() == numVertices );
        if(DEBUG_INSTANCE) {
            System.err.println("Region.addVertex(): tris: "+triangles.size()+", verts "+vertices.size());
        }
        setDirty(true);
    }

    /** Adds a list of {@link Vertex} objects to the Region These vertices are to
     * be binded to OGL objects on the next call to {@code update}
     *
     * @param verts
     *            a list of vertex objects
     *
     * @see update(GL2ES2) */
    public void addVertices(List<Vertex> verts) {
        vertices.addAll(verts);
        numVertices = vertices.size();
        if(DEBUG_INSTANCE) {
            System.err.println("Region.addVertices(): tris: "+triangles.size()+", verts "+vertices.size());
        }
        setDirty(true);
    }

    public void addOutlineShape(OutlineShape shape, AffineTransform t) {
        final List<Triangle> tris = shape.getTriangles(OutlineShape.VerticesState.QUADRATIC_NURBS);
        if(null != tris) {
            if( false && null != t ) {
                for(int i=0; i<tris.size(); i++) {
                    triangles.add( tris.get(i).transform(t) );
                }
            } else {
                triangles.addAll(tris);
            }
            // final List<Vertex> verts = shape.getVertices();
            // vertices.addAll(verts);
            // FIXME: use OutlineShape.getVertices(Runnable task-per-vertex) !!
            for (int j = 0; j < shape.outlines.size(); j++) {
                final ArrayList<Vertex> sovs = shape.outlines.get(j).getVertices();
                for (int k = 0; k < sovs.size(); k++) {
                    final Vertex v;
                    if( null != t ) {
                        v = t.transform(sovs.get(k), null);
                    } else {
                        v = sovs.get(k);
                    }
                    v.setId(numVertices++);
                    vertices.add(v);
                }
            }
            // numVertices = vertices.size();
        }
        if(DEBUG_INSTANCE) {
            System.err.println("Region.addOutlineShape(): tris: "+triangles.size()+", verts "+vertices.size());
        }
        setDirty(true);
    }

    public void validateIndices() {
        final int verticeCountPre = vertices.size();
        for(int i=0; i<triangles.size(); i++) {
            final Triangle t = triangles.get(i);
            final Vertex[] t_vertices = t.getVertices();

            if(t_vertices[0].getId() == Integer.MAX_VALUE){
                t_vertices[0].setId(numVertices++);
                t_vertices[1].setId(numVertices++);
                t_vertices[2].setId(numVertices++);
                vertices.add(t_vertices[0]);
                vertices.add(t_vertices[1]);
                vertices.add(t_vertices[2]);
            }
        }
        if( verticeCountPre < vertices.size() ) {
            setDirty(true);
        }
    }

    public void addOutlineShapes(List<OutlineShape> shapes) {
        for (int i = 0; i < shapes.size(); i++) {
            addOutlineShape(shapes.get(i), null);
        }
        if(DEBUG_INSTANCE) {
            System.err.println("Region.addOutlineShapes(): tris: "+triangles.size()+", verts "+vertices.size());
        }
        setDirty(true);
    }

    /** @return the AxisAligned bounding box of current region */
    public final AABBox getBounds() {
        return box;
    }

    /** Check if this region is dirty. A region is marked dirty when new
     * Vertices, Triangles, and or Lines are added after a call to update()
     *
     * @return true if region is Dirty, false otherwise
     *
     * @see update(GL2ES2) */
    public final boolean isDirty() {
        return dirty;
    }

    protected final void setDirty(boolean v) {
        dirty = v;
    }
}