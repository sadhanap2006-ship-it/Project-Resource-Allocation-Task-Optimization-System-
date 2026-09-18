package com.sist.rms.entity;

/** Status of a planned leave. CANCELLED leave is ignored by conflict detection. */
public enum LeaveStatus { PLANNED, APPROVED, CANCELLED }
