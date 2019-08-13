package org.pale.jcfutils.region;

/**
 * Integer bounding box for regions
 * @author white
 *
 */
public class AABB {
	public int x1,y1,z1;
	public int x2,y2,z2;
	
	public AABB(int xa,int ya, int za, int xb,int yb, int zb) {
		if(xa<xb) {
			x1=xa; x2=xb;
		} else {
			x1=xb; x2=xa;
		}
		if(ya<yb) {
			y1=ya; y2=yb;
		} else {
			y1=yb; y2=ya;
		}
		if(za<zb) {
			z1=za; z2=zb;
		} else {
			z1=zb; z2=za;
		}
	}
	
	@Override
	public String toString() {
		return String.format("(%d,%d,%d)-(%d,%d,%d)",x1,y1,z1,x2,y2,z2);
	}
}
