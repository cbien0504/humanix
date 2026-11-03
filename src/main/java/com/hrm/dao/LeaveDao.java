package com.hrm.dao;

import com.hrm.model.LeaveBalance;
import com.hrm.model.LeaveFilter;
import com.hrm.model.LeaveRequest;
import com.hrm.model.LeaveRequestForm;
import com.hrm.model.LeaveStatus;
import com.hrm.model.LeaveType;
import com.hrm.model.Page;
import com.hrm.model.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaveDao {

    List<LeaveType> leaveTypes();

    Optional<LeaveType> leaveType(long leaveTypeId);

    Optional<LeaveRequest> findById(long leaveRequestId);

    List<LeaveRequest> findOverlapping(long employeeId, LocalDate start, LocalDate end);

    long insert(long employeeId, LeaveRequestForm form, BigDecimal totalDays);

    int changeStatus(long leaveRequestId, LeaveStatus expected, LeaveStatus target, Long approverId, String comment);

    Page<LeaveRequest> search(LeaveFilter filter, PageRequest page);

    List<LeaveBalance> balances(long employeeId, int year);

    boolean hasApprovedLeaveOn(long employeeId, LocalDate date);

    int initBalances(int year);

    long countPending(Long teamOfManagerId);
}
