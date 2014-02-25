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
package jogamp.graph.curve.opengl;

import javax.media.opengl.GL;
import javax.media.opengl.GL2ES2;

import jogamp.graph.curve.opengl.shader.AttributeNames;

import com.jogamp.graph.curve.opengl.GLRegion;
import com.jogamp.graph.curve.opengl.RegionRenderer;
import com.jogamp.graph.geom.Vertex;
import com.jogamp.graph.geom.Triangle;
import com.jogamp.opengl.util.GLArrayDataServer;
import com.jogamp.opengl.util.glsl.ShaderState;

public class VBORegionSPES2 extends GLRegion {
    private GLArrayDataServer verticeAttr = null;
    private GLArrayDataServer texCoordAttr = null;
    private GLArrayDataServer indicesBuffer = null;

    public VBORegionSPES2(final int renderModes) {
        super(renderModes);
    }

    @Override
    public void update(final GL2ES2 gl, final RegionRenderer renderer) {
        if(null == indicesBuffer) {
            final int initialElementCount = 256;
            final ShaderState st = renderer.getShaderState();

            indicesBuffer = GLArrayDataServer.createData(3, GL2ES2.GL_SHORT, initialElementCount, GL.GL_STATIC_DRAW, GL.GL_ELEMENT_ARRAY_BUFFER);

            verticeAttr = GLArrayDataServer.createGLSL(AttributeNames.VERTEX_ATTR_NAME, 3, GL2ES2.GL_FLOAT,
                    false, initialElementCount, GL.GL_STATIC_DRAW);
            st.ownAttribute(verticeAttr, true);

            texCoordAttr = GLArrayDataServer.createGLSL(AttributeNames.TEXCOORD_ATTR_NAME, 2, GL2ES2.GL_FLOAT,
                    false, initialElementCount, GL.GL_STATIC_DRAW);
            st.ownAttribute(texCoordAttr, true);

            if(DEBUG_INSTANCE) {
                System.err.println("VBORegionSPES2 Create: " + this);
            }
        }

        if(DEBUG_INSTANCE) {
            System.err.println("VBORegionSPES2 Indices of "+triangles.size()+", triangles");
        }
        validateIndices();
        // process triangles
        indicesBuffer.seal(gl, false);
        indicesBuffer.rewind();
        for(int i=0; i<triangles.size(); i++) {
            final Triangle t = triangles.get(i);
            final Vertex[] t_vertices = t.getVertices();

            if(t_vertices[0].getId() == Integer.MAX_VALUE){
                throw new RuntimeException("Ooops Triangle #"+i+" - has unindexed vertices");
            } else {
                indicesBuffer.puts((short) t_vertices[0].getId());
                indicesBuffer.puts((short) t_vertices[1].getId());
                indicesBuffer.puts((short) t_vertices[2].getId());
                if(DEBUG_INSTANCE) {
                    System.err.println("VBORegionSPES2.Indices.2: "+
                            t_vertices[0].getId()+", "+t_vertices[1].getId()+", "+t_vertices[2].getId());
                }
            }
        }
        indicesBuffer.seal(gl, true);
        indicesBuffer.enableBuffer(gl, false);

        // process vertices and update bbox
        box.reset();
        verticeAttr.seal(gl, false);
        verticeAttr.rewind();
        texCoordAttr.seal(gl, false);
        texCoordAttr.rewind();
        for(int i=0; i<vertices.size(); i++) {
            final Vertex v = vertices.get(i);
            verticeAttr.putf(v.getX());
            verticeAttr.putf(v.getY());
            verticeAttr.putf(v.getZ());
            box.resize(v.getX(), v.getY(), v.getZ());

            final float[] tex = v.getTexCoord();
            texCoordAttr.putf(tex[0]);
            texCoordAttr.putf(tex[1]);
        }
        verticeAttr.seal(gl, true);
        verticeAttr.enableBuffer(gl, false);
        texCoordAttr.seal(gl, true);
        texCoordAttr.enableBuffer(gl, false);
    }

    @Override
    protected void drawImpl(final GL2ES2 gl, final RegionRenderer renderer, final int[/*1*/] texWidth) {
        verticeAttr.enableBuffer(gl, true);
        texCoordAttr.enableBuffer(gl, true);
        indicesBuffer.bindBuffer(gl, true); // keeps VBO binding

        gl.glDrawElements(GL2ES2.GL_TRIANGLES, indicesBuffer.getElementCount() * indicesBuffer.getComponentCount(), GL2ES2.GL_UNSIGNED_SHORT, 0);

        indicesBuffer.bindBuffer(gl, false);
        texCoordAttr.enableBuffer(gl, false);
        verticeAttr.enableBuffer(gl, false);
    }

    @Override
    public void destroy(final GL2ES2 gl, final RegionRenderer renderer) {
        if(DEBUG_INSTANCE) {
            System.err.println("VBORegionSPES2 Destroy: " + this);
        }
        final ShaderState st = renderer.getShaderState();
        if(null != verticeAttr) {
            st.ownAttribute(verticeAttr, false);
            verticeAttr.destroy(gl);
            verticeAttr = null;
        }
        if(null != texCoordAttr) {
            st.ownAttribute(texCoordAttr, false);
            texCoordAttr.destroy(gl);
            texCoordAttr = null;
        }
        if(null != indicesBuffer) {
            indicesBuffer.destroy(gl);
            indicesBuffer = null;
        }
    }
}
