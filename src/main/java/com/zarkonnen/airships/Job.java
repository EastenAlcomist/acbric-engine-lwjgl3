package com.zarkonnen.airships;


public interface Job {
	public Module module();
	public Resource resource();
	public double priority();
	public boolean active();
	public boolean isCaptain();
	public boolean requiredType(CrewType ct);
	public boolean requiredUnoccupied();
}
