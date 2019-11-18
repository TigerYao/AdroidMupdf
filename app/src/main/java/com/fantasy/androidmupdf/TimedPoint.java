package com.fantasy.androidmupdf;

public class TimedPoint {
	public final float x;
	public final float y;
	public final long timestamp;
	public  int action;

	public TimedPoint(float x, float y) {
		this.x = x;
		this.y = y;
		this.timestamp = System.currentTimeMillis();
	}

	public TimedPoint(float x, float y, int action) {
		this.x = x;
		this.y = y;
		this.timestamp = System.currentTimeMillis();
		this.action = action;
	}

	public float velocityFrom(TimedPoint start) {
		float velocity = distanceTo(start) / (this.timestamp - start.timestamp);
		if (velocity != velocity)
			return 0f;
		return velocity;
	}

	public float distanceTo(TimedPoint point) {
		return (float) Math.sqrt(Math.pow(point.x - this.x, 2) + Math.pow(point.y - this.y, 2));
	}
}
