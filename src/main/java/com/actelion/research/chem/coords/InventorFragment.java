package com.actelion.research.chem.coords;

import com.actelion.research.chem.StereoMolecule;

import java.util.ArrayList;

public class InventorFragment {
	private static final double cCollisionLimitBondRotation = 0.8;
	private static final double cCollisionLimitAtomMovement = 0.5;

	protected int[] mAtom;
	protected int[] mBond;
	protected int[] mAtomIndex;
	protected int[] mPriority;
	protected double[] mAtomX;
	protected double[] mAtomY;

	private StereoMolecule mMol;
	private int mMode;
	private boolean	mMinMaxAvail,mConsiderMarkedAtoms;
	private double mMinX;
	private double mMinY;
	private double mMaxX;
	private double mMaxY;
	private double mCollisionPanalty;
	private int[][] mFlipList;

	protected InventorFragment(StereoMolecule mol, int atoms, int mode) {
		mMol = mol;
		mMode = mode;
		mAtom = new int[atoms];
		mPriority = new int[atoms];
		mAtomX = new double[atoms];
		mAtomY = new double[atoms];
	}

	protected InventorFragment(InventorFragment f, int mode) {
		mMol = f.mMol;
		mMode = mode;
		mAtom = new int[f.size()];
		mPriority = new int[f.size()];
		mAtomX = new double[f.size()];
		mAtomY = new double[f.size()];
		for (int i=0; i<f.size(); i++) {
			mAtom[i]	 = f.mAtom[i];
			mPriority[i] = f.mPriority[i];
			mAtomX[i]	 = f.mAtomX[i];
			mAtomY[i]	 = f.mAtomY[i];
		}
		if (f.mBond != null) {
			mBond = new int[f.mBond.length];
			for (int i=0; i<f.mBond.length; i++)
				mBond[i] = f.mBond[i];
		}
		if (f.mAtomIndex != null) {
			mAtomIndex = new int[f.mAtomIndex.length];
			for (int i=0; i<f.mAtomIndex.length; i++)
				mAtomIndex[i] = f.mAtomIndex[i];
		}
	}

	protected int size() {
		return mAtom.length;
	}

	protected double getWidth() {
		calculateMinMax();
		return mMaxX - mMinX + 1.0;	// add half a bond length on every side
	}

	protected double getHeight() {
		calculateMinMax();
		return mMaxY - mMinY + 1.0;	// add half a bond length on every side
	}

	protected boolean isMember(int atom) {
		for (int i=0; i<mAtom.length; i++)
			if (atom == mAtom[i])
				return true;

		return false;
	}

	protected int getIndex(int atom) {
		for (int i=0; i<mAtom.length; i++)
			if (atom == mAtom[i])
				return i;

		return -1;
	}

	protected void translate(double dx, double dy) {
		for (int i=0; i<mAtom.length; i++) {
			mAtomX[i] += dx;
			mAtomY[i] += dy;
		}
	}

	protected void rotate(double x, double y, double angleDif) {
		for (int i=0; i<mAtom.length; i++) {
			double distance = Math.sqrt((mAtomX[i] - x) * (mAtomX[i] - x)
					+ (mAtomY[i] - y) * (mAtomY[i] - y));
			double angle = InventorAngle.getAngle(x, y, mAtomX[i], mAtomY[i]) + angleDif;
			mAtomX[i] = x + distance * Math.sin(angle);
			mAtomY[i] = y + distance * Math.cos(angle);
		}
	}

	protected void flip(double x, double y, double mirrorAngle) {
		for (int i=0; i<mAtom.length; i++) {
			double distance = Math.sqrt((mAtomX[i] - x) * (mAtomX[i] - x)
					+ (mAtomY[i] - y) * (mAtomY[i] - y));
			double angle = 2 * mirrorAngle - InventorAngle.getAngle(x, y, mAtomX[i], mAtomY[i]);
			mAtomX[i] = x + distance * Math.sin(angle);
			mAtomY[i] = y + distance * Math.cos(angle);
		}
	}

	protected void flipOneSide(int bond) {
		// The fliplist contains for every bond atoms:
		// [0]->the bond atom that lies on the larger side of the bond
		// [1]->the bond atom on the smaller side of the bond
		// [2...n]->all other atoms on the smaller side of the bond.
		//		  These are the ones getting flipped on the mirror
		//		  line defined by the bond.
		if (mFlipList == null)
			mFlipList = new int[mMol.getAllBonds()][];

		if (mFlipList[bond] == null) {
			int[] graphAtom = new int[mAtom.length];
			boolean[] isOnSide = new boolean[mMol.getAllAtoms()];
			int atom1 = mMol.getBondAtom(0, bond);
			int atom2 = mMol.getBondAtom(1, bond);
			graphAtom[0] = atom1;
			isOnSide[atom1] = true;
			int current = 0;
			int highest = 0;
			while (current <= highest) {
				for (int i=0; i<mMol.getAllConnAtoms(graphAtom[current]); i++) {
					int candidate = mMol.getConnAtom(graphAtom[current], i);

					if (!isOnSide[candidate] && candidate != atom2) {
						graphAtom[++highest] = candidate;
						isOnSide[candidate] = true;
					}
				}
				if (current == highest)
					break;
				current++;
			}

			// default is to flip the smaller side
			boolean flipOtherSide = (highest+1 > mAtom.length/2);

			// if we retain core atoms and the smaller side contains core atoms, then flip the larger side
			if ((mMode & CoordinateInventor.MODE_CONSIDER_MARKED_ATOMS) != 0) {
				boolean coreOnSide = false;
				boolean coreOffSide = false;
				for (int i=0; i<mAtom.length; i++) {
					if (mMol.isMarkedAtom(mAtom[i])) {
						if (isOnSide[mAtom[i]])
							coreOnSide = true;
						else
							coreOffSide = true;
					}
				}
				if (coreOnSide != coreOffSide)
					flipOtherSide = coreOnSide;
			}

			int count = 2;
			mFlipList[bond] = new int[flipOtherSide ? mAtom.length-highest : highest+2];
			for (int i=0; i<mAtom.length; i++) {
				if (mAtom[i] == atom1)
					mFlipList[bond][flipOtherSide ? 0 : 1] = i;
				else if (mAtom[i] == atom2)
					mFlipList[bond][flipOtherSide ? 1 : 0] = i;
				else if (flipOtherSide ^ isOnSide[mAtom[i]])
					mFlipList[bond][count++] = i;
			}
		}

		double x = mAtomX[mFlipList[bond][0]];
		double y = mAtomY[mFlipList[bond][0]];
		double mirrorAngle = InventorAngle.getAngle(x, y, mAtomX[mFlipList[bond][1]],
				mAtomY[mFlipList[bond][1]]);

		for (int i=2; i<mFlipList[bond].length; i++) {
			int index = mFlipList[bond][i];
			double distance = Math.sqrt((mAtomX[index] - x) * (mAtomX[index] - x)
					+ (mAtomY[index] - y) * (mAtomY[index] - y));
			double angle = 2 * mirrorAngle - InventorAngle.getAngle(x, y, mAtomX[index], mAtomY[index]);
			mAtomX[index] = x + distance * Math.sin(angle);
			mAtomY[index] = y + distance * Math.cos(angle);
		}
	}

	protected void arrangeWith(InventorFragment f) {
		double maxGain = 0.0;
		int maxCorner = 0;
		for (int corner=0; corner<4; corner++) {
			double gain = getCornerDistance(corner) + f.getCornerDistance((corner>=2) ? corner-2 : corner+2);
			if (maxGain < gain) {
				maxGain = gain;
				maxCorner = corner;
			}
		}

		double sumHeight = getHeight() + f.getHeight();
		double sumWidth = 0.75 * (getWidth() + f.getWidth());
		double maxHeight = Math.max(getHeight(), f.getHeight());
		double maxWidth = 0.75 * Math.max(getWidth(), f.getWidth());

		double bestCornerSize = Math.sqrt((sumHeight - maxGain) * (sumHeight - maxGain)
				+ (sumWidth - 0.75 * maxGain) * (sumWidth - 0.75 * maxGain));
		double toppedSize = Math.max(maxWidth, sumHeight);
		double besideSize = Math.max(maxHeight, sumWidth);

		if (bestCornerSize < toppedSize && bestCornerSize < besideSize) {
			switch(maxCorner) {
				case 0:
					f.translate(mMaxX - f.mMinX - maxGain + 1.0, mMinY - f.mMaxY + maxGain - 1.0);
					break;
				case 1:
					f.translate(mMaxX - f.mMinX - maxGain + 1.0, mMaxY - f.mMinY - maxGain + 1.0);
					break;
				case 2:
					f.translate(mMinX - f.mMaxX + maxGain - 1.0, mMaxY - f.mMinY - maxGain + 1.0);
					break;
				case 3:
					f.translate(mMinX - f.mMaxX + maxGain - 1.0, mMinY - f.mMaxY + maxGain - 1.0);
					break;
			}
		}
		else if (besideSize < toppedSize) {
			f.translate(mMaxX - f.mMinX + 1.0, (mMaxY + mMinY - f.mMaxY - f.mMinY) / 2);
		}
		else {
			f.translate((mMaxX + mMinX - f.mMaxX - f.mMinX) / 2, mMaxY - f.mMinY + 1.0);
		}
	}

	private void calculateMinMax() {
		if (mMinMaxAvail)
			return;

		mMinX = mAtomX[0];
		mMaxX = mAtomX[0];
		mMinY = mAtomY[0];
		mMaxY = mAtomY[0];
		for (int i=0; i<mAtom.length; i++) {
			double surplus = getAtomSurplus(i);

			if (mMinX > mAtomX[i] - surplus)
				mMinX = mAtomX[i] - surplus;
			if (mMaxX < mAtomX[i] + surplus)
				mMaxX = mAtomX[i] + surplus;
			if (mMinY > mAtomY[i] - surplus)
				mMinY = mAtomY[i] - surplus;
			if (mMaxY < mAtomY[i] + surplus)
				mMaxY = mAtomY[i] + surplus;
		}

		mMinMaxAvail = true;
	}

	private double getCornerDistance(int corner) {
		double minDistance = 9999.0;
		for (int atom=0; atom<mAtom.length; atom++) {
			double surplus = getAtomSurplus(atom);
			double d = 0.0;
			switch (corner) {
				case 0:	// top right
					d = mMaxX - 0.5 * (mMaxX + mMinY + mAtomX[atom] - mAtomY[atom]);
					break;
				case 1:	// bottom right
					d = mMaxX - 0.5 * (mMaxX - mMaxY + mAtomX[atom] + mAtomY[atom]);
					break;
				case 2:	// bottom left
					d = 0.5 * (mMinX + mMaxY + mAtomX[atom] - mAtomY[atom]) - mMinX;
					break;
				case 3:	// top left
					d = 0.5 * (mMinX - mMinY + mAtomX[atom] + mAtomY[atom]) - mMinX;
					break;
			}

			if (minDistance > d - surplus)
				minDistance = d - surplus;
		}

		return minDistance;
	}

	private double getAtomSurplus(int atom) {
		return (mMol.getAtomQueryFeatures(mAtom[atom]) != 0) ? 0.6
				: (mMol.getAtomicNo(mAtom[atom]) != 6) ? 0.25 : 0.0;
	}

	protected void generateAtomListsOfFlipBonds(byte[] flipBondType) {

	}

	protected ArrayList<int[]> getCollisionList() {
		mCollisionPanalty = 0.0;
		ArrayList<int[]> collisionList = new ArrayList<int[]>();
		for (int i=1; i<mAtom.length; i++) {
			for (int j=0; j<i; j++) {
				double xdif = Math.abs(mAtomX[i]-mAtomX[j]);
				double ydif = Math.abs(mAtomY[i]-mAtomY[j]);
				double dist = Math.sqrt(xdif * xdif + ydif * ydif);
				if (dist < cCollisionLimitBondRotation) {
					int[] collidingAtom = new int[2];
					collidingAtom[0] = mAtom[i];
					collidingAtom[1] = mAtom[j];
					collisionList.add(collidingAtom);
				}
				double panalty = 1.0 - Math.min(dist, 1.0);
				mCollisionPanalty += panalty * panalty;
			}
		}
		return collisionList;
	}

	protected double getCollisionPanalty() {
		return mCollisionPanalty;
	}

	protected void locateBonds() {
		int fragmentBonds = 0;
		for (int i=0; i<mAtom.length; i++)
			for (int j=0; j<mMol.getAllConnAtoms(mAtom[i]); j++)
				if (mMol.getConnAtom(mAtom[i], j) > mAtom[i])
					fragmentBonds++;

		mBond = new int[fragmentBonds];
		mAtomIndex = new int[mMol.getAllAtoms()];

		fragmentBonds = 0;
		for (int i=0; i<mAtom.length; i++) {
			mAtomIndex[mAtom[i]] = i;
			for (int j=0; j<mMol.getAllConnAtoms(mAtom[i]); j++) {
				if (mMol.getConnAtom(mAtom[i], j) > mAtom[i]) {
					mBond[fragmentBonds] = mMol.getConnBond(mAtom[i], j);
					fragmentBonds++;
				}
			}
		}
	}

	protected void optimizeAtomCoordinates(int atomIndex) {
		double x = mAtomX[atomIndex];
		double y = mAtomY[atomIndex];

		InventorAngle[] collisionForce = new InventorAngle[4];

		int forces = 0;
		for (int i=0; i<mBond.length; i++) {
			if (forces >= 4)
				break;

			if (atomIndex == mAtomIndex[mMol.getBondAtom(0, mBond[i])]
					|| atomIndex == mAtomIndex[mMol.getBondAtom(1, mBond[i])])
				continue;

			double x1 = mAtomX[mAtomIndex[mMol.getBondAtom(0, mBond[i])]];
			double y1 = mAtomY[mAtomIndex[mMol.getBondAtom(0, mBond[i])]];
			double x2 = mAtomX[mAtomIndex[mMol.getBondAtom(1, mBond[i])]];
			double y2 = mAtomY[mAtomIndex[mMol.getBondAtom(1, mBond[i])]];
			double d1 = Math.sqrt((x1-x)*(x1-x)+(y1-y)*(y1-y));
			double d2 = Math.sqrt((x2-x)*(x2-x)+(y2-y)*(y2-y));
			double bondLength = Math.sqrt((x2-x1)*(x2-x1)+(y2-y1)*(y2-y1));

			if (d1<bondLength && d2<bondLength) {
				if (x1 == x2) {
					double d = Math.abs(x-x1);
					if (d<cCollisionLimitAtomMovement)
						collisionForce[forces++] = new InventorAngle(InventorAngle.getAngle(x1,y,x,y),
								(cCollisionLimitAtomMovement-d)/2);
				}
				else if (y1 == y2) {
					double d = Math.abs(y-y1);
					if (d<cCollisionLimitAtomMovement)
						collisionForce[forces++] = new InventorAngle(InventorAngle.getAngle(x,y1,x,y),
								(cCollisionLimitAtomMovement-d)/2);
				}
				else {
					double m1 = (y2-y1)/(x2-x1);
					double m2 = -1/m1;
					double a1 = y1-m1*x1;
					double a2 = y-m2*x;
					double xs = (a2-a1)/(m1-m2);
					double ys = m1*xs+a1;
					double d = Math.sqrt((xs-x)*(xs-x)+(ys-y)*(ys-y));
					if (d<cCollisionLimitAtomMovement)
						collisionForce[forces++] = new InventorAngle(InventorAngle.getAngle(xs,ys,x,y),
								(cCollisionLimitAtomMovement-d)/2);
				}
				continue;
			}

			if (d1<cCollisionLimitAtomMovement) {
				collisionForce[forces++] = new InventorAngle(InventorAngle.getAngle(x1,y1,x,y),
						(cCollisionLimitAtomMovement-d1)/2);
				continue;
			}

			if (d2<cCollisionLimitAtomMovement) {
				collisionForce[forces++] = new InventorAngle(InventorAngle.getAngle(x2,y2,x,y),
						(cCollisionLimitAtomMovement-d2)/2);
				continue;
			}
		}

		if (forces > 0) {
			InventorAngle force = CoordinateInventor.getMeanAngle(collisionForce, forces);
			mAtomX[atomIndex] += force.mLength * Math.sin(force.mAngle);
			mAtomY[atomIndex] += force.mLength * Math.cos(force.mAngle);
		}
	}
}
