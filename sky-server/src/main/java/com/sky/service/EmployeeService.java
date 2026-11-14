package com.sky.service;

import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.dto.PasswordEditDTO;
import com.sky.entity.Employee;
import com.sky.result.PageResult;

import java.util.List;

public interface EmployeeService {

    /**
     * 员工登录
     * @param employeeLoginDTO
     * @return
     */
    Employee login(EmployeeLoginDTO employeeLoginDTO);

    PageResult getPageEmpl(EmployeePageQueryDTO employeePageQueryDTO);

    void addEmpl(EmployeeDTO employeeDTO);

    void changeStatus(Integer status,Long id);

    void updateEmployee(EmployeeDTO employeeDTO);

    Employee getById(Long id);

    void editPassword(PasswordEditDTO passwordEditDTO);
}