package com.exnw.browedit.render;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;

import javax.media.opengl.GL;
import javax.media.opengl.GLException;

import com.exnw.browedit.data.Map;
import com.exnw.browedit.data.Rsm;
import com.exnw.browedit.data.Rsm.RsmMesh;
import com.exnw.browedit.data.Rsw;
import com.exnw.browedit.grflib.GrfLib;
import com.exnw.browedit.math.Matrix4;
import com.exnw.browedit.math.Vector2;
import com.exnw.browedit.math.Vector3;
import com.sun.opengl.util.texture.Texture;
import com.sun.opengl.util.texture.TextureIO;

public class RsmRenderer implements Renderer
{
	Rsw.ModelResource modelProperties;
	Rsm rsm;
	Map map;

	MeshRenderer root;
	List<Texture> textures = new ArrayList<Texture>();

	RsmRenderer(Rsw.ModelResource resource, Map map)
	{
		this.map = map;
		this.modelProperties = resource;
		rsm = new Rsm("data\\model\\" + modelProperties.getModelname());
		
		for(String t : rsm.getTextures())
			textures.add(TextureCache.getTexture("data\\texture\\" + t));
		
		for(Rsm.RsmMesh mesh : rsm.getMeshes())
		{
			if(mesh.getName().equals(rsm.getRoot()))
			{
				root = new MeshRenderer(mesh, rsm);
				break;
			}
		}
		
	}
	public void render(GL gl)
	{
		gl.glPushMatrix();
		gl.glTranslatef(5*map.getGnd().getWidth()+modelProperties.getPosition().getX(), -modelProperties.getPosition().getY(), 5*map.getGnd().getHeight()-modelProperties.getPosition().getZ());
		gl.glRotatef(-modelProperties.getRotation().getZ(), 0, 0, 1);
		gl.glRotatef(-modelProperties.getRotation().getX(), 1, 0, 0);
		gl.glRotatef(modelProperties.getRotation().getY(), 0, 1, 0);
		gl.glScalef(modelProperties.getScale().getX(), -modelProperties.getScale().getY(), modelProperties.getScale().getZ());
		
		gl.glEnable(GL.GL_TEXTURE_2D);
		root.render(gl);
		
		gl.glPopMatrix();
	}
	public void update(Observable o, Object arg)
	{
		
	}
	
	
	class MeshRenderer
	{
		Rsm.RsmMesh rsmMesh;
		List<Texture> textures = new ArrayList<Texture>();
		private List<MeshRenderer> subMeshes;
		private Matrix4 matrix1;
		private Matrix4 matrix2;
		
		public MeshRenderer(RsmMesh rsmMesh, Rsm rsm)
		{
			this.rsmMesh = rsmMesh;
			this.subMeshes = new ArrayList<MeshRenderer>();
			
			for(Rsm.RsmMesh mesh : rsm.getMeshes())
			{
				if(mesh.getParent().equals(this.rsmMesh.getName()))
				{
					this.subMeshes.add(new MeshRenderer(mesh, rsm));
				}
			}
			
			for(Integer tid : rsmMesh.getTextureids())
				textures.add(RsmRenderer.this.textures.get(tid.intValue()));
		}
		
		
		public void render(GL gl)
		{
			gl.glMultMatrixf(getMatrix1().getData(), 0);
			gl.glPushMatrix();
			gl.glMultMatrixf(getMatrix2().getData(), 0);
			
			for(RsmMesh.Surface surface : rsmMesh.getSurfaces())
			{
				textures.get(surface.getTextureid()).bind();
				gl.glBegin(GL.GL_TRIANGLES);
				Vector3 v1 = rsmMesh.getVertices().get(surface.getSurfacevertices()[0]);
				Vector3 v2 = rsmMesh.getVertices().get(surface.getSurfacevertices()[1]);
				Vector3 v3 = rsmMesh.getVertices().get(surface.getSurfacevertices()[2]);
				
				Vector2 t1 = rsmMesh.getTextureCoordinats().get(surface.getTexturevertices()[0]).getCoodinates();
				Vector2 t2 = rsmMesh.getTextureCoordinats().get(surface.getTexturevertices()[1]).getCoodinates();
				Vector2 t3 = rsmMesh.getTextureCoordinats().get(surface.getTexturevertices()[2]).getCoodinates();
				
			/*	v1 = new Vector3(v1.getX(), -v1.getY(), v1.getZ());
				v2 = new Vector3(v2.getX(), -v2.getY(), v2.getZ());
				v3 = new Vector3(v3.getX(), -v3.getY(), v3.getZ());*/
	
				gl.glTexCoord2fv(t1.getData(),0);
				gl.glVertex3fv(v1.getData(),0);
				gl.glTexCoord2fv(t2.getData(),0);
				gl.glVertex3fv(v2.getData(),0);
				gl.glTexCoord2fv(t3.getData(),0);
				gl.glVertex3fv(v3.getData(),0);
				gl.glEnd();
				
			}
			
			gl.glPopMatrix();
			for(MeshRenderer renderer : subMeshes)
			{
				gl.glPushMatrix();
				renderer.render(gl);
				gl.glPopMatrix();
			}
		}
		
		public boolean isRoot()
		{
			return true;
		}
		
		
		public Matrix4 getMatrix1()
		{
			if(matrix1 != null)
				return matrix1;
			
			matrix1 = new Matrix4();
			
			if(isRoot())
			{
				if(subMeshes.size() > 0)
					matrix1.mult(Matrix4.makeTranslation(0,0,0));//-base->bbrange.x, -base->bbmax.y, -base->bbrange.z
				else
					matrix1.mult(Matrix4.makeTranslation(0,0,0));//0, -base->bbmax.y+base->bbrange.y, 0);
			}
			
			if(!isRoot())
				matrix1.mult(Matrix4.makeTranslation(rsmMesh.getPosition2().getX(), rsmMesh.getPosition2().getY(), rsmMesh.getPosition2().getZ()));
				
			if(rsmMesh.getRotationframes().size() == 0)
				matrix1.mult(Matrix4.makeRotation(rsmMesh.getRotationangle(), rsmMesh.getRotationaxis().getX(), rsmMesh.getRotationaxis().getY(), rsmMesh.getRotationaxis().getZ()));
			else
			{
				//TODO: animation
			}
			
			matrix1.mult(Matrix4.makeScale(rsmMesh.getScale().getX(), rsmMesh.getScale().getY(), rsmMesh.getScale().getZ()));
			
			return matrix1;
		}
		
		public Matrix4 getMatrix2()
		{
			if(matrix2 != null)
				return matrix2;

			matrix2 = new Matrix4();
			
			if(isRoot() && subMeshes.size() == 0)
				matrix2.mult(Matrix4.makeTranslation(0, 0, 0)); //-base->bbrange.x, -base->bbrange.y, -base->bbrange.z
			
			if(!isRoot() || subMeshes.size() != 0)
				matrix2.mult(Matrix4.makeTranslation(rsmMesh.getPosition().getX(), rsmMesh.getPosition().getY(), rsmMesh.getPosition().getZ()));
			
			matrix2.mult(rsmMesh.getMatrix());
				
			return matrix2;
		}
		
		
	}
	
	
}
