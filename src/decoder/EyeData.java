package decoder;

/**
 * 
 * FOX 1 Telemetry Decoder
 * @author chris.e.thompson g0kla/ac2cz
 *
 * Copyright (C) 2015 amsat.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 *
 * Bit Measurements that can be dsiplayed on an eye diagram
 * 
 * We have a copy of the audio buckets,which we store so that the eye diagram can be drawn without the data changing mid draw. The
 * averages are calculated to display stats on the eye diagram.
 * 
 *
 */
public class EyeData extends DataMeasure {
	
	protected boolean eyeDataFresh = false;
    int[][] eyeData = null;
	
    int SAMPLE_WINDOW_LENGTH = 0;
    int bucketSize = 0;
    public double bitSNR;
    
    public int lastErasureCount; // number of erasures in the last frame
    public int lastErrorsCount; // number of errors in the last frame
    
    public static final int HIGH = 0;
    public static final int LOW = 1;
    public static final int BIT = 2;
    
    public int clockOffset = 0; // store the offset so we can print it for debug
    public int offsetType = 0;
    
    public EyeData(int l, int b) {
    	MEASURES = 3;
    	AVERAGE_PERIOD = 400; // 350ms to measure a window of 70 bits. 1000 = 1 sec average time
    	init();
    	
    	SAMPLE_WINDOW_LENGTH = l;
    	bucketSize = b;
		eyeData = new int[SAMPLE_WINDOW_LENGTH][];
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			eyeData[i] = new int[bucketSize];
		}

    }
    
	@Override
	public void run() {
		Thread.currentThread().setName("EyeData");
		// No thread, sychronous with the decoder windows
	}
    public void calcAverages() {
    	if (readyToAverage()) {
    		runAverage();
			double noise = sd[LOW] + sd[HIGH];
			double signal = avg[HIGH] - avg[LOW];
			if (signal != 0 && noise != 0) {
				bitSNR = (signal/noise);
			}
			reset();	
		}
    }
    
    public boolean isFresh() { return eyeDataFresh; }
    public void setFreshData(boolean b) { 
    	eyeDataFresh = b;
    }
    public void setData(int window, int j, int value) { 
    	eyeData[window][j] = value;
    }
    public int[][] getData() { 
    	if (clockOffset != 0) {
    		int[][] buffer = offsetEyeData(clockOffset);
    		return buffer; 
    	} else
    		return eyeData;
    }
    
    public void setHigh(int value) {
    	setValue(HIGH, value);
    	setValue(BIT, value);
    }

    public void setLow(int value) {
    	setValue(LOW,value);
    	setValue(BIT, value);
    }

    public void setOffsetLow(int i, int width, int offset) {
    	int value = 0;
    	for (int w=-width; w<width; w++)
    		value += getOffsetValue(i, bucketSize/2+w, offset);
        setValue(LOW,value/(width*2));
        setValue(BIT, value/(width*2));
    }
    public void setOffsetHigh(int i, int width, int offset) {
    	int value = 0;
    	for (int w=-width; w<width; w++)
    		value += getOffsetValue(i, bucketSize/2+w, offset);
    	setValue(HIGH,value/(width*2));
    	setValue(BIT, value/(width*2));
    }
    
    /**
     * We have a value in bucket i at bucket position j and we want to move it by offset
     * There are 5 cases to consider:
     * 1/ WRAP BACK offset is -ve and bucket position j is less then offset and we are in the first bucket
     * 
     * 2/ PREV offset is -ve and bucket position j is less then offset and we are in any bucket except the first
     * 
     * 3/ NEXT offset is +ve and j + offset takes us outside the bucket and we are not in the last bucket
     * 
     * 4/ CURRENT offset is 0 or +ve and j + offset >= 0 and j + offset keeps us in the current bucket
     * 
     * 5/ WRAP FWD
     * 
     * @param i
     * @param j
     * @param offset
     * @return
     */
    private int getOffsetValue(int i, int j, int offset) {
    	int value = 0;
    	// Case 1 - WRAP -VE
    	if (offset < 0) {
    		if (j < Math.abs(offset) && i == 0) {// copy from previous data set
    			value = eyeData[SAMPLE_WINDOW_LENGTH-1][j+bucketSize+offset];
    			offsetType = 1;
    			// PREV
    		} else if (j < Math.abs(offset) && i > 0) {// copy from previous
    			value = eyeData[i-1][j+bucketSize+offset];
    			offsetType = 2;
    			// CURRENT -VE
    		} else if (j >= Math.abs(offset)) { // copy from the current
    			value = eyeData[i][j+offset];
    			offsetType = 5;
    		}
    	}
    	if (offset > 0) { // note that all our offsets are -ve, but this is for future decoders maybe..
    		// NEXT
    	} else if (offset > 0 && j + offset >= bucketSize && i < SAMPLE_WINDOW_LENGTH-1) { // copy from next
    		value = eyeData[i+1][bucketSize-1-j+offset];
    		offsetType = 3;
    		// CURRENT +VE
    	} else if (offset > 0 && j+offset < bucketSize) { // copy from the current
    		value = eyeData[i][j+offset];
    		offsetType = 4;
    		// WRAP +VE
    	} else if (offset > 0 && j + offset >= bucketSize && i == SAMPLE_WINDOW_LENGTH-1) {// copy from next data set
    		offsetType = 6;
    		// There is no data to copy.  Out offset is looking into the future, so wrap
    		value = eyeData[0][bucketSize-1-j+offset];
    		//buffer[a][b++] = 0;
    		//System.err.println("EYE ERROR:i" + i + " j:" + j + " off:" + offset);
    	}
    	return value;
    }

    /*
	 * Tricky to make the eye diagram work for the PSK decoder because we do not move the clock.  Instead we have an offset, so 
	 * we need to work out where each bucket starts.  We get the offset from the decoder and use that the shuffle the samples
	 * backwards or forwards.  Then we can draw the eye diagram in the normal way.
	 */
    private int[][] offsetEyeData(int offset) {
    	clockOffset = offset;
    	
    	int[][] buffer = new int[SAMPLE_WINDOW_LENGTH][];
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			buffer[i] = new int[bucketSize];
		}
    	int a=0; 
		int b=0;
		for (int i=0; i < SAMPLE_WINDOW_LENGTH; i++) {
			for (int j=0; j < bucketSize; j+=1) {
				if (eyeData !=null && a < SAMPLE_WINDOW_LENGTH && b < bucketSize) {
					buffer[a][b++] = getOffsetValue(i,j,offset);
				}
			}
			b=0;
			a++;
		}
    	return buffer;
    }
}
